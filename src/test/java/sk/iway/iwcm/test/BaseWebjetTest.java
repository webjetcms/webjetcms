package sk.iway.iwcm.test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockServletContext;

import net.sourceforge.stripes.controller.StripesFilterIway;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.ConstantsV9;

/**
 *  BaseWebjetTest.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 4.12.2009 14:44:16
 *@modified     $Date: 2009/12/11 15:46:32 $
 */
@SuppressWarnings("java:S2187")
public class BaseWebjetTest
{
	private static final Object STRIPES_INIT_LOCK = new Object();
	private static volatile boolean stripesInitialized = false;
	private static StripesFilterIway stripesFilter;

	static
	{
		Constants.clearValues();
		ConstantsV9.clearValuesWebJet9();
		Tools.reinitialize();

		MockServletContext context = new MockServletContext("Webjet"){
			@Override
			public String getRealPath(String path)
			{
				String basePath = System.getProperty("webjetTestBasepath");
				System.out.println("----> basePath = "+basePath);
				if (basePath==null || basePath.isEmpty()) basePath = "./src/main/webapp/";

				String absolutePath = new File(new File(basePath), path).getAbsolutePath();
				System.out.println("----> absolutePath = "+absolutePath);

				return absolutePath;
			}
		};
		Constants.setServletContext(context);
		setStripesConfig();
		Constants.setString("smtpServer", "mxrelay.dev.iway.sk");

	}

	protected static void setStripesConfig() {
		if (stripesInitialized) {
			return;
		}

		synchronized (STRIPES_INIT_LOCK) {
			if (stripesInitialized) {
				return;
			}

			try {
				// Initialize Stripes once and keep a strong reference to avoid GC of weakly-referenced configuration.
				ServletContext servletContext = Constants.getServletContext();
				MockFilterConfig filterConfig = servletContext != null ? new MockFilterConfig(servletContext) : new MockFilterConfig();
				filterConfig.addInitParameter("ActionResolver.Packages", "net.sourceforge.stripes");
	        		//filterConfig.addInitParameter("LocalePicker.Class", "net.sourceforge.stripes.localization.MockLocalePicker");
				stripesFilter = new StripesFilterIway();
				stripesFilter.init(filterConfig);
				stripesFilter.initLazy();
				stripesInitialized = true;


			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected SimpleQuery query = new SimpleQuery();

	/**
	 * Load constants from database, override default values from Constants class
	 */
	protected void loadConstantsFromDB() {
        try (Connection db_conn = DBPool.getConnection()) {
            Map<String, String> databaseValues = InitServlet.getDatabaseValues(db_conn);
            InitServlet.loadConstants(databaseValues, null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
