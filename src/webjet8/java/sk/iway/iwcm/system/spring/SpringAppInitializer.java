package sk.iway.iwcm.system.spring;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import sk.iway.iwcm.*;
import sk.iway.iwcm.system.ConfDB;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpringAppInitializer implements WebApplicationInitializer
{
	public static String installName;
	public static String logInstallName;
	private List<String> customConfigs = new ArrayList<>();

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException
	{
		Logger.println(this,"SPRING: inicializujem");
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		customConfigs.add("sk.iway.iwcm.system.spring.BaseSpringConfig");

		String contextDbName = servletContext.getInitParameter("webjetDbname");
		Logger.debug(getClass(),"SPRING: contextDbName="+contextDbName);
		InitServlet.setContextDbName(contextDbName);
		//toto musime setnut - inak nebude fungovat Tools.getRealPath pri inite Spring komponent
		Constants.setServletContext(servletContext);

		String installName = getConstant("installName");
		if (Tools.isNotEmpty(installName)) {
			customConfigs.add("sk.iway." + installName + ".SpringConfig");
			SpringAppInitializer.installName = installName;
            Constants.setInstallName(installName);
		}

		String logInstallName = getConstant("logInstallName");
		if (Tools.isNotEmpty(logInstallName)) {
			SpringAppInitializer.logInstallName = logInstallName;

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

		//ak sme Tomcat 7 nemozeme mat spring komponenty (ma staru verziu el), musime inicializovat takto
		Logger.println(this,"ServerInfo: "+servletContext.getServerInfo());
		if (servletContext.getServerInfo().contains("Tomcat/7"))
		{
			customConfigs.add("sk.iway.tomcat7.Tomcat7SpringConfig");
		}

		//WebJET 8
		//customConfigs.add("sk.iway.webjet.v8.SpringConfig");

		//WebJET 9/2021
		customConfigs.add("sk.iway.webjet.v9.V9SpringConfig");

		ctx.setServletContext(servletContext);

		Dynamic dynamic = servletContext.addServlet("springDispatcher", new DispatcherServlet(ctx));
		dynamic.addMapping("/");
		dynamic.setLoadOnStartup(1);

		if (Tools.isEmpty(installName)) {
			//WebJET is not initialized - there is no DB connection, allow only setup
			customConfigs.clear();
			addScanPackagesInit(ctx);
		} else {
			loadConfigs(ctx);
			servletContext.addListener(RequestContextListener.class);
			addScanPackages(ctx);
		}

		servletContext.setAttribute("springContext", ctx);
	}

	private void loadConfigs(AnnotationConfigWebApplicationContext ctx) {
		// filtrovanie neexistujucich tried
		List<String> customConfigsLocal = customConfigs.stream().filter(c-> {
			try {
				return Class.forName(c) != null;
			} catch (ClassNotFoundException e) {
				//sk.iway.iwcm.Logger.error(e);
				Logger.println(this, "SPRING: NEnasiel som custom config (1) pre " + c);
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
					Logger.println(this, "SPRING: nasiel som custom config " + customConfig);
				}
				else {
					Logger.println(this, "SPRING: NEnasiel som custom config (2) pre " + customConfig);
				}
			} catch (Exception e) {
				// config class asi neexistuje.
				Logger.println(this, "SPRING: NEnasiel som custom config (3) pre " + customConfig);
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

		String addPackages = getConstant("springAddPackages");
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

	public static String getInstallName() {
		if (installName == null) {
			installName = getConstant("installName");
		}

		return installName;
	}

	public static String getLogInstallName() {
		if (logInstallName == null) {
			logInstallName = getConstant("logInstallName");
		}

		return logInstallName;
	}

	private static String getConstant(String name) {
		String value = null;
		try
		{
			Connection dbConn = DBPool.getConnection("iwcm");

			Logger.println(SpringAppInitializer.class, "Getting " + name + " from table "+ConfDB.CONF_TABLE_NAME);
			PreparedStatement ps = dbConn.prepareStatement("SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?");
			ps.setString(1, name);
			try
			{
				ResultSet rs = ps.executeQuery();
				try
				{
					if (rs.next())
					{
						value = DB.getDbString(rs, "value");
					}
				}
				finally
				{
					rs.close();
				}
			} finally
			{
				ps.close();
				dbConn.close();
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(SpringAppInitializer.class, ex.getMessage());
		}

		return (value);
	}
}
