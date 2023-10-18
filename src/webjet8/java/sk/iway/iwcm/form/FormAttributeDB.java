package sk.iway.iwcm.form;

import static sk.iway.iwcm.Tools.isEmpty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;

/**
 *  FormAttributeDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: May 19, 2011 2:30:29 PM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class FormAttributeDB
{

	public Map<String, String> filterAttributes(Map<String, String[]> parameters)
	{
		Map<String, String> filtered = new HashMap<String, String>();
		for (Map.Entry<String, String[]> entry : parameters.entrySet())
		{
			String paramName = entry.getKey();
			if (paramName.startsWith("attribute_"))
				filtered.put(paramName.replace("attribute_", ""), entry.getValue()[0]);
		}
		return filtered;
	}

	public void save(String formName, Map<String, String> parameters)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			ps = deleteOldValues(formName, parameters, db_conn);
			ps = insertNewValues(formName, parameters, db_conn);

			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex) {sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2){}
		}
	}

	private PreparedStatement deleteOldValues(String formName, Map<String, String> parameters, Connection db_conn) throws SQLException
	{
		PreparedStatement ps;
		ps = db_conn.prepareStatement("DELETE FROM form_attributes WHERE form_name = ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
		ps.setString(1, formName);
		ps.executeUpdate();
		ps.close();
		return ps;
	}

	private PreparedStatement insertNewValues(String formName, Map<String, String> parameters, Connection db_conn) throws SQLException
	{
		PreparedStatement ps;
		ps = db_conn.prepareStatement("INSERT INTO form_attributes(form_name, param_name, value, domain_id) VALUES (?,?,?,?)");
		ps.setString(1, formName);
		for(Map.Entry<String, String> entry : parameters.entrySet())
		{
			if (isEmpty(entry.getValue()))
				continue;
			ps.setString(2, entry.getKey());
			ps.setString(3, entry.getValue());
			ps.setInt(4, CloudToolsForCore.getDomainId());
			ps.executeUpdate();
		}
		ps.close();
		return ps;
	}

	public Map<String, String> load(String formName)
	{
		final Map<String, String> attributes = new HashMap<String, String>();
		new ComplexQuery().setSql("SELECT * FROM form_attributes WHERE form_name = ? "+CloudToolsForCore.getDomainIdSqlWhere(true)).setParams(formName).list(new Mapper<Void>(){
			public Void map(ResultSet rs) throws SQLException{
				attributes.put(rs.getString("param_name"), rs.getString("value"));
				return null;
			}
		});
		return attributes;
	}
}