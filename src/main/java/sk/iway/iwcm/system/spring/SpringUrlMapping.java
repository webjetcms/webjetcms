package sk.iway.iwcm.system.spring;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.WriteTagToolsForCore;

/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.WJResponseWrapper;
import sk.iway.iwcm.tags.WriteTag;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
*/

/**
 * Drzi info o URL mapovanych na Spring.
 *  SpringUrlMapping.java
 *
 *
 *@Title        webjet8
 *@Company      Interway a.s. (www.interway.sk)
 *@Copyright    Interway a.s (c) 2001-2016
 *@author       Author: mbocko
 *@created      Date: 19.9.2016
 *@modified     Date: 19.9.2016
 */
//@Component
public class SpringUrlMapping// implements ApplicationListener<ContextRefreshedEvent>
{
	private static final String LOGON_URL = "/admin/logon/";

	public static String redirect(String url) {
		return "redirect:" + url;
	}

	public static String redirectToLogon() {
		return redirect(LOGON_URL);
	}

	public static String redirectTo403() {
		return redirect("/403");
	}

	public static void redirectTo403(HttpServletResponse response) throws IOException {
		response.sendRedirect("/403");
	}

	public static String redirectTo404() {
		return redirect("/404");
	}

	public static void redirectTo404(HttpServletResponse response) throws IOException {
		response.sendRedirect("/404");
	}

	public static void redirectToLogon(HttpServletResponse response) throws IOException {
		response.sendRedirect(LOGON_URL);
	}

	/**
	 * Return path to custom component, eg.
	 * /components/user/logon -> /components/INSTALL_NAME/user/logon
	 * @param path
	 * @param request
	 * @return
	 */
	public static String getCustomPath(String path, HttpServletRequest request) {
		String ext = ".jsp";
		String customPath;
		if (path.startsWith("/admin/")) {
			customPath = WriteTagToolsForCore.getCustomPageAdmin(path+ext, request);
		} else {
			customPath = WriteTagToolsForCore.getCustomPath(path+ext, request);
		}
		if (Tools.isNotEmpty(customPath) && customPath.length()>5) {
			return customPath.substring(0, customPath.length()-ext.length());
		}


		return path;
	}

	//jeeff: tuto haluzu s mapovanim springu na /spring nepotrebujeme, kedze uz mame Spring komponenty cez INCLUDE
	//****** kedze je to ale @Component, tak triedu nemozeme len tak lahko zmazat kvoli update procesu ******


/*
	private static Set<String> springUrls;

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	//@Override
	@EventListener
	public void onApplicationEvent(final ContextRefreshedEvent event)
	{
		springUrls = Collections.synchronizedSet(new HashSet<String>());
		for (RequestMappingInfo reqMapInfo : requestMappingHandlerMapping.getHandlerMethods().keySet())
		{
			for (String url : reqMapInfo.getPatternsCondition().getPatterns())
			{
				if (url.contains("{"))
				{
					url = url.substring(0, url.indexOf("{"));
				}

				if (url.contains("*")) {
					url = Tools.replace(url, "*", "");
				}

				springUrls.add(url);
			}
		}
		PathFilter.registerDynamicForward("SpringForwarder", new DynamicForward() {

			@Override
			public boolean isValid(String path)
			{
				if (!path.startsWith("/spring") && springUrls!=null)
				{

					for (String sUrl : springUrls)
					{
						if (path.startsWith(sUrl))
						{
							return true;
						}

					}

				}
				return false;
			}

			@Override
			public boolean forward(String path, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
			{

				String pathForPage = "";
				for (String sUrl : springUrls)
				{
					if (path.startsWith(sUrl))
					{
						pathForPage = sUrl;
					}
				}
				if (Constants.getBoolean("SpringRequiresSecure") && !Tools.isSecure(request))
				{
					Logger.error(this, "ERROR: SPRING vyzaduje httpS pripojenie!!");
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					request.getRequestDispatcher("/403.jsp").forward(request, response);
					//System.out.println("SPRING forward 403");
					return true;
				}

				if (DocTools.testXssStrictGet(path) || DocTools.testXss(path) || path.indexOf('\r')!=-1 || path.indexOf('\n')!=-1 || request.getRequestURI().indexOf("//")!=-1 || path.indexOf('\\')!=-1 || path.indexOf("/../")!=-1)
				{
					if (!DocTools.isXssStrictUrlException(path, "xssProtectionStrictGetUrlException"))
					{
						//je to pokus o XSS: /404.html/'onmouseover=prompt(915761)
						Adminlog.add(Adminlog.TYPE_XSS, "XSS path="+path, -1, -1);
						Logger.error(this, "ERROR: SPRING XSS utok, path="+path);
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						request.getRequestDispatcher("/403.jsp").forward(request, response);
						return true;
					}
				}

				//STRICT XSS FILTER (aplikuje sa na vsetky HTTP poziadavky)
				String strictXssRedirect = DocTools.getXssStrictUrlRedirect(request, path, request.getQueryString());
				if (strictXssRedirect != null)
				{
					Logger.error(this, "ERROR: SPRING XSS utok, path=");
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					request.getRequestDispatcher("/403.jsp").forward(request, response);
					return true;
				}

				int docId = DocDB.getInstance().getVirtualPathDocId(pathForPage, DocDB.getDomain(request));
				if (docId>0)
				{
					WJResponseWrapper respWrapper = null;
					respWrapper = new WJResponseWrapper(response, request);
					request.getRequestDispatcher("/spring"+path).include(request, respWrapper);
//					if (respWrapper.redirectURL!=null)
//					{
//						response.sendRedirect(respWrapper.redirectURL);
//						return true;
//					}

					StringBuilder htmlCode = new StringBuilder(Tools.getStringValue(respWrapper.getOutputOfStreamAsString(), ""));
					htmlCode = WriteTag.fixFlash(htmlCode, request);
					htmlCode = WriteTagToolsForCore.fixXhtml(htmlCode,request);
					//htmlCode = WriteTag.preventSpam(htmlCode, request);
					//htmlCode = WriteTag.secureFormmail(htmlCode, request);
					htmlCode = WriteTagToolsForCore.fixXhtml(htmlCode, request);
					request.setAttribute("proxyOutputData", htmlCode.toString());
					//System.out.println("SPRING forward DOCID was found");
					return false;
				}
				else
				{
					path = "/spring" + path;
					request.getRequestDispatcher(path).forward(request, response);
					//System.out.println("SPRING forward 200 OK");
					return true;
				}

			}
		});
	}

	*/
}
