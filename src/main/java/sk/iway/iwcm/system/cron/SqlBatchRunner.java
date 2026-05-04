package sk.iway.iwcm.system.cron;

import java.util.Arrays;

import sk.iway.iwcm.database.SimpleQuery;

/**
 *  SqlRunner.java
 *
 *  Runs a batch of SQLs received. Each args parameter = SQL
 *  Designed to be run by Cron
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.10.2010 11:33:07
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class SqlBatchRunner
{
	public static void main(String[] args)
	{
		try{
			sk.iway.iwcm.Logger.println(SqlBatchRunner.class, "About to run SQLs: "+Arrays.toString(args));
			if (args == null || args.length == 0)
				return;

			for (String sql : args)
				run(sql);
		}catch (Exception e) {
			sk.iway.iwcm.Logger.println(SqlBatchRunner.class, "Failed to run SQLs");
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private static void run(String sql)
	{
		sk.iway.iwcm.Logger.println(SqlBatchRunner.class, "Executing "+sql);
		new SimpleQuery().execute(sql);
	}
}