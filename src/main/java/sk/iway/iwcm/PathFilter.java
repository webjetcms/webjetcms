package sk.iway.iwcm;

import org.apache.commons.codec.binary.Base64;

import sk.iway.iwcm.analytics.AnalyticsHelper;
import sk.iway.iwcm.common.*;
import sk.iway.iwcm.components.abtesting.ABTesting;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.components.response_header.rest.ResponseHeaderService;
import sk.iway.iwcm.dmail.Sender;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.doc.ninja.Ninja;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.*;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.WJResponseWrapper;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Filter premapovava volania na virtualne adresare a stranky do volani na
 * spravne docid
 *
 * @Title WebJET
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2002
 * @author Craig McClanahan
 * @version $Revision: 1.4 $ $Date: 2004/03/01 17:03:45 $
 * @created Nede?e, 2002, okt?ber 27
 * @modified $Date: 2004/03/01 17:03:45 $
 */
@SuppressWarnings("java:S1075")
public class PathFilter implements Filter
{
	private static final String CHECK_ADMIN_SESSION_KEY = "pathFilter.checkAdmin";
	private static final String CHECK_WEB_ACCESS_SESSION_KEY = "pathFilter.checkWebAccess";
	private static final String CHECK_DOMAIN_SESSION_KEY = "pathFilter.checkDomain";
	private static String customPath = null;
	private static Map<String, EditForm> passwordProtected;
	public static final String REQUEST_START_TIME = "pathFilet.requestStartTime";

	private static String[] bypassPath = null;
	//pocet sekund cache pre staticky obsah
	private static int cacheStaticContentSeconds = -1;
	private static String[] cacheStaticContentSuffixes = null;
	//blocked paths for file/directory metadata disclosure prevention
	private static String[] blockedPaths = null;

	private static List<ResponseHeaderBean> responseHeaders;

	private static Map<String, DynamicForward> dynamicForwards;

	/**
	 * Inicializacia servletu
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		//tu nemozeme pouzivat logger pretoze toto sa vola skor ako zbehne InitServlet
		Logger.info(PathFilter.class, "PathFilter init");
		try
		{
			//inicializuj password protected
			//robime az v doFilter reloadProtectedDirs();

			PathFilter.setCustomPath(filterConfig.getInitParameter("customPath"));
			//aby bolo mozne nastavit aj cez -Dwebjet.customPath=...
			String systemParam = System.getProperty("webjet.customPath");
			if (Tools.isNotEmpty(systemParam)) PathFilter.setCustomPath(systemParam);
			prepareTemplates();
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	public static void registerDynamicForward(String name, DynamicForward dynamicForward)
	{
		if (dynamicForwards==null) dynamicForwards = Collections.synchronizedMap(new HashMap<String, DynamicForward>());
		dynamicForwards.put(name, dynamicForward);
	}

	public static void unregisterDynamicForward(String name)
	{
		if (dynamicForwards==null) return;
		if (dynamicForwards.containsKey(name)) dynamicForwards.remove(name);
	}

	public static void prepareTemplates()
	{
		if (Tools.isNotEmpty(customPath))
		{
			//tu nemozeme pouzivat logger pretoze toto sa vola skor ako zbehne InitServlet
			Logger.info(PathFilter.class, "PathFilterInit - customPath: " + customPath + File.separatorChar + Constants.getInstallName());
			//skopiruj templates do custom adresara
			File templatesDir = new File(customPath + File.separatorChar + Constants.getInstallName() + File.separatorChar
					+ "/templates/");
			if (templatesDir.exists())
			{
				File f;
				f = new File(Tools.getRealPath("/templates/" + Constants.getInstallName() + "/"));
				if (f.exists() == false)
				{
					//vytvor adresar
					if(f.mkdirs() == false) return;
				}
				File[] listFiles = templatesDir.listFiles();
				int size = listFiles.length;
				int i;
				File outFile;
				for (i = 0; i < size; i++)
				{
					f = listFiles[i];
					//Logger.println(this,"copy: " + f.getName());
					outFile = new File(Tools.getRealPath(
							"/templates/" + Constants.getInstallName() + "/" + f.getName()));
					if (outFile.exists() == false || outFile.lastModified() < f.lastModified())
					{
						//skopiruj subory
						FileTools.copyFile(f, outFile);
					}
				}
			}
		}
	}

	public static String getCustomPath()
	{
		return(customPath);
	}

	/**
	 * Vrati REAL PATH pre zadane URL aj s detekciou Custom Path (pouzitelne len pre staticke subory)
	 * @param url
	 * @return
	 */
	public static String getCustomPathRealPath(String url)
	{
		String realPath = PathFilter.getRealPath(url);
		if (PathFilter.getCustomPath() != null)
		{
			//skontroluj, ci pozadovany subor nie je custom
			IwcmFile fCusom = new IwcmFile(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + url.replace('/', File.separatorChar));
			if (fCusom.exists() && fCusom.canRead()) realPath = fCusom.getAbsolutePath();
		}
		return realPath;
	}

	/**
	 * Take this filter out of service.
	 */
	@Override
	public void destroy()
	{
		//
	}

	/**
	 * Select and set (if specified) the character encoding to be used to
	 * interpret request parameters for this request.
	 *
	 * @param chain
	 *           The filter chain we are processing
	 * @exception IOException
	 *               if an input/output error occurs
	 * @exception ServletException
	 *               if a servlet error occurs
	 */
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
	{
		String path = null;
		DebugTimer timer = null;

		HttpServletRequest req = (HttpServletRequest) servletRequest;
		HttpServletResponse res = (HttpServletResponse) servletResponse;

		try
		{
			if (passwordProtected==null) reloadProtectedDirs();

			//System.out.println("PathFilter.doFilter: "+servletRequest.getClass());
			//System.out.println("PathFilter.doFilter: "+(servletRequest instanceof HttpServletRequest));

			//req.setCharacterEncoding("windows-1250");
			//req.setAttribute("SetCharacterEncodingFilter.encoding", "windows-1250");

			path = req.getRequestURI();
			if (Tools.isNotEmpty(req.getContextPath()))
			{
				path = path.substring(req.getContextPath().length());
			}
			if (path.contains("//"))
			{
				path = Tools.replace(path, "/////", "/");
				path = Tools.replace(path, "////", "/");
				path = Tools.replace(path, "///", "/");
				path = Tools.replace(path, "//", "/");
				res.setStatus(302);

				StringBuilder redir = new StringBuilder();
				redir.append(Tools.sanitizeHttpHeaderParam(path));
				String qs = req.getQueryString();
				if (Tools.isNotEmpty(qs)) redir.append('?').append(qs);

				res.setHeader("Location", redir.toString());
				//res.sendRedirect(path);
				return;
			}

			//Logger.println(this,"query string PATH FILTER=" + req.getQueryString());
			String qs = req.getQueryString();
			boolean isFirstPathFilterCall = (req.getAttribute("path_filter_orig_path")==null);
			req.setAttribute("path_filter_query_string", qs);
			req.setAttribute("path_filter_orig_path", path);

			setNginxProxyMode(req, res);
			setXFrameOptions(res);
			setAccessControlAllowOrigin(path, res);
			setXXssProtection(res);
			setFeaturePolicy(res);
			setXRobotsTagValue(path, res);
			setResponseHeaders(path, req, res);

			//blokovanie akcie /showdoc.do
			if(path.startsWith("/showdoc.do") && !"*".equals(Constants.getString("showDocActionAllowedDocids")))
			{
				int docId = Tools.getIntValue(req.getParameter("docid"), -1);
				Identity user = (Identity)req.getSession().getAttribute(Constants.USER_KEY);

				if (Tools.isEmpty(Constants.getString("showDocActionAllowedDocids"))
						|| ((user == null || !user.isAdmin()) && path.startsWith("/showdoc.do") && !isShowDocAllowDocId(docId, "showDocActionAllowedDocids")))
				{
					Adminlog.add(Adminlog.TYPE_CLIENT_SPECIFIC, "Nepovolene pouzitie /showdoc.do docId:"+docId+" showDocActionAllowedDocids : "+ Constants.getString("showDocActionAllowedDocids"), -1, -1);
					res.setStatus(HttpServletResponse.SC_NOT_FOUND);
					forwardSafely("/404.jsp", req, res);
					return;
				}
			}

			if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML && path.startsWith("/showdoc.do") && isFirstPathFilterCall)
			{
				//do this inly if there is only docid and/or language parameter
				int docId = Tools.getIntValue(req.getParameter("docid"), -1);
				String languageParam = req.getParameter("language");
				if (docId > 0 && (req.getParameterMap().size() == 1 || (languageParam != null && req.getParameterMap().size() == 2)))
				{
					//presmeruj sa na stranku v HTML formate
					DocDB docDB = DocDB.getInstance();
					if (InitServlet.isTypeCloud() || (Constants.getBoolean("enableStaticFilesExternalDir") && Constants.getBoolean("multiDomainEnabled")))
					{
						DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, true);
						if (doc!=null)
						{
							GroupDetails group = GroupDetails.getById(doc.getGroupId());
							//MBO: Ak je zapnuty Cloud, otestuje, ci je dany DOC dostupny pre tuto domenu, ak nie, nevykona redirect ale hodi 404
							if (group==null || !group.getDomainName().equals(CloudToolsForCore.getDomainName()))
							{
								Logger.debug(PathFilter.class, "DOC ID="+docId+" is not allowed for domain "+ CloudToolsForCore.getDomainName());
								res.setStatus(HttpServletResponse.SC_NOT_FOUND);
								forwardSafely("/404.jsp", req, res);
								return;
							}
						}
					}
					String redirPath = docDB.getDocLink(docId, req);
					if (languageParam != null) redirPath = Tools.addParameterToUrl(redirPath, "language", languageParam);

					if (redirPath.startsWith("/showdoc.do")==false && Tools.isNotEmpty(redirPath))
					{
						res.setStatus(301);
						//zrusene, pridanie domeny je zbytocne a potom to zle presmerovava ak sme na inom ako 80 porte if (redirPath.toLowerCase().startsWith("http")==false) redirPath = Tools.getBaseHref(req)+redirPath;
						res.setHeader("Location", redirPath);
						//res.sendRedirect(redirPath);
								/*if (timer != null)		//dead code
								{
									timer.diff("   Path filter - chain, path: " + path);
									DBPool.getInstance().logConnections();
								}*/
						return;
					}
				}
			}

			if (req.getRequestURI().indexOf("//")!=-1 || isPathSafe(path)==false)
			{
				if (DocTools.isXssStrictUrlException(path, "xssProtectionStrictGetUrlException")==false)
				{
					//je to pokus o XSS: /404.html/'onmouseover=prompt(915761)
					Adminlog.add(Adminlog.TYPE_XSS, "XSS path="+path, -1, -1);
					res.setStatus(HttpServletResponse.SC_FORBIDDEN);
					forwardSafely("/403.jsp", req, res);
					return;
				}
			}

			if (Tools.replace(path.toLowerCase(), ";jsessionid", "jsessionid").contains(";"))
			{
				//utok typu /;/admin/help/search.jsp, povolene je len ;jsessionid
				res.setStatus(HttpServletResponse.SC_NOT_FOUND);
				forwardSafely("/404.jsp", req, res);
				return;
			}

			//povolenie swagger-ui / defaultnej stranky Apache CXF (/ws)
			if (path.contains("/swagger") || path.contains("/v2/api-docs") || path.equals("/ws") || path.equals("/ws/"))
			{
				boolean swaggerEnabled = Constants.getBoolean("swaggerEnabled");
				if (swaggerEnabled)
				{
				//testni, ci je povoleny aj pre neadmina
				if (Constants.getBoolean("swaggerRequireAdmin"))
				{
					Identity user = UsersDB.getCurrentUser(req);
					if (user==null || user.isAdmin()==false) swaggerEnabled = false;
				}
				}

				if (swaggerEnabled==false)
				{
				Adminlog.add(Adminlog.TYPE_XSS, "Swagger path is not enabled (conf. swaggerEnabled=false), path="+path, -1, -1);
				res.setStatus(HttpServletResponse.SC_NOT_FOUND);
				forwardSafely("/404.jsp", req, res);
						return;
				}
			}

			//check for blocked file/directory metadata
			if (isPathBlocked(path))
			{
				res.setStatus(HttpServletResponse.SC_NOT_FOUND);
				forwardSafely("/404.jsp", req, res);
				return;
			}

			//pred bypass NESMIE byt citany ziaden parameter!!!
			if ("true".equals(servletRequest.getAttribute("PathFilter.bypass")))
			{
				//request ziadno nemodifikujeme
				try
				{
					chain.doFilter(servletRequest, servletResponse);
				}
				catch (SocketException se)
				{
					//toto neriesime
				}
				return;
			}

			//STRICT XSS FILTER (aplikuje sa na vsetky HTTP poziadavky)
			String strictXssRedirect = DocTools.getXssStrictUrlRedirect(req, path, qs);
			if (strictXssRedirect != null)
			{
				res.sendRedirect(strictXssRedirect);
				return;
			}

			req.setAttribute("path_filter_orig_docid", req.getParameter("docid"));

			if ("true".equals(req.getHeader("userInServletContext")))
			{
				Identity user2 = (Identity)Constants.getServletContext().getAttribute(Constants.USER_KEY);
				if (user2 != null)
				{
					LogonTools.setUserToSession(req.getSession(), user2);
					req.setAttribute("NO WJTOOLBAR", "true");
				}
			}

			//kontrola IP adries pre web sidlo, ci sa moze zobrazit
			if (!checkWebAccess(req, path))
			{
				Logger.debug(PathFilter.class, "checkWebAccess=false, forbidden access, path="+path+" ip="+Tools.getRemoteIP(req));
				res.setStatus(HttpServletResponse.SC_FORBIDDEN);
				forwardSafely("/403.jsp", req, res);
				return;
			}

			//kontrola na domenove presmerovania
			String serverName = Tools.getServerName(req, false);
			String redir = DomainRedirectDB.translate(serverName, path, req.getQueryString(), Tools.isSecure(req));
			if (redir != null)
			{
				res.setStatus(301);
				res.setHeader("Location", redir);
				return;
			}

			//System.out.println("PathFilter.doFilter path="+path);

			req.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());

			Identity user = (Identity)req.getSession().getAttribute(Constants.USER_KEY);

			if (path.startsWith("/private/rest/") || path.startsWith("/rest/private/") || path.startsWith("/rest/admin/") || path.startsWith("/websocket/private/") || path.startsWith("/websocket/admin/"))
			{
				if (user == null || ( user.isAdmin()==false && path.contains("/admin/") ))
				{
					Logger.debug(PathFilter.class, "Nepovoleny pristup k REST/WS");
					res.setStatus(HttpServletResponse.SC_FORBIDDEN);
               return;
				}
			}

            ///// SPRING INTEGRACIA
            // ak sa nezacina na /spring (aby sa to necyklilo) a ak je to url ktoru pozna spring, forwardne to na spring servlet
            if (dynamicForwards!=null && !dynamicForwards.isEmpty())
            {
                boolean adminAccessAllowed = checkAccessToAdmin(path, req, res);
                boolean privateAccessAllowed = true;
                if (path.startsWith("/private") && UsersDB.getCurrentUser(req.getSession())==null) privateAccessAllowed = false;

                for (DynamicForward df : dynamicForwards.values())
                {
                    if (df.isValid(path))
                    {
                        if (adminAccessAllowed && privateAccessAllowed)
                        {
									boolean returnAfterForward = df.forward(path, req, res);
									if (returnAfterForward) return;
                        }
                        else
                        {
                        	//ak nie je user prihlaseny a zacina to na /components a neobsahuje rest, tak posli na 403.jsp kde ho moze redirectnut do admin casti (na logon)
							if (path.startsWith("/components") && path.contains("rest")==false)
							{
								res.setStatus(HttpServletResponse.SC_FORBIDDEN);
								forwardSafely("/403.jsp", req, res);
								return;
							}
                           res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                           return;
                        }
                    }
                }
            }

            //toto musi byt az za Springom, aby pre Spring isli aj ostatne HTTP metody
            if (Constants.getBoolean("enableUnsafeHttpMethods") == false && path.contains("/rest")==false)
            {
                String method = req.getMethod().toUpperCase();
                //IE zial robi dotaz na SVG aj ako HEAD, takze to musime mat ako vynimku
                if (path.endsWith(".svg") && "HEAD".equals(method)) method = "allowedSVG";

                //vynimka pre uptimerobot.com, pouzivaju to v statnej sprave
                if ("HEAD".equals(method) || "TRACE".equals(method) || "OPTIONS".equals(method))
                {
                    String userAgent = req.getHeader("User-Agent");
                    if (Tools.isNotEmpty(userAgent) && userAgent.contains("uptimerobot.com"))
                    {
                        method = "allowedUptimerobot";
                    }
                }

                if ("HEAD".equals(method) || "TRACE".equals(method) || "OPTIONS".equals(method))
                {
                    //je to pokus o XSS: /404.html/'onmouseover=prompt(915761)
                    Adminlog.add(Adminlog.TYPE_XSS, "HTTP method not allowed: "+method, -1, -1);
                    res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    forwardSafely("/403.jsp", req, res);
                    return;
                }
            }

			String logLevel = req.getParameter("_logLevel");
			if (logLevel != null) {
				Logger.setWJLogLevel(logLevel);
			}

			if (Logger.isLevel(Logger.DEBUG) && path.indexOf("refresher")==-1 && (path.endsWith(".do") || path.endsWith(".jsp") || path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".htc")))
			{
				if (req.getParameter("showPathFilterTime")!=null || req.getSession().getAttribute("pathFilter.showPathFilterTime")!=null)
				{
					req.getSession().setAttribute("pathFilter.showPathFilterTime", "true");
					timer = new DebugTimer("PathFilter");
				}
				Logger.debug(PathFilter.class, path);
			}

			//trackovanie videni emailu
			String trackGif = Constants.getString("dmailTrackopenGif");
			if(Tools.isNotEmpty(trackGif))
			{
				if(path.contains(trackGif))
				{
					int emailId = Tools.getIntValue(req.getParameter("emailId"), -1);
					if (emailId > 0)
					{
						EmailToolsForCore.addStatOpen(emailId);
					}
				}
			}

			// url tracking with GA or something else
			AnalyticsHelper.track(path, req);

			//trackovanie kliknuti na linku v emaile
			String dmailStatParamValue = req.getParameter(Constants.getString("dmailStatParam"));
			if (Tools.isNotEmpty(dmailStatParamValue))
			{
				int emailId = Sender.getEmailIdFromClickHash(dmailStatParamValue);
				if (emailId > 0)
				{
					String extURL = req.getParameter("extURL");
					if (Tools.isNotEmpty(extURL))
					{
						if (extURL.toLowerCase().trim().startsWith("http")==false)
						{
							try
							{
								String extURL2 = Tools.replace(extURL, "|", "=");
								Base64 b64 = new Base64();
								extURL = new String(b64.decode(extURL2.getBytes()));
							}
							catch (Exception e)
							{
								sk.iway.iwcm.Logger.error(e);
							}
						}
						EmailToolsForCore.addStatClick(emailId, extURL, req.getQueryString(), req, res);
						res.sendRedirect(extURL.replaceAll("[\r\n]", ""));
						return;
					}
					else
					{
						EmailToolsForCore.addStatClick(emailId, path, req.getQueryString(), req, res);
					}
				}
			}

			//kontrola jsessionid v URL, vynimka pre .jsessionid. je pre grpd cookie modul kde sa edituje takyto kluc
			if (path.toLowerCase().contains("jsessionid") || (req.getQueryString()!=null && req.getQueryString().toLowerCase().contains("jsessionid") && req.getQueryString().toLowerCase().contains(".jsessionid.")==false) || req.isRequestedSessionIdFromURL())
			{
				//jsessionid is added to URL by PD4ML, so allow /thumb and /images paths
				if (path.startsWith("/images/")==false && path.startsWith("/thumb/images/")==false)
				{
					String description = "SESSION using jsessionid INVALIDATING, sessionId="+req.getSession().getId()+" req IP="+Tools.getRemoteIP(req);
					Logger.error(PathFilter.class, description);

					//toto stale hlasi acunetix ovs ako neriesene, toto je pokus o riesenie
					req.getSession().invalidate();
					req.getSession(true);
				}

				String pathFixed = path;
				int i = pathFixed.toLowerCase().indexOf(";jsessionid");
				if (i > 0) pathFixed = pathFixed.substring(0, i);

				i = pathFixed.toLowerCase().indexOf("jsessionid");
				if (i > 0) pathFixed = pathFixed.substring(0, i);

				res.sendRedirect(pathFixed);
				return;
			}

			SessionHolder holder = SessionHolder.getInstance();
			if (
					"get".equalsIgnoreCase(req.getMethod())==false ||
				 	(
						//for admin apply to all methods
						path.contains("/admin/") && !path.contains("/scripts/") &&
						(path.contains("/rest/") || path.endsWith(".do") || path.endsWith(".jsp") || path.endsWith(".html") || path.endsWith("/") || path.endsWith(".action"))
					) ||
					(
						path.contains("/rest/")
					)
				)
			{
				//	setni data pre SessionListener - admin cast (ostatne stranky su az dole)
				if (holder.set(req.getSession().getId(), path, req)==false)
				{
					//nastalo session stealing, spravime redirect aby nenastavali exception v JSP

					StringBuilder redirPath = new StringBuilder(path);
					if (Tools.isNotEmpty(req.getQueryString())) redirPath.append('?').append(req.getQueryString());

					res.sendRedirect(Tools.sanitizeHttpHeaderParam(redirPath.toString()));
					return;
				}
			}

			if (path.startsWith("/admin"))
			{
				boolean ret = checkAdmin(req);
				if (!ret)
				{
					Logger.debug(PathFilter.class, "checkAdmin="+ret+" forwarding to 404.jsp");
					//not found posielame aby sa admin cast tvarila akoze vobec neexistuje
					res.setStatus(HttpServletResponse.SC_NOT_FOUND);
					forwardSafely("/404.jsp", req, res);
					return;
				}
				else
				{
					// cesta /admin/statchart je tam kvoli generovaniu PDF aby ho bolo mozne unsecure embednut
					if (Constants.getBoolean("adminRequireSSL") && Tools.isSecure(req) == false && path.indexOf("/admin/statchart")==-1)
					{
						String httpsRedirectUrl = PathFilter.getHttpsRedirectUrl(req);
						Logger.debug(PathFilter.class, "Redirect (adminRequireSSL): " + httpsRedirectUrl);
						res.sendRedirect(httpsRedirectUrl);
						return;
					}
				}

				if ((path.endsWith(".do") || path.endsWith(".struts")) && "post".equalsIgnoreCase(req.getMethod()) && (req.getContentType()==null || req.getContentType().contains("multipart")==false))
				{
				    if (DocTools.isXssStrictUrlException(path, "xssProtectionStrictPostUrlException")==false)
					{
						//validuj token
						boolean isTokenValid = CSRF.verifyTokenAndDeleteIt(req);
						if (isTokenValid == false)
						{
						    Logger.info(PathFilter.class, "Struts token not valid, path="+path);
							req.setAttribute("errorText", Prop.getInstance(req).getText("components.csrfError"));
							forwardSafely("/components/maybeError.jsp", req, res);
							return;
						}
					}
				}
			}

			if (!checkCSRFToken(path, req)) {
				Logger.debug(PathFilter.class, "CSRF token missing - header param X-CSRF-Token");
				res.setStatus(HttpServletResponse.SC_FORBIDDEN);
				forwardSafely("/403.jsp", req, res);
				return;
			}

			//skus prihlasit usera (ak treba)
			if ("true".equals(req.getParameter("doFilterLogon")) || "redir".equals(req.getParameter("doFilterLogon")) || Tools.isNotEmpty(req.getHeader("wjlogontoken")))
			{
				if (user == null)
				{
					String username = req.getParameter("username");
					String password = req.getParameter("password");

					String token = req.getParameter("token");
					if (token == null) token = req.getHeader("wjlogontoken");
					if (Tools.isEmpty(username) && Tools.isEmpty(password) && Tools.isNotEmpty(token))
					{
						try
						{
							//toto je len finta, token zacina na NAHODNY ZNAK aby to nebolo na prvy pohlad base64 automaticky dekodovatelne
							token = Tools.replace(token, "|", "=").substring(1);
							Base64 b64 = new Base64();
							token = new String(b64.decode(token.getBytes()));
							int i = token.indexOf(":");
							if (i>0)
							{
								username = token.substring(0, i);
								password = token.substring(i+1);
							}
						}
						catch (Exception ex) {}
					}

					if (Tools.isNotEmpty(username) && Tools.isNotEmpty(password))
					{
						user = new Identity();
						Map<String, String> errors = new Hashtable<>();
						sk.iway.iwcm.i18n.Prop prop = sk.iway.iwcm.i18n.Prop.getInstance(Constants.getServletContext(), req);
						String forward = LogonTools.logon(username, password, user, errors, req, prop);

						if (req.getAttribute("logon.err.noadmin")!=null)
						{
								//nie je to admin, prihlasme ho ako normalneho usera, ak to zbehne, bude user v session
								LogonTools.logonUser(req, username, password);
						}
						else if (forward.compareTo("logon_ok_admin") != 0 || user == null || user.isAdmin() == false)
						{
							user = null;
						}
						else
						{
							user.setLoginName(username);
							user.setPassword(UserTools.PASS_UNCHANGED);
							user.setValid(true);
							//je korektne prihlaseny
							LogonTools.setUserToSession(req.getSession(), user);
							req.setAttribute("NO WJTOOLBAR", "true");
						}

						if ("redir".equals(req.getParameter("doFilterLogon")))
						{
							res.sendRedirect(path);
							return;
						}
					}
				}
			}

			/*
			  Kontrola pristupu do admin casti (#10398)
			 */
			if (!checkAccessToAdmin(path, req, res))
			{
				if (path.equals("/admin/refresher.jsp") == false && path.equals("/admin/rest/refresher") == false && path.equals("/admin/rest/monitoring/actual") == false)
				{
					Adminlog.add(Adminlog.TYPE_XSS, "Pouzivatel nema pristup do admin casti, presmerovavam na prihlasenie", -1, -1);
				}

				//ak je to nieco ako /admin.zip /admin~ /admin_backup tak nepresmeruj ale daj chybu
				if (path.startsWith("/admin") && path.indexOf("/", 4)==-1 && path.length()>6) {
					Logger.debug(PathFilter.class, "Is file like, forwarding to 404.jsp");
					//not found posielame aby sa admin cast tvarila akoze vobec neexistuje
					res.setStatus(HttpServletResponse.SC_NOT_FOUND);
					forwardSafely("/404.jsp", req, res);
					return;
				}

				//na public node rovno vyhlasme 404, tiez pre nepovolene IP adresy
				if ("public".equals(Constants.getString("clusterMyNodeType")) || checkAdmin(req)==false)
				{
					Logger.debug(PathFilter.class, "Public node, forwarding to 404.jsp");
					//not found posielame aby sa admin cast tvarila akoze vobec neexistuje
					res.setStatus(HttpServletResponse.SC_NOT_FOUND);
					forwardSafely("/404.jsp", req, res);
					return;
				}

				Logger.println(PathFilter.class, "Pouzivatel nema pristup do admin casti, presmerovavam na prihlasenie, path="+path + " allowedUrls="+ Constants.getString("allowAdminUrls"));

            	LogonTools.saveAfterLogonRedirect(req);

				String domainController = AuthenticationFilter.getDomainController();
				if (Tools.isNotEmpty(domainController))
				{
					res.sendRedirect("/ntlm/logon.do?admin=true");
				}
				else if (path.startsWith("/admin/m/"))
				{
					res.sendRedirect("/admin/m/logon.jsp");
				}
				else
				{
					res.sendRedirect(ContextFilter.addContextPath(req.getContextPath(), "/admin/logon/"));
				}
				return;
			}

			if (path.toLowerCase().endsWith(".jsp") || path.endsWith("/"))
			{
				if (path.startsWith("/images") || path.startsWith("/files") || path.startsWith("/shared"))
				{
					Logger.debug(PathFilter.class, "Volane JSP z nepovoleneho adresara, path="+path);
					//not found posielame aby sa admin cast tvarila akoze vobec neexistuje
					res.setStatus(HttpServletResponse.SC_NOT_FOUND);
					forwardSafely("/404.jsp", req, res);
					return;
				}
			}

			if (path.endsWith(".appcache"))
			{
				//handlovanie zapnutia online modu pre appcache
				String appcacheMode = Tools.getCookieValue(req.getCookies(), "appcacheMode", null);
				if ("online".equals(appcacheMode))
				{
					Cookie c = new Cookie("appcacheMode", "");
					c.setPath("/");
					c.setMaxAge(-1);
					c.setHttpOnly(false);
					Tools.addCookie(c, res, req);

					res.setStatus(HttpServletResponse.SC_NOT_FOUND);
					return;
				}
				else if (req.getSession().getAttribute(Constants.USER_KEY)==null)
				{
					//toto nastane, ked user nie je prihlaseny, nemozeme vtedy poslat obsah suboru, lebo sa nacachuje zoznam ako neprihlaseny user
					Logger.debug(PathFilter.class, "User not logged " + path);
					res.setStatus(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}

			//skontroluj password protected
			EditForm ef = isPasswordProtected(path, req);
			if (ef != null && ef.isAccessibleFor(user)==false)
			{
				if (doFileForbiddenRedirect(ef, user, path, req, res)) return;
			}

			if (path.contains("admin/") && (path.endsWith(".js") || path.endsWith(".html")))
			{
				//otestuj, ci existuje JSP nahrada
				File f = new File(Tools.getRealPath(path+".jsp"));
				if (f.exists() && f.canRead())
				{
					f = null;

					res.setHeader("Pragma","No-Cache");
					res.setDateHeader("Expires",0);
					res.setHeader("Cache-Control","no-Cache");

					Logger.debug(PathFilter.class, "Forwarding: "+path+".jsp");

					forwardSafely(path+".jsp", req, res);
					return;
				}
			}
			if (path.endsWith(".jsp"))
			{
				res.setHeader("Pragma","No-Cache");
				res.setDateHeader("Expires",0);
				res.setHeader("Cache-Control","no-Cache");
			}

			//nastav kodovanie JS suborom
			if (path.indexOf("admin/")==-1 && path.endsWith(".js"))
			{
				IwcmFile f = new IwcmFile(Tools.getRealPath(path));

				//otestuj, ci existuje nahrada
				File fNahrada = new File(Tools.getRealPath(path+".jsp"));
				if (fNahrada.exists() && fNahrada.canRead())
				{
					//jsp subor by mal nastavit kodovanie spravne
					//f = fNahrada;
					//tu nespravime nic, musi sa to sprocesovat ako JSP
				}
				else
				{
					IwcmFile fCusom = null;
					if (customPath != null)
					{
						//skontroluj, ci pozadovany subor nie je custom
						fCusom = new IwcmFile(customPath + File.separatorChar + Constants.getInstallName() + path.replace('/', File.separatorChar));
						if (fCusom.exists() && fCusom.canRead()) f = fCusom;
					}
					String customUrl = WriteTagToolsForCore.getCustomPageNull(path, req);
					if (customUrl != null)
					{
						fCusom = new IwcmFile(Tools.getRealPath(customUrl));
						if (fCusom.exists() && fCusom.canRead()) f = fCusom;
					}

					if (f.exists() && f.canRead())
					{
						sk.iway.iwcm.Encoding.setResponseEnc(req, res, "text/javascript");
						boolean staticHeadersSet = setStaticContentHeaders(path, user, req, res);
						setXRobotsTagValue(path, res);

						if (staticHeadersSet && IwcmFsDB.useDBStorage()==false && FileCache.useFileCache())
						{
							if (writeAndCacheFile(path, res)) return;
						}
						FilePathTools.writeFileOut(f, req, res);
						if (timer != null)
						{
							timer.diff("   Path filter - chain, path: " + path);
							DBPool.getInstance().logConnections();
						}

						return;
					}
				}
			}

			//posielaj admin/v9/dist/ alebo /admin/elFinder adresar ako utf-8
			if ((path.startsWith("/admin/v9/dist") || path.startsWith("/admin/elFinder")) && (path.endsWith(".js") || path.endsWith(".css")))
			{
				IwcmFile f = new IwcmFile(Tools.getRealPath(path));
				if (f.exists() && f.canRead())
				{
					String encoding = "text/javascript";
					if (path.endsWith(".css")) encoding = "text/css";
					sk.iway.iwcm.Encoding.setResponseEnc(req, res, encoding);
					setStaticContentHeaders(path, user, req, res);
					setXRobotsTagValue(path, res);

					//Logger.debug(PathFilter.class, "Sending DIST file: " + f.getAbsolutePath());
					FilePathTools.writeFileOut(f, req, res);

					return;
				}
			}

         	setDownloadHeaders(path, req, res);
			boolean staticHeadersSet = setStaticContentHeaders(path, user, req, res);
			if (staticHeadersSet && IwcmFsDB.useDBStorage()==false && FileCache.useFileCache())
			{
				if (writeAndCacheFile(path, res)) return;
			}

			//toto standardne Tomcat nepozna, pridame hlavicky podla specky
			if (path.endsWith(".woff2") || path.endsWith(".woff"))
			{
				if (path.endsWith(".woff2")) res.setHeader("Content-Type", "font/woff2");
				else res.setHeader("Content-Type", "font/woff");
			}

			//disable direct call to abtesting variant URL eg. /investicie/abtestvariantb.html for NON admin users
			if (path.contains(Constants.getString("ABTestingName")))
			{
				if (Constants.getBoolean("ABTestingAllowVariantUrl")==false) {
					if (user == null || user.isAdmin()==false) {
						Logger.debug(PathFilter.class, "ABTesting direct call, not allowed for non admin, forwarding to 404.jsp");
						res.setStatus(HttpServletResponse.SC_NOT_FOUND);
						forwardSafely("/404.jsp", req, res);
						return;
					}
				} else {
					//prefer URL variant instead of cookie value
					req.setAttribute("ABTestingPrefferVariantUrl", "true");
				}
			}

			//accessDocId je docId web stranky, ktoru sa zobrazuje, ku ktorej sa pristupuje z verejnej casti sidla, nie admin cast
			DocDB docDB = DocDB.getInstance();
			String domain = DocDB.getDomain(req);
			int accessDocId = -1;
			//files adresar musime nechat povoleny kvoli linkam z fulltext indexu (/files/subor.xls.html) a presmerovaniu na subor.xls
			if (path.startsWith("/images")==false && path.startsWith("/css")==false && path.startsWith("/jscripts")==false && path.startsWith("/templates")==false)
			{
				if (Constants.getBoolean("ABTesting")==true)
				{
					accessDocId = ABTesting.getVirtualPathDocId(path, domain, req, res);
				}
				else
				{
					//upravene takto ak nie je vobec abtesting trieda dostupna (napr. v cloude)
					accessDocId = docDB.getVirtualPathDocId(path, domain);
				}
			}

			if (accessDocId > 0)
			{
				//kontrola spravnosti domeny
				if (!checkDomain(req))
				{
					Logger.debug(PathFilter.class, "checkDomain=false, forwarding to 404.jsp");
					res.setStatus(HttpServletResponse.SC_NOT_FOUND);
					forwardSafely("/404.jsp", req, res);
					return;
				}

				//porovnaj URL a pripadne sprav redirect
				String realUrl = docDB.getDocLink(accessDocId, req);
				if (realUrl!=null && realUrl.equals(path)==false && !realUrl.contains(Constants.getString("ABTestingName")))
				{
					//URL so znakom * sa odstranuju, musime specialne skontrolovat
					String virtualPath = docDB.getBasicDocDetails(accessDocId, true).getVirtualPath();
					if (virtualPath == null || virtualPath.indexOf("*")==-1)
					{
						Logger.debug(PathFilter.class, "nesedi real URL, redirecting to: "+realUrl);
						res.setStatus(301);
						if (Tools.isNotEmpty(req.getQueryString())) realUrl += '?'+req.getQueryString();
						if (realUrl.toLowerCase().startsWith("http")==false) realUrl = Tools.getBaseHref(req)+realUrl;
						res.setHeader("Location", Tools.sanitizeHttpHeaderParam(realUrl));
						return;
					}
				}

				//	setni data pre SessionListener - customer cast
				String lastURL = path;
				if (req.getQueryString()!=null)
				{
					path = path + "?" + req.getQueryString();
				}

				if (holder.set(req.getSession().getId(), lastURL, req)==false)
				{
					//nastalo session stealing, spravime redirect aby nenastavali exception v JSP

					StringBuilder redirPath = new StringBuilder(path);
					if (Tools.isNotEmpty(req.getQueryString())) redirPath.append('?').append(req.getQueryString());

					res.sendRedirect(Tools.sanitizeHttpHeaderParam(redirPath.toString()));
					return;
				}

				//akoze skuska ochrany pred chybou Cannot create a session after the response has been committed
				if (req.getSession().getAttribute("preventCannotCreateSession")==null)
				{
					req.getSession().setAttribute("preventCannotCreateSession", "1");
				}

				//Logger.println(this,"forwarding to docId: " + docId);
				StringBuilder url = new StringBuilder("/showdoc.do?docid=").append(accessDocId);

				if ("get".equalsIgnoreCase(req.getMethod()))
				{
					String name;
					String[] values;
					Enumeration<String> params = req.getParameterNames();
					while (params.hasMoreElements())
					{
						name = params.nextElement();
						if (name.equals("docid") || name.equals("password")) continue;
						values = req.getParameterValues(name);
						for (int i = 0; i < values.length; i++)
						{
							url.append("&").append(Tools.sanitizeHttpHeaderParam(name)).append('=').append(Tools.sanitizeHttpHeaderParam(values[i]));
						}
					}
				}

				Logger.debug(PathFilter.class, "PathFilter, url: " + url + " domain="+domain+" origUrl="+path+" qs="+req.getQueryString());
				if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML || path.endsWith(".php") || path.endsWith(".asp") || path.endsWith(".aspx") || Constants.getBoolean("forwardVirtualPath"))
				{
					req.setAttribute("docid", Integer.toString(accessDocId));
					Logger.debug(PathFilter.class, "forwarding1: req=" + req + " resp="+res);

					String packagerMode = Constants.getString("packagerMode");
					String packagerModeParam = req.getParameter("_packagerMode");
					if (Tools.isNotEmpty(packagerModeParam)) packagerMode = packagerModeParam;

					if (Tools.isEmpty(packagerMode) || "none".endsWith(packagerMode))
					{
						req.getRequestDispatcher("/showdoc.do?docid=" + accessDocId).forward(req, res);
					}
					else
					{
						long start = Tools.getNow();

						WJResponseWrapper respWrapper = new WJResponseWrapper(res, req);
						req.getRequestDispatcher("/showdoc.do?docid=" + accessDocId).forward(req, respWrapper);
						StringBuilder htmlCode = new StringBuilder(respWrapper.strWriter.getBuffer().toString());

						String fixedHtml = htmlCode.toString();

						//moznost prepnutia packagera (pre kazdy pripad)
						String packagerModeAttr = (String)req.getAttribute("packagerMode");
						if (packagerModeAttr!=null) packagerMode = packagerModeAttr;

						//ak je treba posli redirect
						if (Tools.isNotEmpty(respWrapper.getRedirectURL())) res.sendRedirect(respWrapper.getRedirectURL());

						long end = Tools.getNow();
						Logger.debug(PathFilter.class, "Packager: HTML packaging tooks: " + (end-start) + "ms");

						//zapis vystup
						respWrapper.writeResponseToOriginalOutput(req, fixedHtml);
					}
				}
				else
				{
					Logger.debug(PathFilter.class, "redirecting: req="+req+" resp="+res);
					res.sendRedirect(req.getContextPath() + url.toString());
				}
				if (timer != null)
				{
					timer.diff("   Path filter - chain, path: " + path);
					DBPool.getInstance().logConnections();
				}
				return;
			}
			else
			{
				if ("/sitemap.xml".equals(path))
				{
					//nie je definovana stranka s URL /sitemap.xml, priamo forwardnem
					//je to tu kvoli cloudu, pretoze do 404.jsp uz dorazi request a nema RequestBean (zjavne sa to vola az po skonceni filtrov)
					res.setContentType("text/xml; charset=utf-8");
					res.setStatus(HttpServletResponse.SC_OK);
					String customPage = WriteTagToolsForCore.getCustomPage("/components/sitemap/google-sitemap.jsp", req);
					forwardSafely(customPage, req, res);
					return;
				}
			}

			if ((path.startsWith("/components/") || path.startsWith("/templates/")) && path.lastIndexOf('.') > path.lastIndexOf('/') && path.equals("/components/editoricon.gif")==false)
			{
				try
				{
					//skus najst nahradu za pozadovanu stranku
					String customPage = WriteTagToolsForCore.getCustomPage(path, req);
					if (customPage.equals(path)==false)
					{
						forwardSafely(customPage, req, res);
						return;
					}

					if (path.endsWith("editor_component.jsp"))
					{
						//over ci existuje, ak nie, pouzi defaultnu
						if (WriteTagToolsForCore.isFileExists(path)==false)
						{
							try
							{
								//tu mame plne meno povodneho suboru v include
								String jspFileName = req.getParameter("jspFileName");
								if (jspFileName != null)
								{
									if (jspFileName.startsWith("!INCLUDE(")) jspFileName = jspFileName.substring(9).trim();

									//skus verziu s odstranenou druhou castou, /components/iway/news/editor_component.jsp -> /components/news/editor_component.jsp
									String pathBezDruhejCasti = "/components"+path.substring(path.indexOf("/", 13));
									if (WriteTagToolsForCore.isFileExists(pathBezDruhejCasti))
									{
										//mame custom subor, ak ale existuje nastaveny CONF kluc k danemu suboru nepouzijeme
										String CONF_KEY = jspFileName.substring(0, jspFileName.lastIndexOf('.')).replace('/', '.').substring(1);
										Prop prop = Prop.getInstance(req);
										//ak kluc neexistuje pouzijeme
										if (prop.getText(CONF_KEY+".conf.propSearchKey").equals(CONF_KEY+".conf.propSearchKey") && prop.getText(CONF_KEY+".conf").equals(CONF_KEY+".conf"))
										{
											Logger.debug(PathFilter.class, "forwarding to:"+pathBezDruhejCasti);
											req.getRequestDispatcher(WriteTagToolsForCore.getCustomPage(pathBezDruhejCasti, req)).forward(req, res);
										}
									}
								}
							}
							catch (Exception ex)
							{
								sk.iway.iwcm.Logger.error(ex);
							}

							req.getRequestDispatcher(WriteTagToolsForCore.getCustomPage("/components/editor_component_universal.jsp", req)).forward(req, res);
							return;
						}

					}
				}
				catch (RuntimeException e)
				{
					sk.iway.iwcm.Logger.error(e);
				}

			}
			//podpora custom pluginov v editore - NETREBA uz bereme z dist priecinka
			/*if (path.startsWith("/admin/skins/webjet8/ckeditor/dist/plugins/") || path.startsWith("/admin/skins/webjet8/ckeditor/plugins/"))
			{
				path = Tools.replace(path, "/admin/skins/webjet8/ckeditor/dist/plugins/", "/admin/ckeditor/plugins/");
				path = Tools.replace(path, "/admin/skins/webjet8/ckeditor/plugins/", "/admin/ckeditor/plugins/");
				String customPage = WriteTagToolsForCore.getCustomPageAdmin(path, req);
				if (customPage != null)
				{
					req.getRequestDispatcher(customPage).forward(req, res);
					return;
				}
			}*/
			if (path.endsWith(".php") || path.endsWith(".asp"))
			{
				//skus najst JSP verziu stranky
				String jspPath = path.substring(0, path.length() - 4) + ".jsp";
				File jspFile = new File(Tools.getRealPath(jspPath));
				if (jspFile.exists())
				{
					forwardSafely(jspPath, req, res);
					if (timer != null)
					{
						timer.diff("   Path filter - chain, path: " + path);
						DBPool.getInstance().logConnections();
					}
					return;
				}
			}
			if (customPath != null)
			{
				//skontroluj, ci pozadovany subor nie je custom
				IwcmFile f = new IwcmFile(customPath + File.separatorChar + Constants.getInstallName() + path.replace('/', File.separatorChar));

				if (f.exists()==false)
				{
					f = new IwcmFile(Tools.getRealPath("/templates/" + Constants.getInstallName() + "/assets" + path));
				}

				if (f.exists()==false && path.contains("/admin/docsify/") && "iwcm.interway.sk".equals(req.getServerName()) && user != null && user.isAdmin() && path.contains("..")==false)
                {
                    //je to docsify subor, ten mame inde, musime upravit
                    f = new IwcmFile(Tools.getRealPath("/"));
                    f = new IwcmFile(f.getParentFile(), path.substring(6).replace('/', File.separatorChar));
                }

				//Logger.println(this,"Checking CUSTOM: "+f.getAbsolutePath());
				if (f.exists() && f.isFile())
				{
					//existuje custom subor, posli ho na vystup
					if (path.toLowerCase().endsWith(".jsp") || path.toLowerCase().endsWith(".class"))
					{
						//toto este nevieme, nerob nic
						Logger.error(this,"TOTO AKO CUSTOM NEVIEM: " + path);
					}
					else
					{
						//Logger.println(this,"sending custom page:
						// "+f.getAbsolutePath());
						//je to nejaky obrazok, alebo subor, posli na vystup
						FilePathTools.writeFileOut(f, req, res);
						//Logger.println(this,"Zapisany subor: " + path);
						if (timer != null)
						{
							timer.diff("   Path filter - chain, path: " + path);
							DBPool.getInstance().logConnections();
						}
						return;
					}
				}
			}

			if (FilePathTools.isExternalDir(path) && path.startsWith("/files/protected/")==false)
			{
				//externy adresar pre cloudStaticFilesDir folder
				boolean fileWritten = FilePathTools.writeFileOut(path, req, res);
				if (fileWritten)
				{
					if (timer != null)
					{
						timer.diff("   Path filter - chain, path: " + path);
						DBPool.getInstance().logConnections();
					}
					return;
				}
			}

			//zaslanie fresh suboru pre elfinder (volane len s parametrom v=xxx) po zmene velkosti
			//tomcat 8 totiz nejako divne cachuje obrazky a aj ked ho zmenime tak to nepomoze a vrati povodny
			if (user != null && user.isAdmin() && req.getParameter("v")!=null)
			{
				String mimeType = Constants.getServletContext().getMimeType(path.toLowerCase());
				if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
				if (mimeType.startsWith("image/") && path.startsWith("/images"))
				{
					IwcmFile f = new IwcmFile(Tools.getRealPath(path));
					if (f.exists() && f.canRead()) {
						FilePathTools.writeFileOut(f, req, res);
						//Logger.println(this,"Zapisany subor: " + path);
						if (timer != null)
						{
							timer.diff("   Path filter - chain, path: " + path);
							DBPool.getInstance().logConnections();
						}
						return;
					}
				}
			}


			//preposlanie suboru z historie, tiket 13373, parameter pochadza z file_history.jsp
			if (req.getParameter("fHistoryId")!=null && user != null && user.isAdmin())
			{
				int fHistoryId = Tools.getIntValue(req.getParameter("fHistoryId"), -1);
				if (fHistoryId > 0)
				{
					boolean sendOK = FileHistoryDB.sendFileFromHistory(path, fHistoryId, res);
					if (sendOK == false)
					{
						res.setStatus(404);
					}
					return;
				}
			}



			if (path.startsWith("/templates/")) {
				Ninja.includeNinja(req);
			}

			//forward(request, response);
			//return;
		}
		catch (RuntimeException rex)
		{
			sk.iway.iwcm.Logger.error(rex);
		}
		catch (Exception ex)
		{
			if (ex.getClass().getCanonicalName().startsWith("org.spring")) throw ex;
			else
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		if (timer != null)
		{
			timer.diff("   Path filter - chain, path: " + path);
			DBPool.getInstance().logConnections();
		}

		if (handleStrutsRedirect(req, res)) return;

		try
		{
			// Pass control on to the next filter
			chain.doFilter(servletRequest, servletResponse);
		}
		catch (org.springframework.security.access.AccessDeniedException e)
		{
			//nastane napr. pri volani /admin/adminlog/logging ak user nema pravo na cmp_adminlog
			//rest sluzby si to handluju same, toto sa tyka webview
			//neda sa zachytit priamo AccessDeniedException preto chytame NestedServletException
			if (e.getCause().toString().contains("AccessDeniedException"))
			{
				res.setStatus(HttpServletResponse.SC_FORBIDDEN);
				//ak to nie je rest posli ho aj na defaultnu 403 stranku
				if (path==null || path.contains("/rest")==false) forwardSafely("/403.jsp", req, res);
			}
			else
			{
				throw e;
			}
		}
	}

	//these URL are nahdled by Servlet, so no need to forward it to Spring version
	private static String[] strutsUrlServletExceptions = {
		"/showdoc.do",
		"/admin/offline.do",
		"/admin/docdel.do",
		"/preview.do",
		"/admin/multiplefileupload.do",
		"/formmail.do",
		"/logoff.do",
		"/admin/logoff.do"
	};

	/**
	 * Handle old struts *.do calls, it will be redirected to *.struts mappings which is handled by Spring MVC
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private boolean handleStrutsRedirect(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String path = PathFilter.getOrigPath(request);
		String doShowdocAction = request.getParameter("doShowdocAction");
		if (Tools.isNotEmpty(doShowdocAction) && ShowDoc.isDoShowdocActionAllowed(doShowdocAction)) path = doShowdocAction;
		if (path==null) path = request.getRequestURI();

		if (path.endsWith(".do")) {

			//this is handled directly by servlet, no need to forward
			if (Tools.containsOneItem(strutsUrlServletExceptions, path)==false) {
				Logger.debug(PathFilter.class, "Forwarding old struts path: " + path);
				request.getRequestDispatcher(Tools.replace(path, ".do", ".struts")).forward(request, response);
				return true;
			}
		}
		return false;
	}


	/**
	 * Skontroluje, ci je mozne pristupit k verejnej casti webu, nie admin casti
	 *
	 * @param request
	 * @return
	 */
	public static boolean checkWebAccess(HttpServletRequest request, String path)
	{
        Identity user = UsersDB.getCurrentUser(request);

        //kontrola referer hlavicky (CSRF), je povinna pre POST alebo ked je prihlaseny user
        String pathLC = path.toLowerCase();
        if (user != null || "POST".equalsIgnoreCase(request.getMethod()) || pathLC.endsWith(".do") || pathLC.endsWith(".action"))
        {
            boolean isUrlAccessible = true;
            boolean xsrfUrlException = DocTools.isXssStrictUrlException(path, "xsrfUrlException");
            if (xsrfUrlException==false && isXsrfCheckRequired(request))
            {
                isUrlAccessible = checkXsrf(request);
            }

            //nastal CSRF utok, referer nesedi
            if (isUrlAccessible==false) return false;
        }

		//kontrola pristupu do /components/ adresara
		if ((path.startsWith("/components/") || path.startsWith("/apps/"))
			 && (path.endsWith(".jsp") || path.endsWith(".ftl") || path.endsWith(".html")))
		{
			if (user == null || user.isAdmin()==false)
			{
				boolean canAccess = DocTools.isXssStrictUrlException(path, "componentsDirectCallExceptions");
				if (canAccess==false)
				{
					//ak to zacina na /apps/ over este, ci to nie je platna URL adresa stranky
					int docId = -1;
					if (path.startsWith("/apps/")) docId = DocDB.getInstance().getDocIdFromURLImpl(path, DocDB.getDomain(request));
					if (docId < 1)
					{
						Adminlog.add(Adminlog.TYPE_XSS, "Direct component call, path="+path, -1, -1);
						return false;
					}
				}
				//duplicitna kontrola, v ziadnom pripade nema zmysel pristupovat na komponenty s URL obsahujucim admin a tiez do statistiky
				if (canAccess && path.contains("admin_answer.jsp")) return canAccess;
				if (path.contains("admin"))
				{
					if (user != null && (path.contains("FCKeditor") || path.contains("/components/gallery/admin_gallery_popup.jsp") ||
							path.contains("/components/helpdesk/fotogaleria/admin_upload.jsp")))
					{
						//specialna vynimka pre CK editor pre rozne projekty kde je user prihlaseny ale nie je admin
						//+ #17331 - pridana vynimka pre upload suboru vo fotogalerii v intre
					}
					else
					{
						return false;
					}
				}
				if (path.contains("/components/stat/") && path.contains("stat_async_ajax.jsp") == false) return false;
			}
		}

		//kontrola pristupu podla povolenych IP adries
		HttpSession session = request.getSession();
		Boolean val = (Boolean) Tools.sessionGetAttribute(session, CHECK_WEB_ACCESS_SESSION_KEY);
		if (val != null)
		{
			//Logger.println(this,"Checking to access web from session: " + val.booleanValue());
			return (val.booleanValue());
		}

		boolean ret = Tools.checkIpAccess(request, "webEnableIPs");
		Tools.sessionSetAttribute(session, CHECK_WEB_ACCESS_SESSION_KEY, Boolean.valueOf(ret));
		return (ret);
	}

	/**
	 * Skontroluje, ci je mozne pristupit k verejnej casti webu na zaklade domeny<br />
	 *
	 * Castokrat web najskor bezi na domene nieco.t8.iway.sk a nasledne sa spusti na
	 *	domenie nieco.sk, filter umoznuje zadat domeny, pre ktore bude stranka
	 *	vracat obsah a pre ktore bude robit 404 / redirect na inu domenu.
	 *	@author kmarton
	 *
	 * @param request
	 * @return
	 */
	private boolean checkDomain(HttpServletRequest request)
	{
		HttpSession session = request.getSession();
		Boolean val = (Boolean) Tools.sessionGetAttribute(session, CHECK_DOMAIN_SESSION_KEY);
		if (val != null)
		{
			//Logger.println(this,"Checking DOMAIN to access web from session: " + val.booleanValue());
			return (val.booleanValue());
		}
		String webAllowedDomains = Constants.getString("webAllowedDomains");
		boolean ret = true;
		if (webAllowedDomains != null && webAllowedDomains.length() > 1)
		{
			ret = false;
			StringTokenizer st = new StringTokenizer(webAllowedDomains, ",");
			String domain;
			String myDomain = Tools.getServerName(request);
			Logger.println(this,"Checking: " + myDomain + " vs " + webAllowedDomains);
			while (st.hasMoreTokens())
			{
				domain = st.nextToken();
				if (myDomain.endsWith(domain))
				{
					ret = true;
					break;
				}
			}
		}
		Tools.sessionSetAttribute(session, CHECK_DOMAIN_SESSION_KEY, Boolean.valueOf(ret));
		return (ret);
	}

	private boolean checkAccessToAdmin(String path, HttpServletRequest request, HttpServletResponse response)
	{
		boolean isAdminUrlAccessible = true;
        Identity user = UsersDB.getCurrentUser(request.getSession());

        boolean xsrfUrlException = DocTools.isXssStrictUrlException(path, "xsrfUrlException");

		if(path.startsWith("/admin") ||
			(path.startsWith("/components/") && path.contains("/admin")) ||
			(path.startsWith("/apps/") && path.contains("/admin")))	//pristupuje do admin casti a admin komponent/app
		{
			if(user == null || UsersDB.checkUserPerms(user, "admin|editableGroupsNotEmpty")==false)	//A nie je prihlaseny, alebo nie je prihlaseny ako admin
			{
				isAdminUrlAccessible = false;
				String allowAdminUrls = Constants.getString("allowAdminUrls");
				//ak v starom WJ mali customizovanu premennu a nemaju tam springove prihlasenie, pridajme
				if (allowAdminUrls.contains("/admin/logon.do") && allowAdminUrls.contains("/admin/logon/")==false) allowAdminUrls += ",^/admin/logon$,^/admin/logon/$,^/admin/logon/changePassword$";

				String[] adminUrls = Tools.getTokens(allowAdminUrls, ",", true);

				for(String url: adminUrls)
				{
					if(path.startsWith(url) || ("^"+path+"$").equals(url) || (path+"$").endsWith(url))
					{
						isAdminUrlAccessible = true;
						break;
					}
				}
			}
			if(isAdminUrlAccessible && xsrfUrlException==false)
			{
				isAdminUrlAccessible = checkXsrf(request);
			}

			if (path.endsWith(".html") || path.endsWith(".jsp") || path.endsWith(".action") || path.endsWith(".do") || path.endsWith(".js") || path.endsWith("/") || path.indexOf("ajax")!=-1)
			{
				setUaCompatibleAdmin(path, response);
			}
		}

		if (user != null && user.isAdmin())
		{
            if (xsrfUrlException==false)
            {
                if (isXsrfCheckRequired(request))
                {
                    isAdminUrlAccessible = checkXsrf(request);
                }
            }
        }
		return isAdminUrlAccessible;
	}

    /**
     * Overi, ci je potrebna XSRF kontrola (kontrola referera)
     * ta sa robi pri POST alebo ked v URL su nejake parametre (okrem vynimiek definovanych v xsrfParamNameException ako docid, _logLevel, combineEnabled atd)
     * @param request
     * @return
     */
	private static boolean isXsrfCheckRequired(HttpServletRequest request)
    {
        //ak uz je user prihlaseny ako admin, kontrolujme CSRF vzdy (aby sa nedal spravit utok z inej stranky mazuci napr. data)
        //niektore URL musime whitelistovat, kedze mozu viest z mailu
        boolean isXsrfCheckRequired = false;
        if ("GET".equalsIgnoreCase(request.getMethod()))
        {
            Enumeration<String> paramNames = request.getParameterNames();
            int failsafe = 0;
            int maxFailsafe = 10;

            String xsrfParamNames = Constants.getString("xsrfParamNameExceptionSystem");
            if (Tools.isNotEmpty(Constants.getString("xsrfParamNameException"))) xsrfParamNames += "," + Constants.getString("xsrfParamNameException");

            String[] xsrfParamNameException = Tools.getTokens(xsrfParamNames.toLowerCase(), ",", true);
            while (paramNames.hasMoreElements() && failsafe++ <= maxFailsafe)
            {
                String name = paramNames.nextElement().toLowerCase();
                //natvrdo kvoli performance
                if ("docid".equals(name)) continue;
                //displaytag povolime
                if (name.startsWith("d-")) continue;
                //over voci zoznamu povolenych parametrov
                if (Tools.containsOneItem(xsrfParamNameException, name)) continue;

                isXsrfCheckRequired = true;
            }
            //ak to ma vela parametrov tak to je rovno podozrive
            if (failsafe>=maxFailsafe) isXsrfCheckRequired = true;
        }
        else {
            //pre POST je check required
            isXsrfCheckRequired = true;
        }
        return isXsrfCheckRequired;
    }


	/**
	 * Method checks for XSRF attack, if referrer from request header is not current server
	 * or from list of trusted referrers (constant xsrfReferers, see {@link Constants} ) method denies
	 * access to url and logs attack. for more information see Ticket 11207
	 * @param request request
	 * @return true if xsrf is not detected, else false
	 */
	private static boolean checkXsrf(HttpServletRequest request)
	{
		String trustedXsrfReferers = Constants.getString("xsrfReferers");
		//ten length je tam aby v Oracle DB bolo mozne v konfiguracii nastavit znak -
		if(Tools.isEmpty(trustedXsrfReferers) || trustedXsrfReferers.length()<2) return true;

		String remoteReferrer = request.getHeader("referer");
		if(Tools.isEmpty(remoteReferrer))
        {
            //vynimka pre elfinder download - /admin/elfinder-connector/?cmd=file&target=iwcm_2_L2ZpbGVzL3pfb19wcmlzcF9uYV9wb2hyZWJfcGRmLnBkZg_E_E&download=1&volumes=files
            //nechcel som dat vynimku pre cely elfinder-connector, preto sa testuje parameter download
            String path = PathFilter.getOrigPath(request);
            if ("/admin/elfinder-connector/".equals(path) && "1".equals(request.getParameter("download")))
            {
                return true;
            }

            //do slaka, MSIE posiela empty referer ked otvara popup okno, takze zial MSIE nebude pre GET poziadavky chranene
            String userAgent = request.getHeader("User-Agent");
            boolean isMsie = false;
            if (userAgent!=null && "GET".equalsIgnoreCase(request.getMethod()))
            {
                //vynimka len pre Trident/7.0 co je MSIE 11, starsi MSIE nebude podporovany (jedine ochranu uplne vypnut)
                if (userAgent.contains("Trident/7.0")) isMsie = true;
            }

            if  (isMsie==false && isXsrfCheckRequired(request))
            {
                Adminlog.add(Adminlog.TYPE_XSRF, "Possible XSRF attack, referer is empty", -1, -1);
                return false;
            }
            return true;
        }

		String remoteReferrerServer = "";
		try
		{
			URL remoteReferrerUrl = new URL(remoteReferrer);
			remoteReferrerServer = remoteReferrerUrl.getHost();
		}
		catch (Exception e)
		{
			Logger.debug(PathFilter.class,"Failed to create URL from referrer: " + remoteReferrer);
		}
		for(String trustedReferrer : trustedXsrfReferers.split(","))
		{
			trustedReferrer = trustedReferrer.trim();
			if(trustedReferrer.equalsIgnoreCase(Constants.SERVER_NAME_MACRO))
			{
				trustedReferrer = Tools.getServerName(request, false);
			}
			if (trustedReferrer.startsWith("%") && trustedReferrer.endsWith("%") && trustedReferrer.length()>4 && remoteReferrerServer.contains(trustedReferrer.substring(1, trustedReferrer.length()-1)))
			{
					return true;
			}
			if (trustedReferrer.startsWith("%") && trustedReferrer.length()>2 && remoteReferrerServer.endsWith(trustedReferrer.substring(1)))
			{
				return true;
			}
			if(remoteReferrerServer.equalsIgnoreCase(trustedReferrer))
			{
				return true;
			}
		}
		Adminlog.add(Adminlog.TYPE_XSRF, "Possible XSRF attack from: " + remoteReferrer, -1, -1);

		return false;
	}

	/**
	 * Skontroluje, ci je mozne pristupit k admin casti
	 *
	 * @param request
	 * @return
	 */
	public static boolean checkAdmin(HttpServletRequest request)
	{
		HttpSession session = request.getSession();
		Boolean val = (Boolean) Tools.sessionGetAttribute(session, CHECK_ADMIN_SESSION_KEY);
		if (val != null)
		{
			return (val.booleanValue());
		}

		if ("public".equals(Constants.getString("clusterMyNodeType"))) return false;

		String adminEnableIPs = Constants.getString("adminEnableIPs");
		boolean ret = true;
		if (adminEnableIPs != null && adminEnableIPs.length() > 1)
		{
			ret = false;
			StringTokenizer st = new StringTokenizer(adminEnableIPs, ",");
			String ip;
			String myIP = Tools.getRemoteIP(request);
			Logger.println(PathFilter.class,"Checking: " + myIP + " vs " + adminEnableIPs);
			while (st.hasMoreTokens())
			{
				ip = st.nextToken().trim();
				if (myIP.startsWith(ip))
				{
					ret = true;
					break;
				}
			}
		}
		Tools.sessionSetAttribute(session, CHECK_ADMIN_SESSION_KEY, Boolean.valueOf(ret));
		return (ret);
	}

	/**
	 * Nacita z databazy zoznam adresarov, v ktorych su chranene subory
	 *
	 */
	public static synchronized void reloadProtectedDirs()
	{
		Map<String, EditForm> protectedDirs = new HashMap<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM dirprop"); //NOSONAR
			rs = ps.executeQuery();
			EditForm ef;
			String passProtected;
			while (rs.next())
			{
				passProtected = DB.getDbString(rs, "password_protected");
				if (Tools.isEmpty(passProtected))
				{
					continue;
				}

				ef = new EditForm();
				ef.setPasswordProtectedString(passProtected);
				ef.setOrigDir(DB.getDbString(rs, "dir_url"));
				ef.setIndexFulltext(rs.getBoolean("index_fulltext"));
				ef.setLogonDocId(rs.getInt("logon_doc_id"));

				protectedDirs.put(ef.getOrigDir(), ef);
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception sqlEx)
		{
			//ak este nie je spraveny update toto hadze exception
			if (sqlEx.getMessage().indexOf("doesn't exist")!=-1)
			{
				sk.iway.iwcm.Logger.error(sqlEx);
			}
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
			}
		}
		passwordProtected = protectedDirs;
	}

	/**
	 * Skontroluje, ci zadane URL je v protected zone
	 * @param url
	 * @return EditForm ak je chranene, alebo null
	 */
	public static EditForm isPasswordProtected(String url, HttpServletRequest request)
	{
		if (request == null) return isPasswordProtected(url, null, null);
		return isPasswordProtected(url, request, request.getSession());
	}

	/**
	 * Skontroluje prava k danemu URL (suboru)
	 * @param url - url adresa suboru z adresara /files
	 * @param request - ak nie je null prida sa aj statistika videni (ak statistiku nechceme zaratat nastavime len session)
	 * @param session - session z ktorej sa ziska identita pouzivatela
	 * @return
	 */
	public static EditForm isPasswordProtected(String url, HttpServletRequest request, HttpSession session)
	{
		boolean galleryCheckUserPerms = Constants.getBoolean("galleryCheckUserPerms");
		if (url.startsWith("/files")==false && (url.startsWith("/images/gallery/")==false || galleryCheckUserPerms == false))
		{
			return(null);
		}

		if (session == null && request != null) session = request.getSession();

		EditForm efBestMatch = null;

		if(passwordProtected != null && passwordProtected.size() > 0)
		{
			String testPath = url;
			int start = 1; //nastavit musim na 1, aby v pripade prvej podmienky islo do while cyklu
			if (testPath.endsWith("/")) testPath = testPath.substring(0, testPath.length()-1);
			else if(testPath.contains(".")) //ak sa jedna o subor - vytiahnem adresar
			{
				start = testPath.lastIndexOf("/");
				if(start != -1) testPath = testPath.substring(0, testPath.lastIndexOf("/"));
			}
			EditForm ef = null;
			int failsafe = 0;
			String firstTestPath = "";
			//hladam najblizsi nadradeny adresar kotry je password protected
			while (start > 0 && failsafe++ < 30)
			{
				if(failsafe == 1) firstTestPath = testPath;

				ef = passwordProtected.get(testPath);
				if(ef != null)
				{
					efBestMatch = ef;
					//pokial som nenasiel v prvom pokuse, pridam do zoznamu, nech pri dalsom volani neiterujem zbytocne + ochrana proti DOC utokom
					if(failsafe > 1 && passwordProtected.size() < 2000) passwordProtected.put(firstTestPath, ef);
					break;
				}

				start = testPath.lastIndexOf("/");
				if (start > 0) testPath = testPath.substring(0, start); //substring len v pripade ak lomitko nieje na zaciatku
			}
		}

		if (url.startsWith("/files"))
		{
			//pre files skus skontrolovat ci nie je zaheslovany v sekcii /files/
			DocDB docDB = DocDB.getInstance();
			int docId = docDB.getDocIdFromURLImpl(url+".html", null);
			if (docId < 1) docId = docDB.getDocIdFromURLImpl(url+".html", DocDB.getDomain(request));
			if (docId < 1) docId = docDB.getDocIdFromURLImpl(Tools.replace(url, ".", "-")+".html", null);
			if (docId < 1) docId = docDB.getDocIdFromURLImpl(Tools.replace(url, ".", "-")+".html", DocDB.getDomain(request));
			if (docId > 0)
			{
				DocDetails fileDoc = docDB.getBasicDocDetails(docId, false);
				if (fileDoc != null)
				{
					Identity user = null;
					if (session != null) user = UsersDB.getCurrentUser(session);
					else
					{
						RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
						if (rb!=null && rb.getUserId()>0) user = new Identity(UsersDB.getUser(rb.getUserId()));
					}
					boolean canAccess = DocDB.canAccess(fileDoc, user, true);
					boolean isAvailable = fileDoc.isAvailable();
					if (isAvailable == false && user != null && user.isAdmin() && Constants.getBoolean("adminCheckUserGroups")==false)
					{
						//ak je admin ignorujeme nastavenie available atributu stranky
						isAvailable = true;
					}

					if (!canAccess || !isAvailable)
					{
						efBestMatch = new EditForm();
						//to cislo je bulharska konstanta len aby nam potom nezbehlo ef.isAccessibleFor, pretoze to neriesi podadresare dokumentu
						efBestMatch.setPasswordProtectedString("999888777");
					}
					else
					{
						try
						{
							if (request != null)
							{
								StatDB.add(request.getSession(), request, null, docId);
							}
						}
						catch (Exception e)
						{
							sk.iway.iwcm.Logger.error(e);
						}
					}
				}
			}
		}

		return(efBestMatch);
	}

	/**
	 * Vykona redirect (ak treba) na zobrazenie logon stranky / zamietnutia pristupu k suboru (/files/*), vrati true, ak bol redirect vykonany
	 * @param ef
	 * @param user
	 * @param originalPath
	 * @param req
	 * @param res
	 * @return true ak je redirect vykonany, inak false
	 * @throws IOException
	 * @throws ServletException
	 */
	public static boolean doFileForbiddenRedirect(EditForm ef, Identity user, String originalPath, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
	{
		boolean isAccesible = false;
		if (user != null && ef!=null)
		{
			//otestuj, ci je user v skupine pre tento adresar

			int i;
			int size = ef.getPasswordProtected().length;
			for (i=0; i<size; i++)
			{
				if (user.isInUserGroup(ef.getPasswordProtected()[i]))
				{
					isAccesible = true;
					break;
				}
			}

		}

		if (isAccesible) return false;

		String pathQS = originalPath;
		if (Tools.isNotEmpty(req.getQueryString())) pathQS = originalPath + "?" + req.getQueryString();

		Logger.debug(PathFilter.class, "NOT ACCESSIBLE: path="+pathQS+" user="+user);
		if (user != null)
		{
			//user nema pristup, daj mu forbidden
			int fileAccessDeniedDocId = Constants.getInt("fileAccessDeniedDocId");
			Logger.debug(PathFilter.class, "fileAccessDeniedDocId="+fileAccessDeniedDocId+" ntlm="+AuthenticationFilter.getForbiddenURL());
			if (fileAccessDeniedDocId > 0)
			{
				res.setHeader("Pragma","No-Cache");
				res.setDateHeader("Expires",0);
				res.setHeader("Cache-Control","no-Cache");

				req.setAttribute("logonFormSubmitURL", originalPath);
				req.getRequestDispatcher("/showdoc.do?docid="+fileAccessDeniedDocId+"&origUrl="+Tools.URLEncode(pathQS)).forward(req, res);
			}
			else if (Tools.isNotEmpty(AuthenticationFilter.getForbiddenURL()))
			{
				res.sendRedirect(Tools.addParameterToUrlNoAmp(AuthenticationFilter.getForbiddenURL(), "origUrl", pathQS));
			}
			else
			{
				res.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		}
		else
		{
			//forwardni sa na logon stranku
			req.getSession().setAttribute("afterLogonRedirect", pathQS);

			res.setHeader("Pragma","No-Cache");
			res.setDateHeader("Expires",0);
			res.setHeader("Cache-Control","no-Cache");

			if (ef != null && ef.getLogonDocId()>0)
			{
				//res.sendRedirect("/showdoc.do?docid="+ef.getLogonDocId());

				//request.setAttribute("docDetailsOriginal", doc);

				//pre subor sa musime submitnut priamo na usrlogon.do aby sa najskor spravilo prihlasenie az potom kontrola prav na subor
				req.setAttribute("logonFormSubmitURL", "/usrlogon.do");
				req.getRequestDispatcher("/showdoc.do?docid="+ef.getLogonDocId()+"&dontUpdateLastDocId=true").forward(req, res);
			}
			else
			{
				req.getRequestDispatcher("/components/user/logon.jsp?permsDenied=true").forward(req, res);
			}
		}
		return true;
	}

	/**
	 * Vrati cestu k suboru na disku z daneho URL, berie do uvahy aj custom path
	 * @param url - url adresa suboru, napr. /css/page.css
	 * @return - vrati cestu na disku, napr. /var/webapps/webjet/css/page.css
	 */
	public static String getRealPath(String url)
	{
		if (customPath != null)
		{
			//skontroluj, ci pozadovany subor nie je custom
			File f = new File(customPath + File.separatorChar + Constants.getInstallName() + (url.startsWith("/")?"":File.separatorChar) + url.replace('/', File.separatorChar));
			//Logger.println(this,"Checking CUSTOM: "+f.getAbsolutePath());
			if (f.exists())
			{
				return(f.getAbsolutePath());
			}
		}
		return(Tools.getRealPath(url));
	}

	public static String getOrigPath(HttpServletRequest request)
	{
		String origPath = (String)request.getAttribute("path_filter_orig_path");
		//check for unsafe chars like "'`
		if (isPathSafe(origPath)) return origPath;
		return "";
	}

	/**
	 * Vrati adresu povodnej stranky, v pripade DOCID liniek vratane parametra docid
	 * @param request
	 * @return
	 */
	public static String getOrigPathDocId(HttpServletRequest request)
	{
		if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID && request.getParameter("docid")!=null)
		{
			return "/showdoc.do?docid="+Tools.getDocId(request);
		}
		String origPath = getOrigPath(request);

		if ("/showdoc.do".equals(origPath)) origPath = origPath + "?docid=" + Tools.getDocId(request);

		return origPath;
	}

	/**
	 * Vrati adresu povodnej stranky, v pripade DOCID liniek vratane docid s pridanym
	 * parametrom pre uload suboru pre stripes (aby to Stripes spracoval)
	 * @param request
	 * @return
	 */
	public static String getOrigPathUpload(HttpServletRequest request)
	{
		if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID && request.getParameter("docid")!=null)
		{
			return "/showdoc.do?docid="+Tools.getDocId(request) +"&__sfu=1";
		}
		return getOrigPath(request)+"?__sfu=1";
	}

	public static boolean bypassPath(String path, ServletRequest servletRequest)
	{
		//aby nam lokalne fungoval customPath
		if (Tools.isNotEmpty(PathFilter.getCustomPath()) && "iwcm.interway.sk".equals(servletRequest.getServerName())) return false;

		if (bypassPath == null)
		{
			synchronized(PathFilter.class) //NOSONAR
			{
				if (bypassPath == null)
				{
					String[] tokens = Tools.getTokens(Constants.getString("pathFilterBypassPath"), ",", true);
					bypassPath = tokens == null ? new String[0] : tokens;
				}
			}
		}

		if (bypassPath.length > 0)
		{
			for (int i=0; i<bypassPath.length; i++)
			{
				if (path.startsWith(bypassPath[i])) return(true);
			}
		}

		return(false);
	}

    /**
     * Nastavi cache hlavicky podla konf. premennej cacheStaticContentSeconds a cacheStaticContentSuffixes
     * @param path
     * @param user
     * @param request
     * @param response
     * @return
     */
	public static boolean setStaticContentHeaders(String path, Identity user, HttpServletRequest request, HttpServletResponse response)
	{
		if (path.startsWith("/cache/")==false && request.getParameter("v")!=null)
		{
			//vygeneruj odkaz bez v parametra, negeneruje pre fiktivne /cache/nieco.css linky
			String canonicalLink = Tools.getBaseHref(request) + path;
			response.setHeader("Link", "<"+canonicalLink+">; rel=\"canonical\"");
		}

		//vypnutie cachovania pre obrazky z imageeditora
		if (path.contains("/_tmp_"))
		{
			response.setDateHeader("Expires",0);
			response.setDateHeader("Last-Modified", Tools.getNow());
			response.setHeader("Cache-Control","no-store, no-cache, must-revalidate, max-age=0");
			//response.setHeader("Cache-Control","post-check=0, pre-check=0");
			response.setHeader("Pragma","No-Cache");
			response.setHeader("Etag", "");

			return false;
		}

		if (cacheStaticContentSeconds < 0)
		{
			cacheStaticContentSeconds = Constants.getInt("cacheStaticContentSeconds");
		}

		if (cacheStaticContentSuffixes == null)
		{
			synchronized(PathFilter.class) //NOSONAR
			{
				if (cacheStaticContentSuffixes == null)
				{
					String[] tokens = Tools.getTokens(Constants.getString("cacheStaticContentSuffixes"), ",", true);
					cacheStaticContentSuffixes = tokens == null ? new String[0] : tokens;
				}
			}
		}

		//prefixom /cache mame oznacene veci, co sa musia cachovat kvoli rychlosti admin casti (/admin/scripts/common.jsp)
		if (path.startsWith("/cache/")==false)
		{
			if (cacheStaticContentSeconds<1 || cacheStaticContentSuffixes==null || cacheStaticContentSuffixes.length<1) return false;

			//administratorom cache nenastavujem (okrem admin casti, tam sa to casto nemeni)
			if (user!=null && user.isAdmin() && path.startsWith("/admin")==false && path.startsWith("/components")==false && Constants.getBoolean("cacheStaticContentForAdmin")==false) return false;

			//pre IP pre ktore neevidujeme statistiku nenastavujem (pravdepodobne IWAY + admini)
			if (Constants.getBoolean("cacheStaticContentForAdmin")==false && BrowserDetector.isStatIpAllowedFast(request)==false) return false;
		}
		//String pathLC = path.toLowerCase();
		//v parametri v sa prenasa verzia suboru, v tom pripade cachujeme
		//uz cachujem aj admin cast if (pathLC.startsWith("/admin/") && (request.getParameter("v")==null && request.getParameter("t")==null)) return false;

		int myCacheStaticContentSeconds = cacheStaticContentSeconds;
		if (myCacheStaticContentSeconds < 1 && path.startsWith("/cache/")) myCacheStaticContentSeconds = 300;
		//ak mame parameter v tak natvrdo nastavime cas na 7 dni
		if (request.getParameter("v")!=null) myCacheStaticContentSeconds = 60 * 60 * 24 * 7;
		if (path.contains("ckeditor")) myCacheStaticContentSeconds = 60 * 60 * 24 * 7;
		if (path.contains("assets")) myCacheStaticContentSeconds = 60 * 60 * 24 * 7;
		if (path.contains("_common")) myCacheStaticContentSeconds = 60 * 60 * 24 * 7;
		if (path.contains("/admin/images/")) myCacheStaticContentSeconds = 60 * 60 * 24 * 7;
		if (path.contains("/admin/v9/dist/") && myCacheStaticContentSeconds<301) myCacheStaticContentSeconds = 60 * 30;

		if (myCacheStaticContentSeconds < cacheStaticContentSeconds) myCacheStaticContentSeconds = cacheStaticContentSeconds;

		for (int i=0; i<cacheStaticContentSuffixes.length; i++)
		{
			if (path.endsWith(cacheStaticContentSuffixes[i]))
			{
				setCacheHeaders(myCacheStaticContentSeconds, response);

				return true;
			}
		}

		//cachovanie json pre dashboard
		if (path.contains("/admin/v9/json/")) {
			setCacheHeaders(60 * 60 * 24, response);
		}

		return false;
	}

    /**
     * Nastavi HTTP hlavicku Content-Disposition na hdonotu attachment;filename=abc pre subory v adresaroch /files a /images
     * ktore maju priponu definovanu v konf. premennej forceDownloadSuffixes
     *
     * Umozni to vyvolat download dialog napr. pre pdf subory namiesto ich zobrazenia v prehliadaci
     *
     * @param path
     * @param request
     * @param response
     */
	public static void setDownloadHeaders(String path, HttpServletRequest request, HttpServletResponse response)
    {
        if (path.endsWith("/")) return;

        if (path.startsWith("/images") || path.startsWith("/files"))
        {
            try
            {
                int lomka = path.lastIndexOf("/");
                int bodka = path.indexOf(".", lomka);
                if (lomka > 0 && bodka > lomka)
                {
                    String fileName = path.substring(lomka + 1);

                    if (isForceDownload(fileName))
                    {
                        response.setHeader("Content-Disposition", "attachment;filename="+fileName);
                    }
                }
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }
        }
    }

    public static boolean isForceDownload(String fileName) {
		String forceDownloadSuffixes = Constants.getString("forceDownloadSuffixes");
		String ext = FileTools.getFileExtension(fileName);

		return (Tools.isNotEmpty(forceDownloadSuffixes) && forceDownloadSuffixes.contains(ext));
	}

	/**
	 * Nastavi cache hlavicky na pozadovanu hodnotu v sekundach
	 * @param myCacheStaticContentSeconds
	 * @param response
	 */
	public static void setCacheHeaders(int myCacheStaticContentSeconds, HttpServletResponse response)
	{
		response.setHeader("Cache-Control", "max-age="+myCacheStaticContentSeconds);
		if (response.containsHeader("Pragma"))	response.setHeader("Pragma","");
		if (response.containsHeader("Expires")) response.setDateHeader("Expires", Tools.getNow()+myCacheStaticContentSeconds*1000);
	}

	/**
	 * Cache pre staticke subory, cachuju sa len subory pre ktore sa nastavuje setStaticContentHeaders
	 * @param url
	 * @return
	 */
	public static boolean writeAndCacheFile(String url, HttpServletResponse response)
	{
		//ak je cache vypnuta, neriesime
		if (FileCache.useFileCache() == false) return false;
		//pre DB storage tu cache neriesime, to sa riesi priamo v IwcmFsFilter
		if (IwcmFsDB.useDBStorage()) return false;

		String mimeType = Constants.getServletContext().getMimeType(url.toLowerCase());
		if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
		response.setContentType(mimeType);

		byte[] data = FileCache.getObject(url);
		if (data != null)
		{
			response.setContentLength(data.length);

			try
			{
				OutputStream out=response.getOutputStream();
				out.write(data);
				out.close();
				return true;
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		else
		{
			IwcmFile f = new IwcmFile(Tools.getRealPath(url));
			if (f.length() > FileCache.getMaxFileSize()) return false;

			response.setContentLength((int)f.length());

			//nacitaj data do pola
			data = new byte[(int)f.length()];
			boolean dataCopied = false;

			IwcmInputStream in = null;
			try
			{
				OutputStream out=response.getOutputStream();

				in = new IwcmInputStream(f);

				byte[] buffer = new byte[64000];
				int bytesRead = 0;
				int bytesCopied = 0;
				while ((bytesRead = in.read(buffer, 0, 64000)) != -1)
				{
					//Logger.debug(IwcmFsDB.class, "writing "+bytesRead);
					out.write(buffer, 0, bytesRead);

					System.arraycopy(buffer, 0, data, bytesCopied, bytesRead);
					bytesCopied+=bytesRead;
				}
				out.flush();
				out.close();
				in.close();

				dataCopied = true;
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
			finally
			{
				if (in != null) try { in.close(); } catch (Exception e) {}
			}

			if (dataCopied)
			{
				FileCache.setObject(url, data);
				return true;
			}
		}

		return false;
	}

	/**
	 * Nastavenie hlavicky X-Robots-Tag, viz https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag
	 * @param url
	 * @param response
	 */
	public static void setXRobotsTagValue(String url, HttpServletResponse response)
	{
		String xRobotsTagUrls = Constants.getString("xRobotsTagUrls");
		if (Tools.isEmpty(xRobotsTagUrls)) return;

		if (url.charAt(0)=='/') url = url.toLowerCase(); //aby sa nam NOT_SEARCHABLE_PAGE nezmenilo na male pismena

		String[] paths = Tools.getTokens(xRobotsTagUrls, ",", true);
		for (String path : paths)
		{
			if (ResponseHeaderService.isPathCorrect(path, url))
			{
				String xRobotsTagValue = Constants.getStringExecuteMacro("xRobotsTagValue");
				if (Tools.isNotEmpty(xRobotsTagValue))
				{
					response.setHeader("X-Robots-Tag", xRobotsTagValue);
					return;
				}
			}
		}
	}

	public static void setUaCompatibleAdmin(String path, HttpServletResponse response)
	{
		String xUaCompatibleAdminValue = Constants.getString("xUaCompatibleAdminValue");
		if (Tools.isEmpty(xUaCompatibleAdminValue)) return;


		response.setHeader("X-UA-Compatible", xUaCompatibleAdminValue);
	}

	/**
	 * Nastavi hlavicku X-Frame-Options podla konfiguracnej premennej xFrameOptions
	 * @param response
	 */
	private static void setXFrameOptions(HttpServletResponse response)
	{
		String xFrameOptions = Constants.getString("xFrameOptions");
		if (Tools.isEmpty(xFrameOptions)) return;


		response.setHeader("X-Frame-Options", xFrameOptions);
	}

	private static void setXXssProtection(HttpServletResponse response)
	{
		String xXssProtection = Constants.getString("xXssProtection");
		if (Tools.isEmpty(xXssProtection)) return;

		response.setHeader("X-XSS-Protection", xXssProtection);
	}

	/**
	 * Nastavi hlavicku Feature-Policy podla konfiguracnej premennej featurePolicyHeader
	 * @param response
	 */
	public static void setFeaturePolicy(HttpServletResponse response)
	{
		String featurePolicy = Constants.getString("featurePolicyHeader");
		if (Tools.isEmpty(featurePolicy)) return;

		response.setHeader("Feature-Policy", featurePolicy);
		response.setHeader("Permissions-Policy", featurePolicy);
	}

	/**
	 * Nastavenie hlavicky Access-Control-Allow-Origin
	 * @param url
	 * @param response
	 */
	public static void setAccessControlAllowOrigin(String url, HttpServletResponse response)
	{
		String accessControlAllowOriginUrls = Constants.getString("accessControlAllowOriginUrls");
		if (Tools.isEmpty(accessControlAllowOriginUrls)) return;

		String[] paths = Tools.getTokens(accessControlAllowOriginUrls, ",", true);
		String accessControlAllowOriginValue = Constants.getStringExecuteMacro("accessControlAllowOriginValue");
		for (String path : paths)
		{
			if (ResponseHeaderService.isPathCorrect(path, url))
			{
				if (Tools.isNotEmpty(accessControlAllowOriginValue))
				{
					String accessControlAllowedOrigins = Constants.getString("accessControlAllowedOrigins");
					boolean canAccess = false;
					if (Tools.isEmpty(accessControlAllowedOrigins))
					{
						canAccess = true;
					}
					else
					{
						accessControlAllowedOrigins = Tools.replace(accessControlAllowedOrigins, "\n", ",");
						accessControlAllowedOrigins = Tools.replace(accessControlAllowedOrigins, "\r", ",");
						RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
						if (rb != null) {
							String origin = rb.getHeaderOrigin();
							if (Tools.isNotEmpty(origin))
							{
								if ((","+accessControlAllowedOrigins+",").contains(","+origin+",")) canAccess = true;
							}
						}
					}

					if (canAccess)
					{
						response.setHeader("Access-Control-Allow-Origin", accessControlAllowOriginValue);
						setHeader(response, "Access-Control-Allow-Headers", "accessControlAllowHeaders");
						setHeader(response, "Access-Control-Allow-Methods", "accessControlAllowMethods");
						setHeader(response, "Access-Control-Max-Age", "accessControlMaxAge");
						return;
					}
				}
			}
		}
	}

	/**
	 * Nastavi HTTP hlavicku podla nazvu konfiguracnej premennej
	 * @param response
	 * @param headerName - meno HTTP hlavicky
	 * @param constantName - meno konfiguracnej premennej
	 */
	public static void setHeader(HttpServletResponse response, String headerName, String constantName)
	{
		String value = Constants.getStringExecuteMacro(constantName);
		if (Tools.isEmpty(value)) return;

		if ("&nbsp;".equals(value)) value = " ";

		//Logger.debug(SetCharacterEncodingFilter.class, "Setting header "+headerName+":"+value);

		//replace cr/lf to allow user split Content-Security-Policy values into multiple lines, as header does not allow cr/lf
		value = Tools.replace(value, "\r", " ");
		value = Tools.replace(value, "\n", " ");

		response.setHeader(headerName, value);
	}

	/**
	 * Nastavi hlavicky pre konkretne volania podla konfiguracnej premennej responseHeaders
	 * @param response
	 */
	private static void setResponseHeaders(String path, HttpServletRequest request, HttpServletResponse response) {

		if (path.startsWith("/files") || path.startsWith("/images") || path.startsWith("/shared")) {
			String lngCode = Constants.getString("defaultLanguage");
			//set last known language for files and images folder
			if (path.contains("/en/")) lngCode = "en";
			else if (path.contains("/de/")) lngCode = "de";
			else if (path.contains("/cz/")) lngCode = "cz";
			else if (path.contains("/sk/")) lngCode = "sk";
			else lngCode = PageLng.getUserLng(request);

			String isoCode = PageLng.getUserLngIso(lngCode);
			ResponseHeaderService.setContentLanguageHeader(isoCode, true, request, response);
		}

		// parse and cache
		if(responseHeaders == null) {
			synchronized (PathFilter.class) {

				String[] headers = Tools.getTokens(Constants.getString("responseHeaders"), "\n");
				if(headers.length > 0) {
					responseHeaders = new ArrayList<>();
					for(int i = 0; i < headers.length; i++) {
						responseHeaders.add(new ResponseHeaderBean(headers[i]));
					}
				}
			}
		}

		// set headers
		if(responseHeaders != null) {
			for(ResponseHeaderBean header : responseHeaders) {
				if(path.startsWith(header.getUrl())) {
					response.setHeader(header.getName(), Constants.executeMacro(header.getValue()));
				}
			}
		}

		ResponseHeaderService.setResponseHeaders(path, request, response);
	}

	/**
	 * Ak existuje _mobile.jsp verzia zadaneho JSP vykona nan interny forward a vrati true pre ukoncenie povodneho JSP, nieco ako:
	 * if (PathFilter.forwardToMobileOrTablet("/components/magzilla/new_bug_popup.jsp", pageContext)) return;
	 * @param jspFileName
	 * @param context
	 * @return
	 */
	public static boolean forwardToMobileOrTablet(String jspFileName, PageContext context)
	{
		if (BrowserDetector.isSmartphoneOrTablet((HttpServletRequest)context.getRequest())==false) return false;

		String mobileJsp = Tools.replace(jspFileName, ".jsp", "_mobile.jsp");
		if (FileTools.isFile(mobileJsp)==false) return false;

		try
		{
			context.forward(mobileJsp);
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return false;
	}

	/**
	 * Pouziva sa v spojeni s nginx cache proxy serverom, nastavuje cookie s nazvom nc ktora nasledne v
	 * dalsich http requestoch od klienta zamedzi posielaniu cache vysledkov (ak je nastavena na hodnotu 1)
	 * @param request
	 * @param response
	 */
	public static void setNginxProxyMode(HttpServletRequest request, HttpServletResponse response)
	{
		try
		{
			if (Constants.getBoolean("nginxProxyMode")==false) return;

			String actualCookieValue = Tools.getCookieValue(request.getCookies(), "nc", null);
			Cookie c = new Cookie("nc", "1");
			c.setPath("/");

			//ak potrebujeme nastavit nocache cookie
			if (isNoCacheCookieRequired(request))
			{
				//ak uz je nastavena na 1 tak ju nemusime nastavovat
				if ("1".equals(actualCookieValue)==false) {
					//session cookie
					c.setMaxAge(-1);
					//toto nesmie ist cez Tools.addCookie pretoze sa ani v pripade EU smernice nemoze vypnut
					response.addCookie(c);
				}
			}
			else
			{
				if ("1".equals(actualCookieValue)) {
					//cookie nam prisla v http requeste ale uz ju nemam setovat, takze ju dam zmazat
					c.setMaxAge(0);
					//toto nesmie ist cez Tools.addCookie pretoze sa ani v pripade EU smernice nemoze vypnut
					response.addCookie(c);
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Overi, ci je mozne pouzit nginx proxy rezim (konstanta nginxProxyMode), ak je povoleny, overuje este:
	 * prihlaseneho pouzivatela (rezim nedostupny)
	 * prepnutie verzie cez forceBrowserDetector (ak je ina verzia ako pc nedostupne)
	 * @param request
	 * @return
	 */
	public static boolean isNoCacheCookieRequired(HttpServletRequest request)
	{
		if (request.getSession()!=null)
		{
			if (request.getSession().getAttribute(Constants.USER_KEY)!=null) return true;
			else if (request.getParameter("forceBrowserDetector")!=null && "pc".equals(request.getParameter("forceBrowserDetector"))==false) return true; //NOSONAR
			else if (request.getSession().getAttribute("BrowserDetector.forceBrowserDetector")!=null) return true; //NOSONAR
		}

		return false;
	}


	/**
	 * Vrati URL adresu pre httpS presmerovanie
	 * @param request
	 * @return
	 */
	public static String getHttpsRedirectUrl(HttpServletRequest request)
	{
		String path = PathFilter.getOrigPath(request);
		String qs = request.getQueryString();
		StringBuilder url = new StringBuilder("https://").append(Tools.getServerName(request)).append(path);
		if (Tools.isNotEmpty(qs)) url.append('?').append(qs);
		return Tools.sanitizeHttpHeaderParam(url.toString());
	}

	/**
	 * Otestuje, ci je povolene showdoc volanie pre dane docId
	 * @param docId - id dokumentu
	 * @param constantName - meno konstanty
	 * @return
	 */
	private static boolean isShowDocAllowDocId(int docId, String constantName)
	{
		String showDocAllowDocIds = Constants.getString(constantName);
		if (Tools.isNotEmpty(showDocAllowDocIds))
		{
			StringTokenizer st = new StringTokenizer(showDocAllowDocIds, ",+;");
			while (st.hasMoreTokens())
			{
				int token = Tools.getIntValue(st.nextToken().trim(), -1);
				if(token == -1) continue;	//preskocim
				if(docId == token)
				{
					return true;
				}
			}
		}
		return false;
	}

	public static void resetResponseHeaders() {
		responseHeaders = null;
	}

	public static void resetCacheStaticContentSeconds()
	{
		cacheStaticContentSeconds = -1;
		cacheStaticContentSuffixes = null;
	}

	public static void resetBlockedPaths() {
		blockedPaths = null;
	}

	//pre WebJET 9 kontrolujeme pri REST volaniach CSRF token
	private boolean checkCSRFToken(String path, HttpServletRequest request) {

		//check URL against custom CSRF required URLs #57521
		boolean isCsrfRequired = DocTools.isUrlAllowed(path, "csrfRequiredUrls", false);
		if (isCsrfRequired) {
			String token = request.getParameter(CSRF.getParameterName());
            if (Tools.isEmpty(token)) token = request.getHeader("x-csrf-token");

            if (Tools.isNotEmpty(token) && CSRF.verifyTokenAjax(request.getSession(), token))
            {
                return true;
            }

			return false;
		}

		if (path.startsWith("/admin/rest/")==false || path.startsWith("/admin/rest/document/") || path.startsWith("/admin/rest/datatables/") ) {
			//taketo URL nekontrolujeme
			return true;
		}

		//ak path obsahuje vyraz html tak token nie je potrebny
		if (path.contains("/html") || path.contains("html/") || path.contains("/binary") || path.contains("binary/")) return true;

		//pouziva sa pri prihlaseni tokenom, vtedy CSRF nie je posielane
		if (request.getAttribute("csrfDisabled")!=null) return true;

		String header = request.getHeader("X-CSRF-Token");
		if (Tools.isEmpty(header)) {
			return false;
		}

		return CSRF.verifyTokenAjax(request.getSession(), header);
	}

	private static void setCustomPath(String path) {
		PathFilter.customPath = path;
	}

	/**
	 * Safely forward request dispatcher, need to use this if next statement is return
	 * eg. after checkAdmin you must return from PathFilter even when /404.jsp has compilation errors
	 * @param request
	 * @param response
	 * @param path
	 */
	private static void forwardSafely(String path, HttpServletRequest request, HttpServletResponse response){
		try {
		request.getRequestDispatcher(path).forward(request, response);
		} catch (Exception ex) {
			Logger.error(PathFilter.class, ex);
		}
	}

	/**
	 * Check if path contains blocked file or directory metadata
	 * @param path - the request path
	 * @return true if path should be blocked
	 */
	private static boolean isPathBlocked(String path) {
		if (Tools.isEmpty(path)) return false;

		try {
			if (blockedPaths == null) {
				synchronized(PathFilter.class) //NOSONAR
				{
					if (blockedPaths == null) {
						blockedPaths = Constants.getArray("pathFilterBlockedPaths");
					}
				}
			}

			for (String blockedPath : blockedPaths) {
				if (path.contains(blockedPath)) {
					Logger.debug(PathFilter.class, "Blocked path detected, path="+path+", blocked="+blockedPath);
					return true;
				}
			}
		} catch (Exception ex) {
			//failsafe to concurent modification exception, just to be sure
		}

		return false;
	}

	private static boolean isPathSafe(String path) {
		if (path == null || path.length() < 1) return true;

		if (path.indexOf('\'')!=-1 || path.indexOf('"')!=-1 || path.indexOf('\r')!=-1 || path.indexOf('\n')!=-1 ||
			path.contains("%0D") || path.contains("%0A") || path.contains("%0d") || path.contains("%0a") || //crlf utok
			path.indexOf('\\')!=-1 || path.indexOf("/../")!=-1
		) return false;

		return true;
	}
}
