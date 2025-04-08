package sk.iway.iwcm.form;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;

/**
 *  FormDB.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      Date: 30.10.2007 12:19:41
 *@modified     $Date: 2010/01/20 10:13:22 $
 */
public class FormDB
{
	private static FormDB instance;
	private List<String[]> regExpList = null;

	/**
	 * Zakladna instancia objektu
	 * @return
	 */
	public static FormDB getInstance()
	{
		return getInstance(false);
	}


	/**
	 * Zakladna instancia objektu
	 * @param forceRefresh - true = refresh instancie
	 * @return
	 */
	public static FormDB getInstance(boolean forceRefresh)
	{
		if (instance == null || forceRefresh)
		{
			synchronized (FormDB.class)
			{
				instance = new FormDB();
			}
		}
		return instance;
	}

	/**
	 * Private konstruktor
	 */
	private FormDB()
	{
		Logger.debug(FormDB.class, "FormDB.constructor");
		ClusterDB.addRefresh(FormDB.class);
	}

	public static List<FormDetails> getForms(UserDetails user,boolean alsoArchive)
	{
		List<FormDetails> forms = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT max(id) as id, form_name, max(doc_id) as doc_id FROM forms";
			if(!alsoArchive)
				sql+= " where form_name not like 'Archiv-%' "+CloudToolsForCore.getDomainIdSqlWhere(true);
			else
				sql += " where "+CloudToolsForCore.getDomainIdSqlWhere(false);
			sql += " GROUP BY form_name";

			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			FormDetails form;
			while (rs.next())
			{
				form = new FormDetails();
				form.setId(rs.getInt("id"));
				form.setFormName(rs.getString("form_name"));
				form.setDocId(rs.getInt("doc_id"));
				forms.add(form);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

            if (user != null) {
                forms = filterFormsByUser(user, forms);
            }
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
		return(forms);
	}


	/**
	 * vrati zoznam vsetkych formularov
	 * @return
	 */
	public static List<FormDetails> getForms(UserDetails user)
	{
		return getForms(user, true);
	}

	/**
	 * Vrati polozku data z tabulky forms pre dany formular
	 *
	 * @param id identifikator formulara, ktoreho data chceme ziskat
	 * @return	String data - polozka z tabulky forms pre dany formular
	 */
	public static String getData(int id)
	{
		String data = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT data FROM forms WHERE id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, id);

			rs = ps.executeQuery();

			while (rs.next())
				data = DB.getDbString(rs, "data");

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

		return(data);
	}

	/* optimalizuje len mysql */
	private static boolean optimiseTable(Connection db_conn) throws SQLException
	{
		if (Constants.DB_TYPE == Constants.DB_MYSQL)
		{
			PreparedStatement ps = null;
			String sql = "OPTIMIZE TABLE forms";
			ps = db_conn.prepareStatement(sql);
			boolean res = ps.execute();
			ps.close();
			return res;
		}
		else if (Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			PreparedStatement ps = null;
			String sql = "REINDEX TABLE forms";
			ps = db_conn.prepareStatement(sql);
			boolean res = ps.execute();
			ps.close();
			return res;
		}
		else if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			return true;
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE)
		{
			return true;
		}
		else
		{
			return true;
		}
	}

	private static int updateManageRecord(Connection db_conn, String formName) throws SQLException
	{
		PreparedStatement ps = null;
		String sql = "UPDATE forms_archive f1, forms f2 SET f1.data = f2.data WHERE f1.form_name = f2.form_name AND f2.form_name = ? AND f2.create_date IS NULL AND f1.create_date IS NULL";
		ps = db_conn.prepareStatement(sql);
		ps.setString(1, formName);
		int res = ps.executeUpdate();
		ps.close();
		return res;
	}

	private static boolean addManageRecord(Connection db_conn, String formName) throws SQLException
	{
		PreparedStatement ps = null;
		String sql = "INSERT INTO forms_archive SELECT f.* FROM forms f WHERE f.form_name = ? AND create_date IS NULL";
		ps = db_conn.prepareStatement(sql);
		ps.setString(1, formName);
		boolean res = ps.execute();
		ps.close();
		return res;
	}

	private static boolean tableContainsManageRecord(Connection db_conn, String formName) throws SQLException
	{
		PreparedStatement ps = null;
		ps = db_conn.prepareStatement("SELECT * FROM forms_archive WHERE form_name = ? AND create_date IS NULL");
		ps.setString(1, formName);
		ResultSet rs = ps.executeQuery();
		boolean res = rs.next();
		ps.close();
		return res;
	}

	private static boolean insertRecord(Connection db_conn, int formId) throws SQLException
	{
		PreparedStatement ps = null;
		String sql = "INSERT INTO forms_archive SELECT f.* FROM forms f WHERE id = ? AND create_date IS NOT NULL";
		ps = db_conn.prepareStatement(sql);
		ps.setInt(1, formId);
		boolean res = ps.execute();
		ps.close();
		return res;
	}

	private static boolean removeRecord(Connection db_conn, int formId) throws SQLException
	{
		PreparedStatement ps = null;
		String sql = "DELETE FROM forms WHERE id = ? AND create_date IS NOT NULL";
		ps = db_conn.prepareStatement(sql);
		ps.setInt(1, formId);
		boolean res = ps.execute();
		ps.close();
		return res;
	}

	private static boolean manageRecord(Connection db_conn, String formName) throws SQLException
	{
		if(FormDB.tableContainsManageRecord(db_conn, formName))
		{
			FormDB.updateManageRecord(db_conn, formName);
		}
		else
		{
			FormDB.addManageRecord(db_conn, formName);
		}
		return true;
	}

	private static boolean iterateRows(Connection db_conn, ResultSet rs) throws SQLException
	{
		while (rs.next()){
			int formId = rs.getInt("id");
			FormDB.insertRecord(db_conn, formId);
			FormDB.removeRecord(db_conn, formId);
		}
		return true;
	}

	/**
	 * Prekopiruje cely zaznam s najnizsim Id z vybratych poloziek na koniec tabulky, aby sa na jeho miesto mohla vlozit hlavicka formulara s novym nazvom zvacsa Archiv-'oldName'
	 *
	 * @param 	smallestId		najnizsie Id z vybratych poloziek formulara, ktore chce pouzivatel archivovat
	 * @return	true 				ak sa uspesne prekopiruje(najprv select, potom insert) prvy zaznam na koniec tabulky, inak false
	 */


	/**
	 * Vrati nove Id prekopirovaneho prveho zaznamu z vybratych
	 *
	 * @param	name			nazov formulara, ktoreho chceme zistit jeho nove Id
	 * @param	createdTime datum a cas vytvorenie formulara. Spolu s jeho nazvom by mali presne identifikovat nove Id
	 *
	 * @return 	Vrati nove formId formulara s danym nazvom a datum vytvorenia
	 */
	public static int getNewFormId(String name, Timestamp createdTime)
	{
		int newId = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT max(id) AS id FROM forms WHERE form_name = ? AND create_date = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));

			ps.setString(1, name);
			ps.setTimestamp(2, createdTime);

			rs = ps.executeQuery();

			while (rs.next())
				newId = rs.getInt("id");

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
		return newId;
	}

	/**
	 * Prepise z tabulky forms najnizsiu zvolenu polozku formulara, aby na jeho miesto s rovnakym Id mohol vytvorit hlavicku formulara.
	 * Zvoleny formular je uz prekopirovany na nove Id podla sekvencie DB.
	 *
	 * @param 	smallestId		najnizsi identifikator zvolenych poloziek formulara, ktore je potrebne zarchivovat a prepisat na hlavicku formulara
	 * @param	newName			novy nazov hlavicky archivovaneho formulara
	 * @param	originalFormId	identifikator povodnej hlavicky formulara, ktory chceme zarchivovat. Data jeho hlavicky a data hlavicky novo-vznikajuceho formulara su rovnake.
	 *
	 * @return	ak sa operacia podari true, inak false
	 */

	/**
	 * nastavi vsetkym polozkam s name oldName na name newName, ak zbehne vrati true, inak false
	 *
	 * @param oldName
	 * @param newName
	 *
	 * @return
	 */
	public static boolean setFormName(String oldName, String newName)
	{
		return FormDB.setFormName(oldName, newName, "", 0, true);
	}

	/**
	 * nastavi vsetkym polozkam s name oldName na name newName, ak zbehne vrati true, inak false
	 * @param oldName
	 * @param newName
	 * @return
	 */
	public static boolean setFormName(String oldName, String newName, String idQuery, int smallestId, boolean allRecords)
	{
		boolean updateOk = false;
		int id = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;

		if (idQuery == null)
			idQuery = "";

		if (smallestId == 0 && idQuery.indexOf(" create_date IS NOT NULL") != -1) // ide o pripad archivacie vsetkych zaznamov a ponechanie povodnej hlavicky
			smallestId = FormDB.getFirstFormId(oldName);

		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT id FROM forms WHERE form_name = ? "+CloudToolsForCore.getDomainIdSqlWhere(true) + idQuery;

			id = searchOldNameForm(newName);
			if (id > 0 && allRecords)
			{
				sql = "SELECT id FROM forms WHERE form_name = ? AND id > ? "+CloudToolsForCore.getDomainIdSqlWhere(true);
			}
			/*else if (Tools.isNotEmpty(idQuery) && smallestId != 0)
			{
				FormDB.copySmallestIdForm(smallestId);		//prekopiruje zaznam s prvym Id na koniec tabulky
				FormDB.updateFirstIdForm(smallestId, newName, searchOldNameForm(oldName));	// prepise ho
			}*/
			ps = db_conn.prepareStatement(sql);
			int pIndex = 1;
			ps.setString(pIndex++, oldName.trim());
			if	(id > 0 && allRecords)
			{
				ps.setInt(pIndex++, id);
			}
			FormDB.iterateRows(db_conn, ps.executeQuery());
			ps.close();
			FormDB.manageRecord(db_conn, oldName.trim());
			FormDB.optimiseTable(db_conn);
			db_conn.close();
			ps = null;
			db_conn = null;
			updateOk = true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		/*if (id > 0)	// kontrola, ci nevznikli dve hlavicky formularu, ak ano, tu najnovsiu vymaze
			FormDB.checkAndDeleteMultipleFormHeader(newName);
		*/
		return(updateOk);
	}

	/**
	 * nastavi vsetkym polozkam vytvorenym fordate todate inkluzivne
	 *  s name oldName na name newName, ak zbehne vrati true, inak false
	 * @param oldName
	 * @param newName
	 * @return
	 */
	public static boolean setFormName(String oldName, String newName, Date fromDate, Date toDate,int smallestId)
	{
		boolean updateOk = false;

		if(Tools.isAnyEmpty(oldName,newName))
			return updateOk;

		Connection db_conn = null;
		PreparedStatement ps = null;

		try
		{
			db_conn = DBPool.getConnection();
			String sql = "SELECT id FROM forms WHERE form_name = ? AND create_date >= ? AND create_date < ? and id != ? "+CloudToolsForCore.getDomainIdSqlWhere(true);
			ps = db_conn.prepareStatement(sql);
			int pIndex = 1;
			ps.setString(pIndex++, oldName.trim());
			ps.setTimestamp(pIndex++, new Timestamp(fromDate.getTime()));
			ps.setTimestamp(pIndex++, new Timestamp(toDate.getTime()));
			ps.setInt(pIndex++, smallestId);

			FormDB.iterateRows(db_conn, ps.executeQuery());
			ps.close();
			FormDB.manageRecord(db_conn, oldName.trim());
			FormDB.optimiseTable(db_conn);
			db_conn.close();
			ps = null;
			db_conn = null;
			/*if (smallestId != 0)
			{
				FormDB.copySmallestIdForm(smallestId); // prekopiruje zaznam s prvym
				FormDB.updateFirstIdForm(smallestId, newName, searchOldNameForm(oldName)); // prepise
																													// ho
			}*/
			/*ak sme zarchivovali vsetko zo stareho formu tak vymazem staru hlavicku*/
			int oldCount = new SimpleQuery().forInt("select count(*) from forms where form_name = ? "+CloudToolsForCore.getDomainIdSqlWhere(true), oldName);
			if(oldCount == 1)
				new SimpleQuery().execute("delete from forms where form_name = ? "+CloudToolsForCore.getDomainIdSqlWhere(true), oldName);

			updateOk = true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return(updateOk);
	}

	/**
	 * Vrati id prveho zaznamu formu po jeho hlavicke
	 *
	 * @param	name	nazov formulara, ktoreho chceme zistit jeho najnizsie Id okrem hlavicky
	 * @return 	Vrati najnizsie formId formulara s danym nazvom okrem jeho hlavicky
	 */
	public static int getFirstFormId(String name)
	{
		int id = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT min(id) as id FROM forms WHERE form_name = ? AND create_date IS NOT NULL "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setString(1, name);
			rs = ps.executeQuery();

			if (rs.next())
				id = rs.getInt("id");

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
		return id;
	}

	/**
	 * vrati id prveho zaznamu ak sa name == newName, inac vrati 0
	 * @return
	 */
	public static int searchOldNameForm(String newName){
		int id = 0;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT min(id) as id FROM forms WHERE form_name = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setString(1, newName);
			rs = ps.executeQuery();
			if (rs.next())
			{
				id = rs.getInt("id");
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
		return id;
	}

	/**
	 * Vyfiltruje formulare na zaklade prav pouzivatela na pristup k adresarom a strankam a docId formularu
	 * @param user
	 * @param allForms
	 * @return
	 */
	public static List<FormDetails> filterFormsByUser(UserDetails user, List<FormDetails> allForms)
	{
		List<FormDetails> ret = new ArrayList<>(allForms.size());

		GroupsDB groupsDB = GroupsDB.getInstance();

		int[] userEditableGroups = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
		int[] userEditablePages = Tools.getTokensInt(user.getEditablePages(), ",");
		if ((userEditableGroups == null || userEditableGroups.length<1) && (userEditablePages==null || userEditablePages.length<1)) return allForms;

		DocDB docDB = DocDB.getInstance();
		for (FormDetails form : allForms)
		{
			boolean pridaj = false;
			if (userEditableGroups!=null && userEditableGroups.length>0)
			{
				DocDetails doc = docDB.getBasicDocDetails(form.getDocId(), false);
				if (doc != null)
				{
					for (int groupId : userEditableGroups)
					{
						if (doc.getGroupId()==groupId)
						{
							pridaj = true;
							break;
						}
					}
				}
			}
			if (userEditablePages!=null && userEditablePages.length>0)
			{
				for (int docId : userEditablePages)
				{
					if (form.getDocId()==docId)
					{
						pridaj = true;
						break;
					}
				}
			}

			if (pridaj) ret.add(form);
		}

		return ret;
	}

	/**
	 * Zisti, ci sa v tabulke forms nevyskytuju dve hlavicky formulara a ked ano, tak tu s vyssim Id vymaze. <br />
	 * Vznika vtedy, ak user zarchivuje nejake polozky z formulara a vznikne novy formular najcastejsie s nazvom Archiv-stareMeno. Potom
	 * pri premenovani celeho novo-vzniknuteho formulara na povodne meno vzniknu dva hlavicky - povodna a nova, ktora sa vytvorila pri archivacii poloziek a
	 * nasledne sa premenovala na povodny nazov.
	 *
	 * @param 	name	nazov formulara, ktoreho hlavicku chceme otestovat
	 *
	 * @return	true ak mal formular dve hlavicky a jedna z nich sa vymazala, false ak je vsetko v poriadku a formular mal iba jednu hlavicku
	 */
	public static boolean checkAndDeleteMultipleFormHeader(String name)
	{
		boolean retValue = false;
		List<Integer> formHeaderIdValues = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT id FROM forms WHERE form_name = ? AND create_date IS NULL "+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY id");
			ps.setString(1, name);

			rs = ps.executeQuery();

			while (rs.next())
			{
				retValue = true;
				formHeaderIdValues.add(Integer.valueOf(rs.getInt("id")));
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

		if (formHeaderIdValues.size() > 1)
			deleteFormById(formHeaderIdValues.get(1));	// vymaze najnovsiu nadbytocnu hlavicku

		return(retValue);
	}


	public static boolean isThereFileRestrictionFor(String formName)
	{
		return new SimpleQuery().forInt(
			"SELECT COUNT(*) FROM form_attributes WHERE form_name = ? AND param_name = ? "+CloudToolsForCore.getDomainIdSqlWhere(true), formName, "maxSizeInKilobytes") > 0;
	}

	public static FormFileRestriction getFileRestrictionFor(String formName)
	{
		FormFileRestriction restriction = new FormFileRestriction();
		restriction.setFormName(formName);
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM form_attributes WHERE form_name = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setString(1, formName);
			rs = ps.executeQuery();
			while (rs.next())
			{
				String paramName = rs.getString("param_name");
				String value = rs.getString("value");
				if ("allowedExtensions".equals(paramName))
					restriction.setAllowedExtensions(value);
				if ("maxSizeInKilobytes".equals(paramName))
					restriction.setMaxSizeInKilobytes(Integer.parseInt(value));
				if ("pictureHeight".equals(paramName))
					restriction.setPictureHeight(Integer.parseInt(value));
				if ("pictureWidth".equals(paramName))
					restriction.setPictureWidth(Integer.parseInt(value));
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

		return restriction;
	}

	@SuppressWarnings("unchecked")
	public static List<String> getDistinctFormNames()
	{
		return new SimpleQuery().forList("SELECT DISTINCT(form_name) FROM forms WHERE create_date IS NULL "+CloudToolsForCore.getDomainIdSqlWhere(true));
	}

	public List<String[]> getAllRegularExpression()
	{
		String[] regularExp = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		if(regExpList != null)
		{
			Logger.debug(FormDB.class, "Zoznam nie je prazdny, vraciam zoznam.");
			return regExpList;
		}

		try
		{
			Logger.debug(FormDB.class, "Nacitavam z databazy.");
			List<String[]> newRegExpList = new ArrayList<>();  // pomocna premenna na prevenciu multithreadovych bugov
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM form_regular_exp");
			rs = ps.executeQuery();
			while (rs.next())
			{
				regularExp = new String[3];
				regularExp[0] = DB.getDbString(rs, "title");
				regularExp[1] = DB.getDbString(rs, "type");
				regularExp[2] = DB.getDbString(rs, "reg_exp");
				newRegExpList.add(regularExp);
			}
			regExpList = newRegExpList;
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return regExpList;
	}

	public static String[] getRegExpByType(String type)
	{
		String[] regularExp = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM form_regular_exp WHERE type = ?");
			ps.setString(1, type);
			rs = ps.executeQuery();
			if (rs.next())
			{
				regularExp = new String[3];
				regularExp[0] = DB.getDbString(rs, "title");
				regularExp[1] = DB.getDbString(rs, "type");
				regularExp[2] = DB.getDbString(rs, "reg_exp");
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return regularExp;
	}

	public static boolean saveRegularExpression(String title, String type, String regExp)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("INSERT INTO form_regular_exp (title, type, reg_exp) VALUES (?, ?, ?)");
			ps.setString(1, title);
			ps.setString(2, type);
			ps.setString(3, regExp);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch(SQLException sqlEx)
		{
			sk.iway.iwcm.Logger.error(sqlEx);
			return false;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return true;
	}

	public static boolean updateRegularExpression(String title, String type, String typeOld, String regExp)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE form_regular_exp SET title=?, type=?, reg_exp=? WHERE type=?");
			ps.setString(1, title);
			ps.setString(2, type);
			ps.setString(3, regExp);
			ps.setString(4, typeOld);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return true;
	}

	public static void deleteRegExp(String type)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM form_regular_exp WHERE type = ?");
			ps.setString(1, type);
			ps.execute();

			ps.close();
			db_conn.close();
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
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	/**
    * Vymaze formular z tabulky forms identifikovany jeho Id
    *
    * @param formId	identifikator formulara, ktory chceme vymazat
    *
    * @return true ak sa vymazanie podari, inak false
    */
	public static boolean deleteFormById(int formId)
	{
		try{
			new SimpleQuery().execute("DELETE FROM forms WHERE id = ? "+ CloudToolsForCore.getDomainIdSqlWhere(true), formId);
			return true;
		}
		catch (IllegalStateException e) {
			sk.iway.iwcm.Logger.error(e);
			return false;
		 }
	}

	public static Map<String, Integer> createColNames(String data)
	{
		Map<String, Integer> colNames = new Hashtable<>();
		StringTokenizer st = new StringTokenizer(data, "|");
		String text;
		int i;
		int counter = 0;
		//bereme aj separatory, takze kazdy druhy je \t
		while (st.hasMoreTokens())
		{
			text = st.nextToken();
			try
			{
				i = text.indexOf('~');
				if (i>0)
				{
					if (i<text.length())
					{
						text = text.substring(i+1);
					}
				}
				else if (i==0)
				{
					if (text.length() == 1)
					{
						text = "";
					}
					else
					{
						text = text.substring(1);
					}
				}
			}
			catch (Exception ex)
			{

			}

			if (text!=null && text.length()>0)
			{
				colNames.put(text, Integer.valueOf(counter));
				counter++;
			}
		}
		return (colNames);
	}

	/**
	 * Odstrani z nazvu formularu pomlcky a podtrhovnik aby vyzeral "ludskejsie"
	 * @param name
	 * @return
	 */
	public static String getValueNoDash(String name)
	{
		//aby sa dali robit hodnoty typu ++, +, 0, -, --
		if ("----".equals(name)) return name;
		if ("---".equals(name)) return name;
		if ("--".equals(name)) return name;
		if ("-".equals(name)) return name;

		//http://intra.iway.sk/helpdesk/?bugID=8149
		name = name.replace('_', ' ');
		//toto je dlha pomlcka na kratku
		name = name.replace('–', '-');

		name = name.replace('-', ' ');
		name = Tools.replace(name, "e mail", "e-mail");

		//toto je matica rb
		name = Tools.replace(name, " rb ", " - ");
		if (name.endsWith(" rb") && name.length()>3) name = name.substring(0, name.length()-3);

		//nahrada multiple checkbox
		if (name.endsWith("_cbm") && name.length()>4) name = name.substring(0, name.length()-4);

		if (name.endsWith(" cb") && name.length()>3) name = name.substring(0, name.length()-3);

		return name;
	}

	/**
	 * Vrati formDetails so zadanym id
	 * @param formId
	 * @return
	 */
	public static FormDetails getForm(int formId)
	{
		FormDetails formDetails = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			String sql = "SELECT u.*, f.id, f.form_name, f.data, f.files, f.create_date, f.html, f.note, f.last_export_date, f.doc_id FROM forms f LEFT JOIN  users u ON u.user_id=f.user_id WHERE f.id=? "+CloudToolsForCore.getDomainIdSqlWhere(true, "f");
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, formId);

			rs = ps.executeQuery();

			UserDetails usr;
			UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
			Timestamp timestamp;
			while (rs.next())
			{
				formDetails = new FormDetails();
				formDetails.setId(rs.getInt("id"));

				usr = new UserDetails();
				usr.setUserId(rs.getInt("user_id"));
				usr.setTitle(DB.getDbString(rs, "title"));
				usr.setFirstName(DB.getDbString(rs, "first_name"));
				usr.setLastName(DB.getDbString(rs, "last_name"));
				usr.setLogin(DB.getDbString(rs, "login"));
				usr.setAdmin(rs.getBoolean("is_admin"));
				usr.setUserGroupsIds(DB.getDbString(rs, "user_groups"));
				usr.setUserGroupsNames(userGroupsDB.convertIdsToNames(usr.getUserGroupsIds()));
				usr.setAuthorized(rs.getBoolean("authorized"));
				usr.setCompany(DB.getDbString(rs, "company"));
				usr.setAdress(DB.getDbString(rs, "adress"));
				usr.setCity(DB.getDbString(rs, "city"));
				usr.setPSC(DB.getDbString(rs, "PSC"));
				usr.setCountry(DB.getDbString(rs, "country"));
				usr.setEmail(DB.getDbString(rs, "email"));
				usr.setPhone(DB.getDbString(rs, "phone"));
				usr.setLastLogon(DB.getDbDateTime(rs, "last_logon"));

				usr.setDateOfBirth(DB.getDbDate(rs, "date_of_birth"));
				usr.setSexMale(rs.getBoolean("sex_male"));
				usr.setPhoto(DB.getDbString(rs, "photo"));
				usr.setSignature(DB.getDbString(rs, "signature"));

				formDetails.setUserDetails(usr);
				formDetails.setNote(DB.getDbString(rs, "note"));
				Timestamp lastExportDate = rs.getTimestamp("last_export_date");
				if (lastExportDate != null) formDetails.setLastExportDate(lastExportDate.getTime());
				formDetails.setDocId(rs.getInt("doc_id"));

				formDetails.setFormName(DB.getDbString(rs, "form_name"));
				formDetails.setData(DB.getDbString(rs, "data"));
				formDetails.setFiles(DB.getDbString(rs, "files"));
				timestamp = rs.getTimestamp("create_date");
				if (timestamp != null)
				{
					formDetails.setCreateDate(timestamp.getTime());
					formDetails.setCreateDateString(Tools.formatDateTime(timestamp));
				}

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
		return formDetails;
	}

	/**
	 * Overi, ci uz user odoslal formular s pozadovanym nazvom. Vrati TRUE, ak user este neodoslal form.
	 * @param userId
	 * @param formName
	 * @return
	 */
	public static boolean checkFormSendOnce(int userId, String formName)
	{
		boolean ret = true;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (userId > 0 && Tools.isNotEmpty(formName))
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT * FROM forms WHERE user_id=? AND form_name=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setInt(1, userId);
				ps.setString(2, formName.trim());
				rs = ps.executeQuery();
				if (rs.next())
				{
					ret = false;
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
			}
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
		return(ret);
	}
}
