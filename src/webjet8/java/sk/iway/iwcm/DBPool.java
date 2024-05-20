package sk.iway.iwcm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Set;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.apache.commons.dbcp.ConfigurableDataSource;
import org.apache.commons.dbcp.WebJETAbandonedDataSource;
import org.apache.commons.dbcp.WebJetUcpDataSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.adminlog.AdminlogNotifyManager;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;

import static sk.iway.iwcm.Tools.*;


/**
 *  Database pooling s pouzitim DBCP
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author$
 *@version      $Revision$
 *@created      $Date$
 */
public class DBPool
{
	private static DBPool instance = null;
	private static Hashtable<String, ConfigurableDataSource> dataSourcesTable = null; //NOSONAR
	private static Map<String, EntityManagerFactory> entityManagerFactories;
	private static Map<String, DataSource> externalDataSources = null;
	private static AtomicBoolean hasPrintedStackTrace = new AtomicBoolean(false);

	/**
	 * Je to singleton, ziskame instanciu
	 * @return
	 */
	public static synchronized DBPool getInstance()
	{
		if (instance == null)
		{
			instance = new DBPool();
			instance.initialize();
		}
		return(instance);
	}

	/**
	 * Toto sa pouzije iba pri setupe, inokedy sa nesmie pouzit
	 * @param forceRefresh
	 * @return
	 */
	public static synchronized DBPool getInstance(boolean forceRefresh)
	{
		if (instance == null || forceRefresh)
		{
			instance = new DBPool();
			instance.initialize();
		}
		return(instance);
	}

	/**
	 * Nacitanie konfiguracie z poolman.xml (aj ked sa pouziva DBCP)
	 *
	 */
	@SuppressWarnings("deprecation")
	private synchronized void initialize()
	{
		dataSourcesTable = new Hashtable<>();

		// inicializuj
		Logger.println(this,"DBPool: init");

		String dbname;
		String driver;
		String username;
		String password;
		String url;
		String provider;

		int initialSize;
		int minActive;
		int maxActive;

		boolean fastConnnectionFailOverEnabled = false;
		String ONSConfiguration = "";
		boolean validateConnectionOnBorrow = false;
		String autoCommit = "false";

		int removeAbandonedTimeout;
		int maxWait;

		int counter = 0;


		String systemIwcmDBName = InitServlet.getContextDbName();
		if (Tools.isEmpty(systemIwcmDBName)) systemIwcmDBName = System.getProperty("webjetDbname");
		Logger.println(this, "systemIwcmDBName="+systemIwcmDBName);

		//aj cez <Context ... <Parameter name="webjetDbname" value="/poolman-local.xml" override="true"/> je mozne zadat cestu
		String customPoolmanPath = null;
		if (Tools.isNotEmpty(systemIwcmDBName) && systemIwcmDBName.endsWith(".xml")) {
			customPoolmanPath = systemIwcmDBName;
			systemIwcmDBName = "iwcm";
		}
		String data = readFileContent(customPoolmanPath);

		StringBuilder availableDatabases = null;
		if (data != null)
		{
			try
			{
				DocumentBuilderFactory b = DocumentBuilderFactory.newInstance();
				// to be compliant, completely disable DOCTYPE declaration:
				b.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				DocumentBuilder dc = b.newDocumentBuilder();
				ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
				Document doc = dc.parse(input);

				if(doc != null)
				{
					Vector<Node> list = XmlUtils.getChildNodesByPath(doc.getDocumentElement(), "/datasource");
					if( list != null )
					{
						ConfigurableDataSource ds;
						for (Node n : list)
						{
						    counter++;

							//default values
							driver = "com.mysql.jdbc.Driver";
							username = "user";
							password = "pass";
							url = "jdbc:mysql://localhost/webjet_web?useUnicode=true&characterEncoding=windows-1250";

							initialSize = 0;
							minActive = 0;
							maxActive = 50;

							removeAbandonedTimeout = 600;
							maxWait = 60; //60 sekund

							dbname = XmlUtils.getFirstChildValue(n, "dbname");

							//zadane meno zmenime na iwcm aby ho interne WebJET pouzival
							//riesene kvoli ING kde public node ma iny pripojovaci retazec
							if (Tools.isNotEmpty(systemIwcmDBName) && systemIwcmDBName.equals(dbname))
							{
								Logger.println(DBPool.class, "Changing dbname from "+dbname+" to iwcm");
								dbname = "iwcm";
							}

							if ("autoincrement".equals(systemIwcmDBName) && "iwcm".equals(dbname))
                            {
                                //toto je specialny pripad, ked inicializujeme vsetky spojenia z poolman-conf.xml pre zmenu hesla
                                dbname = "autoincrement"+counter;
                            }

							//iwcm1 preskakujeme pre rychlost inicializacie
							if ("iwcm1".equals(dbname)) continue;

							driver = XmlUtils.getFirstChildValue(n, "driver");
							url = XmlUtils.getFirstChildValue(n, "url");
							username = XmlUtils.getFirstChildValue(n, "username");
							password = XmlUtils.getFirstChildValue(n, "password");
							provider = XmlUtils.getFirstChildValue(n, "provider");
							initialSize = getIntValue(XmlUtils.getFirstChildValue(n, "initialConnections"), initialSize);
							minActive = getIntValue(XmlUtils.getFirstChildValue(n, "minimumSize"), minActive);
							maxActive = getIntValue(XmlUtils.getFirstChildValue(n, "maximumSize"), maxActive);

							fastConnnectionFailOverEnabled = getBooleanValue(XmlUtils.getFirstChildValue(n, "fastConnnectionFailOverEnabled"), fastConnnectionFailOverEnabled);
							validateConnectionOnBorrow = getBooleanValue(XmlUtils.getFirstChildValue(n, "validateConnectionOnBorrow"), validateConnectionOnBorrow);
							ONSConfiguration = getStringValue(XmlUtils.getFirstChildValue(n, "ONSConfiguration"), ONSConfiguration);
							autoCommit = getStringValue(XmlUtils.getFirstChildValue(n, "autoCommit"), autoCommit);

							removeAbandonedTimeout = getIntValue(XmlUtils.getFirstChildValue(n, "connectionTimeout"), removeAbandonedTimeout);
							maxWait = getIntValue(XmlUtils.getFirstChildValue(n, "userTimeout"), maxWait);

							Logger.println(this,"DATA SET from XML ["+dbname+"], maxActive="+maxActive);

							/**
							 * ak su premenne pre db nastavene ako parametre jvm/env, pouzi tie
							 * pre ine ako iwcm spojenie ma meno suffix _dbname, cize napr. -DwebjetDbUserName_ip_data_jpa
							 * -DwebjetDbUserName
							 * -DwebjetDbPassword
							 *	-DwebjetDbUrl
							 *	-DwebjetDbMinimumSize
							 *	-DwebjetDbMaximumSize
							*/
							String envSuffix = "";
							if ("iwcm".equals(dbname)==false) envSuffix = "_"+dbname;
							if(isSystemPropertySet("webjetDbDriver"+envSuffix))
							{
								driver = getSystemProperty("webjetDbDriver"+envSuffix);
							}
							if(isSystemPropertySet("webjetDbUserName"+envSuffix))
							{
								username = getSystemProperty("webjetDbUserName"+envSuffix);
							}
							if(isSystemPropertySet("webjetDbPassword"+envSuffix))
							{
								password = getSystemProperty("webjetDbPassword"+envSuffix);
							}
							if(isSystemPropertySet("webjetDbUrl"+envSuffix))
							{
								url = getSystemProperty("webjetDbUrl"+envSuffix);
							}
							if(isSystemPropertySet("webjetDbMaximumSize"+envSuffix))
							{
								maxActive = Tools.getIntValue(getSystemProperty("webjetDbMaximumSize"+envSuffix),60);
							}

							if ("com.mysql.jdbc.Driver".equals(driver)) driver = "org.mariadb.jdbc.Driver"; //menime driver pre mysql tak aby sa nemuseli prepisovat vsetky poolmany pri update
							if ("COM.mysql.jdbc.Driver".equals(driver)) driver = "com.mysql.jdbc.Driver"; //ak by blo z nejakeho dovodu treba pouzit mysql driver

							if ("org.mariadb.jdbc.Driver".equals(driver)) {
								url = Tools.replace(url, "jdbc:mysql://", "jdbc:mariadb://");
							}

							if ("iwcm".equals(dbname))
					      	{
								if (driver.indexOf("oracle")!=-1)
								{
									Constants.DB_TYPE = Constants.DB_ORACLE;
									ConfDB.CONF_TABLE_NAME = "webjet_conf";
									ConfDB.CONF_PREPARED_TABLE_NAME = "webjet_conf_prepared";
									ConfDB.MODULES_TABLE_NAME = "webjet_modules";
									ConfDB.ADMINLOG_TABLE_NAME = "webjet_adminlog";
									ConfDB.DB_TABLE_NAME = "webjet_db";
									ConfDB.PROPERTIES_TABLE_NAME = "webjet_properties";
								}
								else if (driver.indexOf("jtds")!=-1 || driver.indexOf("microsoft.sqlserver")!= -1)
								{
									Constants.DB_TYPE = Constants.DB_MSSQL;
								}
								else if (driver.indexOf("postgresql")!=-1 || driver.indexOf("pgsql")!= -1)
								{
									Constants.DB_TYPE = Constants.DB_PGSQL;
								}
								else
								{
									Constants.DB_TYPE = Constants.DB_MYSQL;
								}
								//minimumSize = System.getProperty("webjetDbMinimumSize"); //toto sa nikde nepouziva
							}

							if (password == null) password = "";
							password = decryptPassword(password);
							if (isEmpty(provider)) provider = "dbcp";

							if("ucp".equalsIgnoreCase(provider)) {
								ds = new WebJetUcpDataSource(PoolDataSourceFactory.getPoolDataSource(), dbname);
								WebJetUcpDataSource source = (WebJetUcpDataSource)ds;

								source.setConnectionProperty(OracleConnection.CONNECTION_PROPERTY_AUTOCOMMIT, "false");

								Logger.println(DBPool.class, "Using UCP");

                                if(fastConnnectionFailOverEnabled) {
                                	source.setFastConnectionFailoverEnabled(true);
									Logger.println(DBPool.class, "UCP fastConnnectionFailOverEnabled=" + fastConnnectionFailOverEnabled);
								}

								if(isNotEmpty(ONSConfiguration)) {
									source.setONSConfiguration(ONSConfiguration);
									Logger.println(DBPool.class, "UCP ONSConfiguration=" + ONSConfiguration);
								}

								if (driver.contains("oracle")) {
									source.setConnectionProperty("SetBigStringTryClob", "true");
									Logger.println(DBPool.class, "UCP SetBigStringTryClob=" + true);
								}

								if(validateConnectionOnBorrow) {
									source.setValidateConnectionOnBorrow(true);
									Logger.println(DBPool.class, "UCP validateConnectionOnBorrow=" + validateConnectionOnBorrow);
								}

								source.setInitialPoolSize(initialSize);
								Logger.println(DBPool.class, "UCP initialPoolSize=" + initialSize);

								source.setMinPoolSize(minActive);
								Logger.println(DBPool.class, "UCP minPoolSize=" + minActive);

								source.setMaxPoolSize(maxActive);
								Logger.println(DBPool.class, "UCP maxPoolSize=" + maxActive);

							}
							else
							{
								ds = new WebJETAbandonedDataSource();
								WebJETAbandonedDataSource source = (WebJETAbandonedDataSource)ds;
								source.setMaxActive(maxActive);
								Logger.println(this,"max idle="+source.getMaxIdle());
								source.setMaxIdle(3);

								source.setRemoveAbandoned(true);
								source.setRemoveAbandonedTimeout(removeAbandonedTimeout);
								source.setLogAbandoned(true);
								Logger.println(this,"getRemoveAbandonedTimeout="+source.getRemoveAbandonedTimeout());

								source.setDefaultAutoCommit(true);
								if (driver.contains("oracle"))
									source.addConnectionProperty("SetBigStringTryClob", "true");

								source.setAccessToUnderlyingConnectionAllowed(true);
								source.setPoolPreparedStatements(false);

								source.setMaxWait(1000L*maxWait);
							}

							//okrem Oracle mozeme robit takyto validation query
							ds.setPreferredTestQuery("SELECT 1");
							if(driver.contains("oracle"))
								ds.setPreferredTestQuery("select 'validationQuery' from dual");

							ds.setInitialPoolSize(initialSize);
							ds.setDriverClass(driver);
							ds.setUser(username);
							ds.setPassword(password);
							ds.setJdbcUrl(url);

					      dataSourcesTable.put(dbname, ds);

					      Constants.setInt("webjetDbMaximumSize", maxActive);
							Logger.println(DBPool.class, "Initialized Datasource " + dbname + " url=" + url);

					      if (availableDatabases == null)
					      {
					      	availableDatabases = new StringBuilder(dbname);
					      }
					      else
					      {
					      	availableDatabases.append(',').append(dbname);
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

		if (availableDatabases != null)
		{
			Constants.setString("availableDatabases", availableDatabases.toString());
		}

		addExternalDataSources();
	}

	private boolean isSystemPropertySet(String name) {
		String value = System.getProperty(name);
		if (Tools.isNotEmpty(value)) return true;
		value = System.getenv(name);
		if (Tools.isNotEmpty(value)) return true;
		return false;
	}

	private String getSystemProperty(String name) {
		String value = System.getProperty(name);
		if (Tools.isNotEmpty(value)) return value;
		value = System.getenv(name);
		if (Tools.isNotEmpty(value)) return value;
		return "";
	}



    /**
     *
     * @param destroyInstance - ak je true predpoklada sa reinicializacia DBPoolu, inak sa pouziva false, kedy sa vsetko len ukonci (vypnutie servera)
     */
	public synchronized void destroy(boolean destroyInstance)
	{
		Logger.println(this,"DBPool["+Constants.getInstallName()+"] destroy");

		Enumeration<String> keys = dataSourcesTable.keys();
		String key;
		while (keys.hasMoreElements())
		{
			key = keys.nextElement();
			ConfigurableDataSource ds = dataSourcesTable.get(key);
			Logger.println(this,"   Active:" + ds.getNumActive()+" Idle:"+ds.getNumIdle());
			try
			{
				PrintWriter pw = new PrintWriter(System.out); //NOSONAR
				ds.printStackTraces(pw);
				ds.destroy();
			}
			catch (SQLException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		try
		{
		    if (destroyInstance==false)
            {
                for (Enumeration<Driver> e = DriverManager.getDrivers(); e.hasMoreElements(); )
                {
                    Driver driver = e.nextElement();
                    if (driver != null && driver.getClass().getClassLoader() == getClass().getClassLoader())
                    {
                        Logger.println(DBPool.class, "Unloading driver " + driver.toString());
                        DriverManager.deregisterDriver(driver);
                    }
                }
            }
		}
		catch (Exception e)
		{
			System.err.println("Failled to cleanup ClassLoader for webapp " + e.getMessage()); //NOSONAR
			sk.iway.iwcm.Logger.error(e);
		}

		if (destroyInstance) instance = null;
	}

	public void logConnections()
	{
		Enumeration<String> keys = dataSourcesTable.keys();
		String key;
		while (keys.hasMoreElements())
		{
			key = keys.nextElement();
			try
			{
				ConfigurableDataSource ds = dataSourcesTable.get(key);
				Logger.debug(this,"DBPool["+key+"] active: " + ds.getNumActive()+" idle="+ds.getNumIdle());
				ds.printStackTraces();
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	/**
	 * Ziskanie dataSource z Hashtabulky
	 * @param dbName
	 * @return
	 */
	public DataSource getDataSource(String dbName)
	{
		if (dataSourcesTable == null)
		{
			initialize();
		}

		DataSource ds = dataSourcesTable.get(dbName);
		//Logger.println(DBPool.class, "getDataSource 1, ds="+ds);

		if(ds==null)
			ds = externalDataSources.get(dbName);

		//Logger.println(DBPool.class, "getDataSource 2, ds="+ds+" TYPE="+Constants.DB_TYPE+" conf table="+ConfDB.CONF_TABLE_NAME);

		return ds;
	}

	/**
	 * Ziskanie WebJETAbandonedDataSource z Hashtabulky
	 * @param dbName
	 * @return
	 */
	public DataSource getWebJETAbandonedDataSource(String dbName)
	{
		return getDataSource(dbName);
	}


	/**
	 * Vrati DB spojenie do databazy WebJETu
	 * @return
	 */
	public static Connection getConnection()
	{
		return(getConnection("iwcm", false));
	}

	/**
	 * Vrati DB spojenie do databazy WebJETu v rezime Connection.TRANSACTION_READ_UNCOMMITTED
	 * @return
	 */
	public static Connection getConnectionReadUncommited()
	{
		return(getConnection("iwcm", true));
	}

	public static Connection getConnection(HttpServletRequest request)
	{
		return(getConnection(getDBName(request)));
	}

	/**
	 * Vrati DB spojenie so zadanym nazvom
	 * @param dbName
	 * @return
	 */
	public static Connection getConnection(String dbName)
	{
		return getConnection(dbName, false);
	}

	/**
	 * Vrati DB spojenie so zadanym nazvom v rezime Connection.TRANSACTION_READ_UNCOMMITTED
	 * @param dbName
	 * @return
	 */
	public static Connection getConnectionReadUncommited(String dbName)
	{
		return getConnection(dbName, false);
	}

	/**
	 * Vrati DB spojenie so zadanym nazvom, ak je nastavena hodnota readUncomitted na true vrati DB spojenie v rezime Connection.TRANSACTION_READ_UNCOMMITTED
	 * @param dbName
	 * @param readUncomitted
	 * @return
	 */
	private static Connection getConnection(String dbName, boolean readUncomitted)
	{
		if (dbName == null)
		{
			dbName = "iwcm";
		}
		if (dbName.indexOf('.')!=-1)
		{
			//asi to dalo cele meno servera, osetri
			dbName = getDBName(dbName);
		}

		//skus to cez Data Source
		DataSource ds = null;
		try
		{
			Connection con = null;

			ds = DBPool.getInstance().getDataSource(dbName);


			if (ds==null) return(null);
			con = ds.getConnection();
			con.setAutoCommit(true);

			if (readUncomitted) setTransactionIsolationReadUNCommited(con);
			else setTransactionIsolationReadCommited(con);

			return(con);
		}
		catch (Exception ex)
		{
			Logger.error(DBPool.class,"CHYBA!!: nepodarilo sa ziskat connection do DB.");
			if(ex.getMessage() != null && ex.getMessage().contains("Cannot get a connection, pool error Timeout waiting for idle object") && ds != null)
			{
				 try
				 {
					  ConfigurableDataSource cds = (ConfigurableDataSource) ds;
					  Logger.println(DBPool.class, "  Active: " + cds.getNumActive() + " idle=" + cds.getNumIdle());
					  if (cds.getNumActive() == Constants.getInt("webjetDbMaximumSize"))
					  {
							if(hasPrintedStackTrace.compareAndSet(false, true))
							{
								cds.printStackTraces();
								StringWriter sw = new StringWriter();
								cds.printStackTraces(new PrintWriter(sw));
								RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
								if(rb != null && Tools.isNotEmpty(rb.getDomain()) && "iwcm.interway.sk".equals(rb.getDomain()) == false)
								{
									AdminlogNotifyManager.sendNotification(Adminlog.TYPE_SQLERROR, rb, DB.getDbTimestamp(Tools.getNow()),
															"Na domene " + rb.getDomain() + (ClusterDB.isServerRunningInClusterMode() ? ", node: "+Constants.getString("clusterMyNodeName") : "") +
															" bol pravdepodobne dosiahnuty maximalny pocet DB spojeni pre dbName: "+dbName+". " +
															"Treba preverit ci nenastavaju DB leaky, alebo ci netreba zvysit maximumSize alebo nastavit korektne socketTimeout v poolman.xml. " +
															"V logoch by mal byt DB stackTrace - vyhladat 'Printing stack traces'\n\nexception: "+ex+"\n\nstackTraces: "+sw.toString(), false);
								}
							}
					  }
				 }
				 catch (Exception ex2)
				 {
					  sk.iway.iwcm.Logger.error(ex2);
				 }
			}
			sk.iway.iwcm.Logger.error(ex);
		}

		//no a teraz je to uplne v prdeli...
		return(null);
	}

	@SuppressWarnings("unused")
	private static void logCaller()
	{
		Throwable trace = new Exception().fillInStackTrace();
		//0 - this, 1 - getConnection, 2 - getConnection, 3 - caller
		if (trace.getStackTrace().length > 3)
		{
			String message = String.format("%s.%s() requested DB connection", trace.getStackTrace()[3].getClassName(), trace.getStackTrace()[3].getMethodName()
			);
			Logger.debug(DBPool.class, message);
		}
	}

	public static void setTransactionIsolationReadCommited(Connection dbConn)
	{
		if (Constants.DB_TYPE==Constants.DB_ORACLE) return;

		try
		{
			if (dbConn.getTransactionIsolation()!=Connection.TRANSACTION_READ_COMMITTED)
			{
				dbConn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	public static void setTransactionIsolationReadUNCommited(Connection dbConn)
	{
		if (Constants.DB_TYPE==Constants.DB_ORACLE) return;

		try
		{
			if (dbConn.getTransactionIsolation()!=Connection.TRANSACTION_READ_UNCOMMITTED)
			{
				dbConn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 * @de3precated - DBName nie je potrebne
	 * @param request
	 * @return
	 */
	public static String getDBName(HttpServletRequest request)
	{
		if (request == null)
		{
			return("iwcm");
		}
		return(getDBName(Tools.getServerName(request)));
	}

	/**
	 * @de3precated - DBName nie je potrebne
	 * @param domain
	 * @return
	 */
	public static String getDBName(String domain)
	{
		return("iwcm");
	}

	public static String readFileContent()
	{
		return readFileContent(null);
	}

	public static String readFileContent(String customPoolmanPath)
	{
		StringBuilder contextFile = new StringBuilder();

		String poolmanPath = System.getProperty("webjetPoolmanPath");

		try
		{
			InputStream is;

			if (Tools.isNotEmpty(customPoolmanPath)) {
				poolmanPath = customPoolmanPath;
			}

			if (poolmanPath==null || poolmanPath.length()<1)
			{
				is = DBPool.class.getResourceAsStream("/poolman.xml");
				poolmanPath = "poolman.xml";
			}
			else if (new File(poolmanPath).exists())
			{
				//je to priamo cesta k suboru
				is = new FileInputStream(poolmanPath);
			}
			else
			{
				//je to cesta v classes adresari (napr. poolman-devel.xml)
				is = DBPool.class.getResourceAsStream(poolmanPath);
			}
			if (is == null) {
				//ak sa nenasiel, skus este raz /poolman.xml
				is = DBPool.class.getResourceAsStream("/poolman.xml");
				poolmanPath = "poolman.xml";
			}
			if (is == null) {
				//skus natvrdo /WEB-INF/classes/poolman.xml
				String data = FileTools.readFileContent("/WEB-INF/classes/poolman.xml");
				if (data != null && data.length()>30) contextFile.append(data);
			}

			if (contextFile.length()>30) {
				//it's allready readed
			}
			else if (is != null) {
				InputStreamReader isr = new InputStreamReader(is);
				char[] buff = new char[8000];
				int len;
				String line;
				while ((len = isr.read(buff))!=-1)
				{
					line = new String(buff, 0, len);
					contextFile.append(line);
				}
				isr.close();
				is.close();
			} else {
				sk.iway.iwcm.Logger.error(DBPool.class, poolmanPath+" file doesn't exists");
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		Runtime rt = Runtime.getRuntime();
	   long free = rt.freeMemory();
	   long total = rt.totalMemory();
	   long used = total - free;
	   long max = rt.maxMemory();
	   int proces = rt.availableProcessors();
	   Logger.println(DBPool.class,"Free  = "+(free/1024/1024) +" MB ("+free +" bytes)<br>");
	   Logger.println(DBPool.class,"Total = "+(total/1024/1024)+" MB ("+total+" bytes)<br>");
	   Logger.println(DBPool.class,"Used = "+(used/1024/1024)+" MB ("+used+" bytes)<br>");
	   Logger.println(DBPool.class,"Max  = "+(max/1024/1024)+" MB ("+max+" bytes)<br>");
	   Logger.println(DBPool.class,"Processors = "+proces);

		//Logger.println(this,"xml="+contextFile);

   		return(contextFile.toString());
	}

	private static int getIntValue(String value, int defaultValue)
	{
		int ret = defaultValue;
		try
		{
			if (value!=null)
			{
				ret = Integer.parseInt(value.trim());
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}

	/**
	 * Vrati nazvy DataSource-ov z hashtabulky
	 * @return
	 */
	public static Set<String> getDataSourceNames()
	{
		Set<String> dataSourceNames = new HashSet<>();
		dataSourceNames.addAll(dataSourcesTable.keySet());
		if (externalDataSources != null) dataSourceNames.addAll(externalDataSources.keySet());
		return dataSourceNames;
	}

	/**
	 * Inicializacia JPA (vola sa z InitServlet-u), bezdovodne NEVOLAT, inak sa entityManagerFactories odznova inicializuju!!!
	 */
	public static void jpaInitialize()
	{
		DebugTimer dt = new DebugTimer("DBPool JPA init");
		entityManagerFactories = new HashMap<>();

		for(String name : getDataSourceNames())
		{
			if (JpaTools.isJPADatasource(name))
			{
				dt.diff("START <:--:> "+name);

				EntityManagerFactory factory = new WebJETPersistenceProvider().createEntityManagerFactory(name, null);

				entityManagerFactories.put(name, factory);
				dt.diff("END <:--:> "+name+" factory="+factory);
			}
		}
		dt.diff("-----JPA INIT E N D-------");
	}

	/**
	 * zatvorenie entityManagerFactories (vola sa v destroy-i InitServlet-u)
	 */
	public static void jpaDestroy()
	{
	    if (entityManagerFactories==null) return;
		for (EntityManagerFactory factory : entityManagerFactories.values())
		{
			if (factory.isOpen())
				factory.close();
		}
	}

	/**
	 * vrati EntityManagerFactory pre zadany nazov DB spojenia
	 * @param dbName - Nazov DB spojenia
	 * @return
	 */
	public static EntityManagerFactory getEntityManagerFactory(String dbName)
	{
		return entityManagerFactories.get(dbName);
	}

	/**
	 * naplni mapu "externalDataSources" datasourcami z aplikacneho servera, ktorych nazov obsahuje "webjet"
	 */
	private static void addExternalDataSources()
	{
		Logger.println(DBPool.class, "Adding external datasources");

		Map<String, DataSource> allDataSources = new HashMap<>();
		Map<String, DataSource> wjDataSources = new HashMap<>();

		try
		{
			addAllExternalDatasources(new InitialContext(), allDataSources, "");

			for(Entry<String, DataSource> entry : allDataSources.entrySet())
			{
				if(entry.getKey().contains("/webjet/"))
				{
					String dsName = entry.getKey().substring(entry.getKey().lastIndexOf("/") + 1);
					Logger.println(DBPool.class, "Adding JNDI datasource "+entry.getKey()+" dsName="+dsName);
					//kluc v mape je nazov za poslednim lomitkom /
					DataSource ds = entry.getValue();
					wjDataSources.put(dsName, ds);
					if ("iwcm".equals(dsName))
					{
						//nastav driver test
						//Logger.debug(DBPool.class, ds.toString());
						Connection conn = ds.getConnection();
						String databaseName = conn.getMetaData().getDatabaseProductName();

						Logger.println(DBPool.class, "Database name: "+databaseName);

						if (Tools.isNotEmpty(databaseName))
						{
							databaseName = databaseName.toLowerCase();
							if (databaseName.indexOf("oracle")!=-1 || entry.getKey().contains("/oracle/"))
							{
								Logger.println(DBPool.class, "Setting ORACLE dialect - "+databaseName);
								Constants.DB_TYPE = Constants.DB_ORACLE;
								ConfDB.CONF_TABLE_NAME = "webjet_conf";
					      		ConfDB.CONF_PREPARED_TABLE_NAME = "webjet_conf_prepared";
					      		ConfDB.MODULES_TABLE_NAME = "webjet_modules";
					      		ConfDB.ADMINLOG_TABLE_NAME = "webjet_adminlog";
					      		ConfDB.DB_TABLE_NAME = "webjet_db";
					      		ConfDB.PROPERTIES_TABLE_NAME = "webjet_properties";
							}
							if (databaseName.indexOf("microsoft")!=-1 || entry.getKey().contains("/mssql/"))
							{
								Logger.println(DBPool.class, "Setting MS SQL dialect - "+databaseName);
								Constants.DB_TYPE = Constants.DB_MSSQL;
							}
						}
						conn.close();
					}
				}
			}
		}
		catch(Exception e){sk.iway.iwcm.Logger.error(e);}

		externalDataSources = wjDataSources;
	}

	/**
	 * ziska vsetky datasourcy z aplikacneho servera a prida ich do mapy
	 * @param ctx
	 * @param map
	 * @param fullPath
	 */
	private static void addAllExternalDatasources(Context ctx, Map<String, DataSource> map, String fullPath)
	{
		try
		{
			String defaultJndiPath = "/jndi/webjet/iwcm"; //NOSONAR

			DataSource dataSource = (DataSource)ctx.lookup(defaultJndiPath);
			Logger.println(DBPool.class, "DBPool: addAllExternalDatasources - "+defaultJndiPath+", defaultDataSource="+dataSource);
			if (dataSource != null)
			{
				map.put(defaultJndiPath, dataSource);
			}
		}
		catch (javax.naming.NameNotFoundException e)
		{
			//do nothing
		}
		catch (Exception e)
		{
			Logger.debug(DBPool.class, e.getMessage());
			//sk.iway.iwcm.Logger.error(e);
		}

		try
		{
			String defaultJndiPath = "/jdbc/webjet/iwcm"; //NOSONAR

			DataSource dataSource = (DataSource)ctx.lookup(defaultJndiPath);
			Logger.println(DBPool.class, "DBPool: addAllExternalDatasources - "+defaultJndiPath+", defaultDataSource="+dataSource);
			if (dataSource != null)
			{
				map.put(defaultJndiPath, dataSource);
			}
		}
		catch (javax.naming.NameNotFoundException e)
		{
			//do nothing
		}
		catch (Exception e)
		{
			Logger.debug(DBPool.class, e.getMessage());
			//sk.iway.iwcm.Logger.error(e);
		}

		try
		{
			String defaultJndiPath = "java:comp/env/jdbc/webjet/iwcm";

			DataSource dataSource = (DataSource)ctx.lookup(defaultJndiPath);
			Logger.println(DBPool.class, "DBPool: addAllExternalDatasources - "+defaultJndiPath+", defaultDataSource="+dataSource);
			if (dataSource != null)
			{
				map.put(defaultJndiPath, dataSource);
			}
		}
		catch (javax.naming.NameNotFoundException e)
		{
			//do nothing
		}
		catch (Exception e)
		{
			Logger.debug(DBPool.class, e.getMessage());
			//sk.iway.iwcm.Logger.error(e);
		}

		try
		{
			Logger.println(DBPool.class, "DBPool: addAllExternalDatasources");

			String namespace = ctx instanceof InitialContext ? ctx.getNameInNamespace() : "";

			Logger.println(DBPool.class, "DBPool: addAllExternalDatasources, namespace2="+namespace);

			NamingEnumeration<NameClassPair> list = ctx.list(namespace);

			//Object dataSource = ctx.lookup("java:comp/env/jndi/webjet/iwcm");
			//Logger.println(DBPool.class, "DBPool: addAllExternalDatasources, dataSource="+dataSource);

			Logger.println(DBPool.class, "DBPool: addAllExternalDatasources, list="+list);
			while (list.hasMoreElements())
			{
				try
				{
					NameClassPair next = list.next();
					String name = next.getName();
					String jndiPath = namespace + name;
					Logger.println(DBPool.class, "Scanning "+jndiPath);

					//toto na Tomcate scanovalo vsetky subory
					if ("Resources".equals(jndiPath)) continue;
					try
					{
						Object tmp = ctx.lookup(jndiPath);
						if (tmp instanceof Context)
						{
							addAllExternalDatasources((Context) tmp, map, fullPath+"/"+jndiPath);
						}
						else if(tmp instanceof DataSource)
						{
							map.put(fullPath+"/"+jndiPath, (DataSource) tmp);
						}
					}
					catch (Exception e){}
				}
				catch(Exception e){sk.iway.iwcm.Logger.error(e);}
			}
		}
		catch (javax.naming.NameNotFoundException e)
		{
			//do nothing
		}
		catch(Exception e)
		{
			Logger.debug(DBPool.class, e.getMessage());
			//sk.iway.iwcm.Logger.error(e);
		}
	}

	 /**
	  * Cez reflection zisti, ci existuje trieda na desifrovanie, ak ano pokusi sa desifrovat heslo. Ak nie vrati povodne heslo
	  * Ticket: #19322 - NN IT - sifrovanie hesiel DB
	  * @param encryptedPassword - "cesta k triede s metodou decrypt"-"sifrovane_heslo" napr: sk.iway.nn.system.CustomPasswordDecryption-1DV4hO5IVqOXpMahxGJ8iuEBN/dTSiQXGXEBZABAr0E=
	  * @author	$(prau)
	  */
	 private static String decryptPassword(String encryptedPassword)
	 {
		  if(Tools.isNotEmpty(encryptedPassword) && encryptedPassword.startsWith("sk.iway.") && encryptedPassword.contains("-"))
		  {
				try
				{
					 Class<?> c = Class.forName(encryptedPassword.substring(0, encryptedPassword.indexOf("-")));
					 Object o = c.getDeclaredConstructor().newInstance();
					 Method m;
					 Class<?>[] parameterTypes = new Class[] {String.class};
					 Object[] arguments = new Object[] {encryptedPassword.substring(encryptedPassword.indexOf("-")+1)};
					 m = c.getMethod("decrypt", parameterTypes);
					 String decryptedPassword = (String)m.invoke(o, arguments);
					 if(Tools.isNotEmpty(decryptedPassword))
					 {
						  Logger.debug(DBPool.class, "Decrypted password used.");
						  return decryptedPassword;
					 }
				}
				catch (Exception ex)
				{
					 sk.iway.iwcm.Logger.error(ex);
					 Logger.debug(DBPool.class, "Default password used.");
				}
		  }
		  return encryptedPassword;
	 }
}
