package sk.iway.iwcm.test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.mock.web.MockServletContext;

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
public class BaseWebjetTest
{

	static
	{
		Constants.clearValues();
		ConstantsV9.clearValuesWebJet9();
		Tools.reinitialize();
		Constants.setServletContext(new MockServletContext("Webjet"){
			@Override
			public String getRealPath(String path)
			{
				String basePath = System.getProperty("webjetTestBasepath");
				System.out.println("----> basePath = "+basePath);
				if (basePath==null || basePath.length()<1) basePath = "./src/main/webapp/";

				String absolutePath = new File(new File(basePath), path).getAbsolutePath();
				System.out.println("----> absolutePath = "+absolutePath);

				return absolutePath;
			}
		});
		Constants.setString("smtpServer", "mxrelay.dev.iway.sk");
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
