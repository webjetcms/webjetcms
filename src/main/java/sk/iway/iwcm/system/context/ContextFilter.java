package sk.iway.iwcm.system.context;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DebugTimer;

/**
 *  ContextFilter.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.7.2012 9:21:26
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContextFilter implements Filter
{
	FilterConfig filterConfig;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
   {
		this.filterConfig = filterConfig;
   }

	@Override
	public void destroy()
   {
	//
   }

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
	{
		boolean debug = Constants.getBoolean("contextPathDebug");

		String path = null;
		try
		{
			HttpServletRequest req = (HttpServletRequest) servletRequest;
			//req.getSession().setAttribute("websphere", "1");

			String contextPathAdmin = Constants.getString("contextPathAdmin");
			if (isRunning(req)==false && Tools.isEmpty(contextPathAdmin))
			{
				chain.doFilter(servletRequest, servletResponse);
				return;
			}

			HttpServletResponse res = (HttpServletResponse) servletResponse;
			path = req.getRequestURI();

			//pred bypass NESMIE byt citany ziaden parameter!!!
			if ("true".equals(servletRequest.getAttribute("PathFilter.bypass")) || path.indexOf("/mp4streaminghttp/")!=-1 || path.indexOf("/flvstreaminghttp/")!=-1)
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

			if (path.endsWith("/") || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".jsp") || path.endsWith(".do") || path.endsWith(".html") || path.endsWith(".action") || path.endsWith(".aspx") || path.endsWith(".asp") || path.endsWith(".php"))
			{
				ContextResponseWrapper wrapper = new ContextResponseWrapper(res,req);
				if (debug) Logger.debug(ContextFilter.class, "Changing URL's in path:"+path+" ORIG SESSION ID="+req.getSession().getId());

				if (isRunning(req))
				{
					//if (debug) Logger.debug(ContextFilter.class, "Creating request wrapper, session="+req.getSession().getId()+" path="+path);

					//bezime na nejakom kontexte, musime ho pridat
					ContextRequestWrapper contextRequest = new ContextRequestWrapper(req);

					//if (debug) Logger.debug(ContextFilter.class, "docid="+contextRequest.getParameter("docid")+" req1="+ ((HttpServletRequest)contextRequest).getParameter("docid")+" req2="+((ServletRequest)contextRequest).getParameter("docid"));

					chain.doFilter(contextRequest, wrapper);

					doFilterAddContextPathImpl(contextRequest, res, path, wrapper, debug);
				}
				else
				{
					//sme ROOT kontext, ale admin cast bezi na nejakom kontexte, musime ho odstranit z URL adries
					chain.doFilter(req, wrapper);

					doFilterRemoveContextPathImpl(req, res, path, contextPathAdmin, wrapper, debug);
				}

			}
			else
			{
				chain.doFilter(servletRequest, servletResponse);
			}
		}
		catch (Exception e)
		{
			Logger.println(ContextFilter.class, "CHYBA URL:"+path);
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Prida k URL adresam, presmerovaniam atd kontext path
	 * public je to kvoli 404.jsp
	 * @param req
	 * @param res
	 * @param path
	 * @param wrapper
	 */
	public static void doFilterAddContextPathImpl(HttpServletRequest req, HttpServletResponse res, String path, ContextResponseWrapper wrapper, boolean debug)
	{
		try
		{
			/*
			Collection<String> cookieHeaders = res.getHeaders("Set-Cookie");
			Logger.debug(ContextFilter.class, "22222222222222 ORIG RESPONSE cookies="+cookieHeaders);
			for (String value : cookieHeaders)
			{
				Logger.debug(ContextFilter.class, "22222222222222 ORIG RESPONSE cookie HEADER, value="+value);
				if (value.contains("JSESSIONID"))
				{
					//Logger.debug(ContextFilter.class, "ADDING ORIG RESPONSE cookie HEADER, value="+value);
					//response.addHeader("Set-Cookie", value);
				}
			}
			*/

			if (wrapper.getRedirectURL() != null)
			{
				writeCookies(wrapper, res, req);

				if (wrapper.getRedirectURL().startsWith(req.getContextPath()) || wrapper.getRedirectURL().startsWith("http") ||
					 wrapper.getRedirectURL().startsWith("/")==false)
				{
					//pre /wj7/showdoc.do, http://www.sme.sk, savedir.do
					if (debug) Logger.debug(ContextFilter.class, "Redirect URL:"+wrapper.getRedirectURL());
					res.sendRedirect(wrapper.getRedirectURL());
				}
				else
				{
					if (Tools.isSecure(req))
					{
						String redirUrl = wrapper.getRedirectURL();
						if (redirUrl.startsWith("http")==false) redirUrl = Tools.getBaseHref(req) + redirUrl;

						if (debug) Logger.debug(ContextFilter.class, "Redirect URL addcontext:"+redirUrl);
						res.sendRedirect(redirUrl);
					}
					else
					{
						if (debug) Logger.debug(ContextFilter.class, "Redirect URL addcontext:"+req.getContextPath()+wrapper.getRedirectURL());
						res.sendRedirect(req.getContextPath()+wrapper.getRedirectURL());
					}
				}
			}
			else
			{
				try
				{
					//Logger.debug(ContextFilter.class, "222222222222222 ORIG RESPONSE HEADER names="+res);
					//Logger.debug(ContextFilter.class, "222222222222222 ORIG RESPONSE HEADER names="+res.getHeaderNames());

					//zakomentovane kvoli WebSphere, pretoze to resetlo aj hlavicky a potom sa neobjavila JSESSIONID hlavicka korektne
					//res.resetBuffer();
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}

				DebugTimer dt = new DebugTimer("replacing url: "+path);

				if (wrapper.getCharacterEncoding()!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting characterEncoding:"+wrapper.getCharacterEncoding());
					res.setCharacterEncoding(wrapper.getCharacterEncoding());
				}
				if (wrapper.getContentType()!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting contentType:"+wrapper.getContentType());
					if (wrapper.getContentType().indexOf("charset=")==-1)
					{
						res.setContentType(wrapper.getContentType()+"; charset=" + SetCharacterEncodingFilter.selectEncoding(req));
					}
					else
					{
						res.setContentType(wrapper.getContentType());
					}
				}
				if (wrapper.getLocale()!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting locale:"+wrapper.getLocale());
					res.setLocale(wrapper.getLocale());
				}
				//TODO: zamysliet sa nad tym, co s ostanymi hlavickami
				if (wrapper.getHeader("Location")!=null)
				{
					String redirect = wrapper.getHeader("Location");
					if (redirect.toLowerCase().startsWith("http")==false && redirect.startsWith(req.getContextPath())==false) redirect = req.getContextPath()+redirect;
					if (debug) Logger.debug(ContextFilter.class, "Setting Location:"+redirect);

					//odstran dvojity context path
					redirect = Tools.replace(redirect, req.getContextPath()+req.getContextPath(), req.getContextPath());

					res.setHeader("Location", redirect);
				}
				setHeader("Content-Disposition", debug, wrapper, res);
				setHeader("X-Robots-Tag", debug, wrapper, res);
				setHeader("X-UA-Compatible", debug, wrapper, res);
				setHeader("Content-Encoding", debug, wrapper, res);
				setHeader("Cache-Control", debug, wrapper, res);
				setHeader("Pragma", debug, wrapper, res);
				setHeader("Expires", debug, wrapper, res);

				writeCookies(wrapper, res, req);

				if (wrapper.getErrorCode()!=0)
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting error:"+wrapper.getErrorCode()+" message="+wrapper.getErrorMessage());
					if (wrapper.getErrorMessage()==null) res.sendError(wrapper.getErrorCode());
					else res.sendError(wrapper.getErrorCode(), wrapper.getErrorMessage());
				}
				else if (wrapper.getStatusCode()!=0)
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting status:"+wrapper.getStatusCode());
					res.setStatus(wrapper.getStatusCode());
				}
				else
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting status 200");
					res.setStatus(HttpServletResponse.SC_OK);
				}


				wrapper.writeResponseToOriginalOutput(req, false);

				if (debug) dt.diff("done");
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private static void setHeader(String name, boolean debug, ContextResponseWrapper wrapper, HttpServletResponse res)
	{
		if (wrapper.getHeader(name)!=null)
		{
			if (debug) Logger.debug(ContextFilter.class, "Setting "+name+":"+wrapper.getHeader(name));
			res.setHeader(name, wrapper.getHeader(name));
		}
	}

	/**
	 * Tato cast sa pouziva, ak admin cast je na nejakom kontexte a public cast je ako root aplikacia, je to tak v TB
	 * @param req
	 * @param res
	 * @throws IOException
	 * @throws ServletException
	 */
	private void doFilterRemoveContextPathImpl(HttpServletRequest req, HttpServletResponse res, String path, String contextPathAdmin, ContextResponseWrapper wrapper, boolean debug)
	{
		try
		{
			if (wrapper.getRedirectURL() != null)
			{
				writeCookies(wrapper, res, req);

				if (wrapper.getRedirectURL().startsWith(contextPathAdmin) || wrapper.getRedirectURL().indexOf(contextPathAdmin)!=-1)
				{
					//pre /wj7/showdoc.do, http://www.sme.sk, savedir.do
					String redir = removeContextPath(contextPathAdmin, wrapper.getRedirectURL());
					if (debug) Logger.debug(ContextFilter.class, "INV Redirect URL removecontext:"+wrapper.getRedirectURL()+" removed="+redir);
					res.sendRedirect(redir);
				}
				else
				{
					if (Tools.isSecure(req))
					{
						String redirUrl = wrapper.getRedirectURL();
						if (redirUrl.startsWith("http")==false) redirUrl = Tools.getBaseHref(req)+redirUrl;

						if (debug) Logger.debug(ContextFilter.class, "INV Redirect URL (nowsecure):"+redirUrl);
						res.sendRedirect(redirUrl);
					}
					else
					{
						if (debug) Logger.debug(ContextFilter.class, "INV Redirect URL:"+wrapper.getRedirectURL());
						res.sendRedirect(wrapper.getRedirectURL());
					}
				}
			}
			else
			{
				try
				{
					res.reset();
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}

				DebugTimer dt = new DebugTimer("INV replacing url: "+path);

				writeCookies(wrapper, res, req);

				if (wrapper.getCharacterEncoding()!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "INV Setting characterEncoding:"+wrapper.getCharacterEncoding());
					res.setCharacterEncoding(wrapper.getCharacterEncoding());
				}
				else
				{
					if (debug) Logger.debug(ContextFilter.class, "INV characterEncoding is NULL:"+wrapper.getCharacterEncoding());
				}
				if (wrapper.getContentType()!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "INV Setting contentType:"+wrapper.getContentType());
					if (wrapper.getContentType().indexOf("charset=")==-1)
					{
						res.setContentType(wrapper.getContentType()+"; charset=" + SetCharacterEncodingFilter.selectEncoding(req));
					}
					else
					{
						res.setContentType(wrapper.getContentType());
					}
				}
				if (wrapper.getLocale()!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "INV Setting locale:"+wrapper.getLocale());
					res.setLocale(wrapper.getLocale());
				}
				//TODO: zamysliet sa nad tym, co s ostanymi hlavickami
				if (wrapper.getHeader("Location")!=null)
				{
					String redirect = wrapper.getHeader("Location");
					if (redirect.startsWith(contextPathAdmin) || redirect.indexOf(contextPathAdmin)!=-1) redirect = removeContextPath(contextPathAdmin, redirect);
					if (debug) Logger.debug(ContextFilter.class, "INV Setting Location:"+redirect);
					res.setHeader("Location", redirect);
				}
				if (wrapper.getHeader("Content-Disposition")!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting Content-Disposition:"+wrapper.getHeader("Content-Disposition"));
					res.setHeader("Content-Disposition", wrapper.getHeader("Content-Disposition"));
				}
				if (wrapper.getHeader("X-Robots-Tag")!=null)
				{
					if (debug) Logger.debug(ContextFilter.class, "Setting X-Robots-Tag:"+wrapper.getHeader("Content-Disposition"));
					res.setHeader("X-Robots-Tag", wrapper.getHeader("X-Robots-Tag"));
				}

				if (wrapper.getErrorCode()!=0)
				{
					if (debug) Logger.debug(ContextFilter.class, "INV Setting error:"+wrapper.getErrorCode()+" message="+wrapper.getErrorMessage());
					if (wrapper.getErrorMessage()==null) res.sendError(wrapper.getErrorCode());
					else res.sendError(wrapper.getErrorCode(), wrapper.getErrorMessage());
				}
				else if (wrapper.getStatusCode()!=0)
				{
					if (debug) Logger.debug(ContextFilter.class, "INV Setting status:"+wrapper.getStatusCode());
					res.setStatus(wrapper.getStatusCode());

					/*
					if (404==wrapper.getStatusCode())
					{
						String contentType = "text/html; charset=" + SetCharacterEncodingFilter.selectEncoding(req);
						if (debug) Logger.debug(ContextFilter.class, "INV Setting 404 B contentType:"+contentType);
						res.setCharacterEncoding(SetCharacterEncodingFilter.selectEncoding(req));
						res.setContentType(contentType);
					}
					*/
				}
				else
				{
					if (debug) Logger.debug(ContextFilter.class, "INV Setting status 200");
					res.setStatus(HttpServletResponse.SC_OK);
				}

				wrapper.writeResponseToOriginalOutput(req, true);

				if (debug) dt.diff("INV done");
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vrati true ak sa pouziva ContextFilter, inak false
	 * @param request
	 * @return
	 */
	public static boolean isRunning(HttpServletRequest request)
	{
		if (Tools.isNotEmpty(request.getContextPath()) && "/".equals(request.getContextPath())==false) return true;
		return false;
	}

	/**
	 * Opravi cesty pre dany HTML kod
	 * @param htmlCode
	 * @return
	 */
	public static String addContextPath(String contextPath, String htmlCode)
	{
		//DebugTimer dt = new DebugTimer("replacing, size="+htmlCode.length());

		if (Tools.isEmpty(contextPath) || "/".equals(contextPath)) return htmlCode;

		if (htmlCode.startsWith("/") && htmlCode.startsWith(contextPath)==false && htmlCode.length() < 2048 && htmlCode.indexOf('\n')==-1)
		{
			//dt.diff("replace done JE TO URL: "+htmlCode);
			//asi sa jedna o samostatnu URL adresu
			return contextPath+htmlCode;
		}

		//TODO: spravit optimalnejsie, nielen cez Tools.replace
		htmlCode = Tools.replace(htmlCode, "\"/admin/", "\""+contextPath+"/admin/");
		htmlCode = Tools.replace(htmlCode, "'/admin/", "'"+contextPath+"/admin/");
		htmlCode = Tools.replace(htmlCode, "/components/", contextPath+"/components/");
		htmlCode = Tools.replace(htmlCode, "'/css/", "'"+contextPath+"/css/");
		htmlCode = Tools.replace(htmlCode, "\"/css/", "\""+contextPath+"/css/");

		htmlCode = Tools.replace(htmlCode, "/jscripts/", contextPath+"/jscripts/");
		htmlCode = Tools.replace(htmlCode, "/showdoc.do", contextPath+"/showdoc.do");
		htmlCode = Tools.replace(htmlCode, "/formmail.do", contextPath+"/formmail.do");
		htmlCode = Tools.replace(htmlCode, "\"/FormMailAjax.action", "\""+contextPath+"/FormMailAjax.action");
		htmlCode = Tools.replace(htmlCode, "/preview.do", contextPath+"/preview.do");
		htmlCode = Tools.replace(htmlCode, "/chat.do", contextPath+"/chat.do");
		htmlCode = Tools.replace(htmlCode, "(\"/inquiry.", "(\""+contextPath+"/inquiry.");
		htmlCode = Tools.replace(htmlCode, "href=\"/", "href=\""+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "href='/", "href='"+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "src=\"/", "src=\""+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "src='/", "src='"+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "action=\"/", "action=\""+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "url(/", "url("+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "url('/", "url('"+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "\"/images/", "\""+contextPath+"/images/");
		htmlCode = Tools.replace(htmlCode, "'/images/", "'"+contextPath+"/images/");
		htmlCode = Tools.replace(htmlCode, "\"/thumb/", "\""+contextPath+"/thumb/");
		htmlCode = Tools.replace(htmlCode, "'/thumb/", "'"+contextPath+"/thumb/");
		htmlCode = Tools.replace(htmlCode, "\"/files/", "\""+contextPath+"/files/");
		htmlCode = Tools.replace(htmlCode, "'/files/", "'"+contextPath+"/files/");
		htmlCode = Tools.replace(htmlCode, "wjPopup('/", "wjPopup('"+contextPath+"/");
		htmlCode = Tools.replace(htmlCode, "\"/templates/", "\""+contextPath+"/templates/");
		htmlCode = Tools.replace(htmlCode, "'/templates/", "'"+contextPath+"/templates/");

		//fix na dvojite nastavenie
		htmlCode = Tools.replace(htmlCode, contextPath+contextPath, contextPath);
		htmlCode = Tools.replace(htmlCode, contextPath+contextPath, contextPath);
		htmlCode = Tools.replace(htmlCode, "!INCLUDE("+contextPath, "!INCLUDE(");
		htmlCode = Tools.replace(htmlCode, contextPath+"/admin/m"+contextPath, contextPath+"/admin/m");
		htmlCode = Tools.replace(htmlCode, "/sitemap"+contextPath+"/jscripts/", "/sitemap/jscripts/");
		htmlCode = Tools.replace(htmlCode, "/calendar"+contextPath+"/jscripts/", "/calendar/jscripts/");
		htmlCode = Tools.replace(htmlCode, contextPath+"/thumb"+contextPath, contextPath+"/thumb");
		htmlCode = Tools.replace(htmlCode, "/assets"+contextPath+"/admin/", "/assets/admin/");

		//Logger.debug(ContextFilter.class, "addContextPath, cp="+contextPath+" in="+Constants.getInstallName());
		if (contextPath.equals("/"+Constants.getInstallName())==false)
		{
			///web/components/web/admin/admin_appstore.jsp
			htmlCode = Tools.replace(htmlCode, contextPath+"/components"+contextPath+"/admin/", contextPath+"/components/admin/");
			///web/components/infolib/web/admin/admin_migracia_adresar.jsp
			htmlCode = Tools.replace(htmlCode, "/"+Constants.getInstallName()+contextPath+"/admin/",  "/"+Constants.getInstallName()+"/admin/");
		}


		//ajax dotazy priamo na Stripes
		htmlCode = Tools.replace(htmlCode, "/sk/iway/iwcm"+contextPath+"/components/", contextPath+"/sk/iway/iwcm/components/");
		//cesty v admin casti k pomocnikovi
		htmlCode = Tools.replace(htmlCode, "='sk"+contextPath+"/", "='sk/");
		htmlCode = Tools.replace(htmlCode, "='cz"+contextPath+"/", "='cz/");
		htmlCode = Tools.replace(htmlCode, "='en"+contextPath+"/", "='en/");
		htmlCode = Tools.replace(htmlCode, "/sk"+contextPath+"/", "/sk/");
		htmlCode = Tools.replace(htmlCode, "/cz"+contextPath+"/", "/cz/");
		htmlCode = Tools.replace(htmlCode, "/en"+contextPath+"/", "/en/");
		htmlCode = Tools.replace(htmlCode, "/imageeditor"+contextPath+"/", "/imageeditor/");

		//common adresar s admin castou
		htmlCode = Tools.replace(htmlCode, "/_common"+contextPath+"/admin", "/_common/admin");

		//fixnutie url typu //www.facebook.com
		htmlCode = Tools.replace(htmlCode, contextPath+"//www.", "//www.");

		String contextFilterAddPathReplaces = Constants.getString("contextFilterAddPathReplaces");
		if (Tools.isNotEmpty(contextFilterAddPathReplaces))
		{
			htmlCode = doPathReplace(htmlCode, contextFilterAddPathReplaces, contextPath);
		}

		//dt.diff("replace done");

		return htmlCode;
	}

	/**
	 * Vykona nahradu pathReplaces vo formate  stary|novy\n
	 * pouziva sa pre constants premenne contextFilterAddPathReplaces a contextFilterRemovePathReplaces
	 * @param htmlCode
	 * @param pathReplaces
	 * @param contextPath
	 * @return
	 */
	private static String doPathReplace(String htmlCode, String pathReplaces, String contextPath)
	{
		try
		{
			String[] replaces = Tools.getTokens(pathReplaces, "\n");
			if (replaces==null || replaces.length==0) return htmlCode;
			for (String pair : replaces)
			{
				if (Tools.isEmpty(pair)) continue;

				pair = Tools.replace(pair, "{CP}", contextPath);

				String[] pairArray = pair.split("\\|");
				if (pairArray==null || pairArray.length!=2) continue;

				htmlCode = Tools.replace(htmlCode, pairArray[0], pairArray[1]);
			}
		}
		catch (Exception ex)
		{
		}

		return htmlCode;
	}

	/**
	 * Odstrani z HTML kodu odkaz na ContextPath
	 * @param contextPath
	 * @param htmlCode
	 * @return
	 */
	public static String removeContextPath(String contextPath, String htmlCode)
	{
		if (Tools.isEmpty(htmlCode)) return htmlCode;

		//Logger.debug(ContextFilter.class, "removeContextPath, html="+htmlCode);

		htmlCode = Tools.replace(htmlCode, contextPath+"/", "/");

		String contextFilterRemovePathReplaces = Constants.getString("contextFilterRemovePathReplaces");
		if (Tools.isNotEmpty(contextFilterRemovePathReplaces))
		{
			htmlCode = doPathReplace(htmlCode, contextFilterRemovePathReplaces, contextPath);
		}

		//Logger.debug(ContextFilter.class, "removeContextPath, html ret="+htmlCode);

		return htmlCode;
	}

	private static void writeCookies(ContextResponseWrapper wrapper, HttpServletResponse response, HttpServletRequest request)
	{
		//pridaj session cookie
		try
		{
			//Logger.debug(ContextFilter.class, "setting SESSION ID cookie, id="+request.getSession().getId());
			//uz asi netreba, robilo problem na WebSphere, na Tomcate bezi aj bez tohto
			/*
			if (request.getSession()!=null && request.getSession().getId()!=null)
			{
				String requestValue = Tools.getCookieValue(request.getCookies(), "JSESSIONID", null);
				if (requestValue==null || requestValue.equals(request.getSession().getId())==false)
				{
					String cookiePath = request.getContextPath();
					if (Tools.isEmpty(cookiePath)) cookiePath = "/";

					//response.addHeader("Set-Cookie", "JSESSIONID="+request.getSession().getId()+"; Path="+cookiePath+"; HttpOnly");
				}
			}
			*/
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		List<Cookie> cookies = wrapper.getCookies();
		if (cookies != null && cookies.size()>0)
		{
			for (Cookie c : cookies)
			{
				Logger.debug(ContextFilter.class, "addCookie: "+c.getName()+" "+c.getPath()+" "+c.getValue());

				Tools.addCookie(c, response, request);
			}
		}

		boolean debug = Constants.getBoolean("contextPathDebug");
		if (debug)
		{
			Collection<String> cookieHeaders = response.getHeaders("Set-Cookie");
			for (String value : cookieHeaders)
			{
				Logger.debug(ContextFilter.class, "ORIG RESPONSE cookie HEADER, value="+value);
				if (value.contains("JSESSIONID"))
				{
					//Logger.debug(ContextFilter.class, "ADDING ORIG RESPONSE cookie HEADER, value="+value);
					//response.addHeader("Set-Cookie", value);
				}
			}
		}
	}
}
