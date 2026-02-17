package sk.iway.iwcm.components.proxy;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.proxy.jpa.ProxyBean;


/**
 *  ProxyServlet.java - vykona proxy volanie na vzdialeny server
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 1.3.2006 16:13:59
 *@modified     $Date: 2008/11/11 15:37:57 $
 */
public class Proxy
{
	protected Proxy() {
		//utility class
	}

	/**
	 * Volanie proxy metody na zaklade nastaveneho parametra proxyMethod
	 * @param proxy
	 * @param req
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public static void service(ProxyBean proxy, HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException
	{
		/*if ("ProxyByHttpClient".equals(proxy.getProxyMethod()))
		{
			ProxyByHttpClient.service(proxy, req, response);
			return;
		}
		else*/ if ("ProxyByHttpClient4".equals(proxy.getProxyMethod()) || "ProxyByHttpClient".equals(proxy.getProxyMethod()))
		{
			ProxyByHttpClient4.service(proxy, req, response);
			return;
		}
		ProxyBySocket.service(proxy, req, response);
	}

	/**
	 * Returns true if the response will be included into webpage from CMS
	 * @param proxy
	 * @param path
	 * @return
	 */
	public static boolean isIncludeIntoPage(ProxyBean proxy, String path) {
		//zdetekuj priponu a ci to vkladame do WJ alebo nie
		String ext = null;
		int dotIndex = path.lastIndexOf('.');
		if (dotIndex > 0) ext = path.substring(dotIndex).toLowerCase();
		boolean includeIntoPage = false;
		if (ext == null && Tools.isNotEmpty(proxy.getIncludeExt()) && proxy.getIncludeExt().length()>1) {
			includeIntoPage = true;
		} else {
			String[] exts = Tools.getTokens(proxy.getIncludeExt(), ",", true);
			if (exts != null) {
				for (String myExt : exts) {
					if (myExt.equals(ext))
					{
						includeIntoPage = true;
						break;
					}
				}
			}
		}
		return includeIntoPage;
	}
}
