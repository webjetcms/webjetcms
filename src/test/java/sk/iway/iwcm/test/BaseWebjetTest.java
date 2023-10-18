package sk.iway.iwcm.test;

import java.io.File;

import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.database.SimpleQuery;

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
		Constants.setServletContext(new MockServletContext("Webjet"){
			@Override
			public String getRealPath(String path)
			{
				String basePath = System.getProperty("webjetTestBasepath");
				System.out.println("----> basePath = "+basePath);
				if (basePath==null || basePath.length()<1) basePath = "./WebContent";

				String absolutePath = new File(new File(basePath), path).getAbsolutePath();
				System.out.println("----> absolutePath = "+absolutePath);

				return absolutePath;
			}
		});
	}
	
	
	protected SimpleQuery query = new SimpleQuery();
}
