package net.sourceforge.stripes.controller;

import java.beans.Introspector;
import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import net.sourceforge.stripes.config.BootstrapPropertyResolver;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;

/**
 * StripesFilterIway.java - upravena verzia Stripes filtra pre potreby WebJETu<br />
 *
 * - dynamicke ziskanie packages sa presunulo pri aktualizacii na Stripes 1.5.3 do InitServlet, Properties sa uz loaduju cez Constants s predponou 'stripes.'.
 *   Ak taka konstanta neexistuje, pouzije sa klasicky Stripes kod.<br />
 *
 * - filter bypass pre specificke URL
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      Date: 1.10.2007 9:51:07
 *@modified     $Date: 2009/11/16 08:48:35 $
 */
public class StripesFilterIway extends StripesFilter
{
	private static volatile StripesFilterIway instance;

	private volatile boolean wasInit = false;
	private volatile boolean initError = false;
	private FilterConfig filterConfig = null;

	@Override
	public void init(FilterConfig filterConfig)
			throws ServletException
	{
		this.filterConfig = filterConfig;
		instance = this;
	}

	/**
	 * Initializes Stripes outside of HTTP request (e.g. in background thread), normally it is initialized lazily on first request.
	 * Initialization is performed only after WebJET is initialized, because Stripes requires WebJET configuration.
	 * @return true if Stripes is initialized and can be used
	 */
	public static boolean ensureInitialized()
	{
		StripesFilterIway filter = instance;
		if (filter == null) return false;
		if (!filter.wasInit)
		{
			if (!InitServlet.isWebjetInitialized()) return false;
			try
			{
				filter.initLazy();
			}
			catch (ServletException ex)
			{
				Logger.error(StripesFilterIway.class, ex);
				return false;
			}
		}
		return !filter.initError;
	}

	@Override
	public void destroy()
	{
		try
		{
			//super.destroy();
			filterConfig.getServletContext().removeAttribute(StripesFilter.class.getName());
			//Log.cleanup();
			Introspector.flushCaches();
			//configurations.clear();
		}
		catch (Exception ex) {}
		if (instance == this) instance = null;
		Logger.debug(StripesFilterIway.class, "StripesFilter Destroyed");
	}

	public synchronized void initLazy() throws ServletException
	{
		try
		{
			//doble check
			if (wasInit) return;

			//inicializacia properties sa presunula do Constants s predponou stripes. a inicializacia balickov do InitServlet.java

			BootstrapPropertyResolver bootstrap = new BootstrapPropertyResolver(filterConfig);

			Logger.println(StripesFilterIway.class, "Stripes ActionResolver.Packages=" + bootstrap.getProperty("ActionResolver.Packages"));
			Logger.println(StripesFilterIway.class, "Stripes MultipartWrapper.Class=" + bootstrap.getProperty("MultipartWrapper.Class"));
			Logger.println(StripesFilterIway.class, "Stripes LocalizationBundleFactory.Class=" + bootstrap.getProperty("LocalizationBundleFactory.Class"));
			//System.out.println("ActionBeanPropertyBinder.Class=" + bootstrap.getProperty("ActionBeanPropertyBinder.Class"));

			super.init(filterConfig);
		}
		catch (Exception ex)
		{
			System.out.println("Stripes INIT error:"+ex.getMessage());
			Logger.error(ex);
			initError = true;
		}

		wasInit = true;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		//Logger.println(this,"___________________________________________");
		//System.out.println("Stripes.doFilter - bypass="+servletRequest.getAttribute("PathFilter.bypass"));
		if (!wasInit)
		{
			initLazy();
		}

		if (initError || "true".equals(servletRequest.getAttribute("PathFilter.bypass")))
		{
			//request ziadno nemodifikujeme
			//System.out.println("Stripes bypassing");

			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		super.doFilter(servletRequest, servletResponse, filterChain);
	}
}
