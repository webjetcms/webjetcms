package sk.iway.iwcm.system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.ninja.Ninja;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.users.UserDetails;

public class ConfDB
{
	public static String CONF_TABLE_NAME = "_conf_"; //NOSONAR
	public static String CONF_PREPARED_TABLE_NAME = "_conf_prepared_"; //NOSONAR
	public static String MODULES_TABLE_NAME = "_modules_"; //NOSONAR
	public static String ADMINLOG_TABLE_NAME = "_adminlog_"; //NOSONAR
	public static String DB_TABLE_NAME = "_db_"; //NOSONAR
	public static String PROPERTIES_TABLE_NAME = "_properties_"; //NOSONAR

	protected ConfDB() {
		//utility class
	}

	public static List<ConfDetails> getConfig()
	{
		return(getConfig(null));
	}

	/**
	 * Vrati konfiguracne hodnotu, ak prefix nie je null tak zacinajuce sa na hodnotu prefixu
	 * @param prefix
	 * @return
	 */
	public static List<ConfDetails> getConfig(String prefix)
	{
		List<ConfDetails> conf = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//nacitaj data z DB
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM " + ConfDB.CONF_TABLE_NAME + " ORDER BY name");
			rs = ps.executeQuery();
			String value;
			String name;
			while (rs.next())
			{
				//for e.g. export we need unfiltered values
				name = DB.getDbString(rs, "name", false);
				value = DB.getDbString(rs, "value", false);

				if (Tools.isEmpty(prefix) || name.startsWith(prefix))
				{
					conf.add(new ConfDetails(name, value, DB.getDate(rs, "date_changed")));
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
		return conf;
	}

	public static List<ConfDetails> filterConfDetailsByPerms(Identity user, List<ConfDetails> confList)
	{
		//odfiltruj zoznam podla prav
		String configEnabledKeys = Constants.getStringExecuteMacro("configEnabledKeys");

		if (Tools.isEmpty(configEnabledKeys) || user.isEnabledItem("conf.show_all_variables")) return confList;

		List<ConfDetails> filtered = new ArrayList<>();
		String[] enabledKeys = Tools.getTokens(configEnabledKeys, ",");
		for (ConfDetails c : confList)
		{
			if (isKeyVisibleToUser(user, enabledKeys, c.getName()))
			{
				filtered.add(c);
			}
		}

		return filtered;
	}

	public static List<String> filterByPerms(Identity user, List<String> confList)
	{
		//odfiltruj zoznam podla prav
		String configEnabledKeys = Constants.getStringExecuteMacro("configEnabledKeys");

		if (Tools.isEmpty(configEnabledKeys) || user.isEnabledItem("conf.show_all_variables")) return confList;

		List<String> filtered = new ArrayList<>();
		String[] enabledKeys = Tools.getTokens(configEnabledKeys, ",");
		for (String c : confList)
		{
			if (isKeyVisibleToUser(user, enabledKeys, c))
			{
				filtered.add(c);
			}
		}

		return filtered;
	}

	public static boolean isKeyVisibleToUser(Identity user, String key) {
		//odfiltruj zoznam podla prav
		String configEnabledKeys = Constants.getStringExecuteMacro("configEnabledKeys");

		if (Tools.isEmpty(configEnabledKeys) || user.isEnabledItem("conf.show_all_variables")) return true;

		String[] enabledKeys = Tools.getTokens(configEnabledKeys, ",");
		return isKeyVisibleToUser(user, enabledKeys, key);
	}

	public static boolean isKeyVisibleToUser(Identity user, String[] enabledKeys, String key)
	{
		if (Tools.isEmpty(key) || user.isEnabledItem("conf.show_all_variables")) return true;

		for (String testKey : enabledKeys)
		{
			if (key.startsWith(testKey))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean deleteName(String name)
	{
		Connection db_conn = null;
		PreparedStatement psDelete = null;
		try
		{
			//nacitaj data z DB
			String sqlDel = "";
			//editacia DB _conf_
			if (name != null)
			{
				db_conn = DBPool.getConnection();
				sqlDel = "DELETE FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
				psDelete = db_conn.prepareStatement(sqlDel);
				psDelete.setString(1, name);
				psDelete.execute();
				//Logger.println(this,"Number of DELETED rows= " + update);
				psDelete.close();
				db_conn.close();
				psDelete = null;
				db_conn = null;
				Adminlog.add(Adminlog.TYPE_CONF_DELETE, "Zmazana konfiguracna premenna: "+name, -1, -1);

				if ("statLanguageDomain".equals(name)) StatDB.setLanguageDomainTable(null);
				if (name.startsWith("multiDomainAlias:")) MultiDomainFilter.clearDomainAlias();
				if ("responseHeaders".equals(name)) PathFilter.resetResponseHeaders();

				//if (update != 0)
				return true;
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
				if (psDelete != null)
					psDelete.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return false;
	}

	/**
	 * Pokusi sa desifrovat hodnotu ktora je zasifrovana v konfiguracii
	 * @param value
	 * @return
	 */
	public static String tryDecrypt(String value)
	{
		if (value!=null && value.length()>32 && value.startsWith("encrypted:"))
		{
			try {
				Password pass = new Password();
				String decoded = pass.decrypt(value.substring("encrypted:".length()));
				value = decoded;
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		return value;
	}

	public static boolean setName(String nameParam, String value)
	{
		boolean ret = false;
		Connection db_conn = null;
		PreparedStatement psInsert = null;
		try
		{
			String name = null;
			if (nameParam != null) {
				name = cleanTextRemoveNonPrintableCharacters(nameParam);
			}

			//vyvolaj event
			ConfDetails conf = null;
			if (name != null && value != null) {
				 conf = new ConfDetails(name, value);
			}
			if (conf != null) (new WebjetEvent<ConfDetails>(conf, WebjetEventType.ON_START)).publishEvent();

			//nacitaj data z DB
			db_conn = DBPool.getConnection();
			//editacia DB _conf_

			if (name != null && value != null)
			{
				String sqlUpdate = "UPDATE " + ConfDB.CONF_TABLE_NAME + " SET value=?, date_changed=? WHERE name=?";
				if (SetCharacterEncodingFilter.getCurrentRequestBean() != null &&
					SetCharacterEncodingFilter.getCurrentRequestBean().getUserId() > 0)
				{
					StringBuilder message = new StringBuilder("Nastavena konfiguracna premenna: ").append(name).append('\n');
					if (Tools.isNotEmpty(Constants.getString(name)))
						message.append(Constants.getString(name)).append(" => ");
					message.append(value);
					Adminlog.add(Adminlog.TYPE_CONF_UPDATE, message.toString(), -1, -1);
				}
				psInsert = db_conn.prepareStatement(sqlUpdate);
				psInsert.setString(1, value);
				psInsert.setTimestamp(2, new java.sql.Timestamp(Tools.getNow()));
				psInsert.setString(3, name);
				int update = psInsert.executeUpdate();
				psInsert.close();
				psInsert = null;
				if (update < 1)
				{
					String sqlIns = "INSERT INTO " + ConfDB.CONF_TABLE_NAME + " (name, value, date_changed) VALUES (?,?,?)";
					psInsert = db_conn.prepareStatement(sqlIns);
					psInsert.setString(1, name);
					psInsert.setString(2, value);
					psInsert.setTimestamp(3, new java.sql.Timestamp(Tools.getNow()));
					psInsert.execute();
					psInsert.close();
					psInsert = null;
				}
				ret = true;
				//refreshni hodnotu v Constants, odporucany je ale aj tak restart

				setConstantValueImpl(name, value);
			}
			db_conn.close();
			db_conn = null;

			if (conf != null) (new WebjetEvent<ConfDetails>(conf, WebjetEventType.AFTER_SAVE)).publishEvent();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (psInsert != null)
					psInsert.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return ret;
	}

	private static void setConstantValueImpl(String name, String valueParam)
	{
		String value = tryDecrypt(valueParam);

		if ("linkType".equals(name))
		{
			if ("html".equalsIgnoreCase(value))
			{
				Constants.setInt(name, Constants.LINK_TYPE_HTML);
			}
			else
			{
				Constants.setInt(name, Constants.LINK_TYPE_DOCID);
			}
		}
		else
		{
			if ("statLanguageDomain".equals(name)) StatDB.setLanguageDomainTable(null);
			else if (name.startsWith("multiDomainAlias:")) MultiDomainFilter.clearDomainAlias();
			else if ("logLevel".equals(name)) Logger.setWJLogLevel(value);
			else if ("logLevels".equals(name)) Logger.setWJLogLevels(Logger.getLogLevelsMap(value));
			else if ("cacheStaticContentSeconds".equals(name) || "cacheStaticContentSuffixes".equals(name)) PathFilter.resetCacheStaticContentSeconds();
			else if ("responseHeaders".equals(name)) PathFilter.resetResponseHeaders();
			else if ("constantsAliasSearch".equals(name)) Constants.setConstantsAliasSearch("true".equals(value));
			else if ("multiDomainFolders".equals(name)) MultiDomainFilter.clearDomainFolders();
			else if ("xssHtmlAllowedFields".equals(name)) DB.resetHtmlAllowedFields();
			else if ("ninjaNbspReplaceRegex".equals(name)) Ninja.resetNbspReplaceRegex();
			Constants.setString(name, value);
		}
	}

	/**
	 * Zadisabluje polozku menu vsetkym administratorom
	 *
	 * @param menuItemName
	 * @return
	 */
	public static List<UserDetails> disableMenuItemAll(String menuItemName)
	{
		List<UserDetails> users = new ArrayList<>();
		List<UserDetails> changedUsers = new ArrayList<>();
		if(Tools.isEmpty(menuItemName)) return changedUsers;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//nacitaj data z DB
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE is_admin=?");
			ps.setBoolean(1, true);
			rs = ps.executeQuery();
			while (rs.next())
			{
				UserDetails user = new UserDetails();
				user.setUserId(rs.getInt("user_id"));
				user.setFirstName(DB.getDbString(rs, "first_name"));
				user.setLastName(DB.getDbString(rs, "last_name"));
				users.add(user);
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
			//preiteruj cez userov a zrus im danu polozku
			boolean found;
			for (UserDetails user : users)
			{
				ps = db_conn.prepareStatement("SELECT * FROM user_disabled_items WHERE user_id=? AND item_name=?");
				ps.setInt(1, user.getUserId());
				ps.setString(2, menuItemName);
				rs = ps.executeQuery();
				found = false;
				if (rs.next())
				{
					found = true;
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
				if (!found)
				{
					ps = db_conn.prepareStatement("INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)");
					ps.setInt(1, user.getUserId());
					ps.setString(2, menuItemName);
					ps.execute();
					ps.close();
					ps = null;
					changedUsers.add(user);
				}
			}
			db_conn.close();
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
		return (changedUsers);
	}

	/**
	 * Ziskam si premennu z DB
	 *
	 * @param name -
	 *           nazov premennej
	 * @return
	 */
	public static ConfDetails getVariable(String name)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (Tools.isNotEmpty(name))
			{
				//nacitaj data z DB (uncomited je tu kvoli clustru a MonitoringManager citaniu session hodnot)
				db_conn = DBPool.getConnectionReadUncommited();
				ps = db_conn.prepareStatement("SELECT * FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?");
				ps.setString(1, name);
				rs = ps.executeQuery();
				String value;
				ConfDetails cDet = null;
				if (rs.next())
				{
					value = DB.getDbString(rs, "value");
					cDet = new ConfDetails(name, value, DB.getDate(rs, "date_changed"));
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
				return (cDet);
				//  Logger.println(this,"Name= " +varName+ "\t\t Value= " +varValue+
				// "\t\t Action= " +varAction);
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
		return (null);
	}

	/**
	 * Vrati zoznam moznych konfiguracnych premennych pre zadanu JSP stranku (pouziva pomocnik)
	 * @param url
	 * @return
	 */
	public static List<ConfDetails> getConfForJsp(String url)
	{
		List<ConfDetails> list = new ArrayList<>();
		try
		{
			//najskor uprav URL na nazov modulu
			int i = url.lastIndexOf('/');
			if (i != -1) url = url.substring(i+1);

			i = url.lastIndexOf('.');
			if (i != -1) url = url.substring(0, i);

			if ("banner_system".equals(url)) url = "banner";
			else if ("forms".equals(url)) url = "form";
			else if ("related_pages".equals(url)) url = "related-pages";
			else if ("dynamic_tags".equals(url)) url = "webpages";
			else if ("reservations".equals(url)) url = "reservation";

			Logger.debug(ConfDB.class, "getConfForJsp url = "+url);

			for (ConfDetails conf : Constants.getAllValues())
			{
				if (conf.getModules()!=null && (conf.getModules().indexOf(url)==0 || conf.getModules().indexOf(";"+url)!=-1)) list.add(conf);
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return list;
	}

	/**
	 * Vyhladavanie v konfiguracii (meno a popis)
	 * @param text
	 * @return
	 */
	public static List<ConfDetails> searchConfig(String text)
	{
		text = DB.internationalToEnglish(text).toLowerCase();
		List<ConfDetails> list = new ArrayList<>();
		try
		{
			for (ConfDetails conf : Constants.getAllValues())
			{
				if (conf.getName().toLowerCase().indexOf(text)!=-1 || DB.internationalToEnglish(conf.getDescription()).toLowerCase().indexOf(text)!=-1)
				{
					list.add(conf);
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return list;
	}

    /**
     * Obnovi v Constants konfiguracnu premmenu podla hodnoty v databaze
     * @param name
     */
    public static void refreshVariable(String name)
	 {
	 	 String value = new SimpleQuery().forString("SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?",name);
		 setConstantValueImpl(name, value);
    }

	private static String cleanTextRemoveNonPrintableCharacters(String text)
	{
		// strips off all non-ASCII characters
		text = text.replaceAll("[^\\x00-\\x7F]", "");

		// erases all the ASCII control characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

		// removes non-printable characters from Unicode
		text = text.replaceAll("\\p{C}", " ");

		text = Tools.replace(text, "'", "");
		text = Tools.replace(text, " ", "");

		return text.trim();
	}

    public static boolean setNamePrepared(String name, String value, Date datePrepared)
 	{
 		boolean ret = false;
 		Connection db_conn = null;
 		PreparedStatement psInsert = null;
 		try
 		{
 			//nacitaj data z DB
 			db_conn = DBPool.getConnection();
 			//editacia DB _conf_
 			if (name != null && value != null)
 			{
 				/*String sqlUpdate = "UPDATE " + ConfDB.CONF_PREPARED_TABLE_NAME + " SET value=?, date_changed=?, date_prepared=? WHERE name=?";
 				if (SetCharacterEncodingFilter.getCurrentRequestBean() != null &&
 					SetCharacterEncodingFilter.getCurrentRequestBean().getUserId() > 0)
 				{
 					StringBuilder message = new StringBuilder("Nastavena konfiguracna premenna: ").append(name).append('\n');
 					if (Tools.isNotEmpty(Constants.getString(name)))
 						message.append(Constants.getString(name)).append(" => ");
 					message.append(value);
 					//Adminlog.add(Adminlog.TYPE_CONF_UPDATE, message.toString(), -1, -1);
 				}
 				psInsert = db_conn.prepareStatement(sqlUpdate);
 				psInsert.setString(1, value);
 				psInsert.setTimestamp(2, new java.sql.Timestamp(Tools.getNow()));

 				if (datePrepared!=null) psInsert.setTimestamp(4, new java.sql.Timestamp(datePrepared.getTime()));
				else psInsert.setNull(4, Types.TIMESTAMP);

				psInsert.setString(3, name);

 				int update = psInsert.executeUpdate();
 				psInsert.close();
 				psInsert = null;

 				*/
				Integer userId = null;

				RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
				if (rb != null) userId = Integer.valueOf(rb.getUserId());

				String sqlIns = "INSERT INTO " + ConfDB.CONF_PREPARED_TABLE_NAME + " (name, value, date_changed, date_prepared, user_id) VALUES (?,?,?,?,?)";
				psInsert = db_conn.prepareStatement(sqlIns);
				psInsert.setString(1, name);
				psInsert.setString(2, value);
				psInsert.setTimestamp(3, new java.sql.Timestamp(Tools.getNow()));

				if (datePrepared!=null) psInsert.setTimestamp(4, new java.sql.Timestamp(datePrepared.getTime()));
				else psInsert.setNull(4, Types.TIMESTAMP);

				if (userId != null) psInsert.setInt(5, userId.intValue());
				else psInsert.setNull(5, Types.INTEGER);

				psInsert.execute();
				psInsert.close();
				psInsert = null;
 				ret = true;
 			}
 			db_conn.close();
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
 				if (psInsert != null)
 					psInsert.close();
 				if (db_conn != null)
 					db_conn.close();
 			}
 			catch (Exception ex2)
 			{
 			}
 		}
 		return ret;
 	}

 	public static boolean deleteNamePrepared(String name, long now)
 	{
 		Connection db_conn = null;
 		PreparedStatement psDelete = null;
 		try
 		{
 			String sqlDel = "";
 			if (name != null)
 			{
 				db_conn = DBPool.getConnection();
 				sqlDel = "UPDATE " + ConfDB.CONF_PREPARED_TABLE_NAME + " SET date_published=? WHERE name=? AND date_published IS NULL AND date_prepared IS NOT NULL AND date_prepared < ?";
 				psDelete = db_conn.prepareStatement(sqlDel);
				psDelete.setTimestamp(1, new Timestamp(Tools.getNow()));
 				psDelete.setString(2, name);
				psDelete.setTimestamp(3, new Timestamp(now));
 				psDelete.execute();
 				psDelete.close();
 				db_conn.close();
 				psDelete = null;
 				db_conn = null;
 				//Adminlog.add(Adminlog.TYPE_CONF_DELETE, "Zmazana prepared konfiguracna premenna: "+name, -1, -1);
 				return true;
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
 				if (psDelete != null)
 					psDelete.close();
 				if (db_conn != null)
 					db_conn.close();
 			}
 			catch (Exception ex2)
 			{
 			}
 		}
 		return false;
 	}

	/**
	 * Vrati predpripravenu konfiguracne hodnotu, ak prefix nie je null tak zacinajuce sa na hodnotu prefixu
	 * @param prefix
	 * @return
	 */
	public static List<ConfPreparedDetails> getConfigPrepared(String prefix)
	{
		List<ConfPreparedDetails> conf = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//nacitaj data z DB
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM " + ConfDB.CONF_PREPARED_TABLE_NAME + " ORDER BY name");
			rs = ps.executeQuery();
			String value;
			String name;
			while (rs.next())
			{
				name = DB.getDbString(rs, "name");
				value = DB.getDbString(rs, "value");

				if (Tools.isEmpty(prefix) || name.startsWith(prefix))
				{
					conf.add(new ConfPreparedDetails(name, value, DB.getDate(rs, "date_changed"), DB.getDate(rs, "date_prepared")));
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
			if (conf.size() > 0)
				return (conf);
			//  Logger.println(this,"Name= " +varName+ "\t\t Value= " +varValue+
			// "\t\t Action= " +varAction);
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
		return (null);
	}
}
