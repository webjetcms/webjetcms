package sk.iway.iwcm.form;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;

/**
 *  FormResults.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 16.6.2010 15:02:07
 *@modified     Date: 19.9.2019 TMA (#33463/#5)
 */
public class FormResults implements Iterable<Map<String,String>>
{
	 String formName;
	 String dateFromS;
	 String dateToS;

	 List<Map<String, String>> results = new ArrayList<Map<String,String>>();

	 public FormResults()
	 {
	 }

	 public FormResults(String formName)
 	 {
		this.formName = formName;
		loadRows();
	 }

	 public void loadRows()
	 {
		List<String> rows = loadDataFromDB();
		for (String data : rows)
		{
			FormDetails postedForm = new FormDetails();
			postedForm.setData(data);
			results.add(postedForm.getNameValueTable());
		}
	 }

	/**
	 * can't be replaced by {@link SimpleQuery}, because of odd MsSQL CLOB data retrieval.
	 */
	private List<String> loadDataFromDB()
	{
		List<String> data = new ArrayList<String>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			 db_conn = DBPool.getConnection();
			 StringBuilder sql = new StringBuilder("SELECT data FROM forms WHERE create_date IS NOT NULL AND form_name = ? ");
			 if (Tools.isNotEmpty(this.dateFromS))
			 {
				  sql.append("AND create_date >= ? ");
			 }
			 if (Tools.isNotEmpty(this.dateToS))
			 {
				  sql.append("AND create_date < ? ");
			 }
			 sql.append(CloudToolsForCore.getDomainIdSqlWhere(true));
			 System.out.println("SQL: " + sql);
			 ps = db_conn.prepareStatement(sql.toString());
			 int psCounter = 1;
			 ps.setString(psCounter++, formName);
			 if (Tools.isNotEmpty(this.dateFromS))
			 {
				  ps.setTimestamp(psCounter++, new Timestamp(DB.getTimestamp(this.dateFromS, "0:00:00")));
			 }
			 if (Tools.isNotEmpty(this.dateToS))
			 {
				  ps.setTimestamp(psCounter++, new Timestamp(DB.getTimestamp(this.dateToS, "23:59:59")));
			 }

			rs = ps.executeQuery();
			while (rs.next())
				data.add(rs.getString("data"));
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2){sk.iway.iwcm.Logger.error(ex2);}
		}
		return data;
	}

	public List<Map<String, String>> results()
	{
		return results;
	}
	@Override
	public Iterator<Map<String, String>> iterator()
	{
		return results.iterator();
	}

	public int size()
	{
		return results.size();
	}

	 public FormResults setFormName(String formName)
	 {
		  this.formName = formName;
		  return this;
	 }

	 public FormResults setDateFromS(String dateFromS)
	 {
		  this.dateFromS = dateFromS;
		  return this;
	 }

	 public FormResults setDateToS(String dateToS)
	 {
		  this.dateToS = dateToS;
		  return this;
	 }
}
