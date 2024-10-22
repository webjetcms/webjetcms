package sk.iway.iwcm.system.spring;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import sk.iway.iwcm.*;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpringAppInitializer implements WebApplicationInitializer
{
	private List<String> customConfigs = new ArrayList<>();

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException
	{
		boolean initialized = InitServlet.initializeWebJET(servletContext);
		String installName = Constants.getInstallName();

		Logger.println(this,"SPRING: onStartup");
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		customConfigs.add("sk.iway.iwcm.system.spring.BaseSpringConfig");

		if (initialized) {
			String contextDbName = servletContext.getInitParameter("webjetDbname");
			Logger.debug(getClass(),"SPRING: contextDbName="+contextDbName);
			InitServlet.setContextDbName(contextDbName);

			if (Tools.isNotEmpty(installName)) {
				customConfigs.add("sk.iway." + installName + ".SpringConfig");
				Constants.setInstallName(installName);
			}

			String logInstallName = Constants.getLogInstallName();
			if (Tools.isNotEmpty(logInstallName)) {
				String logClassName = "sk.iway." + logInstallName + ".LogSpringConfig";
				//over ci existuje trieda LogSpringConfig kvoli spatnej kompatibilite boli stare ako SpringConfig
				try {
					Class.forName(logClassName);
					customConfigs.add(logClassName);
				} catch (ClassNotFoundException e) {
					//nenasiel sa LogSpringConfig, skusime teda pridat po starom
					customConfigs.add("sk.iway." + logInstallName + ".SpringConfig");
				}

			}

			//WebJET 9/2021
			customConfigs.add("sk.iway.webjet.v9.V9SpringConfig");
		}

		ctx.setServletContext(servletContext);

		Dynamic dynamic = servletContext.addServlet("springDispatcher", new DispatcherServlet(ctx));
		dynamic.addMapping("/");
		dynamic.setLoadOnStartup(1);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(Constants.getString("defaultEncoding"));
		servletContext.addFilter("SpringEncodingFilter", filter).addMappingForUrlPatterns(null, false, "/*");

		if (initialized == false) {
			//WebJET is not initialized - there is no DB connection, allow only setup
			customConfigs.clear();
			addScanPackagesInit(ctx);
		} else {
			loadConfigs(ctx);
			servletContext.addListener(RequestContextListener.class);
			addScanPackages(ctx);
			servletContext.addListener(new ContextLoaderListener(ctx));
		}

		servletContext.setAttribute("springContext", ctx);

		if (initialized) {
			// spring security filter
			final DelegatingFilterProxy springSecurityFilterChain = new DelegatingFilterProxy("springSecurityFilterChain");
			final FilterRegistration.Dynamic addedFilter = servletContext.addFilter("springSecurityFilterChain", springSecurityFilterChain);
			addedFilter.addMappingForUrlPatterns(null, false, "/*");
		} else {
			//it is normally initialized in V9SpringConfig, but we need to add it here for setup/bad db connection
			servletContext.addFilter("failedSetCharacterEncodingFilter", new SetCharacterEncodingFilter()).addMappingForUrlPatterns(null, false, "/*");
		}
	}

	private void loadConfigs(AnnotationConfigWebApplicationContext ctx) {
		// filtrovanie neexistujucich tried
		List<String> customConfigsLocal = customConfigs.stream().filter(c-> {
			try {
				return Class.forName(c) != null;
			} catch (ClassNotFoundException e) {
				//sk.iway.iwcm.Logger.error(e);
				Logger.println(this, "SPRING: NOT found custom config (1) " + c);
			}
			return false;
		}).collect(Collectors.toList());
		Class<?>[] objectArray = new Class[customConfigsLocal.size()];

		// naplnenie pola tried
		for (int i = 0; i < customConfigsLocal.size(); i++) {
			String customConfig = customConfigsLocal.get(i);
			try {
				Class<?> aClass = Class.forName(customConfig);
				if (aClass != null) {
					objectArray[i] = aClass;
					Logger.println(this, "SPRING: found custom config " + customConfig);
				}
				else {
					Logger.println(this, "SPRING: NOT found custom config (2) " + customConfig);
				}
			} catch (Exception e) {
				// config class asi neexistuje.
				Logger.println(this, "SPRING: found custom config (3) " + customConfig);
			}

		}
		ctx.register(objectArray);
	}

	private void addScanPackages(AnnotationConfigWebApplicationContext ctx) {
		List<String> packages = new ArrayList<>();
		packages.add("sk.iway.iwcm.calendar");
		packages.add("sk.iway.iwcm.system.spring");
		packages.add("sk.iway.iwcm.users");
		packages.add("sk.iway.iwcm.rest");
		packages.add("sk.iway.iwcm.components");
		packages.add("sk.iway.iwcm.users");
		packages.add("sk.iway.iwcm.components");
		packages.add("sk.iway.iwcm.editor");
		packages.add("sk.iway.iwcm.admin");
		/* vyhladava rekurzivne
		packages.add("sk.iway.iwcm.components.events");
		packages.add("sk.iway.iwcm.components.quiz");
		packages.add("sk.iway.iwcm.components.inquirySimple");
		packages.add("sk.iway.iwcm.components.organization");
		packages.add("sk.iway.iwcm.components.inzercia");
		packages.add("sk.iway.iwcm.components.restaurant_menu");
		*/
		packages.add("sk.iway.iwcm.doc.templates");
		packages.add("sk.iway.iwcm.system.datatables");
		packages.add("sk.iway.iwcm.logon");
		packages.add("sk.iway.iwcm.doc.groups");
		packages.add("sk.iway.iwcm.grideditor");
		//packages.add("sk.iway.intranet.dms");
		packages.add("sk.iway.iwcm.localconf");

		String addPackages = Constants.getString("springAddPackages");
		if (Tools.isNotEmpty(addPackages)) {
			packages.addAll(Tools.getStringListValue(Tools.getTokens(addPackages, ",")));
		}

		Logger.println(getClass(), String.format("Spring scan packages: %s", Tools.join(packages, ", ")));
		ctx.scan(packages.toArray(new String[packages.size()]));
	}

	/**
	 * Packages for setup
	 * @param ctx
	 */
	private void addScanPackagesInit(AnnotationConfigWebApplicationContext ctx) {
		List<String> packages = new ArrayList<>();
		packages.add("sk.iway.iwcm.system.spring");
		packages.add("sk.iway.iwcm.setup");
		Logger.println(getClass(), String.format("Spring scan packages: %s", Tools.join(packages, ", ")));
		ctx.scan(packages.toArray(new String[packages.size()]));
	}
}
