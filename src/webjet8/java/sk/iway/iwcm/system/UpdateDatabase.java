package sk.iway.iwcm.system;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.rest.ProductListService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stripes.SyncDirAction;
import sk.iway.iwcm.sync.WarningListener;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.spring.SpringAppInitializer;
import sk.iway.iwcm.update.DomainIdUpdateService;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;


/**
 *  Aktualizuje databazu
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Nedele, 2004, marec 7
 *@modified     $Date: 2004/03/14 20:23:31 $
 */
public class UpdateDatabase
{
	protected UpdateDatabase() {
		//utility class
	}

	/**
	 *  This method is called during startup to update database
	 */
	public static void update()
	{
		Logger.println(UpdateDatabase.class,"----- Updating database [DBType="+Constants.DB_TYPE+"] -----");
		//aktualizuj databazu
		autoUpdateDatabase();

		//aktualizuj zlozitejsie veci
		splitFullName();
		disabledItems();
		prepareSync();
		setRegDate();
		importMeniny();
		convertPerexGroups();
		fixGroupIdInStatViews();
		mediaGroupsUpdate();

		updateMediaDomainIdColumn();
		updateEmailsCampainDomainIdColumn();

		deletePoiClasses();

		// upravi uz existujuce browserId, aby sa nepocitali do statistiky kvoli novym upravam v kode
		UpdateDatabase.fixBrowserId();

		disabledItemsConfigRights();

		updateStatViewsColumns();

		updateStopwords();

		zmluvyOrganizacieUpdate();
		//zatial nie, ponechame do WJ8 updateArchiveFormat();

        setDefaultMapProvider();

		statErrorAddDomainId();

		updateInvoiceContacts();

		Logger.println(UpdateDatabase.class,"----- Database updated  -----");
	}

	/**
	 * This method is called after full startup where Spring is initialized and JPA entities are available
	 */
	public static void updateWithSpringInitialized() {
		SpringAppInitializer.dtDiff("----- Updating database with Spring/JPA initialized [DBType="+Constants.DB_TYPE+"] -----");
		updateInvoicePrices();

		if(InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
			DomainIdUpdateService.updateExportDatDomainId();
			DomainIdUpdateService.updatePerexGroupDomainId();
		}

		SpringAppInitializer.dtDiff("----- Database updated  -----");
	}

	/*
	private static void updateArchiveFormat(){
		Connection db_conn = null;
		PreparedStatement ps = null;

		db_conn = DBPool.getConnection();

		try
		{
			String sql = "INSERT INTO forms_archive SELECT f.* FROM forms f WHERE form_name like 'Archiv-%'";
			ps = db_conn.prepareStatement(sql);
			ps.execute();
			ps.close();

			sql = "DELETE FROM forms f WHERE form_name like 'Archiv-%'";
			ps = db_conn.prepareStatement(sql);
			ps.execute();
			ps.close();

			sql =  "UPDATE forms_archive SET form_name = SUBSTRING(form_name, 8) WHERE form_name like 'Archiv-%'";
			ps = db_conn.prepareStatement(sql);
			ps.execute();

		}
		catch (SQLException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			if(ps != null)
			{
				try
				{
					ps.close();
				}
				catch (SQLException e)
				{
					sk.iway.iwcm.Logger.error(e);
				}
			}
		}

	}*/

	/**
	 * Skontroluje ci je pocet stopslov v db rovnaky ako v subore a ak nie je, spravi update
	 */
	private static void updateStopwords()
	{

		int databaseCount = new SimpleQuery().forInt("select count(*) from stopword");
		int fileCount = 0;

		if (FileTools.isFile("/WEB-INF/sql/stopwords.csv")==false) return;

		Scanner scanner;
		try
		{
			scanner = new Scanner(new File(Tools.getRealPath("/WEB-INF/sql/stopwords.csv")),"UTF-8");
			while (scanner.hasNextLine())
			{
				fileCount++;
				scanner.nextLine();
			}
		}
		catch (FileNotFoundException e)
		{
			sk.iway.iwcm.Logger.error(e);
			return;
		}

		/*check count*/

		if (databaseCount != fileCount)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection();


				ps = db_conn.prepareStatement("delete from stopword");
				ps.executeUpdate();
				ps.close();

				ps = db_conn.prepareStatement("insert into stopword (word,language) values (?,?)");
				scanner.close();
				scanner = new Scanner(new File(Tools.getRealPath("/WEB-INF/sql/stopwords.csv")),"UTF-8");

				while (scanner.hasNext())
				{
					String line = scanner.nextLine();
					String[] split = line.split(";");
					String stopword = split[0].trim();
					String language = split[1].trim();
					ps.setString(1, stopword);
					ps.setString(2, language);
					ps.executeUpdate();
				}
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
				}
			}
		}
	}

	public static void updateAfterInit()
	{
		configureModules();
	}

	/**
	 * Automaticka aktualizacia databazy na zaklade XML suboru
	 */
	@SuppressWarnings("resource")
	private static void autoUpdateDatabase(String url, String dbName) throws IOException
	{
		IwcmFile f = new IwcmFile(Tools.getRealPath(url));
		if (f.exists()==false || f.canRead()==false) return;

		Logger.println(UpdateDatabase.class,"--> updating from file: " + f.getName());
		InputStream is = new IwcmInputStream(f);
		is = SyncDirAction.checkXmlForAttack(is);
		XMLDecoder decoder = new XMLDecoder(is, null, new WarningListener());
		Object objUpdates = decoder.readObject();

		if (!(objUpdates instanceof List)) return;

		List<?> updates = (List<?>)objUpdates;
		for (Object objUdb : updates)
		{

			if (!(objUdb instanceof UpdateDBBean)) continue;

			UpdateDBBean udb = (UpdateDBBean)objUdb;
			updateDB(udb, dbName);
		}
		is.close();
	}

	/**
	 * Automaticka aktualizacia databazy na zaklade XML suboru
	 */
	private static void autoUpdateDatabase()
	{
		try
		{
			autoUpdateDatabase("/WEB-INF/sql/autoupdate.xml", "iwcm");
			//update pre webjet9 a dalsie podla skinu
			autoUpdateDatabase("/WEB-INF/sql/autoupdate-"+Constants.getString("defaultSkin")+".xml", "iwcm");

			autoUpdateDatabase("/WEB-INF/sql/autoupdate-"+Constants.getInstallName()+".xml", "iwcm");

			//check for autoupdate-INSTALL_NAME-DBNAME.xml
			IwcmFile dir = new IwcmFile(Tools.getRealPath("/WEB-INF/sql"));
			if (dir.isDirectory())
			{
				IwcmFile[] files = dir.listFiles();
				int size = files.length;
				int i;
				for (i=0; i<size; i++)
				{
					IwcmFile f = files[i];
					if (f.isFile() && f.getName().startsWith("autoupdate-"+Constants.getInstallName()))
					{
						int start = ("autoupdate-"+Constants.getInstallName()).length() + 1;
						int end = f.getName().length() - 4;
						if (start>end)
							continue;

						String dbName = f.getName().substring(start, end).trim();

						Logger.println(UpdateDatabase.class,"--> updating from file: " + f.getName() + " database: " + dbName);
						autoUpdateDatabase("/WEB-INF/sql/"+f.getName(), dbName);
					}
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private static Set<String> allreadyExecutedUpdates = null;
	public static boolean isAllreadyUpdated(String note)
	{
		if (allreadyExecutedUpdates==null)
		{
			List<String> notes = new SimpleQuery().forListString("SELECT note FROM "+ConfDB.DB_TABLE_NAME);
			allreadyExecutedUpdates = new HashSet<>(notes);
		}

		if (allreadyExecutedUpdates.contains(note)) return true;

		return false;
	}

	public static void saveSuccessUpdate(String note)
	{
		String sqlUpdate = "INSERT INTO "+ConfDB.DB_TABLE_NAME+" (create_date, note) VALUES (?, ?)";
		new SimpleQuery().execute(sqlUpdate, new Timestamp(Tools.getNow()), note);

		if (allreadyExecutedUpdates != null) allreadyExecutedUpdates.add(note);
	}

	/**
	 * Skontroluje, ci treba aktualizovat DB, ak ano, aktuzlizuje
	 * @param udb - Bean s popisom aktualizacie
	 * @return
	 */
	private static boolean updateDB(UpdateDBBean udb, String dbName)
	{
		if (isAllreadyUpdated(udb.getNote())) return true;

		boolean ret = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		Statement sta = null;
		ResultSet rs = null;
		String sql = null;
		try
		{
			db_conn = DBPool.getConnection(dbName);

			boolean updateSuccess;
			Logger.println(UpdateDatabase.class,"   " + udb.getNote()+" ");
			//aktualizuj DB
			if (Constants.DB_TYPE == Constants.DB_MYSQL)
			{
				sql = udb.getMysql();
			}
			else if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				sql = udb.getMssql();
			}
			else if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				sql = udb.getOracle();
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				sql = udb.getPgsql();
			}

			updateSuccess = true;
			if (Tools.isNotEmpty(sql))
			{
				StringTokenizer st = new StringTokenizer(sql, ";");
				int counter = 1;
				int count = st.countTokens();
				Logger.println(UpdateDatabase.class, "count="+count+" ");
				String defaultEngine = Constants.getString("mariaDbDefaultEngine");
				while (st.hasMoreTokens())
				{
					try
					{

						sql = st.nextToken().trim();

						//na public nodoch nevykonavam GRANT prikazy
						if (sql.indexOf("GRANT")!=-1 && "public".equals(Constants.getString("clusterMyNodeType"))) continue;

						if (sql.contains("{CONSTRAIN:")) {
							//MSSQL nema drop constrain podla mena ale musi sa najskor ziskat z DB presny nazov, az potom sa da spravit drop
							try {
								String tableName = sql.substring(sql.indexOf("ALTER TABLE")+11, sql.indexOf("DROP CONSTRAINT")).trim();
								int i = sql.indexOf("{CONSTRAIN:");
								String constrainName = sql.substring(sql.indexOf(":", i)+1, sql.indexOf("}", i));
								String constrainSql;
								if ("PK".equals(constrainName)) {
									constrainSql = "SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = N'"+tableName+"'";
								} else {
									constrainSql = "SELECT name FROM sys.default_constraints WHERE OBJECT_NAME(parent_object_id) = N'"+tableName+"' AND name LIKE '%__"+constrainName+"__%'";
								}

								String constrainKey = new SimpleQuery(dbName).forString(constrainSql);

								String replaceText = sql.substring(i, sql.indexOf("}", i)+1);
								sql = Tools.replace(sql, replaceText, constrainKey);
							}
							catch (Exception ex) {
								Logger.error(UpdateDatabase.class, ex);
							}
						}

						if (Tools.isNotEmpty(sql) && sql.startsWith("//")==false)
						{
							if (Constants.DB_TYPE == Constants.DB_ORACLE)
							{
								sql = Tools.replace(sql, "|", ";");
								sql = Tools.replace(sql, ";;", "||");
							} else if (Constants.DB_TYPE == Constants.DB_MYSQL && "myisam".equalsIgnoreCase(defaultEngine)==false) {
								sql = Tools.replace(sql, "ENGINE=MyISAM", "ENGINE="+defaultEngine);
								sql = Tools.replace(sql, "engine=MyISAM", "ENGINE="+defaultEngine);
							}
							Logger.println(UpdateDatabase.class, "["+counter+"/"+count+"] "+sql);
							counter++;

							sta = db_conn.createStatement();
							sta.execute(sql);
							sta.close();

							Logger.println(UpdateDatabase.class, "[OK] ");

							if (sql.toLowerCase().indexOf(ConfDB.CONF_TABLE_NAME)!=-1) updateConstantsValue(sql);
						}

					}
					catch (SQLException e)
					{
						String message = e.getMessage().toLowerCase();
						//POZOR: message je LOWER CASE, tak aj porovnanie musi byt
						if (
								message.contains("already exists") ||
								message.contains("duplicate column name") ||
								message.contains("is specified more than once") ||
								message.contains("duplicate entry") ||
								message.contains("already an object") ||
								message.contains("tabuľke existuje") ||
								message.contains("duplicate key") ||
								message.contains("názov už používa existujúci objekt") ||
								message.contains("už existuje") ||
								message.contains("already used") ||
								message.contains("porušenie jedinečného obmedzenia") ||
								message.contains("already indexed") ||
								(message.contains("can't drop column") && message.contains("check that it exists")) ||
								(message.contains("can't drop index") && message.contains("check that it exists")) ||
								message.contains("ora-01442 ") || message.contains("stĺpec, ktorý má byť modifikovaný na not null, je už not null") ||
								//mssql premenovanie stlpca, ktory uz je premenovany
								message.contains("either the parameter @objname is ambiguous or the claimed @objtype (column) is wrong") ||
								message.contains("column to be modified to NULL cannot be modified to NULL")

							)
						{
							//uz existuje, je to OK
							Logger.println(UpdateDatabase.class, "[EXISTS] ");
							Logger.debug(UpdateDatabase.class, e.getMessage());
						}
						else if (sql.contains("DROP") && message.contains("does not exist"))
						{
							//uz neexistuje, je to OK
							Logger.println(UpdateDatabase.class, "[NOT EXISTS] ");
							Logger.debug(UpdateDatabase.class, e.getMessage());
						}
						else
						{
							//Logger.System.out.println("SQL: "+sql);
							Logger.error(UpdateDatabase.class,String.format("note: %s, [FAIL], %nSQL: %s", udb.getNote(), sql));
							//sk.iway.iwcm.Logger.error(e);
							sk.iway.iwcm.Logger.error(e);
							updateSuccess = false;
						}
					}
					finally
					{
						try
						{
							if (sta != null)
								sta.close();
						}
						catch (Exception ex2)
						{
						}
					}
				}
			}

			if (updateSuccess)
			{
				//zapis, ze je to aktualizovane
				saveSuccessUpdate(udb.getNote());
				Logger.println(UpdateDatabase.class,"[OK]");
			}

			rs = null;
			ps = null;
			sta = null;
		}
		catch (Exception ex)
		{
			Logger.error(UpdateDatabase.class,"SQL: " + sql);
			sk.iway.iwcm.Logger.error(ex);
			Logger.println(UpdateDatabase.class,udb.getNote() + " [FAIL]");
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
				if (sta != null)
					sta.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return(ret);
	}

	/**
	 * ak SQL prikaz obsahoval insert do conf tabulky skus danu hodnotu updatnut, kedze updateDB sa vykona az po nahrati Constants z DB
	 * @param sql - INSERT INTO webjet_conf (name, value) VALUES ('editorNewDocDefaultAvailableChecked', 'false')
	 */
	private static void updateConstantsValue(String sql)
	{
		try
		{
			ConfDetails conf = null;
			if (sql.toLowerCase().indexOf("update "+ConfDB.CONF_TABLE_NAME)!=-1)
			{
				//update
				//UPDATE webjet_conf SET serverMonitoringEnable='false'
				int setStart = sql.indexOf("SET ");
				if (setStart == -1) return;
				int rovnasa = sql.indexOf('=', setStart);
				if (rovnasa < setStart) return;

				//ziskaj meno
				String confName = sql.substring(setStart+4, rovnasa);
				conf = ConfDB.getVariable(confName);
			}
			else
			{
				//insert
				//INSERT INTO webjet_conf (name, value) VALUES ('editorNewDocDefaultAvailableChecked', 'false')
				int apostrofStart = sql.indexOf('\'');
				if (apostrofStart == -1) return;
				int apostrofKoniec = sql.indexOf('\'', apostrofStart+1);
				if (apostrofKoniec < apostrofStart) return;

				//ziskaj meno
				String confName = sql.substring(apostrofStart+1, apostrofKoniec);
				conf = ConfDB.getVariable(confName);
			}

			if (conf == null && sql.contains("SET name='syncGroupAndWebpageTitle' WHERE name='groupCreateBlankWebpageAfterCreate'")) {
				//specialita rozdelenia konf. premennej
				conf = ConfDB.getVariable("syncGroupAndWebpageTitle");
				if (conf != null) {
					conf.setName("syncGroupAndWebpageTitle");

					//vytvor nanovo aj povodnu premennu, ta bola premenovana
					ConfDB.setName("groupCreateBlankWebpageAfterCreate", conf.getValue());
					if (ClusterDB.isServerRunningInClusterMode()) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-groupCreateBlankWebpageAfterCreate");
				}
			}

			if (conf != null)
			{
				Logger.debug(UpdateDatabase.class, "Updating Constants name="+conf.getName()+" value="+conf.getValue());
				ConfDB.setName(conf.getName(), conf.getValue());
				if (ClusterDB.isServerRunningInClusterMode()) ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-"+conf.getName());
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Konverzia pristupovych prav na novy format
	 */
	private static void disabledItems()
	{
		//najskor skontroluj, ci je uz vytvorena tabulka
		if (isAllreadyUpdated("disabled items pouzivatelov")==false) return;

		String note = "Konverzia pristupovych prav";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			int i;
			//nacitaj si zoznam userov
			ps = db_conn.prepareStatement("SELECT * FROM  users WHERE is_admin=?");
			ps.setBoolean(1, true);
			rs = ps.executeQuery();
			List<Identity> users = new ArrayList<>();
			boolean isMenu;
			StringBuilder menus;
			while (rs.next())
			{
				Identity user = new Identity();
				user.setUserId(rs.getInt("user_id"));
				user.setFirstName(DB.getDbString(rs, "first_name"));
				user.setLastName(DB.getDbString(rs, "last_name"));
				//davame to do company!!!
				user.setCompany(DB.getDbString(rs, "disabled_items"));

				menus = new StringBuilder();
				for (i = 0; i < 15; i++)
				{
					isMenu = rs.getBoolean("menu" + i);
					//user.setMenu(i, );
					if (isMenu)
					{
						menus.append('1');
					}
					else
					{
						menus.append('0');
					}
				}
				user.setAdress(menus.toString());

				users.add(user);
			}
			rs.close();
			ps.close();

			//vymaz tabulku prav
			ps = db_conn.prepareStatement("DELETE FROM user_disabled_items");
			ps.execute();
			ps.close();

			String[] menuNames = new String[15];
			menuNames[0] = "menuWebpages";
			menuNames[1] = "menuTemplates";
			menuNames[2] = "menuUsers";
			menuNames[3] = "menuStats";
			menuNames[4] = "menuEmail";
			menuNames[5] = "menuCalendar";
			menuNames[6] = "menuForms";
			menuNames[7] = "menu_7";
			menuNames[8] = "editorMiniEdit";
			menuNames[9] = "menuQa";
			menuNames[10] = "menuFbrowser";
			menuNames[11] = "menuInquiry";
			menuNames[12] = "menuGallery";
			menuNames[13] = "menu_13";
			menuNames[14] = "menu_14";

			//ok, mame userov, naimportuj to do novej tabulky
			StringTokenizer st;
			String disabledItem;
			for (Identity user : users)
			{
				Logger.println(UpdateDatabase.class,"Konverujem prava: user_id="+user.getUserId()+" name="+DB.internationalToEnglish(user.getFullName()));
				//najskor zapis menu

				for (i = 0; i < 15; i++)
				{
					if (user.getAdress().charAt(i) == '0' && menuNames[i].length()>1)
					{
						ps = db_conn.prepareStatement("INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)");
						ps.setInt(1, user.getUserId());
						ps.setString(2, menuNames[i]);
						ps.execute();
						ps.close();
					}
				}

				//teraz zapis disabled items - disabled items mame v company
				st = new StringTokenizer(user.getCompany(), ",");
				while (st.hasMoreTokens())
				{
					disabledItem = st.nextToken();
					if (disabledItem.length()>1)
					{
						ps = db_conn.prepareStatement("INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)");
						ps.setInt(1, user.getUserId());
						ps.setString(2, disabledItem);
						ps.execute();
						ps.close();
					}
				}
			}

			//zapis, ze sme to uz skonvertovali
			saveSuccessUpdate(note);

			//dropni fields
			String sql = "ALTER TABLE  users DROP ";
			if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				sql = "ALTER TABLE  users DROP COLUMN ";
			}
			//dropni menuXX
			for (i = 0; i < 15; i++)
			{
				ps = db_conn.prepareStatement(sql+"menu"+i);
				ps.execute();
				ps.close();
			}

			ps = db_conn.prepareStatement(sql+"disabled_items");
			ps.execute();
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
				if (db_conn!=null) db_conn.close();
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}
	}

	/**
	 * Rozdelenie celeho mena na titul, meno, priezvisko
	 */
	private static void splitFullName()
	{
		//najskor skontroluj, ci je uz vytvorena tabulka
		if (isAllreadyUpdated("rozdelenie full name na meno a priezvisko")==false) return;

		String note = "implemetacia rozdelenia full name";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			//nacitaj si zoznam userov
			ps = db_conn.prepareStatement("SELECT * FROM  users");
			rs = ps.executeQuery();
			List<Identity> users = new ArrayList<>();
			while (rs.next())
			{
				Identity user = new Identity();
				user.setUserId(rs.getInt("user_id"));
				user.splitFullName(DB.getDbString(rs, "full_name"));
				users.add(user);
			}
			rs.close();
			ps.close();

			//ok, mame userov, rozdel im meno a priezvisko
			for (Identity user : users)
			{
				Logger.println(UpdateDatabase.class,"Konverujem cele meno: user_id="+user.getUserId()+" name="+DB.internationalToEnglish(user.getFullName()));

				ps = db_conn.prepareStatement("UPDATE  users SET title=?, first_name=?, last_name=? WHERE user_id=?");
				ps.setString(1, user.getTitle());
				ps.setString(2, user.getFirstName());
				ps.setString(3, user.getLastName());
				ps.setInt(4, user.getUserId());
				ps.execute();
				ps.close();
			}

			//zapis, ze sme to uz skonvertovali
			saveSuccessUpdate(note);

			String sql = "ALTER TABLE  users DROP full_name";
			if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				sql = "ALTER TABLE  users DROP COLUMN full_name";
			}
			//dropni full name
			ps = db_conn.prepareStatement(sql);
			ps.execute();
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
				if (db_conn!=null) db_conn.close();
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}
	}

	/**
	 * Priprava synchronizacie medzi live a stage serverom
	 *
	 */
	private static void prepareSync()
	{
		//najskor skontroluj, ci je uz vytvorena tabulka
		if (isAllreadyUpdated("id a stav synchronizacie (status: 0=novy, 1=updated, 2=synchronized)")==false) return;

		String note = "priprava synchronizacie";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			Logger.println(UpdateDatabase.class,"PREPARE SYNC: groups");

			//groups
			ps = db_conn.prepareStatement("UPDATE groups SET sync_id=group_id, sync_status=2");
			ps.execute();
			ps.close();

			Logger.println(UpdateDatabase.class,"PREPARE SYNC: documents");

			//	documents
			ps = db_conn.prepareStatement("UPDATE documents SET sync_id=doc_id, sync_status=2");
			ps.execute();
			ps.close();

			Logger.println(UpdateDatabase.class,"PREPARE SYNC: documents_history");

			//	documents_history
			ps = db_conn.prepareStatement("UPDATE documents_history SET sync_id=history_id, sync_status=2");
			ps.execute();
			ps.close();

			//zapis, ze sme to uz skonvertovali
			saveSuccessUpdate(note);

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
				if (db_conn!=null) db_conn.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}
	}

	/**
	 * Nastavi registracny datum pouzivatelom (podla last_logon, alebo na aktualny datum)
	 *
	 */
	private static void setRegDate()
	{
		String note = "nastavenie reg date pouzivatelov";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			Logger.println(UpdateDatabase.class,"Nastavenie reg date pouzivatelov");

			ps = db_conn.prepareStatement("UPDATE  users SET reg_date=last_logon WHERE reg_date IS NULL");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("UPDATE  users SET reg_date=? WHERE reg_date IS NULL");
			ps.setTimestamp(1, new Timestamp(Tools.getNow()));
			ps.execute();
			ps.close();

			//zapis, ze sme to uz skonvertovali
			saveSuccessUpdate(note);

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
				if (db_conn!=null) db_conn.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}
	}



	private static void importMeniny()
	{
		//skontroluj ci existuje DB tabulka
		if (isAllreadyUpdated("kalendar s meninami")==false) return;

		String note = "import kalendara menin z excelu";
		if (isAllreadyUpdated(note)) return;

		try
		{
			FileInputStream in = new FileInputStream(Tools.getRealPath("/WEB-INF/sql/meniny.xls"));
			MeninyImport mi = new MeninyImport(in, null, null);
			Prop prop = Prop.getInstance(Constants.getServletContext(), "sk", false);
			mi.doImport(prop);

			//zapis do DB, ze je to importnute
			saveSuccessUpdate(note);

			in.close();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 * Zkonvertuje perex_group v tab. documents z perex_group_name na
	 * perex_group_id. Novy zapis je vo forme ",ID," .
	 *
	 */
	public static void convertPerexGroups()
	{
		String note = "konverzia perex_group v tab. documents z name na id";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			Logger.println(UpdateDatabase.class,"Converting perex_group");
			Map<String, Integer> perexGroups = new Hashtable<>();
			ps = db_conn.prepareStatement("SELECT * FROM perex_groups");
			rs = ps.executeQuery();
			while (rs.next())
			{
				perexGroups.put(DB.getDbString(rs, "perex_group_name"), Integer.valueOf(rs.getInt("perex_group_id")));
			}
			rs.close();
			ps.close();

			if (perexGroups.size() > 0)
			{
				String gName;
				String perexGroupStr = null;
				StringBuilder newPerexGroupStr;
				StringTokenizer st;
				List<DocDetails> groups = new ArrayList<>();

				ps = db_conn.prepareStatement("SELECT * FROM documents WHERE perex_group IS NOT NULL");
				rs = ps.executeQuery();
				while (rs.next())
				{
					DocDetails docDet = new DocDetails();
					docDet.setDocId(rs.getInt("doc_id"));
					docDet.setPerexGroupString(DB.getDbString(rs, "perex_group"));
					groups.add(docDet);
				}
				rs.close();
				ps.close();

				for (DocDetails docDet : groups)
				{
					newPerexGroupStr = null;
					perexGroupStr = docDet.getPerexGroupString();
					st = new StringTokenizer(perexGroupStr, ",");
					while (st.hasMoreTokens())
					{
						gName = st.nextToken().trim();
						if (perexGroups.get(gName) != null)
						{
							if (newPerexGroupStr==null)
							{
								newPerexGroupStr = new StringBuilder(",").append(perexGroups.get(gName)).append(',');
							}
							else
							{
								newPerexGroupStr.append(perexGroups.get(gName)).append(',');
							}
						}
					}
					//Logger.println(UpdateDatabase.class,"****\nDocID: " +docDet.getDocId()+ "\nold groups: " +perexGroupStr+ "\nnew groups; " +newPerexGroupStr);

					if (newPerexGroupStr!=null)
					{
						ps = db_conn.prepareStatement("UPDATE documents SET perex_group=? WHERE doc_id=?");
						ps.setString(1, newPerexGroupStr.toString());
						ps.setInt(2, docDet.getDocId());
						ps.execute();
						ps.close();
					}
				}
				//Logger.println(UpdateDatabase.class,"****");
			}

			//zapis, ze sme to uz skonvertovali
			saveSuccessUpdate(note);

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
				if (db_conn!=null) db_conn.close();
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}
	}

	/**
	 * ImportImpl cistej databazy
	 * @return - null, alebo text chybovej hlasky
	 */
	@SuppressWarnings("java:S106")
	public static String fillEmptyDatabaseMySQL()
	{
		System.out.println("fillEmptyDatabaseMySQL");

		StringBuilder errMsg = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = null;
		try
		{
			//over, ci mame nejaku databazu
			boolean hasDatabase = false;

			System.out.println("fillEmptyDatabaseMySQL 1");

			db_conn = DBPool.getConnection();
			try
			{
				ps = db_conn.prepareStatement("SHOW TABLES");
				rs = ps.executeQuery();
				if (rs.next())
				{
					hasDatabase = true;
				}
			}
			finally
			{
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}

			System.out.println("fillEmptyDatabaseMySQL 2");

			System.out.println("hasDatabase="+hasDatabase);

			if (hasDatabase)
			{
				return(null);
			}

			//	nacitaj obsah suboru
			String data = FileTools.readFileContent("/WEB-INF/sql/blank_web.sql", "utf-8");
			String defaultEngine = Constants.getString("mariaDbDefaultEngine");

			//System.out.println(data);

			StringTokenizer st = new StringTokenizer(data, ";");

			while (st.hasMoreTokens())
			{
				sql = st.nextToken();
				if (Tools.isNotEmpty(sql))
				{
					if ("myisam".equalsIgnoreCase(defaultEngine)==false) {
						sql = Tools.replace(sql, "ENGINE=MyISAM", "ENGINE="+defaultEngine);
						sql = Tools.replace(sql, "engine=MyISAM", "ENGINE="+defaultEngine);
					}

					System.out.println("Executing: "+sql);

					ps = db_conn.prepareStatement(sql);
					ps.execute();
					ps.close();
				}
			}

			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			if (ex.getMessage()!=null) errMsg = new StringBuilder(ex.getMessage());
			if (sql != null && errMsg!=null)
			{
				errMsg.append(" - ").append(sql);
			}
			sk.iway.iwcm.Logger.error(ex);
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

		if (errMsg == null) return null;

		return(errMsg.toString());
	}

	/**
	 * ImportImpl cistej databazy
	 * @return - null, alebo text chybovej spravy
	 */
	public static String fillEmptyDatabaseMSSQL()
	{
		StringBuilder errMsg = new StringBuilder();

		Connection db_conn = null;
		Statement ps = null;
		ResultSet rs = null;
		String sql = null;
		try
		{
			//over, ci mame nejaku databazu
			boolean hasDatabase = false;

			db_conn = DBPool.getConnection();
			try
			{
				ps = db_conn.createStatement();
				rs = ps.executeQuery("SELECT * FROM "+ConfDB.CONF_TABLE_NAME);
				if (rs.next())
				{

				}
				hasDatabase = true;
			}
			catch (Exception ex)
			{
				//databaza nie je naplnena
			}
			finally
			{
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}

			if (hasDatabase)
			{
				return(null);
			}

			//	nacitaj obsah suboru
			String data = FileTools.readFileContent("/WEB-INF/sql/blank_web_mssql.sql", "utf-8");
			StringTokenizer st = new StringTokenizer(data, ";");
			while (st.hasMoreTokens())
			{
				sql = st.nextToken();
				if (Tools.isNotEmpty(sql))
				{
					Logger.println(UpdateDatabase.class,"Executing: "+sql);

					ps = db_conn.createStatement();
					ps.execute(sql);
					ps.close();
				}
			}

			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			errMsg.append(ex.getMessage());
			if (sql != null)
			{
				errMsg.append(" - ").append(sql);
			}
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

		return(errMsg.toString());
	}

	/**
	 * Naplnenie oracle databazy (ked je prazdna)
	 * @return
	 */
	public static String fillEmptyDatabaseOracle()
	{
		Logger.println(UpdateDatabase.class,"fillEmptyDatabaseOracle");

		StringBuilder errMsg = new StringBuilder();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = null;
		try
		{
			//over, ci mame nejaku databazu
			boolean hasDatabase = false;

			db_conn = DBPool.getConnection();
			try
			{
				ps = db_conn.prepareStatement("SELECT * FROM documents");
				rs = ps.executeQuery();
				if (rs.next())
				{

				}
				hasDatabase = true;
			}
			catch (Exception ex)
			{
				//databaza nie je naplnena
			}
			finally
			{
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}

			//Logger.println(UpdateDatabase.class,"Oracle hasDatabase="+hasDatabase);

			if (hasDatabase)
			{
				return(null);
			}

			//	nacitaj obsah suboru
			String data = FileTools.readFileContent("/WEB-INF/sql/blank_web_oracle.sql", "utf-8");
			StringTokenizer st = new StringTokenizer(data, ";");
			java.sql.Statement s;
			while (st.hasMoreTokens())
			{
				sql = st.nextToken().trim();
				if (Tools.isNotEmpty(sql) && sql.startsWith("#")==false)
				{
					sql = Tools.replace(sql, "|", ";");
					Logger.println(UpdateDatabase.class,"Executing: "+sql);
					if (sql.indexOf("TRIGGER")!=-1)
					{
						sql = sql.replace('\n', ' ');
						sql = sql.replace('\r', ' ');
						sql = sql.replace('\t', ' ');
					}
					s = db_conn.createStatement();
					s.execute(sql);
					s.close();
					if (Constants.getBoolean("jtdsCommit")) db_conn.commit();
				}
			}

			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			errMsg.append(ex.getMessage());
			if (sql != null)
			{
				errMsg.append(" - ").append(sql);
			}
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

		return(errMsg.toString());
	}

	@SuppressWarnings("java:S106")
	public static String fillEmptyDatabasePgSQL(String schema)
	{
		System.out.println("fillEmptyDatabasePgSQL, schema="+schema);

		StringBuilder errMsg = null;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = null;
		try
		{
			//over, ci mame nejaku databazu
			boolean hasDatabase = false;

			System.out.println("fillEmptyDatabasePgSQL 1");

			db_conn = DBPool.getConnection();
			try
			{
				ps = db_conn.prepareStatement("SELECT * FROM documents");
				rs = ps.executeQuery();
				if (rs.next())
				{
					hasDatabase = true;
				}
			}
			catch (Exception ex)
			{
				//databaza nie je naplnena
			}
			finally
			{
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}

			System.out.println("fillEmptyDatabasePgSQL 2");

			System.out.println("hasDatabase="+hasDatabase);

			if (hasDatabase)
			{
				return(null);
			}

			//	nacitaj obsah suboru
			String data = FileTools.readFileContent("/WEB-INF/sql/blank_web_pgsql.sql", "utf-8");

			//System.out.println(data);

			StringTokenizer st = new StringTokenizer(data, ";");

			while (st.hasMoreTokens())
			{
				sql = st.nextToken();
				if (Tools.isNotEmpty(sql))
				{
					if ("webjet_cms".equals(schema)==false) {
						sql = Tools.replace(sql, "CREATE SCHEMA IF NOT EXISTS \"webjet_cms\"", "CREATE SCHEMA IF NOT EXISTS \""+schema+"\"");
						sql = Tools.replace(sql, "SCHEMA \"webjet_cms\"", "SCHEMA \""+schema+"\"");
						sql = Tools.replace(sql, "\"webjet_cms\".", "\""+schema+"\".");
					}

					System.out.println("Executing: "+sql);

					ps = db_conn.prepareStatement(sql);
					ps.execute();
					ps.close();
				}
			}

			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			if (ex.getMessage()!=null) errMsg = new StringBuilder(ex.getMessage());
			if (sql != null && errMsg!=null)
			{
				errMsg.append(" - ").append(sql);
			}
			sk.iway.iwcm.Logger.error(ex);
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

		if (errMsg == null) return null;

		return(errMsg.toString());
	}

	private static void configureModules()
	{
		if ("public".equals(Constants.getString("clusterMyNodeType")))
		{
			return;
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement("UPDATE user_disabled_items SET item_name=? WHERE item_name=?");
			ps.setString(1, "cmp_stat");
			ps.setString(2, "menuStats");
			ps.execute();
			ps.close();
			ps = null;

			ps = db_conn.prepareStatement("UPDATE user_disabled_items SET item_name=? WHERE item_name=?");
			ps.setString(1, "cmp_calendar");
			ps.setString(2, "menuCalendar");
			ps.execute();
			ps.close();
			ps = null;

			ps = db_conn.prepareStatement("UPDATE user_disabled_items SET item_name=? WHERE item_name=?");
			ps.setString(1, "cmp_form");
			ps.setString(2, "menuForms");
			ps.execute();
			ps.close();
			ps = null;

			//zadisabluj moduly, ktore sa objavili prvy krat
			List<ModuleInfo> modules = Modules.getInstance().getAvailableModules();
			for (ModuleInfo mi : modules)
			{
				if (mi.isDefaultDisabled())
				{
					disableFirstTimeModule(mi.getItemKey(), db_conn);
				}
				if (mi.getSubmenus()!=null && mi.getSubmenus().size()>0)
				{
					for (ModuleInfo submi : mi.getSubmenus())
					{
						if (submi.isDefaultDisabled()) disableFirstTimeModule(submi.getItemKey(), db_conn);
					}
				}
			}

			if(db_conn != null)
			{
				db_conn.close();
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
				if (db_conn != null)
					db_conn.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}

	/**
	 * Zakaze modul vsetkym pouzivatelom, ak je to prva inicializacia modulu
	 * @param itemKey
	 * @param db_conn
	 * @throws SQLException
	 */
	private static void disableFirstTimeModule(String itemKey, Connection db_conn)
	{
		String note = "NEW MODULE: "+itemKey;
		if (isAllreadyUpdated(note)) return;

		List<UserDetails> changedUsers = ConfDB.disableMenuItemAll(itemKey);
		Logger.println(UpdateDatabase.class,"Disabling " + itemKey + " for all users ("+changedUsers.size()+")");

		saveSuccessUpdate(note);
	}

	private static void fixGroupIdInStatViews()
	{
		//	otestuj, ci uz nebol importovany subor
		String note = "nastavenie hodnot group_id pre tabulku stat_views";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			String sql = "SELECT doc_id, title, navbar, external_link, group_id, virtual_path, available, show_in_menu FROM documents ORDER BY doc_id ASC";

			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();
			List<DocDetails> list = new ArrayList<>();
			while (rs.next())
			{
				DocDetails doc = new DocDetails();
				doc.setDocId(rs.getInt("doc_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				doc.setNavbar(DB.getDbString(rs, "navbar"));
				doc.setExternalLink(DB.getDbString(rs, "external_link"));
				doc.setGroupId(rs.getInt("group_id"));
				doc.setVirtualPath(DB.getDbString(rs, "virtual_path"));
				doc.setAvailable(rs.getBoolean("available"));
				doc.setShowInMenu(rs.getBoolean("show_in_menu"));

				list.add(doc);
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;

			int counter = 0;
			int size = list.size();
			DebugTimer timer = new DebugTimer("");
			for (DocDetails doc : list)
			{
				counter++;
				Logger.info(UpdateDatabase.class, "Updating stat ["+counter+"/"+size+"]: docId="+doc.getDocId()+" title="+DB.internationalToEnglish(doc.getTitle()));
				ps = db_conn.prepareStatement("UPDATE stat_views SET group_id=? WHERE doc_id=?");
				ps.setInt(1, doc.getGroupId());
				ps.setInt(2, doc.getDocId());
				ps.execute();
				ps.close();
				ps = null;

				ps = db_conn.prepareStatement("UPDATE stat_views SET last_group_id=? WHERE last_doc_id=?");
				ps.setInt(1, doc.getGroupId());
				ps.setInt(2, doc.getDocId());
				ps.execute();
				ps.close();
				ps = null;

				Logger.info(UpdateDatabase.class, " [OK] - " + timer.getDiff() + " ms");
			}

			db_conn.close();
			ps = null;
			db_conn = null;

			//zapis do DB, ze je to importnute
			saveSuccessUpdate(note);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn!=null) db_conn.close();
				if (rs!=null) rs.close();
				if (ps!=null) ps.close();
			}
			catch (Exception ex2)
			{

			}
		}
	}

	public static void deletePoiClasses()
	{
		IwcmFile f = new IwcmFile(Tools.getRealPath("/WEB-INF/classes/org/apache/poi/"));
		FileTools.deleteDirTree(f);
	}

	/**
	 * Funkcia, ktora vsetky vyskyty browserId v existujuich tabulkach zvysi o konstantu 1 500 000, nad ktorou identifikujeme neprihlasenych pouzivatelov
	 * kvoli integrite statistickych zaznamov, ovplyvnuje tabulku stat_from a stat_views
	 */
	public static void fixBrowserId()
	{
		fixBrowserId(false);
	}


	public static void fixBrowserId(boolean forceUpdate)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String note = "14.3.2009 [kmarton] nastavenie spravnych hodnot browserId kvoli kompatibilite noveho kodu, ovplynuje tabulku stat_from a stat_views";

		try
		{
			boolean found = false;

			if (forceUpdate == false)
			{
				//	otestuj, ci uz takato operacia neprebehla
				if (isAllreadyUpdated(note)) found = true;
			}

			if (found == true || Constants.getBoolean("updateDisableFixBrowserId"))
			{
				return;
			}

			db_conn = DBPool.getConnection();

			//vygeneruje vsetky mozne nazvy tabuliek, ktore obsahuju browserId (hlavne parcionovane stat_views) a ulozi do Listu
			List<String> tableBrowserIdNames = UpdateDatabase.generateBrowserIdTableNames();

			int count=1;

			for (String tableBrowserIdName : tableBrowserIdNames)
			{
				Logger.println(UpdateDatabase.class, "Updating browserId in table "+tableBrowserIdName+" "+count+"/"+tableBrowserIdNames.size());

				String sql = "UPDATE " + tableBrowserIdName + " SET browser_id = browser_id + " + Constants.getString("unloggedUserBrowserId") + " WHERE browser_id < "+Constants.getString("loggedUserBrowserId");

				try
				{
					ps = db_conn.prepareStatement(sql);
					int updated = ps.executeUpdate();
					Logger.println(UpdateDatabase.class, "Updated "+tableBrowserIdName+" "+updated+" rows");
					ps.close();
					ps = null;
				}
				catch (Exception e)
				{
					// ak tabulka s danym nazvom neexistuje, vypise to a pokracuje
					Logger.info(UpdateDatabase.class, "\nTable - " + tableBrowserIdName + " doesn't exist.\n");
					try
					{
						if (ps != null) ps.close();
					}
					catch (Exception ex2) {}
				}
				count++;
			}

			if (forceUpdate == false)
			{
				saveSuccessUpdate(note);
			}

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
	}
	/**
	 * Funkcia, ktora vrati v Liste vsetky mozne nazvy tabuliek, ktore obsahuju browserId, ktore je potrebne zmenit
	 * <br /> Ak je nastavena konstanta statEnableTablePartitioning na false, vratia sa iba tabulky stat_from a stat_views
	 * <br /> Inak sa vratia vsetky particiovane tabulky od 2000_1 po aktualnu
	 *
	 * @author kmarton
	 * @return zoznam nazvov tabuliek
	 */
	private static List<String> generateBrowserIdTableNames()
	{
		List<String> tableBrowserIdNames = new ArrayList<>();

		tableBrowserIdNames.add("stat_from");
		tableBrowserIdNames.add("stat_views");

		if (!Constants.getBoolean("statEnableTablePartitioning"))
			return tableBrowserIdNames;

		Calendar cal = Calendar.getInstance();

		for (int year = 2000; year < (cal.get(Calendar.YEAR)+1); year++)
		{
			for (int month = 1; month < 13; month++)
			{
				tableBrowserIdNames.add("stat_views_"+year+"_"+month);
				if ((year == cal.get(Calendar.YEAR)) && (month == (cal.get(Calendar.MONTH)+1)))
					break;
			}
		}

		return (tableBrowserIdNames);
	}

	/**
	 * Zmena konfiguracie prav pre ovladaci panel. Po novom uz nie je jedno privilegium pre vsetky polozky Ovladacieho panela, ale na pre kazdu polozku sa prava nastavuju osobitne
	 * Metoda pre vsetkych uzivatelov ktori maju Ovladaci panel (menuConfig) ako disabled_item, prida dalsich 12 prav Ovladacieho panela do disabled items
	 */
	public static void disabledItemsConfigRights()
	{
		String note = "11.01.2010 [jraska] rozdelenie pristupovych prav ovladacieho panela na jednotlive podkategorie";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			SimpleQuery sq = new SimpleQuery();
			String [] newRights = {"modUpdate","cmp_attributes","cmp_adminlog","edit_text","export_offline","cmp_clone_structure","cmp_data_deleting","cmp_server_monitoring","cmp_redirects","modRestart","cmp_crontab","make_zip_archive"};
			//sq.execute("", newRights[0], newRights[1], newRights[2], newRights[3], newRights[4], newRights[5], newRights[6], newRights[7], newRights[8], newRights[9], newRights[10], newRights[11]);
			sq.execute("DELETE FROM user_disabled_items WHERE item_name IN (?,?,?,?,?,?,?,?,?,?,?,?)", (Object[])newRights);

			ps = db_conn.prepareStatement("SELECT user_id FROM user_disabled_items WHERE item_name=?");
			ps.setString(1, "menuConfig");
			rs = ps.executeQuery();
			while (rs.next())
			{
				int user_id = rs.getInt("user_id");
				for(int i=0;i<newRights.length;i++)
				{
					sq.execute("INSERT INTO user_disabled_items(user_id,item_name) VALUES(?,?)", user_id,newRights[i]);
				}

			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			//zapis do DB, ze je to importnute
			saveSuccessUpdate(note);
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
	}


	public static void updateStatViewsColumns()
	{
		String note = "14.5.2010 [jeeff] pridanie novych stlpcov do stat_views";
		if (Constants.getBoolean("updateDisableFixBrowserId") || isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 1);
		long to = cal.getTimeInMillis();
		cal.set(Calendar.YEAR, 2000);
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		long from = cal.getTimeInMillis();

		String[] suffixes = StatNewDB.getTableSuffix(from, to);
		for (int s=0; s<suffixes.length; s++)
		{
			try
			{
				Logger.println(UpdateDatabase.class, "Updating stat_views columns "+(s+1)+"/"+suffixes.length+" "+suffixes[s]);

				db_conn = DBPool.getConnection();

				StringBuilder sql = new StringBuilder("ALTER TABLE stat_views");
				sql.append(suffixes[s]);
				sql.append(' ');

				if (Constants.DB_TYPE==Constants.DB_ORACLE) sql.append("ADD (browser_ua_id INT, platform_id INT, subplatform_id INT, country VARCHAR(4))");
				else if (Constants.DB_TYPE==Constants.DB_MSSQL) sql.append("ADD browser_ua_id INT, platform_id INT, subplatform_id INT, country VARCHAR(4)");
				else sql.append("ADD browser_ua_id INT, ADD platform_id INT, ADD subplatform_id INT, ADD country VARCHAR(4)");

				ps = db_conn.prepareStatement(sql.toString());
				ps.execute();
				ps.close();
				ps = null;

				//nastav prazdne hodnoty
				sql = new StringBuilder("UPDATE stat_views");
				sql.append(suffixes[s]);
				sql.append(" SET browser_ua_id=0, platform_id=0, subplatform_id=0, country='unkn'");

				ps = db_conn.prepareStatement(sql.toString());
				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;
			}
			catch (Exception ex)
			{
				if (ex.getMessage().indexOf("exist")==-1 && ex.getMessage().indexOf("duplicate")==-1)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
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
		}

		//zapis do DB, ze je to aktualizovane
		saveSuccessUpdate(note);
	}

	public static void statErrorAddDomainId()
	{
		String note = "20.3.2024 [jeeff] stat_error add domain_id column";
		if (isAllreadyUpdated(note)) return;

		Connection db_conn = null;
		PreparedStatement ps = null;

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 1);
		long to = cal.getTimeInMillis();
		cal.set(Calendar.YEAR, 2000);
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		long from = cal.getTimeInMillis();

		String[] suffixes = StatNewDB.getTableSuffix(from, to);
		for (int s=0; s<suffixes.length; s++)
		{
			try
			{
				Logger.println(UpdateDatabase.class, "Add domain_id to stat_error"+suffixes[s]+" "+(s+1)+"/"+suffixes.length);

				db_conn = DBPool.getConnection();

				StringBuilder sql = new StringBuilder("ALTER TABLE stat_error");
				sql.append(suffixes[s]);
				sql.append(' ');

				sql.append("ADD domain_id INT DEFAULT 0 NOT NULL");

				ps = db_conn.prepareStatement(sql.toString());
				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;
			}
			catch (Exception ex)
			{
				if (ex.getMessage().indexOf("exist")==-1 && ex.getMessage().indexOf("duplicate")==-1)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
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
		}

		//zapis do DB, ze je to aktualizovane
		saveSuccessUpdate(note);
	}

	/**
	 * Zmluvy - migracia do novej struktury, ak sa pouzivali zmluvy pred pridanim organizacie (#23935)
	 */
	public static void zmluvyOrganizacieUpdate()
	{
		//ak sa pouzivali zmluvy pred pridanim organizacie
		if(Constants.getInt("zmluvyApproveGroupId") != -1)
		{
			Logger.println(UpdateDatabase.class, "(zmluvyOrganizacieUpdate) updating zmluvy, zmluvy_organizacia, zmluvy_organizacia_users, zmluvy_organizacia_approvers");
			try {
				//ak existuje novy typ zmluv s pridanim organizacie
				Class.forName("sk.iway.iwcm.components.zmluvy.ZmluvyOrganizaciaBean");
				//ak mame zmluvy vo viacerych domenach, vytvorim pre kazdu domenu default organizaciu
				@SuppressWarnings("unchecked")
				List<Integer> domeny = DB.queryForList("SELECT DISTINCT domain_id FROM zmluvy");
				if(domeny != null && domeny.size() > 0)
				{
					for(Integer did : domeny)
					{
						if(did.intValue() > 1)
						{
							DB.execute("INSERT INTO zmluvy_organizacia (nazov, domain_id) VALUES (?, ?);", "default", did);
							int orgId = DB.queryForInt("SELECT MAX(zmluvy_organizacia_id) FROM zmluvy_organizacia");
							DB.execute("UPDATE zmluvy SET organizacia_id = ? WHERE domain_id = ?", Integer.valueOf(orgId), did);
						}
					}
				}
				@SuppressWarnings("unchecked")
				List<Integer> organizacie = DB.queryForList("SELECT zmluvy_organizacia_id FROM zmluvy_organizacia");
				//migrujem schvalovatelov zo skupiny definovanej v zmluvyApproveGroupId do tabulky zmluvy_organizacia_approvers
				if(Constants.getInt("zmluvyApproveGroupId") > 0)
				{
					List<UserDetails> approvers = UsersDB.getUsersByGroup(Constants.getInt("zmluvyApproveGroupId")); //vsetci schvalovatelia
					if(approvers != null && approvers.size() > 0)
					{
						for(UserDetails u : approvers)
						{
							for(Integer oid : organizacie)
								DB.execute("INSERT INTO zmluvy_organizacia_approvers (organizacia_id, user_id) VALUES (?, ?);", oid, Integer.valueOf(u.getUserId()));
						}
					}
				}
				//ak sa pouzivali zmluvy (ratam aspon s troma, ak by niekto skusal vytvarat nejake testovacie), zmigrujem tych co maju prava na modul Zmluvy presunut do zmluvy_organizacia_users
				if(DB.queryForInt("SELECT count(*) FROM zmluvy") > 2)
				{
					List<UserDetails> admins = UsersDB.getAdmins();
					if(admins != null && admins.size() > 0)
					{
						for(UserDetails u : admins)
						{
							if(DB.queryForInt("SELECT count(*) FROM user_disabled_items WHERE user_id=? AND item_name = ?", Integer.valueOf(u.getUserId()), "cmp_zmluvy") == 0)
								for(Integer oid : organizacie)
									DB.execute("INSERT INTO zmluvy_organizacia_users (organizacia_id, user_id) VALUES (?, ?);", oid, Integer.valueOf(u.getUserId()));
						}
					}
				}
				//vymazem z premennych
				if (ConfDB.deleteName("zmluvyApproveGroupId"))
				{
					if (Constants.deleteConstant("zmluvyApproveGroupId") && ClusterDB.isServerRunningInClusterMode())
						ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-zmluvyApproveGroupId");
				}
			} catch( ClassNotFoundException e ) {
				//my class isn't there!
			}
			catch( Exception ex ) {
				//asi nastala SQL chyba
				sk.iway.iwcm.Logger.error(ex);
			}
		}
	}
	public static void mediaGroupsUpdate()
	{
		if ("public".equals(Constants.getString("clusterMyNodeType"))) return;

		try
		{
			//tu si cachujeme vlozene media skupiny, nech nemusime po to chodit stale do DB

			Map<String, Integer> groupIdTable = new Hashtable<>();

			int rows = DB.queryForInt("SELECT count(*) FROM media_group_to_media"); // ak este nieje ziadny zaznam, spustim import
			if (rows == 0)
			{
				@SuppressWarnings("unchecked")
				List<Number> media = DB.queryForList("SELECT media_id FROM media");
				if (media != null && media.size() > 0)
				{
					for (Number m : media)
					{
						String oldGroups = DB.queryForString("SELECT media_group FROM media WHERE media_id = ?", m);
						if (oldGroups != null)
						{
							//String[] oldGroupsArray = oldGroups.split(",");
							String[] oldGroupsArray = Tools.getTokens(oldGroups, ",", true);

							for (String g : oldGroupsArray)
							{
								Integer mediaGroupId = groupIdTable.get(g);
								if (mediaGroupId == null)
								{
									int newGroupId = DB.queryForInt("SELECT media_group_id FROM media_groups WHERE media_group_name = ?", g);
									if (newGroupId == 0)
									{
										DB.execute("INSERT INTO media_groups (media_group_id, media_group_name) VALUES (?, ?)", PkeyGenerator.getNextValue("MediaGroup"), g);
										Logger.info(UpdateDatabase.class, "Vytvaram novu media skupinu: " + g + "<br>");
										newGroupId = DB.queryForInt("SELECT media_group_id FROM media_groups WHERE media_group_name = ?", g);
									}
									//vloz do cache pre dalsie pouzitie
									mediaGroupId = Integer.valueOf(newGroupId);
									groupIdTable.put(g, mediaGroupId);
								}

								try
								{
									//robime priamo insert, ak by bola duplicita, tak to padne, je to vyrazne rychlejsie ako neustale testovat ci uz zaznam existuje
									DB.execute("INSERT INTO media_group_to_media (media_group_id, media_id) VALUES (?,?)", mediaGroupId, m);
									Logger.info(UpdateDatabase.class, "Priradujem media skupinu: " + g + " ku mediu s ID " + m + "<br>");
								} catch (Exception ex) {}
							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

    /**
     * Nastavi map providera na GoogleMap ak je neprazdna konf. premenna googleMapsApiKey (lebo default je OpenStreetMap)
     */
    public static void setDefaultMapProvider()
    {
		String note = "10.03.2018 [lbalat] nastavenie defaultneho map providera podla nastavenia googleMapsApiKey";
		if (isAllreadyUpdated(note)) return;

		try
		{
			if (Tools.isNotEmpty(Constants.getString("googleMapsApiKey")))
			{
					SimpleQuery sq = new SimpleQuery();
					sq.execute("INSERT INTO " + ConfDB.CONF_TABLE_NAME + " (name, value) VALUES ('mapProvider', 'GoogleMap')");
			}
		}
		catch (Exception ex)
		{
			//ak to padlo, asi uz je konf. premenna mapProvider nastavena, nevadi, povazujeme za vybavene
		}

		saveSuccessUpdate(note);
    }

	public static void updateMediaDomainIdColumn() {
		String note = "8.6.2023 [sivan] pridanie stlpcu domain_id do tabulky media";
		if(!Constants.getBoolean("enableStaticFilesExternalDir") || isAllreadyUpdated(note)) return;

		try {
			//We are using DISTINCT, because we set same domainid for all media records that obtain same media_fk_id (aka docId)
			@SuppressWarnings("unchecked")
			List<Number> mediaFkIds = DB.queryForList("SELECT DISTINCT media_fk_id FROM media");

			for(Number mediaFkId : mediaFkIds) {

				DocDB docDB = DocDB.getInstance();
				String domain = docDB.getDomain(mediaFkId.intValue());

				Integer domainId = GroupsDB.getDomainId(domain);
				if(domainId == -1) domainId = 1;

				try {
					//Set domain id based on media_fk_id (aka docId)
					DB.execute("UPDATE media SET domain_id = ? WHERE media_fk_id = ?", domainId, mediaFkId);
					Logger.info(UpdateDatabase.class, "Nastavujem media domainId = " + domainId + " pre docId = " + mediaFkId + " <br>");
				} catch (Exception ex) {}
			}

		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}

		//zapis do DB, ze domain_id stlpec bol uz aktualizovany
		saveSuccessUpdate(note);
	}

	public static void updateEmailsCampainDomainIdColumn() {
		String note = "18.3.2024 [sivan] pridanie stlpcu domain_id do tabulky emails_campain";
		if(isAllreadyUpdated(note)) return;

		try {
			@SuppressWarnings("unchecked")
			List<String> emailsCampainUrl = DB.queryForList("SELECT DISTINCT url FROM emails_campain");

			for(String url : emailsCampainUrl) {
				int domainId = getDomainIdBasedOnUrl(url);

				try {
					//Set domain id based on URL
					DB.execute("UPDATE emails_campain SET domain_id = ? WHERE url = ?", domainId, url);
					Logger.info(UpdateDatabase.class, "Setting emails_campain domainId = " + domainId + " for url = " + url);
				} catch (Exception ex) {}

				try {
					//Set domain id based on URL
					DB.execute("UPDATE emails SET domain_id = ? WHERE url LIKE ?", domainId, url+"%");
					Logger.info(UpdateDatabase.class, "Settings emails domainId = " + domainId + " for url = " + url);
				} catch (Exception ex) {}
			}

		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}

		//zapis do DB, ze domain_id stlpec bol uz aktualizovany
		saveSuccessUpdate(note);
	}

	private static int getDomainIdBasedOnUrl(String url) throws Exception {
		int domainId = 1;
		if( Tools.isNotEmpty(url) ) {
			DocDetails doc = WebpagesService.getBasicDocFromUrl(url);
			if(doc != null) {
				GroupDetails group = doc.getGroup();
				if(group != null) {
					String domainName = group.getDomainName();
					if( Tools.isNotEmpty(domainName) ) {
						domainId = GroupsDB.getDomainId(domainName);
						if(domainId < 1) domainId = 1;
					}
				}
			} else {
				try {
					//get domainId from URL
					if (url.startsWith("http")) {
						int to = url.indexOf("/", 8);
						if (to==-1) to = url.indexOf(":", 8);
						if (to==-1) to = url.indexOf("?", 8);

						String domainName = url.substring(url.indexOf("://")+3, to);
						int portDelimiter = domainName.indexOf(":");
						if (portDelimiter > 0) domainName = domainName.substring(0, portDelimiter);

						domainId = GroupsDB.getDomainId(domainName);
					}
				} catch (Exception e) {
					//do nothing
				}
			}
		}

		return domainId;
	}

	private static String getInvoiceUpdateContact(String colunmnFrom, String columnTo) {
		return "UPDATE basket_invoice SET " + columnTo + " = " + colunmnFrom + " WHERE " + columnTo + " IS NULL OR " + columnTo + " = ''";
	}

	private static void updateInvoiceContacts() {
		String note = "01.04.2025 [sivan] dopln do basket_invoice chybajuce contact udaje dane v delivery";
		if(isAllreadyUpdated(note)) return;

		try {
			DB.execute( getInvoiceUpdateContact("delivery_name", "contact_first_name") );
			DB.execute( getInvoiceUpdateContact("delivery_surname", "contact_last_name") );
			DB.execute( getInvoiceUpdateContact("delivery_street", "contact_street") );
			DB.execute( getInvoiceUpdateContact("delivery_city", "contact_city") );
			DB.execute( getInvoiceUpdateContact("delivery_zip", "contact_zip") );
			DB.execute( getInvoiceUpdateContact("delivery_country", "contact_country") );
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}

		saveSuccessUpdate(note);
	}

	//update method that will loop ALL invoices and set itemQty, priceNotVat, priceVat
	private static void updateInvoicePrices() {
		try {
			String note = "01.04.2025 [sivan] dopln do basket_invoice_items chybajuce itemQty, priceNotVat, priceVat";
			if(isAllreadyUpdated(note)) return;

			BasketInvoicesRepository bir = Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class);
			BasketInvoiceItemsRepository biir = Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class);
			BasketInvoicePaymentsRepository bipr = Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class);
			if (bir == null || biir == null || bipr == null) {
				Logger.error(UpdateDatabase.class, "BasketInvoicesRepository bean not found");
				return;
			}

			DebugTimer dt = new DebugTimer("Updating invoice prices ");

			int pageSize = 100;
			int pageNumber = 0;
			int failsafe = 0;
			Page<BasketInvoiceEntity> page;
			do {
				page = bir.findAll(PageRequest.of(pageNumber, pageSize));
				List<BasketInvoiceEntity> items = page.getContent();
				dt.diffInfo("[page "+pageNumber+"/"+page.getTotalPages()+"], rows="+items.size());
				// calculate itemQty, priceNotVat, priceVat
				for (BasketInvoiceEntity invoice : items) {
					Integer itemsCount = 0;
					BigDecimal totalPrice = BigDecimal.ZERO; //NO VAT
					BigDecimal totalPriceVat = BigDecimal.ZERO; //WITH VAT

					List<BasketInvoiceItemEntity> invoiceItems = biir.findAllByInvoiceIdAndDomainId(invoice.getId(), invoice.getDomainId());

					for(BasketInvoiceItemEntity item : invoiceItems) {
						itemsCount += item.getItemQty();
						totalPrice = totalPrice.add( item.getItemPriceQty() );
						totalPriceVat = totalPriceVat.add( item.getItemPriceVatQty() );
					}

					//Set and save invoice
					invoice.setItemQty(itemsCount);
					invoice.setPriceToPayNoVat(totalPrice);
					invoice.setPriceToPayVat(totalPriceVat);
					invoice.setBalanceToPay( totalPriceVat.subtract( ProductListService.getPayedPrice(invoice.getId(), bipr) ) );

					bir.save(invoice);
				}
				pageNumber++;
			} while (!page.isLast() && failsafe++ < 5000);

			dt.diffInfo("DONE");

			saveSuccessUpdate(note);
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}
}