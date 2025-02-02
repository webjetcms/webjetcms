package sk.iway.iwcm;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.apache.commons.dbcp.ConfigurableDataSource;

import cryptix.provider.key.RawSecretKey;
import cryptix.util.core.Hex;
import sk.iway.Password;
import sk.iway.iwcm.dmail.Sender;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.FileCache;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.JarPackaging;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.UpdateDatabase;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.cluster.ClusterRefresher;
import sk.iway.iwcm.system.cron.CronDB;
import sk.iway.iwcm.system.cron.CronFacade;
import sk.iway.iwcm.system.cron.CronTask;
import sk.iway.iwcm.system.cron.WebjetDatabaseTaskSource;
import sk.iway.iwcm.system.proxy.WebJETProxySelector;
import sk.iway.iwcm.users.UserGroupsDB;
/**
 *  Inicializacia systemu, nastavenie databazy, overenie licencie
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.28 $
 *@created      Nedela, 2002, jun 9
 *@modified     $Date: 2004/03/22 08:09:03 $
 */
//@WebServlet(name = "iwcminit", urlPatterns = {"/init"}, loadOnStartup = 1)
public class InitServlet extends HttpServlet
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3175770407979840865L;

	private static String actualVersion = "2024.0.52.{minor.number} {build.date}";

	private static final int DOMAIN_LEN = 10;

	private static String[] domain = null;

	private static boolean valid = false;

	private static int licenseId = -1;

	private static boolean webjetInitialized = false;
	private static boolean webjetConfigured = false;

	private static final Date SERVER_START_DATETIME = new Date();

	private transient ClusterRefresher clusterRefresher = null;

	private static String contextDbName = null;

	/**
	 *  Description of the Method
	 *
	 *@exception  ServletException  Description of the Exception
	 */
	@Override
	public void init() throws ServletException
	{
		DebugTimer dt = new DebugTimer("InitServlet.init");

		Logger.debug(getClass(),"init start");
		setContextDbName(getServletContext().getInitParameter("webjetDbname"));
		Logger.debug(getClass(),"contextDbName="+contextDbName);
		Constants.clearValues();
		//inicializuj casti pre nove verzie WebJETu
		ConstantsV9.clearValuesWebJet9();

		String dbName = getInitParameter("dbName");
		//Constants.addDomains(dbName, domains);

		if (dbName == null || dbName.length() < 1)
		{
			dbName = "iwcm";
		}
		Logger.println(this, "dbName="+dbName);

		Logger.println(this, "-----------------------------------------------");
		Logger.println(this, "WebJET initializing, root: " + getServletContext().getRealPath("/"));
		Logger.println(this, "");

		int days_remaining = 0;
		setLicenseId(-1);
		String modulesEnableList = null;

		//automaticke nastavenie JarPackaging ak existuje JAR subor (aby sa dala DB pouzivat aj pri standardnom vyvoji vo WJ)
		JarPackaging.initialize();

		Connection db_conn = null;
		try
		{
			//toto potrebujeme nastavit pred DB inicializaciou
			String mariaDbDefaultEngine = getInitParameter("mariaDbDefaultEngine", null);
			if (Tools.isNotEmpty(mariaDbDefaultEngine)) Constants.setString("mariaDbDefaultEngine", mariaDbDefaultEngine);

			Logger.println(this,"Checking database connection: ");
			db_conn = DBPool.getConnection(dbName);
			if (db_conn == null)
			{
				Logger.println(this,"   Database connection: [FAILED]");
				Logger.println(this,"ERROR: Server halted (Database connection failed).");
				return;
			}

			Logger.println(this,"   Database connection: [OK]");

			//String serverType = getInitParameter("serverType", db_conn);

			Map<String, String> databaseValues = getDatabaseValues(db_conn);

			db_conn.close();
			db_conn = null;

			try
			{
				//nastavenie proxy
				String proxyHost = getInitParameter("proxyHost", databaseValues);

				if (proxyHost != null && proxyHost.length() > 1)
				{
					int proxyPort = Tools.getIntValue(getInitParameter("proxyPort", databaseValues), 0);
					String proxyUser = getInitParameter("proxyUser", databaseValues);
					String proxyPassword = getInitParameter("proxyPassword", databaseValues);
					String proxyHostsException = getInitParameter("proxyHostsException", databaseValues);
					String proxyHostHttps = getInitParameter("proxyHostHttps", databaseValues);
					int proxyPortHttps = Tools.getIntValue(getInitParameter("proxyPortHttps", databaseValues), 0);

					//ak mame prazdne pouzi rovnake ako pre http, ak chceme vypnut httpS proxy treba v konfigu nastavit host na X a port na 1, vtedy sa nepouzije
					if (Tools.isEmpty(proxyHostHttps)) proxyHostHttps = proxyHost;
					if (proxyPortHttps == 0) proxyPortHttps = proxyPort;

					WebJETProxySelector.initProxy(proxyHost, proxyPort, proxyUser, proxyPassword, proxyHostsException, proxyHostHttps, proxyPortHttps);
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}

			//testni kluc
			try
			{
				if (databaseValues.size() < 1) {
					Logger.println(this,"ERROR: not configured");
					webjetConfigured = false;
					throw new Exception("Not configured");
				}
				webjetConfigured = true;

				xjava.security.Cipher alg = null;
				try
				{
					java.security.Security.addProvider(new cryptix.provider.Cryptix());
					//alg = xjava.security.Cipher.getInstance("Rijndael", "Cryptix");
					alg = xjava.security.Cipher.getInstance(new cryptix.provider.cipher.Rijndael(), null, null);
				}
				catch (Exception e)
				{
					System.out.println("Error instancing Cryptix " + e.getMessage()); //NOSONAR
					sk.iway.iwcm.Logger.error(e);
				}


				String license = getInitParameter("license", databaseValues);

				if (license == null || license.length() < 10)
				{
					Logger.println(this,"ERROR: missing license");
					throw new Exception("Missing license");
				}

				//skontroluj licenciu
				if (license.length()>32)
				{
					//uprav podla udajov
					int pos = Tools.getIntValue(Character.toString(license.charAt(0)), -1);
					int len = Tools.getIntValue(Character.toString(license.charAt(1)), -1);
					if (len==-1)
					{
						if ('a'==license.charAt(1)) len=10;
						else if ('b'==license.charAt(1)) len=11;
						else if ('c'==license.charAt(1)) len=12;
						else if ('d'==license.charAt(1)) len=13;
						else if ('e'==license.charAt(1)) len=14;
						else if ('f'==license.charAt(1)) len=15;
						else if ('g'==license.charAt(1)) len=16;
						else if ('h'==license.charAt(1)) len=17;
						else if ('i'==license.charAt(1)) len=18;
						else if ('j'==license.charAt(1)) len=19;
						else if ('k'==license.charAt(1)) len=20;
						else if ('l'==license.charAt(1)) len=21;
						else if ('m'==license.charAt(1)) len=22;
						else if ('n'==license.charAt(1)) len=23;
					}
					if (pos < 1 || len < 1)
					{
						Logger.println(this,"ERROR: wrong license");
						throw new Exception();
					}
					//ziskaj podstring
					StringBuilder licData = new StringBuilder(license.substring(pos+2, pos+len+2));
					//Logger.println(this,"licData="+licData);

					//uprav license
					license = license.substring(2, pos+2) + license.substring(pos+len+2);
					//Logger.println(this,"license="+license);

					licData.append("                               ");
					if (licData.charAt(0)=='0') Constants.setString("wjVersion", "B");
					else if (licData.charAt(0)=='1') Constants.setString("wjVersion", "P");
					else if (licData.charAt(0)=='2') Constants.setString("wjVersion", "E");
					else if (licData.charAt(0)=='3') Constants.setString("wjVersion", "C");
					else if (licData.charAt(0)=='4') Constants.setString("wjVersion", "I");
					else if (licData.charAt(0)=='5') Constants.setString("wjVersion", "D");
					else if (licData.charAt(0)=='6') Constants.setString("wjVersion", "M");
					else if (licData.charAt(0)=='7') Constants.setString("wjVersion", "O");

					modulesEnableList = licData.substring(1).trim();
				}

				//dekoduj licenciu
				//Logger.println(this,"---->"+license);
				String decoded = decrypt(license, alg);
				//Logger.println(this,"====>"+decoded);
				String[] domainDecoded = new String[1];
				domainDecoded[0] = decoded.substring(0, DOMAIN_LEN);

				if (domainDecoded[0].lastIndexOf('#') != -1)
				{
					domainDecoded[0] = domainDecoded[0].substring(domainDecoded[0].lastIndexOf('#') + 1);
				}
				setDomain(domainDecoded);

				decoded = align(decoded, 16, false);

				//dekoduj datum
				String s_date = decoded.substring(DOMAIN_LEN, DOMAIN_LEN + 3);

				String s_month = s_date.substring(0, 1);
				String s_year = "20" + s_date.substring(1);

				//Logger.println(this,"m="+s_month+" y="+s_year+" decoded="+decoded);

				int month;
				if (s_month.compareTo("A") == 0)
				{
					month = 10;
				}
				else if (s_month.compareTo("B") == 0)
				{
					month = 11;
				}
				else if (s_month.compareTo("C") == 0)
				{
					month = 12;
				}
				else
				{
					month = Integer.parseInt(s_month);
				}
				int year = Integer.parseInt(s_year);

				Calendar cal = Calendar.getInstance();

				long l_now = cal.getTime().getTime();

				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month-1);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 59);
				cal.set(Calendar.SECOND, 59);
				//cal.add(Calendar.DAY_OF_YEAR, -1);

				//InitServlet.calendarValidUntil = cal;

				SimpleDateFormat formatter = new SimpleDateFormat(Constants.getString("dateTimeFormat"));
				Logger.println(this,"License is valid until: " + formatter.format(cal.getTime()));

				long l_license = cal.getTime().getTime();

				long diff = l_license - l_now;

				if (diff < 0)
				{
					Logger.println(this,"ERROR: License is out of date, please contact\n  InterWay (www.interway.sk)\n  for new license.");
					return;
				}

				days_remaining = (int) (diff / 86400000L);
				Logger.println(this,"Remaining: " + days_remaining + " days");

				if (days_remaining < 29)
				{
					Logger.println(this,"License will expire soon, please contact\n  InterWay (www.interway.sk)\n  for new license.");
				}

				String sLicId = decoded.substring(DOMAIN_LEN + 3);
				//dekoduj
				byte[] b = sLicId.getBytes();
				int i = b[2] - 32;
				int num = i * 4096;

				i = b[1] - 32;
				num = num + (i * 64);

				i = b[0] - 32;
				num = num + i;

				setLicenseId(num);

				//konektni sa na license server a over licenciu
				boolean serverValid = false;
				String serverDomain = "";
				int serverChalenge = -1;
				boolean connectSuccess = false;
				try
				{
					Logger.println(this,"Verifying license, please wait...");
					String pDocRoot = URLEncoder.encode(getServletContext().getRealPath("/")+";"+dbName+";"+getInitParameter("installName", databaseValues), "windows-1250");
					String urlStr = "http://license.interway.sk/verify.do?id=" + num + "&sdt=" + l_now + "&docRoot=" + pDocRoot;
					//System.out.println(urlStr);
					URL url = new URL(urlStr);

					URLConnection conn = url.openConnection();

					conn.setDoOutput(true);
					conn.setDefaultUseCaches(false);
					conn.setUseCaches(false);

					conn.setRequestProperty("Content-Type", "text/plain; charset=windows-1250");
					conn.setRequestProperty("Cache-Control", "no-cache");

					BufferedReader br = null;
					try
					{
						br = new BufferedReader(new InputStreamReader(conn.getInputStream(), Constants.FILE_ENCODING));
						String msg;
						while ((msg = br.readLine()) != null)
						{
							//System.out.println("SERVER RESPONSE: "+msg);

							if (msg.compareTo("valid=true") == 0)
							{
								serverValid = true;
								connectSuccess = true;
							}
							if (msg.startsWith("domain="))
							{
								serverDomain = msg.substring(7);
								connectSuccess = true;
							}
							if (msg.startsWith("chalenge="))
							{
								serverChalenge = Integer.parseInt(msg.substring(9));
								connectSuccess = true;
							}
						}
					}
					finally { if (br != null) br.close(); }
				}
				catch (Exception ex)
				{
					connectSuccess = false;
				}

				if (connectSuccess)
				{
					//over integritu kluca
					b = license.getBytes();
					int sum = 0;
					int len;
					len = b.length;

					for (i = 0; i < len; i++)
					{
						sum = sum + b[i];
					}

					//System.out.println("SERVER LEN="+len+" sum="+sum);

					if (sum != serverChalenge)
					{
						Logger.println(this,"ERROR: server challenge different!");
						throw new Exception();
					}

					if (!serverDomain.endsWith(InitServlet.domain[0]))
					{
						Logger.println(this,"ERROR: invalid domain!");
						throw new Exception();
					}

					if (serverValid == false)
					{
						Logger.println(this,"ERROR: license is invalid");
						throw new Exception();
					}
				}

				dt.diff("License is verifyied.");
				InitServlet.setValid(true);

				//nacitaj dalsie licencie, ak existuju
				String licenseDomains = getInitParameter("licenseDomains", databaseValues);
				if (Tools.isNotEmpty(licenseDomains))
				{
					List<String> domainList = new ArrayList<>();
					domainList.add(domainDecoded[0]);
					String[] licenseDomainsArr = Tools.getTokens(licenseDomains, ",\n");
					for (String domainKey : licenseDomainsArr)
					{
						if (domainKey.indexOf(":")>0) domainKey = domainKey.substring(domainKey.indexOf(":")+1); //NOSONAR
						//System.out.println("domainKey="+domainKey);
						decoded = decrypt(domainKey, alg);
						domainList.add(decoded);
					}
					domainDecoded = domainList.toArray(new String[0]);
					setDomain(domainDecoded);
				}

				for (i = 0; i<InitServlet.domain.length; i++)
				{
					Logger.println(this, "License domain: " + InitServlet.domain[i]);
				}
			}
			catch (Exception ex)
			{
				if ("Missing license".equals(ex.getMessage())) {
					//start as open source license
					Constants.setString("wjVersion", "O");
					InitServlet.setValid(true);

					Logger.println(this,"License: OpenSource/Community");

					String[] domainDecoded = new String[1];
					domainDecoded[0] = "";
					setDomain(domainDecoded);
				} else if ("Not configured".equals(ex.getMessage())) {
					Logger.println(this,"ERROR: Server not configured.");
					Logger.println(this,"ERROR: Server not configured.");
					Logger.println(this,"ERROR: Server not configured.");
					Logger.println(this,"ERROR: Server not configured.");
					Logger.println(this,"ERROR: Server not configured.");
					Logger.println(this,"ERROR: Server not configured.");
					Logger.println(this,"ERROR: Server not configured.");
					Logger.println(this,"ERROR: Server not configured.");
					return;
				} else {
					sk.iway.iwcm.Logger.error(ex);
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					Logger.println(this,"ERROR: Server halted (license is not valid).");
					return;
				}
			}

			String installName = getInitParameter("installName", databaseValues);
			if (installName == null || installName.trim().length() < 1)
			{
				try
				{
					String path = getServletContext().getRealPath("/");
					if (path != null)
					{
						File f = new File(path);
						installName = f.getName();
						installName = Tools.replace(installName, ".sk", "");
						installName = Tools.replace(installName, ".cz", "");
						installName = Tools.replace(installName, ".com", "");
						installName = Tools.replace(installName, "www.", "");
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
				Logger.println(this,"guesing install name: "+installName);
			}

			Constants.setInstallName(installName);
			Logger.setInstallName(installName);

			if ("O".equals(Constants.getString("wjVersion"))==false) {
				Constants.setString("amchartLicense", ConfDB.tryDecrypt("encr"+"ypted:f4a06"+"45be29a4d976"+"9f5f8683d106619"));
			}

			dt.diff("Before loadConstants");
			loadConstants(databaseValues);
			dt.diff("After loadConstants");

			String clusterMyNodeName = getInitParameter("clusterMyNodeName");
			int webjetNodeId = Tools.getIntValue(System.getProperty("webjetNodeId"), -1);
			if (webjetNodeId==-1) webjetNodeId = Tools.getIntValue(System.getenv("webjetNodeId"), -1);
			if (webjetNodeId==-1) webjetNodeId = Tools.getIntValue(getInitParameter("webjetNodeId", null),-1);
			if (Tools.isEmpty(clusterMyNodeName) && webjetNodeId>=0)
			{
				clusterMyNodeName = "node"+webjetNodeId;
				Logger.println(this,"INIT: pkeyGenOffset="+webjetNodeId);
				Constants.setInt("pkeyGenOffset", webjetNodeId);
			}
			//v dockeri v rezime auto pouzijeme hostname ako nodeId
			if ("auto".equals(Constants.getString("clusterNames")))
			{
				clusterMyNodeName = trimHostname(System.getenv("HOSTNAME"));
				if (Tools.isEmpty(clusterMyNodeName))
				{
					try
					{
						InetAddress ip = InetAddress.getLocalHost();
						String hostname = ip.getHostName();
						if (Tools.isNotEmpty(hostname))
						{
							//MacBook-Pro-uzivatela-Lubos -> MacBook-Pro-Lubos
							hostname = Tools.replace(hostname, "-uzivatela-", "-");
							//odstran aj pomlcky
							hostname = Tools.replace(hostname, "-", "");
							clusterMyNodeName = trimHostname(hostname);
						}
						else clusterMyNodeName = trimHostname(ip.getHostAddress());
					} catch (Exception ex) {
						sk.iway.iwcm.Logger.error(ex);
					}
				}
				if (Tools.isEmpty(clusterMyNodeName)) clusterMyNodeName = trimHostname("auto-"+Tools.getNow());

				//if it's default value change it to 1 to always load pkeyGen value from DB
				if (Constants.getInt("pkeyGenBlockSize")==10) Constants.setInt("pkeyGenBlockSize", 1);
			}
			if (Tools.isNotEmpty(clusterMyNodeName))
			{
				Logger.println(this,"INIT: clusterMyNodeName="+clusterMyNodeName);
				Constants.setString("clusterMyNodeName", clusterMyNodeName);
				ClusterDB.cleanup();
			}
			else Constants.setString("clusterMyNodeName", "");

			String clusterMyNodeType = getInitParameter("clusterMyNodeType");
			if (Tools.isEmpty(clusterMyNodeType) && "public".equals(System.getProperty("webjetNodeType")))
			{
				clusterMyNodeType = "public";
			}

			if (FileTools.isDirectory("/admin")==false) clusterMyNodeType = "public";

			if (Tools.isNotEmpty(clusterMyNodeType))
			{
				Logger.println(this,"INIT: clusterMyNodeType="+clusterMyNodeType);
				Constants.setString("clusterMyNodeType", clusterMyNodeType);
			}
			else Constants.setString("clusterMyNodeType", "full");

			//aby sme v clustri mohli mat admin cast za NTLM autorizaciou
			String NTLMDomainController = getInitParameter("NTLMDomainController");
			if (Tools.isNotEmpty(NTLMDomainController))
			{
				Logger.println(this,"INIT: NTLMDomainController="+NTLMDomainController);
				Constants.setString("NTLMDomainController", NTLMDomainController);
			}

			//v clustri mozeme mat tuto hodnotu pre niektore nody rozdielnu
			String baseHrefLoopback = getInitParameter("baseHrefLoopback");
			if (Tools.isNotEmpty(baseHrefLoopback))
			{
				Logger.println(this,"INIT: baseHrefLoopback="+baseHrefLoopback);
				Constants.setString("baseHrefLoopback", baseHrefLoopback);
			}

			//moznost nastavenia separatne pre verejne nody cez Tomcat ako -DwebjetLuceneIndexDir=WEB-INF/lucene_index/node1/
			String luceneIndexDir = System.getProperty("webjetLuceneIndexDir");
			if (Tools.isNotEmpty(luceneIndexDir))
			{
				Logger.println(this,"INIT: luceneIndexDir="+luceneIndexDir);
				Constants.setString("luceneIndexDir", luceneIndexDir);
			}

			Logger.println(this,"Constants loaded");
			dt.diff("Constants loaded");

			Logger.setWJLogLevel(Constants.getString("logLevel"));

			Tools.reinitialize();

			//toto musime vykonat, kedze JPA sa inicializuje uz skor z defaultneho nastavenia (a potom ignoruje loadnute hodnoty)
			DB.resetHtmlAllowedFields();

			//reinit iwfsdb
			IwcmFsDB.init();

			//musime aj reloadnut text.properties
			if (Tools.isNotEmpty(Constants.getString("propInstallNames")))
			{
				Prop.getInstance(true);
			}

			/**
			 *	Inicializacia Stripes od WebJET 7 na tomto mieste kvoli moznosti mat
				*	pre rozne instalacie WJ rozne properties na tom istom tomcatovi, predtym sa to ukladalo do System.setProperty
				*/

			StringBuilder packages = new StringBuilder("sk.iway.iwcm.system,")
													.append("sk.iway.iwcm.stripes,")
													.append("sk.iway.iwcm.components,")
													.append("sk.iway.iwcm.components.welcome,")
													.append("sk.iway.iwcm.components.uschovna,")
													.append("sk.iway.iwcm.components.sharepoint,")
													.append("sk.iway.iwcm.i18n,")
													.append("sk.iway.iwcm.form,")
													.append("sk.iway.magzilla,")
													.append("sk.iway.iwcm.users,")
													.append("sk.iway.iwcm.gallery,")
													.append("sk.iway.iwcm.inquiry,")
													.append("sk.iway.iwcm.mobile,")
													.append("sk.iway.iwcm.dmail,")
													.append("sk.iway.").append(Constants.getInstallName());

			String addPackages = Constants.getString("stripesAddPackages");

			if (Tools.isNotEmpty(addPackages))
			{
				addPackages = Tools.replace(addPackages, ".*", "");
				packages.append(',').append(addPackages);
			}


			Logger.println(InitServlet.class, "Stripes init, packages = " + packages);

			//System.setProperty("ActionResolver.Packages", packages); //
			Constants.setString("stripes.ActionResolver.Packages", packages.toString());

			/*
				ukoncena inicializacia Stripes balickov
				*/

			if ("false".equals(getInitParameter("useSMTPServer")))
			{
				Constants.setString("useSMTPServer","false");
			}
			else if ("true".equals(getInitParameter("useSMTPServer")))
			{
				//we must explicitly check true because of empty/other value
				Constants.setString("useSMTPServer","true");
			}

			String smtpServer = System.getProperty("webjetSmtpServer");
			if (Tools.isNotEmpty(smtpServer))
			{
				Logger.println(this,"INIT: smtpServer="+smtpServer);
				Constants.setString("smtpServer", smtpServer);
			}

			Constants.setServletContext(getServletContext());

			loadVersion();

			Constants.setString("defaultEncoding", SetCharacterEncodingFilter.encoding);
			Logger.println(InitServlet.class, "Setting defaultEncoding to "+Constants.getString("defaultEncoding"));
			Logger.println(this,"update database call");

			dt.diff("before update database");
			UpdateDatabase.update();
			dt.diff("after update database, cp="+PathFilter.getCustomPath());

			String logLevels = Constants.getString("logLevels", "");
			if (Tools.isNotEmpty(logLevels)) {
				Logger.setWJLogLevels(Logger.getLogLevelsMap(logLevels));
			}

			try
			{
				if (Tools.isEmpty(PathFilter.getCustomPath()))
				{
					//posli email
					StringBuilder emailBody = new StringBuilder();

					Logger.println(this, "get hostname");
					String serverName = InetAddress.getLocalHost().getHostName();
					Logger.println(this, "get hostname done");
					emailBody.append("ServerName: ").append(serverName).append('\n');
					//emailBody += "ServerIP: " + InetAddress.getLocalHost().getHostAddress() + "\n";

					InetAddress[] ipecky = InetAddress.getAllByName(serverName);
					int i;
					for (i = 0; i < ipecky.length; i++)
					{
						emailBody.append("ServerIP[").append(i + 1).append("]: ").append(ipecky[i].getHostAddress()).append(' ').append(ipecky[i].getHostName()).append('\n');
					}

					dt.diff("mam ipecky");

					emailBody.append("InstallName: ").append(Constants.getString("installName")).append("\n");
					if (Tools.isNotEmpty(Constants.getLogInstallName())) emailBody.append("LogInstallName: ").append(Constants.getLogInstallName()).append("\n");
					emailBody.append("wjVersion: ").append(Constants.getString("wjVersion")).append('\n');
					emailBody.append("version: ").append(getActualVersionLong()).append('\n');
					emailBody.append("Remaining: ").append(days_remaining).append('\n');
					emailBody.append("LicenseID: ").append(licenseId).append('\n');
					emailBody.append("Domain: ").append(InitServlet.domain[0]).append('\n');
					emailBody.append("Web root: ").append(getServletContext().getRealPath("/")).append('\n');
					emailBody.append("user: ").append(System.getProperty("user.name")).append('\n');
					String logInstallNameSuffix = "";
					if (Tools.isNotEmpty(Constants.getLogInstallName())) logInstallNameSuffix = "/"+Constants.getLogInstallName();
					SendMail.send("WebJET", "restart@webjetcms.sk", "lubos.balat@interway.sk", "WebJET restart [" + Constants.getString("installName") + logInstallNameSuffix + "] "+actualVersion+" rem: " + days_remaining + " server: " + InetAddress.getLocalHost().getHostName(), emailBody.toString());

					dt.diff("after email send");
				}
			} catch (Exception ex) {
				//silent
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			Logger.println(this,"   Database connection: [FAILED]");
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		Logger.println(InitServlet.class, "Initializing pkey generator");

		Constants.setServletContext(getServletContext());

		//inicializuj PkeyGenerator
		PkeyGenerator.getInstance();
		dt.diff("after pkey generator");

		Logger.println(InitServlet.class, "Initializing sender");

		Sender sender = Sender.getInstance();
		if (sender!=null && sender.getToSendCount() > 0)
		{
			Logger.println(this,"Emails in queue: " + sender.getToSendCount());
		}

		dt.diff("after sender");

		Logger.println(InitServlet.class, "Initializing docDB");

		DocDB.getInstance();
		UserGroupsDB.getInstance();

		dt.diff("after docDB");

		//toto tu uz netreba, kedze to zbehne pri instancii GroupsDB az na konci
		//GroupsDB.getInstance(true);
		Modules.getInstance(modulesEnableList, true);

		//nacitaj chranene adresare
		PathFilter.reloadProtectedDirs();

		//SendMailMultipart.sendLater("Janko Hraďż˝ko", "jeeff@jeeff.sk", "lubos@balat.sk", null, "Predmet ďż˝ďż˝čťžďż˝ďż˝ďż˝ďż˝", "Toto je body... ďż˝ďż˝čťžďż˝ďż˝ďż˝ďż˝ <img src=\"/images/wjlogo.gif\"><br> ahoj <b>TUCNE</b><a href='/showdoc.do?docid=4'>ODKAZ</a>", "http://iwcm.interway.sk", "18.11.2004", "14:32");

		// JRASKA: inicializacia Eclipselink JPA
		try
		{
			Logger.debugMemInfo();

			DBPool.jpaInitialize();

			Logger.debugMemInfo();
		}
		catch (RuntimeException ex2)
		{
			Logger.println(this," [FAIL]");
			sk.iway.iwcm.Logger.error(ex2);
		}

		dt.diff("after JPA");

		UpdateDatabase.updateAfterInit();

		dt.diff("after update afret init");

		String clusterNodeName = Constants.getString("clusterMyNodeName");

		StringBuilder logMessage = new StringBuilder();
		logMessage.append("InitServlet: ").append(getServletContext().getRealPath("/")).append("\n");
		logMessage.append("user: ").append(System.getProperty("user.name")).append("\n");
		if (Tools.isNotEmpty(clusterNodeName)) logMessage.append("node:").append(clusterNodeName).append("\n");
		logMessage.append("version: ").append(getActualVersionLong());
		Adminlog.add(Adminlog.TYPE_INIT, logMessage.toString(), -1, -1);

		clusterRefresher = new ClusterRefresher();

		dt.diff("after cluster refresher");

		//spusti cron4j
		try
		{
			CronFacade cf = CronFacade.getInstance();
			cf.setTaskSource(new WebjetDatabaseTaskSource());
			cf.start();

			//spustim tie ktore sa maju spustit hned po starte
			List<CronTask> startupTasks = CronDB.getCronTasksRunAtStartup();
			if(startupTasks != null)
			{
				for(CronTask t : startupTasks)
				{
					if(ClusterDB.isServerRunningInClusterMode()==false || "all".equals(t.getClusterNode()) || clusterNodeName.equals(t.getClusterNode()))
					{
						Logger.println(InitServlet.class, "Spustam cron {"+t.getId()+"} "+t.getTask()+" "+t.getParams());
						cf.runSimpleTaskOnce(t);
					}
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		//Logger.println(this,"---------------- INIT DONE --------------");

		dt.diff("Init done");

		WebJETProxySelector.setAuthenticator();

		PathFilter.prepareTemplates();

		FileCache.init();

		Logger.println(this,"---------------- INIT DONE, version: "+getActualVersionLong()+" --------------");


		/*Logger.println(InitServlet.class, "ZISKAVAM 3 DB SPOJENIA A NEZATVARAM");
		DBPool.getConnection();
		DBPool.getConnection();
		DBPool.getConnection();
		DBPool.getConnection();
		DBPool.getConnection();
		DBPool.getConnection();
		DBPool.getConnection();
		DBPool.getConnection();
		DBPool.getConnection();*/

		//zavolanie localconf pri lokalnom developmente
		if (FileTools.isFile("/localconf.jsp"))
		{
			Logger.println(this,"VOLAM localconf.jsp");
			//String localconf = Tools.downloadUrl("http://iwcm.interway.sk:8080/localconf.jsp");
			Tools.setTimeout(() -> Tools.downloadUrl("http://iwcm.interway.sk:8080/localconf.jsp"), 30000);
			Tools.setTimeout(() -> Tools.downloadUrl("http://iwcm.interway.sk/localconf.jsp"), 35000);
			//Logger.println(this,"VOLAM localconf.jsp, vystup:\n"+localconf);
		}

		//aby sa inicializoval SK properties subor za kazdych okolnosti (je default)
		Tools.setTimeout(() -> Prop.getInstance("sk"), 5000);

		//delete old struts-config.xml if we user jar packaging
		deleteOldStrutsConfig();

		dt.diff("DONE");

		setWebjetInitialized(true);
	}

	/**
	 *  Description of the Method
	 */
	@Override
	public void destroy()
	{
		setWebjetInitialized(false);

		Logger.println(this,"Destroying Cron4j");
		CronFacade.getInstance().stop();
		Logger.println(this,"Cron 4j destroyed");

		Sender sender = Sender.getInstance();
		if (sender != null)
		{
			sender.cancelTask();
		}

		SpamProtection.destroy();

		if (clusterRefresher != null)
		{
			clusterRefresher.cancelTask();
			clusterRefresher = null;
		}

		//JRASKA destroy JPA
		DBPool.jpaDestroy();

		try
		{
			DataSource ds = DBPool.getInstance().getDataSource("iwcm");
			if (ds instanceof ConfigurableDataSource)
			{
				ConfigurableDataSource cds = (ConfigurableDataSource)ds;
				cds.printStackTraces();
			}
		}
		catch (SQLException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		DBPool.getInstance().destroy(false);
	}

	/**
	 * Nahra z properties suboru aktualnu verziu podla build time
	 */
	private void loadVersion()
	{
		String buildDate = "";
		String minorVersion = "";
		try
		{
			IwcmFile propFile = new IwcmFile(Tools.getRealPath("/WEB-INF/build.properties"));
			IwayProperties prop = new IwayProperties();
			prop.load(propFile);

			buildDate = prop.getProperty("build.date");
			minorVersion = prop.getProperty("minor.number");
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		setActualVersion(Tools.replace(InitServlet.actualVersion, "{build.date}", buildDate));
		setActualVersion(Tools.replace(InitServlet.actualVersion, "{minor.number}", minorVersion));
	}

	private Map<String, String> getDatabaseValues(Connection db_conn)
	{
		Map<String, String> databaseValues = new Hashtable<>();
		try
		{
			PreparedStatement ps = db_conn.prepareStatement("SELECT * FROM "+ConfDB.CONF_TABLE_NAME+" ORDER BY name");
			ResultSet rs = ps.executeQuery();
			String name;
			String value;
			while (rs.next())
			{
				name = DB.getDbString(rs, "name");
				value = DB.getDbString(rs, "value");

				databaseValues.put(name, value);
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(InitServlet.class, ex.getMessage());
		}

		return databaseValues;
	}

	public void loadConstants(Map<String, String> databaseValues)
	{
		try
		{
			Map<String, String> skipValues = new Hashtable<>();

			int i = getInt("rootGroupId", databaseValues);
			skipValues.put("rootGroupId", "true");
			if (i > 0)
			{
				Constants.setInt("rootGroupId", i);
			}
			i = getInt("tempGroupId", databaseValues);
			skipValues.put("tempGroupId", "true");
			if (i > 0)
			{
				Constants.setInt("tempGroupId", i);
			}
			i = getInt("menuGroupId", databaseValues);
			skipValues.put("menuGroupId", "true");
			if (i > 0)
			{
				Constants.setInt("menuGroupId", i);
			}
			i = getInt("headerFooterGroupId", databaseValues);
			skipValues.put("headerFooterGroupId", "true");
			if (i > 0)
			{
				Constants.setInt("headerFooterGroupId", i);
			}
			i = getInt("newDocumentId", databaseValues);
			skipValues.put("newDocumentId", "true");
			Constants.setInt("newDocumentId", i);

			//pkeyGenerator
			i = getInt("pkeyGenIncrement", databaseValues);
			skipValues.put("pkeyGenIncrement", "true");
			if (i > 0)
			{
				Constants.setInt("pkeyGenIncrement", i);
			}
			//nacitame z web.xml kedze v clustri je spolocna DB
			i = Tools.getIntValue(getInitParameter("pkeyGenOffset"), 0);
			skipValues.put("pkeyGenOffset", "true");
			if (i > 0)
			{
				Constants.setInt("pkeyGenOffset", i);
			}

			String linkType = getInitParameter("linkType", databaseValues);
			skipValues.put("linkType", "true");
			if (linkType != null && linkType.equalsIgnoreCase("html"))
			{
				Constants.setInt("linkType", Constants.LINK_TYPE_HTML);
			}
			else if (linkType != null && linkType.equalsIgnoreCase("docid"))
			{
				Constants.setInt("linkType", Constants.LINK_TYPE_DOCID);
			}

			String param = getInitParameter("imagesRootDir", databaseValues);
			skipValues.put("imagesRootDir", "true");
			if (param != null && param.length() > 0)
			{
				if ("/".equals(param))
				{
					param = "";
				}
				Constants.setString("imagesRootDir", param);
			}

			param = getInitParameter("galleryDirName", databaseValues);
			skipValues.put("galleryDirName", "true");
			if (param != null && param.length() > 0)
			{
				if ("/".equals(param))
				{
					param = "";
				}
				Constants.setString("galleryDirName", param);
			}

			param = getInitParameter("filesRootDir", databaseValues);
			skipValues.put("filesRootDir", "true");
			if (param != null && param.length() > 0)
			{
				if ("/".equals(param))
				{
					param = "";
				}
				Constants.setString("filesRootDir", param);
			}

			param = getInitParameter("adminCheckUserGroups", databaseValues);
			skipValues.put("adminCheckUserGroups", "true");
			if (param != null && param.length() > 0)
			{
				Constants.setBoolean("adminCheckUserGroups", param);
			}

			param = getInitParameter("adminRequireSSL", databaseValues);
			skipValues.put("adminRequireSSL", "true");
			if (param != null && param.length() > 0)
			{
				Constants.setBoolean("adminRequireSSL", param);
			}

			param = getInitParameter("exportFlash", databaseValues);
			skipValues.put("exportFlash", "true");
			if (param != null && param.length() > 0)
			{
				Constants.setBoolean("exportFlash", param);
			}

			param = getInitParameter("exportDocsHtml", databaseValues);
			skipValues.put("exportDocsHtml", "true");
			if (param != null && param.length() > 0)
			{
				Constants.setBoolean("exportDocsHtml", param);
			}

			param = getInitParameter("editorEnableXHTML", databaseValues);
			skipValues.put("editorEnableXHTML", "true");
			if (param != null && param.length() > 0)
			{
				Constants.setBoolean("editorEnableXHTML", param);
			}

			//Ak je true, vsetky nazvy konstant sa budu menit na domena-nazovKonstanty (pouzitelne napr. pri multiwebe)
			if("true".equals(getInitParameter("constantsAliasSearch", databaseValues)))
				Constants.setConstantsAliasSearch(true);
			else
				Constants.setConstantsAliasSearch(false);


			String name;
			String value;

			List<LabelValueDetails> names = new ArrayList<>();
			for (Map.Entry<String, String> entry : databaseValues.entrySet())
			{
				name = entry.getKey();
				value = entry.getValue();

				if (skipValues.get(name)==null)
				{
					names.add(new LabelValueDetails(name, value));
				}
				else
				{
					Logger.println(this,"skipping: " + name);
				}
			}

			//Inicializacia WJ podla runtime parametrov (#20549)
			Properties systemProperties = System.getProperties();
			String prefix = "webjet.";
			if(systemProperties!=null)
			{
				for(Map.Entry<Object, Object> prop : systemProperties.entrySet())
				{
					if(prop.getKey()==null)
						continue;
					name = prop.getKey().toString();

					if(prop.getValue()==null)
						value=null;
					else
						value=prop.getValue().toString();

					//pridame iba tie s prefixom webjet. a odstranime prefix
					if(name.startsWith(prefix) && name.length()>prefix.length())
						names.add(new LabelValueDetails(name.substring(prefix.length()), value));
				}
			}

			//Inicializacia WJ podla enviroment parametrov (#20549)
			Map<String, String> envProperties = System.getenv();
			prefix = "webjet_";
			if(envProperties!=null)
			{
				for(Map.Entry<String, String> prop : envProperties.entrySet())
				{
					if(prop.getKey()==null)
						continue;
					name = prop.getKey();

					if(prop.getValue()==null)
						value=null;
					else
						value=prop.getValue();

					//pridame iba tie s prefixom webjet. a odstranime prefix
					if(name.startsWith(prefix) && name.length()>prefix.length())
						names.add(new LabelValueDetails(name.substring(prefix.length()), value));
				}
			}

			//iteruj cez names a nahraj hodnoty z web.xml / databazy
			for (LabelValueDetails lvd : names)
			{
				value = getInitParameter(lvd.getLabel(), databaseValues);
				if (value != null)
				{
					//?? je to INT?
					//Logger.println(this,"INIT SYSPROP/ENV: " + lvd.getLabel()+"="+value);
					if ("true".equals(value) || "false".equals(value))
					{
						boolean boolValue = false;
						if ("true".equals(value)) boolValue = true;
						Constants.setBoolean(lvd.getLabel(), boolValue);
					}
					else
					{
						Constants.setString(lvd.getLabel(), value);
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
	 *  Description of the Method
	 *
	 *@param  request  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	public static boolean verify(HttpServletRequest request)
	{
		if (!valid)
		{
			return (false);
		}
		//false to not use alias because using alias you can use license from another domain
		String serverName = Tools.getServerName(request, false);

		//default allowed domains
		if (serverName.equals("iwcm.interway.sk") || serverName.equals("neweb.interway.sk") || serverName.equals("localhost") ||
			serverName.endsWith(".iway.sk") || serverName.endsWith(".iway.local") || serverName.endsWith(".iwcp.dev") || serverName.endsWith("npp.int-dev.iway"))
		{
			return (true);
		}
		if (domain != null)
		{
			if (domain.length==1)
			{
				if (serverName.endsWith(domain[0])) {
					return (true);
				}
			}
			else
			{
				//Logger.println(this,"verify: domain="+domain+" request="+request.getServerName());
				for (int i = 0; i < domain.length; i++) {
					if (serverName.endsWith(domain[i])) {
						return (true);
					}
				}
			}
		}
		Logger.println(InitServlet.class,"Refusing: " + serverName + " original=" + request.getServerName());
		Logger.println(InitServlet.class,"The license doesn't allow this domain.");
		return (false);
	}

	/**
	 *  Gets the int attribute of the InitServlet object
	 *
	 *@param  name  Description of the Parameter
	 *@return       The int value
	 */
	private int getInt(String name, Map<String, String> databaseValues)
	{
		String value = getInitParameter(name, databaseValues);
		if (value != null)
		{
			try
			{
				int i = Integer.parseInt(value);
				return (i);
			}
			catch (Exception ex)
			{
				Logger.error(this,"ERROR: Init param " + name + " must contain NUMBER");
				return (-1);
			}
		}
		return (-1);
	}

	private String getInitParameter(String name, Map<String, String> databaseValues)
	{
		String value = null;
		String source = null;

		if (databaseValues != null)
		{
			value = databaseValues.get(name);
			if (value != null) source = "db";
		}

		//moznost nastavenia cez system premenne, typicky JAVA_OPTS -Dwebjet.nazov=Hodnota...
		String valueSystem = System.getProperty("webjet."+name);
		if (Tools.isNotEmpty(valueSystem))
		{
			value = valueSystem;
			source = "SystemProperty webjet.";
		}
		else
		{
			valueSystem = System.getProperty("webjet_" + name);
			if (Tools.isNotEmpty(valueSystem))
			{
				value = valueSystem;
				source = "SystemProperty webjet_";
			}
		}

		//moznost nastavenia cez enviroment premennu (ala nova premenna prostredia, cize napr. set webjet_useSMTPServer=false v sh scripte)
		String valueEnv = System.getenv("webjet_"+name);
		if (Tools.isNotEmpty(valueEnv))
		{
			value = valueEnv;
			source = "ENV webjet_";
		}

		//moznost nastavenia custom hodnoty cez web.xml ako webjet-NAZOV_PREMENNEJ
		String valueCustom = getInitParameter("webjet-"+name);
		if (Tools.isNotEmpty(valueCustom))
		{
			value = valueCustom;
			source = "InitParameter webjet-";
		}

		//moznost nastavenia custom hodnoty v <Content elemente server.xml <Parameter name="webjet_XXX" value="vvv" override="true"/>
		String valueContext = getServletContext().getInitParameter("webjet_"+name);
		if (Tools.isNotEmpty(valueContext))
		{
			value = valueContext;
			source = "InitParameter-context webjet_";
		}

		if (value==null)
		{
			value = getInitParameter(name);
			if (value != null)
			{
				Logger.println(this,"INIT (xml): " + name + "=" + value);
			}
		}
		else
		{
			source = " ("+source+")";
			Logger.println(this,"INIT"+source+": " + name + "=" + value);
		}

		value = ConfDB.tryDecrypt(value);

		return(value);
	}

	/**
	 *  Description of the Method
	 *
	 *@param  text    Description of the Parameter
	 *@param  length  Description of the Parameter
	 *@param  right   Description of the Parameter
	 *@return         Description of the Return Value
	 */
	private String align(String text, int length, boolean right)
	{
		if (right)
		{
			if (text.length() < length)
			{
				text = "#####################################" + text;
			}
			if (text.length() > length)
			{
				text = text.substring(text.length() - length);
			}
			return (text);
		}
		else
		{
			if (text.length() < length)
			{
				text = text + "                                              ";
			}
			if (text.length() > length)
			{
				text = text.substring(0, length);
			}
			return (text);
		}
	}

	/**
	 *  Desifrovanie hesla
	 *
	 *@param  password       zasifrovane heslo
	 *@return                heslo
	 *@exception  Exception  Description of the Exception
	 */
	private String decrypt(String password, xjava.security.Cipher alg) throws Exception
	{
		if (password == null)
		{
			return ("");
		}
		if (password.length() < 10)
		{
			return (password);
		}
		if (alg == null) return "";

		byte[] pass = Password.toByteArray(password);
		byte[] dct;
		String b;

		StringBuilder sb = new StringBuilder();
		sb.append("5f456cad56789c6d51b8987b");
		sb.insert(0, "ab256c5a325d6c6a");
		sb.append("654d654c89a987b951cbacdb");

		RawSecretKey key = new RawSecretKey("Rijndael", Hex.fromString(sb.toString()));

		alg.initDecrypt(key);
		dct = alg.crypt(pass);
		b = Hex.toString(dct);

		String decoded = new String(Password.toByteArray(b)).trim();
		return (decoded);
	}

	public static boolean restart()
	{
		boolean ret = false;

		touch("/WEB-INF/lib/webjet-8.7-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-8.8-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-8.9-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2021.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2022.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2023.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2024.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2025.0-SNAPSHOT.jar");

		touch("/WEB-INF/lib/wjstripes-1.6.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/wjcron4j-2.2.5-SNAPSHOT.jar");
		touch("/WEB-INF/lib/wjdisplaytag-1.2-SNAPSHOT.jar");

		File f = new File(Tools.getRealPath("/WEB-INF/classes/sk/iway/iwcm/InitServlet.class"));
		if (f.exists())
		{
			ret = true;
		}

		f = new File(Tools.getRealPath("/WEB-INF/web.xml"));
		if (f.exists())
		{
			ret = true;
		}

		f = new File(Tools.getRealPath("/WEB-INF/classes/sk/updater/ResponseServlet.class"));
		if (f.exists())
		{
			ret = true;
		}

		f = new File(Tools.getRealPath("/WEB-INF/classes/sk/updater/InitServlet.class"));
		if (f.exists())
		{
			ret = true;
		}

		f = new File(Tools.getRealPath("/WEB-INF/classes/sk/iway/iwcm/setup/SetupSaveAction.class"));
		if (f.exists())
		{
			ret = true;
		}

		f = new File(Tools.getRealPath("/WEB-INF/classes/sk/iway/iwcm/setup/SetupAction.class"));
		if (f.exists())
		{
			ret = true;
		}

		Logger.println(InitServlet.class,"RESTART request ret="+ret);
		Logger.error(InitServlet.class,"RESTART request ret="+ret);

		return(ret);
	}

	private static boolean touch(String url) {
		boolean ret = false;
		File f = new File(Tools.getRealPath(url));
		if (f.exists())
		{
			Logger.println(InitServlet.class,"RESTART request InitServlet " + f.getAbsolutePath());
			f.setLastModified(Tools.getNow()); //NOSONAR
			ret = true;
		}
		return ret;
	}

	public static int getLicenseId()
	{
		return(licenseId);
	}

	public static String getActualVersionLong()
	{
		StringBuilder ver = new StringBuilder(actualVersion);

		String wjVersion = Constants.getString("wjVersion");
		if ("E".equals(wjVersion) && "zaskis".equals(Constants.getInstallName())) ver.append(" Unlimited");
		else if ("E".equals(wjVersion) && "zsr".equals(Constants.getInstallName())) ver.append(" Unlimited");
		else if ("E".equals(wjVersion)) ver.append(" Enterprise");
		else if ("P".equals(wjVersion)) ver.append(" Professional");
		else if ("D".equals(wjVersion)) ver.append(" MSG");
		else if ("I".equals(wjVersion)) ver.append(" NET");
		else if ("C".equals(wjVersion)) ver.append(" Cestovka");
		else if ("M".equals(wjVersion) && "cloud".equals(Constants.getInstallName())) ver.append(" Cloud");
		else if ("M".equals(wjVersion)) ver.append(" Multiweb");
		else if ("O".equals(wjVersion)) ver.append(" Community (Open Source)");
		else ver.append(" Basic");

		String nodeName = Constants.getString("clusterMyNodeName");
		if (Tools.isNotEmpty(nodeName)) ver.append(" (").append(nodeName).append(" / ").append(Constants.getString("clusterMyNodeType")).append(')');

		String tomcat = System.getProperty("iway.tomcat");
		if (Tools.isNotEmpty(tomcat)) ver.append(" server: ").append(tomcat);

		return(ver.toString());
	}

    /**
     * Vrati suffix WebJET brandu pre logo, pre Intranet vrati net, pre dmail vrati msg inak cms
     * @return
     */
	public static String getBrandSuffix()
    {
        String wjVersion = Constants.getString("wjVersion");

        if ("I".equals(wjVersion)) return "net";
        else if ("D".equals(wjVersion)) return "msg";

        return "cms";
    }

	public static boolean isWebjetInitialized()
	{
		return webjetInitialized;
	}

	/**
	 * Returns true if WebJET is configured (has records in conf table)
	 * @return
	 */
	public static boolean isWebjetConfigured()
	{
		return webjetConfigured;
	}

	public static void setContextDbName(String name)
	{
		contextDbName = name;
	}

	public static String getContextDbName()
	{
		return contextDbName;
	}

	public static boolean isTypeCloud()
	{
		return "M".equals(Constants.getString("wjVersion"));
	}

	private static void setActualVersion(String version) {
		InitServlet.actualVersion = version;
	}

	private static void setLicenseId(int id) {
		InitServlet.licenseId = id;
	}

	private static void setDomain(String[] domain) {
		InitServlet.domain = domain;
	}

	public static boolean isValid() {
		return valid;
	}

	private static void setValid(boolean valid) {
		InitServlet.valid = valid;
	}

	private static void setWebjetInitialized(boolean webjetInitialized) {
		InitServlet.webjetInitialized = webjetInitialized;
	}

	public static Date getServerStartDatetime() {
		return SERVER_START_DATETIME;
	}

	private void deleteOldStrutsConfig() {
		try {
			String virtualPath = "/WEB-INF/struts-config.xml";
			if (Constants.getBoolean("enableJspJarPackaging") && JarPackaging.isJarPackaging(virtualPath)==false) {
				//struts-config.xml is on file system, delete it and use one from jar file
				File f = new File(Tools.getRealPath(virtualPath));
				if (f.exists()) f.delete();
			}
		} catch (Exception ex) {
			Logger.error(InitServlet.class, ex);
		}
	}

	/**
	 * Trim hostname to 16 chars from start, or if clusterHostnameTrimFromEnd=true from end
	 * On k8s hostnames have random values on end of the name (not from start like standard domains)
	 * @param hostname
	 * @return
	 */
	private String trimHostname(String hostname) {
		if (Tools.isEmpty(hostname)) return hostname;

		if (Constants.getBoolean("clusterHostnameTrimFromEnd") && hostname.length()>16) {
			hostname = hostname.substring(hostname.length()-16);
		} else {
			hostname = DB.prepareString(hostname, 16);
		}

		return hostname;
	}

	public static String getActualVersion() {
		return actualVersion;
	}
}
