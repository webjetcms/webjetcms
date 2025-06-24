package sk.iway.iwcm.system.spring;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.DebugTimer;

import javax.servlet.FilterRegistration;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import java.util.ArrayList;
import java.util.List;

public class SpringAppInitializer implements WebApplicationInitializer
{
	private static DebugTimer dtGlobal = null;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException
	{
		List<String> springConfigClasses = new ArrayList<>();
		dtGlobal = new DebugTimer("WebJET.init");
		boolean initialized = InitServlet.initializeWebJET(dtGlobal, servletContext);
		String installName = Constants.getInstallName();

		Logger.println(this,"SPRING: onStartup");
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		springConfigClasses.add("sk.iway.iwcm.system.spring.BaseSpringConfig");

		//WebJET 9/2021
		springConfigClasses.add("sk.iway.webjet.v9.V9SpringConfig");

		if (initialized) {
			String contextDbName = servletContext.getInitParameter("webjetDbname");
			Logger.debug(getClass(),"SPRING: contextDbName="+contextDbName);
			InitServlet.setContextDbName(contextDbName);

			if (Tools.isNotEmpty(installName)) {
				springConfigClasses.add("sk.iway." + installName + ".SpringConfig");
				Constants.setInstallName(installName);
			}

			String logInstallName = Constants.getLogInstallName();
			if (Tools.isNotEmpty(logInstallName)) {
				String logClassName = "sk.iway." + logInstallName + ".LogSpringConfig";
				//over ci existuje trieda LogSpringConfig kvoli spatnej kompatibilite boli stare ako SpringConfig
				try {
					Class.forName(logClassName);
					springConfigClasses.add(logClassName);
				} catch (ClassNotFoundException e) {
					//nenasiel sa LogSpringConfig, skusime teda pridat po starom
					springConfigClasses.add("sk.iway." + logInstallName + ".SpringConfig");
				}

			}
		}

		ctx.setServletContext(servletContext);

		Dynamic dynamic = servletContext.addServlet("springDispatcher", new DispatcherServlet(ctx));
		dynamic.addMapping("/");
		dynamic.setLoadOnStartup(1);

		String stripesPostSize = Constants.getString("stripes.FileUpload.MaximumPostSize");
		stripesPostSize = Tools.replace(stripesPostSize, "m", "000000");
		stripesPostSize = Tools.replace(stripesPostSize, "g", "000000000");
		long maxPostSize = Tools.getLongValue(stripesPostSize, 0L);

		// Set multipart config (example values)
		MultipartConfigElement multipartConfig = new MultipartConfigElement(
			null,// location (null = default temp dir)
			maxPostSize,  // maxFileSize
			maxPostSize,  // maxRequestSize
			0    // fileSizeThreshold
		);
		dynamic.setMultipartConfig(multipartConfig);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(Constants.getString("defaultEncoding"));
		servletContext.addFilter("SpringEncodingFilter", filter).addMappingForUrlPatterns(null, false, "/*");

		if (initialized == false) {
			//WebJET is not initialized - there is no DB connection, allow only setup
			springConfigClasses.clear();
			addScanPackagesInit(ctx);
		} else {
			loadSpringConfigs(springConfigClasses, ctx);
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

		dtGlobal.diff("Spring onStartup done");

		if (initialized) InitServlet.setSpringInitialized();
	}

	private void loadSpringConfigs(List<String> customConfigs, AnnotationConfigWebApplicationContext ctx) {

		List<Class<?>> classList = new ArrayList<>();

		// naplnenie pola tried
		for (String customConfig : customConfigs) {
			try {
				Class<?> aClass = Class.forName(customConfig);
				if (aClass != null) {
					classList.add(aClass);
					Logger.println(this, "SPRING: found custom config " + customConfig);
				}
				else {
					Logger.println(this, "SPRING: NOT found custom config 1 " + customConfig);
				}
			} catch (Exception e) {
				// config class asi neexistuje.
				Logger.println(this, "SPRING: NOT found custom config 2 " + customConfig);
			}

		}

		ctx.register(classList.toArray(new Class[classList.size()]));
	}

	private void addScanPackages(AnnotationConfigWebApplicationContext ctx) {
		List<String> packages = new ArrayList<>();
		packages.add("sk.iway.iwcm.system.spring");

		String addPackages = Constants.getString("springAddPackages");
		if (Tools.isNotEmpty(addPackages)) {
			packages.addAll(Tools.getStringListValue(Tools.getTokens(addPackages, ",")));
		}

		if (packages.isEmpty()==false) {
			Logger.println(getClass(), String.format("Spring scan packages: %s", Tools.join(packages, ", ")));
			ctx.scan(packages.toArray(new String[packages.size()]));
		}
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

	public static void dtDiff(String message) {
		if (dtGlobal!=null) dtGlobal.diffInfo(message);
	}
}
