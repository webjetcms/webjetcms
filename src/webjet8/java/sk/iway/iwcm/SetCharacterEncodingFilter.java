package sk.iway.iwcm;

import org.apache.commons.io.IOUtils;
import org.displaytag.filter.BufferedResponseWrapper;
import org.displaytag.filter.BufferedResponseWrapper13Impl;
import org.displaytag.filter.ExportDelegate;
import org.displaytag.tags.TableTag;
import org.displaytag.tags.TableTagParameters;
import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.common.PdfTools;
import sk.iway.iwcm.components.proxy.Proxy;
import sk.iway.iwcm.components.proxy.jpa.ProxyBean;
import sk.iway.iwcm.components.proxy.ProxyDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.WJResponseWrapper;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;
import sk.iway.iwcm.system.monitoring.MemoryMeasurement;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;
import sk.iway.iwcm.system.ntlm.NtlmLogonAction;
import sk.iway.iwcm.system.ntlm.RedirectedException;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *  Example filter that unconditionally sets the character encoding to be used
 *  in parsing the incoming request to a value specified by the <strong>encoding
 *  </string> filter initialization parameter in the web app deployment
 *  descriptor (</code>/WEB-INF/web.xml</code>). This filter could easily be
 *  extended to be more intelligent about what character encoding to set, based
 *  on characteristics of the incoming request (such as the values of the <code>Accept-Language</code>
 *  and <code>User-Agent</code> headers, or a value stashed in the current
 *  user's session).
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       Craig McClanahan
 *@version      $Revision: 1.5 $ $Date: 2003/07/29 17:13:02 $
 *@created      Nedeďż˝e, 2002, oktďż˝ber 27
 *@modified     $Date: 2003/07/29 17:13:02 $
 */

public class SetCharacterEncodingFilter extends OncePerRequestFilter
{

   // ----------------------------------------------------- Instance Variables


   /**
    *  The default character encoding to set for requests that pass through this
    *  filter.
    */
   protected static String encoding = "utf-8";

   public static final String PDF_PRINT_PARAM = "_printAsPdf";

   //customizovana error hlaska pri nedostupnosti DB spojenia pri starte servera
   private static Map<String, String> dbErrorMessageText = new Hashtable<>();
   static {
   	dbErrorMessageText.put("",  "Nastala chyba pri komunikacii s databazou, skuste sa prosim pripojit neskor.<br/>Error occured during database query, please try connecting later.");
	}


   // --------------------------------------------------------- Public Methods

   	/**
	 * Take this filter out of service.
	 */
	@Override
	public void destroy() {
		requests.clear();

		Map<EntityManagerFactory, EntityManager> map = threadEntityManagers.get();

		if (map != null) {
			map.clear();
		}
		threadEntityManagers.remove();
	}


   private static Map<Long, RequestBean> requests = new HashMap<>();

	/**
	 * ThreadLocal mapa, uchovavajuca pary EntityManagerFactory - EntityManager
	 */
   private static ThreadLocal<Map<EntityManagerFactory, EntityManager>> threadEntityManagers = new ThreadLocal<>();


   /**
    * Zaregistruje aktualny request ako RequestBean, je to public kvoli tomu, ze po prechode do 404 stranky sa filter ukonci a context removne
    * Vzdy je nasledne potrebne volat unregisterDataContext pre vycistenie hash tabulky!!
    * @param req
    */
   public static void registerDataContext(ServletRequest req)
   {
   	//iba HttpServletRequest ma session
		if (req instanceof HttpServletRequest)
		{
			HttpServletRequest request = (HttpServletRequest)req;
			registerDataContext(request);
		}
   }


   public static void registerDataContext(HttpServletRequest request)
   {
		RequestBean requestBean = new RequestBean();

		if (request != null)
		{
			//iba HttpServletRequest ma session
			HttpSession session = (request).getSession();

			String url = request.getRequestURI();
			if (ContextFilter.isRunning(request))
			{
				url = ContextFilter.removeContextPath(request.getContextPath(), url);
			}


			Identity user = UsersDB.getCurrentUser(session);
			if (user != null)
			{
				requestBean.setUserId(user.getUserId());
				requestBean.setUserAdmin(user.isAdmin());
			} else {
				requestBean.setUserId(-1);
				requestBean.setUserAdmin(false);
			}
			requestBean.setRemoteIP(Tools.getRemoteIP(request));
			requestBean.setRemoteHost(Tools.getRemoteHost(request));
			requestBean.setBaseHref(Tools.getBaseHref(request));
			requestBean.setLng(PageLng.getUserLng(request));
			requestBean.setUrl(url);
			requestBean.setQueryString(request.getQueryString());
			requestBean.setSessionId(session.getId());
			requestBean.setParameters(request.getParameterMap());
			requestBean.setHeaderOrigin(request.getHeader("origin"));
			requestBean.setReferrer(request.getHeader("referer"));
			requestBean.setCryptoPrivateKey((String)Tools.sessionGetAttribute(session, "JPACryptoConverter.privateKey"));

			if (Constants.getServletContext().getAttribute("springContext") != null) {
				requestBean.setSpringContext((ApplicationContext) Constants.getServletContext().getAttribute("springContext"));
			}
			else
            {
                requestBean.setSpringContext(null);
            }

			String ua = request.getHeader("User-Agent");
			if (ua == null) ua = "unknown";
			requestBean.setUserAgent(ua);
			if (InitServlet.isTypeCloud())
			{
				requestBean.setDomain(Tools.getServerName(request));
			} else
			{
				requestBean.setDomain(DocDB.getDomain(request));
			}
			requestBean.setServerName(request.getServerName());
			requestBean.setHttpProtocol(Tools.isSecure(request) ? "https" : "http");
			requestBean.setHttpPort(request.getServerPort());

			String path = request.getRequestURI();
			if (ContextFilter.isRunning(request)) path = ContextFilter.removeContextPath(request.getContextPath(), path);

			if (path.startsWith("/admin/elfinder-connector/") || path.equals("/admin/elFinder/gethash.jsp"))
			{
				requestBean.setRequest(request);
			}
		}

		requests.put(Thread.currentThread().getId(), requestBean);
   }

   /**
    * Odregistrovanie RequestBeanu, musi sa volat ak zavolate register (napr. v 404 stranke)
    */
   public static void unRegisterDataContext()
   {
		//requests.remove(Thread.currentThread().getId());
		closeEntityManagers();
   }

   /**
    * inicializacia mapy EntityManager-ov
    */
   private static void initializeEntityManagerMap()
   {
		Map<EntityManagerFactory, EntityManager> map = threadEntityManagers.get();

		if (map == null)
		{
			map = new ConcurrentHashMap<>();
			threadEntityManagers.set(map);
		}
   }

   /**
    * upratanie pouzitych EntityManager-ov
    */
   private static void closeEntityManagers()
   {
		Map<EntityManagerFactory, EntityManager> map = threadEntityManagers.get();

		if (map == null)
		{
			// ziadne nemame
			return;
		}

		threadEntityManagers.remove();

		for (EntityManager entityManager : map.values())
		{
			EntityTransaction transaction =	entityManager.getTransaction();

			// ak niekto nechal otvorene transakcie, spravime rollback
			if (transaction != null)
			{
				if (transaction.isActive())
					transaction.rollback();
			}
			if (entityManager.isOpen())
			{
				entityManager.close();
			}
		}
   }

   public static void printDbErrorMessage(String path, HttpServletRequest request, HttpServletResponse res) throws IOException
   {
        Logger.println(SetCharacterEncodingFilter.class, "Printing DB error message");
        res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		res.setContentType("text/html; charset=" +getEncoding());

		String key = "";

		try
		{
			if (path!=null && path.length()>2 && path.startsWith("/"))
			{
				int i = path.indexOf("/", 1);
				if (i>2)
				{
					key = path.substring(1, i);
					Logger.debug(SetCharacterEncodingFilter.class, "key="+key+" path="+path);
				}
			}
		}
		catch (Exception ex)
		{
		    sk.iway.iwcm.Logger.error(ex);
		}

		String message = dbErrorMessageText.get(key);
		if (message==null) message = dbErrorMessageText.get("");

		message = Tools.replace(message, "!BASE_HREF!", Tools.getBaseHref(request));
		message = Tools.replace(message, "!DOMAIN_NAME!", DocDB.getDomain(request));

	   	//Logger.debug(SetCharacterEncodingFilter.class, message);
		res.getWriter().print(message);
   }

   /**
    *  Select and set (if specified) the character encoding to be used to
    *  interpret request parameters for this request.
    *
    *@param  request               The servlet request we are processing
    *@param  chain                 The filter chain we are processing
    *@param  response              Description of the Parameter
    *@exception  IOException       if an input/output error occurs
    *@exception  ServletException  if a servlet error occurs
    */
   @Override
   public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
         FilterChain chain)
          throws IOException, ServletException
   {
	String KEY = "SetCharacterEncodingFilter.doFilter";
	if (request.getAttribute(KEY) != null) {
		chain.doFilter(request, response);
		return;
	}
	request.setAttribute(KEY, "1");
	//Logger.debug(SetCharacterEncodingFilter.class, "doFilterInternal, url="+request.getRequestURI());

   	long startTime = System.currentTimeMillis();
   	MemoryMeasurement memoryConsumed = new MemoryMeasurement();
   	try
   	{
   		String path = request.getRequestURI();
   		String pathNoContext = path;
		if (ContextFilter.isRunning(request)) pathNoContext = ContextFilter.removeContextPath(request.getContextPath(), path);

		if (Logger.isLevel(Logger.DEBUG)) {
			if (pathNoContext != null && pathNoContext.indexOf("/images/") == -1 && pathNoContext.indexOf("/css/") == -1 && pathNoContext.indexOf("/scripts/") == -1 && pathNoContext.endsWith(".gif") == false && pathNoContext.endsWith(".png") == false && pathNoContext.startsWith("/admin/refresher.jsp") == false && pathNoContext.startsWith("/components/messages/refresher-ac.jsp") == false) {
				Logger.debug(SetCharacterEncodingFilter.class, request.getMethod() + " " + DocDB.getDomain(request) + path + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
			}
		}

		if (pathNoContext == null) {
			pathNoContext = "/";
		}

		//POZOR PRED TYMTO MIESTOM NESMIE BYT ZIADNE CITANIE PARAMETROV!!!!!!!
	    // Select and set (if needed) the character encoding to be used
	    String currentEncoding = selectEncoding(request);
	    if (currentEncoding != null)
	    {
	    	request.setCharacterEncoding(currentEncoding);
	    }

	    registerDataContext(request);
	   	//System.out.println("SetCharacterEncodingFilter.doFilter");

		if (pathNoContext.startsWith("/wjerrorpages/")
						|| ( InitServlet.isWebjetInitialized()==false && (pathNoContext.startsWith("/templates/")) )
						|| ( InitServlet.isWebjetInitialized()==false && (pathNoContext.startsWith("/components/_common/combine.jsp")) )
		) {
			Constants.setServletContext(request.getServletContext());
			request.getRequestDispatcher(pathNoContext).forward(request, response);
			return;
		}

   		if (InitServlet.isWebjetInitialized()==false)
   		{
   			printDbErrorMessage(pathNoContext, request, response);

   			return;
   		}

	      //aby mem.jsp bolo dostupne v kazdej situacii bez ohladu na DB spojenia
	      if (pathNoContext.startsWith("/admin/mem2.jsp"))
	      {
	      	request.getRequestDispatcher(pathNoContext).forward(request, response);
	      	return;
	      }

	      //toto bypasne pathFilter
			if (PathFilter.bypassPath(path, request))
			{
				Logger.debug(SetCharacterEncodingFilter.class, "PathFilter.bypass: " + path);
				request.setAttribute("PathFilter.bypass", "true");
				chain.doFilter(request, response);
				return;
			}

			ProxyDB proxyDB = null;
			try
			{
				proxyDB = ProxyDB.getInstance();
			}
			catch (NullPointerException npe)
			{
				//nastala chyba pripojenia do DB
   				printDbErrorMessage(pathNoContext, request, response);
   			return;
			}

			Identity user = (Identity)request.getSession().getAttribute(Constants.USER_KEY);
			if (AuthenticationFilter.weTrustIIS() && Tools.getUserPrincipal(request) != null &&
						Tools.isNotEmpty(Tools.getUserPrincipal(request).getName()))
			{
				if (isLoggedAsSomeoneElse(request, user))
				{
					Logger.debug(getClass(), "Redirecting user - logged in ambigously - " + user.getLogin());
					String pathQS = path;
					if (Tools.isNotEmpty(request.getQueryString())) pathQS = path + "?" + request.getQueryString();
					response.sendRedirect( Tools.addParameterToUrlNoAmp(AuthenticationFilter.getForbiddenURL(), "origUrl", pathQS));
					LogonTools.setUserToSession(request.getSession(), null);
					return;
				}

				if (user == null)
				{
					//skontroluj, ci nie sme nahodou forbidden stranka
					String pathQS = path;
					if (Tools.isNotEmpty(request.getQueryString())) pathQS = path + "?" + request.getQueryString();

					//ak sa nejedna o forbidden stranku, skus prihlasenie
					if (pathQS.equals(AuthenticationFilter.getForbiddenURL())==false)
					{
						try
						{
							Logger.debug(SetCharacterEncodingFilter.class, "Actual URL: " + pathQS + " forbiddenURL: " + AuthenticationFilter.getForbiddenURL());

							user = logUserInViaNtlm(request, response);
						}
						catch (RedirectedException re)
						{
							Logger.debug(SetCharacterEncodingFilter.class, "after logUserInViaNtlm, re="+re.getMessage());
							sk.iway.iwcm.Logger.error(re);
							//je nastaveny redirect, takze sa nemoze pokracovat
							return;
						}
					}
				}
				//ak si stale null, tak vypis nam informacie
				if (user == null)
				{
					Logger.debug(getClass(), "Automatic NtlmLogin unsuccessful");
				}
			}

			ProxyBean proxy = proxyDB.getProxy(pathNoContext);
			if (proxy != null)
			{
				Logger.debug(SetCharacterEncodingFilter.class, "Forwarding to proxy: "+proxy.getRemoteServer());

				Proxy.service(proxy, request, response);

				return;
			}

			//ochrana pred priamym volanim Stripes action metod
			if (Constants.getBoolean("stripesEnableDirectActionCall")==false && pathNoContext.endsWith(".action"))
			{
				if (pathNoContext.indexOf("Ajax.action")!=-1||pathNoContext.indexOf("Download.action")!=-1||pathNoContext.indexOf("Upload.action")!=-1)
				{
					//toto su defaultne povolene akcie
				}
				else
				{
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					request.getRequestDispatcher("/403.jsp").forward(request, response);
					return;
				}
			}

			//nastavenie security headrov
			setCommonHeaders(response, request);

			if ("true".equals(request.getParameter(PDF_PRINT_PARAM)) && "true".equals(request.getParameter(PDF_PRINT_PARAM+"No"))==false)
	      {
	         Logger.debug( SetCharacterEncodingFilter.class, "------------> ROBIM EXPORT PRE PDF");

	         WJResponseWrapper wrapper = new WJResponseWrapper(response,request);

	         wrapper.setCharacterEncoding(currentEncoding);

	         chain.doFilter(request, wrapper);

	         //ExportDelegate.writeExport((HttpServletResponse) servletResponse, servletRequest, wrapper);
	         String content = wrapper.strWriter.getBuffer().toString();
	         Logger.debug( SetCharacterEncodingFilter.class, content);

	         Logger.debug(SetCharacterEncodingFilter.class, "response is commited: "+response.isCommitted());

	         // clear headers
	         if (!response.isCommitted())
	         {
	             response.reset();
	         }

	         //content = CoBrand.getCleanBodyIncludeStartEnd(content, "<body", "</body>");

	         response.setContentType("application/pdf");
			 if ("true".equals(request.getParameter(PDF_PRINT_PARAM+"NoAttachment"))==false) {
				StringBuilder fileName = new StringBuilder("export.pdf");
				try
				{
					int docId = DocDB.getDocIdFromURL(path, DocDB.getDomain(request));
					if(docId > 0)
						request.setAttribute("docId", Integer.toString(docId));

					String pathTmp = path.endsWith("/") ? path.substring(0, path.length()-1) : path;
					fileName = new StringBuilder(pathTmp.substring(pathTmp.lastIndexOf('/')+1));
					if (fileName.indexOf(".")!=-1) fileName = new StringBuilder(fileName.substring(0, fileName.lastIndexOf("."))).append(".pdf");
					else fileName.append(".pdf");
				} catch (Exception ex) {}

				response.setHeader("Content-Disposition", "attachment; filename=" + fileName.toString());
			 }

	         setCommonHeaders(response, request);

	         //ak to islo napriamo tak to zhadzovalo IIS ISAPI filter!!!
	         ByteArrayOutputStream baos = new ByteArrayOutputStream();
	         PdfTools.renderHtmlCode(content, baos, request);

	         response.setContentLength(baos.size());
	         response.getOutputStream().write(baos.toByteArray());
	         response.getOutputStream().flush();
	         response.getOutputStream().close();

	         return;
	      }
			else if (request.getParameter(TableTagParameters.PARAMETER_EXPORTING) != null)
	      {
			Logger.debug( SetCharacterEncodingFilter.class, "------------> ROBIM EXPORT PRE DISPLAYTAG");

	         BufferedResponseWrapper wrapper = new BufferedResponseWrapper13Impl(response);

	         Map<String, Boolean> contentBean = new HashMap<>(4);
	         contentBean.put(TableTagParameters.BEAN_BUFFER, Boolean.TRUE);

	         request.setAttribute(TableTag.FILTER_CONTENT_OVERRIDE_BODY, contentBean);

	         //WebSphere toto nevie...
	         wrapper.setCharacterEncoding(currentEncoding);

	         chain.doFilter(request, wrapper);

	         ExportDelegate.writeExport(response, request, wrapper);

	      }
			else
	      {
		      try
		      {
		         //zaloguj start
		         //Logger.println(this,"Editor start");
		         java.util.Date startDate = new java.util.Date();

		         request.setAttribute("generationStartDate", startDate);
		      }
		      catch (Exception ex)
		      {

		      }

		      // Pass control on to the next filter
		      chain.doFilter(request, response);
	      }

			long timeTaken = System.currentTimeMillis() - startTime;
			if(Constants.getBoolean("serverMonitoringEnablePerformance"))
				savePerformanceMeasure(request, timeTaken, memoryConsumed.diff());
   	}
   	finally
   	{
   		//to robi niekedy problem aj mimo cloudu, takze racej rusim if (InitServlet.isTypeCloud()==false) requests.remove(Thread.currentThread().getId());
   		closeEntityManagers();
   		//kontrola serializovatelnosti session
			if(Constants.getBoolean("enableSessionSerializableCheck") && Tools.getServerName(request).indexOf("iwcm.interway.sk")!= -1)
			{
				try
				{
					checkSessionSerializable(request);
				}
				catch (ObjectNotSerializableException e)
				{
				    if(!e.getMessage().endsWith("editorForm"))
                    {
                        sk.iway.iwcm.Logger.error(e);
                    }
                    else
                    {
                        Logger.debug(this,"Error: "+e.getMessage());
                    }
				}
			}
   	}
   }

   /**
    * Nastavi secutiry hlavicky na response
    * @param res
    * @param req
    */
   public static void setCommonHeaders(HttpServletResponse res, HttpServletRequest req)
   {
		String path = PathFilter.getOrigPath(req);
		if (path == null)
		{
			path = req.getRequestURI();
			if (Tools.isNotEmpty(req.getContextPath()))
			{
				path = path.substring(req.getContextPath().length());
			}
		}

   	PathFilter.setHeader(res, "X-Frame-Options", "xFrameOptions");
		PathFilter.setAccessControlAllowOrigin(path, res);
		PathFilter.setHeader(res, "X-XSS-Protection", "xXssProtection");
		PathFilter.setHeader(res, "Server", "serverName");
		if (Tools.isSecure(req))
		{
			PathFilter.setHeader(res, "Strict-Transport-Security", "strictTransportSecurity");
		}
		PathFilter.setHeader(res, "X-Content-Type-Options", "xContentTypeOptions");
		if (path != null && path.toLowerCase().endsWith(".svg")) {
			//pre SVG mame separe CSP hlavicky
			PathFilter.setHeader(res, "Content-Security-Policy", "contentSecurityPolicySvg");
		} else {
			PathFilter.setHeader(res, "Content-Security-Policy", "contentSecurityPolicy");
		}
		PathFilter.setHeader(res, "Referrer-Policy", "refererPolicy");

		PathFilter.setFeaturePolicy(res);

		//overi, ci nemame nejaky displaytag parameter, ak ano generujme noindex, nofollow hlavicku
		Enumeration<String> params = req.getParameterNames();
		while (params.hasMoreElements())
		{
			String name = params.nextElement();
			if (name.startsWith("d-"))
			{
				//name.endsWith("-p") - toto nedavame, to je strankovanie a to chceme indexovat
				if (name.endsWith("-s") || name.endsWith("-o"))
				{
					//#35701
					PathFilter.setXRobotsTagValue("NOT_SEARCHABLE_PAGE", res);
					break;
				}
			}
		}

   }

	/**
	 * Skontroluje su objekty v session serializovatelne
	 * @param servletRequest
	 * @throws ObjectNotSerializableException
	 */
   private void checkSessionSerializable(ServletRequest servletRequest) throws ObjectNotSerializableException
	{
		if (servletRequest instanceof HttpServletRequest) {
		   HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		   HttpSession session = httpRequest.getSession(false);
		   if (session != null) {
		       boolean serializable = true;
		       StringBuilder items = new StringBuilder();
		       Enumeration<String> names = session.getAttributeNames();
		       while (names.hasMoreElements()) {
		           String attrName = names.nextElement();
		           if ("sessionCssParsed".equals(attrName) || "editorForm".equals(attrName) || "ShowdocAction.showDocData".equals(attrName)) continue;
		           boolean attributeSerializable = serialize(
		                   attrName, Tools.sessionGetAttribute(session, attrName)
		           );
		           if (!attributeSerializable) {
		               if (items.length() > 0) {
		                   items.append(", ");
		               }
		               items.append(attrName);
		           }
		           serializable &= attributeSerializable;
		       }

		       if (!serializable) {
		           throw new ObjectNotSerializableException(
		                   "These objects stored in session attributes " +
		                       "are not serializable (see detailed log): " +
		                       items
		           );
		       }
		   }
        }
	}

   /**
    * Serializes object into byteArray
    *
    * @param attrName
    * @param object
    * @return
    */
   private boolean serialize(String attrName, Object object) {
       ByteArrayOutputStream bos = null;
       ObjectOutput out = null;
       try{
           bos = new ByteArrayOutputStream();
           out = new ObjectOutputStream(bos);
           out.writeObject(object);
           return true;
       } catch (IOException ex) {
           String msg = "Failed to serialize attribute: " + attrName;
           Logger.debug(SetCharacterEncodingFilter.class, msg + ": " +ex.getMessage());
           return false;
       } finally {
           IOUtils.closeQuietly(bos);
           if (out != null) {
               try { out.close(); } catch (IOException ignored) {}
           }
       }
   }

	private void savePerformanceMeasure(HttpServletRequest request, long timeTakenToExecute, long memoryDiff)
	{
		if (!isRequestAimedOnHtmlPage(request))
			return;

		StringBuilder measurementKey = new StringBuilder(request.getRequestURI());
		appendQueryStringIfNecessary(measurementKey, request);
		ExecutionTimeMonitor.recordDocumentExecution(measurementKey.toString(), timeTakenToExecute, memoryDiff);
	}

	private boolean isRequestAimedOnHtmlPage(HttpServletRequest request)
	{
		String uri = request.getRequestURI();

		if (uri == null)
			return false;

		return uri.contains("showdoc.do") || uri.contains(".html") || uri.endsWith("/");
	}

	private void appendQueryStringIfNecessary(StringBuilder measurementKey, HttpServletRequest request)
	{
		if (Tools.isEmpty(request.getQueryString()))
			return;

		String uri = request.getRequestURI();
		String queryString = request.getQueryString();
		if (Constants.getBoolean("appendQueryStringWhenMonitoringDocuments"))
		{
			measurementKey.append(queryString);
		}
		else if (uri.contains("showdoc.do"))
		{
			Matcher docIdMatcher = Pattern.compile("docid=([0-9]*)").matcher(queryString); //NOSONAR

			if (docIdMatcher.find())
			{
				measurementKey.append('?').append("docid=").append(docIdMatcher.group(1));
			}
		}
	}

	/**
    *  Place this filter into service.
    *
    * @exception  ServletException  Description of the Exception
    */
	@Override
   public void initFilterBean() throws ServletException
   {
		try
		{
			//skus to precitat zo suboru
			File dir = new File(getServletContext().getRealPath("/wjerrorpages/"));
			if (dir!=null && dir.exists() && dir.canRead())
			{
				for (File f : dir.listFiles())
					{
						//podpora pre dberror-en.html atd
						if (f.isFile() && f.getName().startsWith("dberror"))
						{
							String key = "";
							int dot = f.getName().indexOf(".");
							int i = f.getName().indexOf("-");
							if (i > 0 && dot > i) key = f.getName().substring(i+1, dot);

							try
							{
								StringBuilder contextFile = new StringBuilder();
								InputStreamReader isr = new InputStreamReader(new FileInputStream(f), SetCharacterEncodingFilter.encoding);
								char[] buff = new char[64000];
								int len;
								while ((len = isr.read(buff)) != -1)
								{
									contextFile.append(buff, 0, len);
								}
								isr.close();

								SetCharacterEncodingFilter.dbErrorMessageText.put(key, contextFile.toString());
							}
							catch (Exception ex)
							{

							}
						}
					}
			}
			else
			{

			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
   }


   // ------------------------------------------------------ Protected Methods

   /**
    *  Select an appropriate character encoding to be used, based on the
    *  characteristics of the current request and/or filter initialization
    *  parameters. If no character encoding should be set, return <code>null</code>
    *  . <p>
    *
    *  The default implementation unconditionally returns the value configured
    *  by the <strong>encoding</strong> initialization parameter for this
    *  filter.
    *
    *@param  request  The servlet request we are processing
    *@return          Description of the Return Value
    */
   public static String selectEncoding(ServletRequest request)
   {
      try
      {
         //quick hack pre HP...
         HttpServletRequest req = (HttpServletRequest)request;

         //kvoli ajax requestom, ktore musia ist cez utf-8, riesenie je nazvat subory, na ktore idu ajax requesty pomocou ajax_utf-8
         // a takisto sa to vola aj ked sa uklada web stranka, lebo to od WJ7 ide cez ajax, aby ukladalo aj diakritiku
         if ("utf-8".equals(SetCharacterEncodingFilter.encoding)==false
         			&& (
	         			req.getRequestURI().indexOf("ajax_utf-8") != -1 ||
	         			req.getRequestURI().indexOf("editor.do") != -1 ||
	         			req.getRequestURI().indexOf("editor_component.jsp") != -1 ||
	         			(req.getQueryString()!=null && req.getQueryString().indexOf("ajax_utf-8") != -1)
         			)
         	)
         {
         	//Logger.println(this,"SCEF: utf8");
         	req.setAttribute("SetCharacterEncodingFilter.encoding", "utf-8");
            return("utf-8");
         }
         req.setAttribute("SetCharacterEncodingFilter.encoding", SetCharacterEncodingFilter.encoding);
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
      return (SetCharacterEncodingFilter.encoding);
   }

   public static String getEncoding()
   {
   	return(encoding);
   }

   	public static RequestBean getCurrentRequestBean()
	{
   		try
		{
   			return requests.get(Thread.currentThread().getId());
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return null;
	}

	/**
	 * Set RequestBean for current thread, USE WITH CAUTION!
	 * Only if you know what you are doing - usualy in async processing/future tasks.
	 * @param requestBean
	 */
	public static void setCurrentRequestBean(RequestBean requestBean)
	{
		try
		{
			Logger.debug("Setting request bean to thread: "+Thread.currentThread().getId());
			requests.put(Thread.currentThread().getId(), requestBean);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

   private boolean isLoggedAsSomeoneElse(HttpServletRequest request, Identity user)
	{
		if (Tools.getUserPrincipal(request) == null || user == null)
			return false;
		String userNameFromPrincipal = parseUserNameFromPrincipal(request);
		String userNameOfLoggedUser = user.getLogin();
		return !userNameFromPrincipal.equalsIgnoreCase(userNameOfLoggedUser);
	}

	private String parseUserNameFromPrincipal(HttpServletRequest req)
	{
		try
		{
			String principalName = Tools.getUserPrincipal(req).getName();
			//Logger.debug(getClass(), "Parsing login from user principal...["+principalName+"]");
			String userNameFromPrincipal = principalName.substring( principalName.indexOf('\\') + 1 );
			///userNameFromPrincipal = userNameFromPrincipal.substring(0, userNameFromPrincipal.length());
			//Logger.debug(getClass(), "Parsed '"+userNameFromPrincipal+"'");
			return userNameFromPrincipal;
		}
		catch (RuntimeException e) {
			//Logger.println(getClass(), "Nastala chyba pri spracovavani user principal-u: "+e.getMessage());
			return "";
		}
	}

	private Identity logUserInViaNtlm(HttpServletRequest request, HttpServletResponse response) throws RedirectedException
	{
		try
		{
			Logger.debug(SetCharacterEncodingFilter.class, "logUserInViaNtlm() START");
			String login = AuthenticationFilter.negotiateIIS(request, response, false).getName();
			Logger.debug(getClass(), "NtlmPasswordAuthentication.getUsername() => " + login );
			UserDetails loadedUser = NtlmLogonAction.authentificateUserAgainstLdap(request, response, login);

			String password = loadedUser.getPassword();
			if (Constants.getBoolean("passwordUseHash"))
			{
				//v BasicNtlmLogonAction to takto nastavime a ulozime, kedze heslo realne nevieme
				password = loadedUser.getLogin();
				Logger.debug(NtlmLogonAction.class, "Pass use hash, nastavujem na "+password);
			}

			if (loadedUser.isAdmin()==false || "public".equals(Constants.getString("clusterMyNodeType")))
			{
				LogonTools.logonUser(request, loadedUser.getLogin(), password);
			}
			else
			{
				Identity user = new Identity(loadedUser);
				Map<String, String> errors = new Hashtable<>();
				LogonTools.logon(loadedUser.getLogin(), password, user, errors, request, Prop.getInstance(request));

				Logger.debug(SetCharacterEncodingFilter.class, "errors.ERROR_KEY="+errors.get("ERROR_KEY"));

				if (errors.get("ERROR_KEY")!=null)
				{
					user = null;
				}
				else
				{
					// Save our logged-in user in the session
					user.setValid(true);
					LogonTools.setUserToSession(request.getSession(), user);
				}
			}

		}
		catch (RedirectedException re)
		{
			Logger.debug(SetCharacterEncodingFilter.class, "ERROR: RedirectedException "+re.getMessage());
			sk.iway.iwcm.Logger.error(re);
			throw new RedirectedException();
		}
		catch (Exception e)
		{
			Logger.debug(SetCharacterEncodingFilter.class, "ERROR: Exception "+e.getMessage());
			sk.iway.iwcm.Logger.error(e);
			//nepodarilo sa prihlasenie - tok ostane rovnaky, ako predtym
		}

		Identity loggedUser = UsersDB.getCurrentUser(request);

		Logger.debug(SetCharacterEncodingFilter.class, "logUserInViaNtlm() END, user="+loggedUser);

		return loggedUser;
	}


	/**
	 * Vrati EntityManager pre zadany nazov DB spojenia (v povodnom JPA to je persistenceUnit)
	 *
	 * @param dbName - nazov DB spojenia
	 * @return EntityManager alebo null, ak pre DB spojenie neexistuje EntityManagerFactory
	 */
	public static EntityManager getEntityManager(String dbName)
	{
		EntityManagerFactory factory = DBPool.getEntityManagerFactory(dbName);

		if (factory == null)
		{
			  Logger.error(SetCharacterEncodingFilter.class, "Nenajdeny EntityManagerFactory pre DB spojenie '"+dbName+"'");
			  return null;
		}

		return getEntityManager(factory);
	}

	/**
	 * Vrati EntityManager pre defaultny nazov DB spojenia ("iwcm")
	 * @return an @EntityManager
	 */
	public static EntityManager getEntityManager()
	{
		return getEntityManager("iwcm");
	}

	/**
	 * Ziska EntityManager pre zadany EntityManagerFactory
	 *
	 * Ak sa EntityManager nachadza v thread mape, tak ho vrati z mapy, inak vytvori novy Entity Manager pouzitim
	 * zadaneho EntityManagerFactory a ulozi ho do thread mapy.
	 *
	 * @param factory - EntityManagerFactory
	 * @return EntityManager
	 */
	private static EntityManager getEntityManager(EntityManagerFactory factory)
	{
   	Map<EntityManagerFactory, EntityManager> map = threadEntityManagers.get();

		EntityManager entityManager = null;

		if (map == null)
		{
	   	initializeEntityManagerMap();
	   	map = threadEntityManagers.get();
	   	if(map == null)
	   	{
				Logger.error(SetCharacterEncodingFilter.class, "CHYBA pri inicializaci, mapy ThreadEntityManagers. Mapa nie je inicializovana, vraciam NULL!!!");
	   		return null;
	   	}
		}

		entityManager = map.get(factory);

		if (entityManager == null || entityManager.isOpen()==false)
		{
			entityManager = factory.createEntityManager();
			map.put(factory, entityManager);
		}

		return entityManager;
	}
}

