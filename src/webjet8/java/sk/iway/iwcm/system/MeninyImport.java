package sk.iway.iwcm.system;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *  MeninyImport.java - import menin z Excelu
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2005/10/25 06:48:05 $
 *@modified     $Date: 2005/10/25 06:48:05 $
 */
public class MeninyImport extends ExcelImportJXL
{
	PreparedStatement ps;
	String sql;

	private int day, month;
	private String name, lng;

	public MeninyImport(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);
		Logger.println(this,"MeninyImport constructor");
		//vymaz aktualne data
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM calendar_name_in_year");
			ps.execute();
			ps.close();
			ps = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	/**
	 * pridanie riadku do zoznamu riadkov
	 * @param row
	 */
	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		if (row.length<3)
		{
			return;
		}

		day = getIntValue(row, "day");
		month = getIntValue(row, "month");
		name = getValue(row, "name");
		lng = getValue(row, "lng");

		if (day>0 && month>0 && Tools.isNotEmpty(name) && Tools.isNotEmpty(lng))
		{
			Logger.println(this,"   importujem meniny: " + day + "." + month + " " + name + " [" + lng + "]");
			sql = "INSERT INTO calendar_name_in_year (day, month, name, lng) VALUES (?, ?, ?, ?) ";
		   ps = db_conn.prepareStatement(sql);
		   ps.setInt(1, day);
		   ps.setInt(2, month);
			ps.setString(3, name);
			ps.setString(4, lng);

		   ps.execute();
		   ps.close();
		}
		else
		{
			Logger.println(this,"   skipping: " + day + "." + month + " " + name + " [" + lng + "]");
		}
	}
}


