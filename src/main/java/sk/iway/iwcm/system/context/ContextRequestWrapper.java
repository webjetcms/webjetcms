package sk.iway.iwcm.system.context;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;

/**
 *  ContextRequestWrapper.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.8.2012 14:23:53
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContextRequestWrapper extends HttpServletRequestWrapper
{
	private HttpServletRequest original;
	private final String contextPath;
	private Map<String, List<String>> changedParameters;

	public ContextRequestWrapper(HttpServletRequest request)
	{
		super(request);
		this.contextPath = request.getContextPath();
		this.original = request;
		changedParameters = new Hashtable<>();
	}

	/**
	 * Odstrani context path z celej sady parametrov
	 * @param values
	 * @return
	 */
	private String[] removeCp(String[] values)
	{
		String[] newValues = new String[values.length];
		for (int i=0; i<values.length; i++)
		{
			newValues[i] = ContextFilter.removeContextPath(contextPath, values[i]);
		}
		return newValues;
	}

	@Override
	public String getParameter(String name)
	{
		String ret = ContextFilter.removeContextPath(contextPath, original.getParameter(name));
		List<String> changed = changedParameters.get(name);
		if (changed != null && changed.size()>0) ret = ContextFilter.removeContextPath(contextPath, changed.get(0));
		//Logger.debug(ContextRequestWrapper.class, "getParameter n="+name+"="+original.getParameter(name)+" super="+super.getParameter(name)+" ret="+ret);
		return ret;
	}

	@Override
	public Map<String, String[]> getParameterMap()
	{
		//Logger.debug(ContextRequestWrapper.class, "getParameterMap");
		Map<String, String[]> newMap = new LinkedHashMap<>();
		Set<Map.Entry<String, String[]>> set = original.getParameterMap().entrySet();
		for(Map.Entry<String, String[]> me : set)
		{
			newMap.put(me.getKey(), removeCp(me.getValue()));
		}

		//prenes zmeny z changed parameters
		for (Map.Entry<String, List<String>> me : changedParameters.entrySet())
		{
			//Logger.debug(ContextRequestWrapper.class, "Pridavam parameter: "+me.getKey()+" val="+me.getValue());
			newMap.put(me.getKey(), removeCp(me.getValue().toArray(new String[0])));
		}

		return newMap;
	}

	@Override
	public String[] getParameterValues(String name)
	{
		//Logger.debug(ContextRequestWrapper.class, "getParameterValues, name="+name);
		return removeCp(original.getParameterValues(name));
	}

	@Override
	public void setRequest(ServletRequest request)
	{
		super.setRequest(request);
		if (request instanceof HttpServletRequest)
		{
			original = (HttpServletRequest)request;
			//TODO: sparsovanie QS na parametre
			parseUrlParameters(((HttpServletRequest)request).getQueryString());
		}
	}

	/**
	 * Toto sa vola napr. v PathFilter ked sa spravi include /showdoc.do?docid=XX a musia sa sparsovat aj QueryString parametre
	 * @param url
	 */
	private void parseUrlParameters(String url)
	{
		if (Tools.isEmpty(url)) return;

		String[] urlParts = url.split("\\?");
		if (urlParts.length > 1)
		{
			String query = urlParts[1];
			for (String param : query.split("&"))
			{
				try
				{
					String[] pair = param.split("=");
					String key = URLDecoder.decode(pair[0], SetCharacterEncodingFilter.getEncoding());
					String value = "";
					if (pair.length > 1)
					{
						value = URLDecoder.decode(pair[1], SetCharacterEncodingFilter.getEncoding());
					}
					List<String> values = changedParameters.get(key);
					if (values == null)
					{
						values = new ArrayList<>();
						changedParameters.put(key, values);
					}
					values.add(value);
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}
			}
		}
	}

	@Override
	public String getServletPath()
	{
		String servletPath = super.getServletPath();

		if (Tools.isNotEmpty(contextPath) && "/".equals(contextPath)==false && servletPath.startsWith(contextPath))
		{
			try
			{
				servletPath = servletPath.substring(contextPath.length());
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}

		}

		return servletPath;
	}

	@Override
	public HttpSession getSession()
	{
		if (Constants.getBoolean("contextPathDebug")) Logger.debug(ContextRequestWrapper.class, "GET SESSION="+original.getSession().getId());

		return original.getSession();
	}
}
