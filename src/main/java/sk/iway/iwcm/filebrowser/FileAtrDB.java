package sk.iway.iwcm.filebrowser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;

/**
 *  Objekt na pracu s atributmi suboru
 *
 *@Title        WebJET 4.0
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Streda, 2003, október 15
 *@modified     $Date: 2004/02/27 16:12:18 $
 */
public class FileAtrDB
{
	protected FileAtrDB() {
		//utility class
	}

	/**
	 *  Description of the Field
	 */
	public static final int TYPE_STRING = 0;
	/**
	 *  Description of the Field
	 */
	public static final int TYPE_INT = 1;
	/**
	 *  Description of the Field
	 */
	public static final int TYPE_BOOL = 2;

	/**
	 *  vrati zoznam vsetkych atributov (aj nevyplnenych) pre dany subor
	 *
	 *@param  link     - cela cesta k suboru (URL)
	 *@param  group    - skupina atributov
	 *@param  request
	 *@return
	 */
	public static List<FileAtrBean> getAtributes(String link, String group, HttpServletRequest request)
	{
		List<FileAtrBean> ret = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			String sql = "SELECT fad.*, fa.link, fa.file_name, fa.value_string, fa.value_int, fa.value_bool FROM file_atr_def fad " +
					"LEFT JOIN file_atr fa ON fad.atr_id = fa.atr_id " +
					"AND fa.link=? " +
					"WHERE fad.atr_group=? " +
					"ORDER BY fad.order_priority ";
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, link);
			ps.setString(2, group);
			rs = ps.executeQuery();
			FileAtrBean atr;
			while (rs.next())
			{
				atr = new FileAtrBean();
				atr.setAtrId(rs.getInt("atr_id"));
				atr.setAtrName(DB.getDbString(rs, "atr_name"));
				atr.setOrderPriority(rs.getInt("order_priority"));
				atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
				atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
				atr.setAtrType(rs.getInt("atr_type"));
				atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
				atr.setLink(DB.getDbString(rs, "link"));
				atr.setFileName(DB.getDbString(rs, "file_name"));
				atr.setValueString(DB.getDbString(rs, "value_string"));
				atr.setValueInt(rs.getInt("value_int"));
				atr.setValueBool(rs.getBoolean("value_bool"));
				atr.setTrueValue(DB.getDbString(rs, "true_value"));
				atr.setFalseValue(DB.getDbString(rs, "false_value"));
				ret.add(atr);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return (ret);
	}

	/**
	 *  Vrati List skupin, ktory ako hodnoty obsahuje dalsie Listy s
	 *  atributmi v danej skupine
	 *
	 *@param  link     - cela cesta k suboru (URL)
	 *@param  request
	 *@return
	 */
	public static List<List<FileAtrBean>> getAtributes(String link, HttpServletRequest request)
	{
		List<List<FileAtrBean>> ret = new ArrayList<>();
		List<FileAtrBean> atrs;
		for (LabelValueDetails lvd : getAtrGroups(request))
		{
			//Logger.println(this,"lvd->"+lvd.getLabel());
			//ok mame grupu
			atrs = getAtributes(link, lvd.getLabel(), request);
			ret.add(atrs);
		}

		return (ret);
	}

	/**
	 *  vrati atribut atrId
	 *
	 *@param  atrId
	 *@param  request
	 *@return
	 */
	public static FileAtrBean getAtrDef(int atrId, HttpServletRequest request)
	{
		FileAtrBean atr = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			String sql = "SELECT * FROM file_atr_def WHERE atr_id=?";
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, atrId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				atr = new FileAtrBean();
				atr.setAtrId(rs.getInt("atr_id"));
				atr.setAtrName(DB.getDbString(rs, "atr_name"));
				atr.setOrderPriority(rs.getInt("order_priority"));
				atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
				atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
				atr.setAtrType(rs.getInt("atr_type"));
				atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
				atr.setTrueValue(DB.getDbString(rs, "true_value"));
				atr.setFalseValue(DB.getDbString(rs, "false_value"));
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return (atr);
	}

	/**
	 *  vrati zoznam skupin atributov
	 *
	 *@param  request
	 *@return
	 */
	public static List<LabelValueDetails> getAtrGroups(HttpServletRequest request)
	{
		List<LabelValueDetails> groups = new ArrayList<>();
		LabelValueDetails lvd;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			String sql = "SELECT distinct atr_group FROM file_atr_def ORDER BY atr_group";
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next())
			{
				lvd = new LabelValueDetails();
				lvd.setLabel(DB.getDbString(rs, "atr_group"));
				lvd.setValue(lvd.getLabel());
				groups.add(lvd);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return (groups);
	}

	/**
	 *  Vytvori tabulku so zoznamom suborov a jednotlivymi atributmi
	 *
	 *@param  rootPath    adresar, v ktorom sa ma nachadzat subor
	 *@param  includeSub  ak true, vratane podadresarov
	 *@param  group       skupina atributov, pre ktoru robime vypis (alebo null)
	 *@param  request     Description of the Parameter
	 *@return             The atributesTable value
	 */
	public static List<FileAtrRowBean> getAtributesTable(String rootPath, boolean includeSub, String group, HttpServletRequest request)
	{
		List<FileAtrRowBean> rows = new ArrayList<>();

		if (group != null && Tools.isEmpty(group))
		{
			group = null;
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			StringBuilder sql = new StringBuilder("SELECT fad.*, fa.file_name, fa.link, fa.value_string, fa.value_int, fa.value_bool ").append(
					"FROM file_atr_def fad ").append(
					"LEFT JOIN file_atr fa ON fad.atr_id = fa.atr_id ").append(
					"WHERE fad.atr_id>0 ");
			if (group != null)
			{
				sql.append("AND fad.atr_group=?  ");
			}

			sql.append("ORDER BY fa.file_name, fa.link, fad.order_priority ");

			//Logger.println(this,sql);

			ps = db_conn.prepareStatement(sql.toString());
			//ps.setInt(1, docId);
			if (group != null)
			{
				ps.setString(1, group);
			}
			rs = ps.executeQuery();
			String lastLink = null;
			FileAtrRowBean rowBean = null;
			FileAtrBean atr = null;
			while (rs.next())
			{
				atr = new FileAtrBean();
				atr.setAtrId(rs.getInt("atr_id"));
				atr.setAtrName(DB.getDbString(rs, "atr_name"));
				atr.setOrderPriority(rs.getInt("order_priority"));
				atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
				atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
				atr.setAtrType(rs.getInt("atr_type"));
				atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
				atr.setFileName(DB.getDbString(rs, "file_name"));
				atr.setLink(DB.getDbString(rs, "link"));
				atr.setValueString(DB.getDbString(rs, "value_string"));
				atr.setValueInt(rs.getInt("value_int"));
				atr.setValueBool(rs.getBoolean("value_bool"));
				atr.setTrueValue(DB.getDbString(rs, "true_value"));
				atr.setFalseValue(DB.getDbString(rs, "false_value"));

				if (lastLink == null)
				{
					//zaciatocna inicializacia
					lastLink = atr.getLink();

					rowBean = new FileAtrRowBean();

					rowBean.setFileName(atr.getFileName());
					rowBean.setLink(DB.getDbString(rs, "link"));
				}

				if (lastLink.equals(atr.getLink()))
				{
					if (rowBean!=null) rowBean.addAtr(atr);
				}
				else
				{
					rows.add(rowBean);

					rowBean = new FileAtrRowBean();

					rowBean.setFileName(atr.getFileName());
					rowBean.setLink(DB.getDbString(rs, "link"));

					rowBean.addAtr(atr);

					lastLink = atr.getLink();
				}
			}

			if (rowBean != null)
			{
				rows.add(rowBean);
			}

			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return (rows);
	}

	/**
	 *  Nacita zoznam suborov s atributom daneho nazvu, pouziva sa pre vytvorenie
	 *  stromu atributov
	 *
	 *@param  atrName     nazov atributu, pre ktory vytvarame strom
	 *@param  rootPath    adresar, v ktorom sa ma nachadzat subor (alebo null)
	 *@param  includeSub  ak true, berieme aj podadresare
	 *@param  group       nazov skupiny atributov (alebo null)
	 *@param  request     Description of the Parameter
	 *@return             The atributesTree value
	 */
	public static List<FileAtrBean> getAtributesTree(String atrName, String rootPath, boolean includeSub, String group, HttpServletRequest request)
	{
		List<FileAtrBean> rows = new ArrayList<>();

		if (group != null && Tools.isEmpty(group))
		{
			group = null;
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			String sql = "SELECT fad.*, fa.file_name, fa.link, fa.value_string, fa.value_int, fa.value_bool " +
					"FROM file_atr_def fad " +
					"LEFT JOIN file_atr fa ON fad.atr_id = fa.atr_id " +
					"WHERE fad.atr_name=? ";
			if (group != null)
			{
				sql += "AND fad.atr_group=?  ";
			}

			sql += "ORDER BY value_string, fa.file_name, fa.link, fad.order_priority ";

			//Logger.println(this,sql);

			ps = db_conn.prepareStatement(sql);
			ps.setString(1, atrName);
			if (group != null)
			{
				ps.setString(2, group);
			}
			rs = ps.executeQuery();
			FileAtrBean atr = null;
			while (rs.next())
			{
				atr = new FileAtrBean();
				atr.setAtrId(rs.getInt("atr_id"));
				atr.setAtrName(DB.getDbString(rs, "atr_name"));
				atr.setOrderPriority(rs.getInt("order_priority"));
				atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
				atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
				atr.setAtrType(rs.getInt("atr_type"));
				atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
				atr.setFileName(DB.getDbString(rs, "file_name"));
				atr.setLink(DB.getDbString(rs, "link"));
				atr.setValueString(DB.getDbString(rs, "value_string"));
				atr.setValueInt(rs.getInt("value_int"));
				atr.setValueBool(rs.getBoolean("value_bool"));
				atr.setTrueValue(DB.getDbString(rs, "true_value"));
				atr.setFalseValue(DB.getDbString(rs, "false_value"));

				rows.add(atr);
			}

			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return (rows);
	}

	/**
	 * Vrati zoznam pouzitych hodnot pre dane id atributu
	 * @param atrId
	 * @param request
	 * @return
	 */
	public static List<LabelValueDetails> getUsedAtrValues(int atrId, HttpServletRequest request)
	{
		List<LabelValueDetails> groups = new ArrayList<>();
		LabelValueDetails lvd;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			String sql = "SELECT distinct value_string FROM file_atr WHERE atr_id=? ORDER BY value_int, value_string";
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, atrId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				lvd = new LabelValueDetails();
				lvd.setLabel(DB.getDbString(rs, "value_string"));
				lvd.setValue(lvd.getLabel());
				groups.add(lvd);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return (groups);
	}

	/**
	 * Naplni informaciu o adresari z databazy (tabulka dir_url), jedna sa o
	 * vlastnosti adresara
	 * @param ef - edit form, musi mat vyplnene origDir
	 */
	public static void fillEditForm(EditForm ef)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM dirprop WHERE dir_url = ?");
			ps.setString(1, ef.getOrigDir());
			rs = ps.executeQuery();
			if (rs.next())
			{
				ef.setIndexFulltext(rs.getBoolean("index_fulltext"));
				ef.setPasswordProtectedString(DB.getDbString(rs, "password_protected"));
				ef.setLogonDocId(rs.getInt("logon_doc_id"));
			}
			rs.close();
			ps.close();
			rs = null;
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
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}
}
