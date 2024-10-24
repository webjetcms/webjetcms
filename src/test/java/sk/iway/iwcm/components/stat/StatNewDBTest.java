package sk.iway.iwcm.components.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.stat.PartitionedTable;
import sk.iway.iwcm.stat.StatNewDB;

/**
 *  StatNewDBTest.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 6.8.2012 14:18:45
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class StatNewDBTest
{
	@Test
	void testTableExists()
	{
		boolean mycheck =false;
		Calendar now = Calendar.getInstance();
		String tablename= PartitionedTable.StatViews.getTableName() + "_" + now.get(Calendar.YEAR) + "_" + now.get(Calendar.MONTH);
		System.out.println(tablename);
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT * FROM " + tablename;

				ps = db_conn.prepareStatement(sql);

				rs = ps.executeQuery();
				mycheck = true;
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
			}
			catch (Exception ex)
			{

				String errorMessage = ex.getMessage();
				System.out.println("errorMessage: "+errorMessage);
				if (errorMessage.contains("Invalid object name") || errorMessage.contains("doesn't exist") || errorMessage.contains("not exist") || errorMessage.contains("ORA-00942"))
				{
					mycheck = false;
				}
				else
					mycheck = true;
			}
			assertEquals(mycheck, StatNewDB.tableExists(tablename));
	}
}
