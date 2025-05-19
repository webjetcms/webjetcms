package sk.iway.iwcm.setup;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Vector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.ui.Model;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.XmlUtils;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.UpdateDatabase;

/**
 * SetupAction.java - priprava formularu pre nastavenie systemu
 */
@SuppressWarnings("java:S106")
public class SetupActionsService {

	private static final String FORWARD = "/admin/setup/setup";
	private static final String SAVED = "/admin/setup/setup_saved";

	private SetupActionsService() {
		// Private constructor to hide the implicit public one.
	}

	private static boolean isHostAllowed(String serverName) {
		if ("iwcm.interway.sk".equals(serverName) || "localhost".equals(serverName)) return true;
		return false;
	}

	private static String readPoolman() {
		// nacitaj DB konfiguraciu (ak existuje)
		String systemIwcmDBName = InitServlet.getContextDbName();
		if (Tools.isEmpty(systemIwcmDBName)) systemIwcmDBName = System.getProperty("webjetDbname");
		System.out.println("systemIwcmDBName="+systemIwcmDBName);

		//aj cez <Context ... <Parameter name="webjetDbname" value="/poolman-local.xml" override="true"/> je mozne zadat cestu
		String customPoolmanPath = null;
		if (Tools.isNotEmpty(systemIwcmDBName) && systemIwcmDBName.endsWith(".xml")) {
			customPoolmanPath = systemIwcmDBName;
			systemIwcmDBName = "iwcm";
		}
		String data = DBPool.readFileContent(customPoolmanPath);

		return data;
	}

	public static String setupAction(Model model, HttpServletRequest request, HttpServletResponse response, String lng) throws IOException {

		if (InitServlet.isWebjetInitialized() || isHostAllowed(request.getServerName()) == false) {
			return null;
		}

		String data = readPoolman();

		if ((data != null && data.length()>30) && isHostAllowed(request.getServerName()) == false) {
			System.out.println("poolman allready exists");
			return null;
		}

		SetupFormBean sForm = new SetupFormBean();

		if (data != null && Tools.isNotEmpty(data)) {
			try {
				DocumentBuilderFactory b = DocumentBuilderFactory.newInstance();
				// to be compliant, completely disable DOCTYPE declaration:
				b.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				DocumentBuilder dc = b.newDocumentBuilder();
				ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
				Document doc = dc.parse(input);
				if (doc != null) {
					Node n = XmlUtils.getFirstChild(doc.getDocumentElement(), "datasource");
					if (n != null) {
						sForm.setDbUsername(XmlUtils.getFirstChildValue(n, "username"));
						sForm.setDbPassword(XmlUtils.getFirstChildValue(n, "password"));

						try {
							String driver = XmlUtils.getFirstChildValue(n, "driver");
							if (driver != null) {
								if (driver.contains("Oracle"))
									sForm.setDbDriver("oracle.jdbc.driver.OracleDriver");
								else if (driver.contains("jtds"))
									sForm.setDbDriver("net.sourceforge.jtds.jdbc.Driver");
								else if (driver.contains("postgresql"))
									sForm.setDbDriver("org.postgresql.Driver");
							}
							String url = XmlUtils.getFirstChildValue(n, "url");
							if (url != null) {
								String serverNameDelimiter = "://";
								int iServerName = url.indexOf(serverNameDelimiter);
								if (iServerName == -1) {
									serverNameDelimiter = "@";
									iServerName = url.indexOf(serverNameDelimiter); // oracle verzia
								}
								if (iServerName > 0) {
									int iSchemaName = url.indexOf("/", iServerName + serverNameDelimiter.length());
									if (iSchemaName > iServerName) {
										String serverName = url.substring(iServerName + serverNameDelimiter.length(),
												iSchemaName);
										int i = serverName.indexOf(":");
										if (i > 0) {
											sForm.setDbDomain(serverName.substring(0, i));
											sForm.setDbPort(serverName.substring(i + 1));
										} else {
											sForm.setDbDomain(serverName);
										}

										String schemaName = url.substring(iSchemaName + 1);
										i = schemaName.indexOf("?");
										if (i == -1)
											i = schemaName.indexOf(";");

										if (i > 0) {
											sForm.setDbName(schemaName.substring(0, i));
											sForm.setDbParameters(schemaName.substring(i + 1));
										} else {
											sForm.setDbName(schemaName);
										}

										String installName = sForm.getDbName();
										i = installName.indexOf("_web");
										if (i > 0)
											installName = installName.substring(0, i);
										sForm.setConf_installName(installName);
									}

									if (driver != null && driver.contains("Oracle")) {
										sForm.setDbParameters(sForm.getDbName());
										sForm.setDbName("");
									}

								}
							}
						} catch (Exception ex) {
							ex.printStackTrace(System.err);
						}
					}
				}
			} catch (Exception ex) {
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		data = FileTools.readFileContent("/WEB-INF/web-runtime.xml");
		if (Tools.isNotEmpty(data)) {
			try {
				DocumentBuilderFactory b = DocumentBuilderFactory.newInstance();
				// to be compliant, completely disable DOCTYPE declaration:
				b.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				DocumentBuilder dc = b.newDocumentBuilder();
				ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
				Document doc = dc.parse(input);
				if (doc != null) {
					Vector<Node> servletNodes = XmlUtils.getChildNodes(doc.getDocumentElement(), "servlet");
					if (servletNodes != null && servletNodes.size() > 0) {
						for (Node servletNode : servletNodes) {
							Vector<Node> initParamNodes = XmlUtils.getChildNodes(servletNode, "init-param");
							if (initParamNodes != null && initParamNodes.size() > 0) {
								for (Node initParamNode : initParamNodes) {
									String paramName = XmlUtils.getFirstChildValue(initParamNode, "param-name");
									String paramValue = XmlUtils.getFirstChildValue(initParamNode, "param-value");

									if (Tools.isEmpty(paramValue))
										continue;

									if ("license".equals(paramName)) {
										sForm.setConf_license(paramValue);
									} else if ("smtpServer".equals(paramName)) {
										sForm.setConf_smtpServer(paramValue);
									}
								}
							}
						}
					}
				}
			} catch (Exception ex) {
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		if (Tools.isEmpty(sForm.getConf_installName())) {
			// zdetekuj install name
			String path = Constants.getServletContext().getRealPath("/");

			String installName = "";
			if (path != null) {
				File f = new File(path);
				installName = f.getName();
				installName = Tools.replace(installName, ".sk", "");
				installName = Tools.replace(installName, ".cz", "");
				installName = Tools.replace(installName, ".com", "");
				installName = Tools.replace(installName, "www.", "");
			}
			sForm.setConf_installName(installName);
			sForm.setDbName(installName + "_web");
		}

		if(Tools.isNotEmpty(lng)) {
            PageLng.setUserLng(request, response, lng);
            request.getSession().setAttribute(Prop.SESSION_I18N_PROP_LNG, lng);
			sForm.setPageLngIndicator(lng);
    	} else {
			String oldLng = (String) request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
			if(Tools.isNotEmpty(oldLng)) sForm.setPageLngIndicator(oldLng);
		}

		setModel(model, sForm, false, false);

		return FORWARD;
	}

	public static String setupSaveAction(SetupFormBean setupForm, Model model, HttpServletRequest request, HttpServletResponse response) {
		if (InitServlet.isWebjetInitialized() || isHostAllowed(request.getServerName()) == false){
			System.out.println("WebJET is allready initialized");
			return SAVED;
		}

		String data = readPoolman();
		if ((data != null && data.length() > 30) && isHostAllowed(request.getServerName()) == false) {
			System.out.println("poolman allready exists");
			return null;
		}

		//uloz DB konfiguraciu
		if (data == null || data.length() < 30)
			savePoolman(setupForm);

		//vytvor/napln databazu
		boolean dbConnectOK = false;
		String connErrMsg = "";
		Connection con = null;
		try {
			System.out.println("Checking database connection: ");

			try {
				Class.forName(setupForm.getDbDriver()).getDeclaredConstructor().newInstance();
			} catch (Exception ex) {
				connErrMsg += "Could Not Find the database Driver: " + setupForm.getDbDriver();
			}

			String userName = setupForm.getDbUsername();
			String password = setupForm.getDbPassword();
			if (Tools.isEmpty(userName)) userName = null;
			if (Tools.isEmpty(password)) password = null;
			con = DriverManager.getConnection(getDBURLString(setupForm), userName, password);
			con.close();
			dbConnectOK = true;
		}
		catch (Exception ex) {
			try {
				if (con != null) con.close();
			} catch (Exception e) {}

			String msg = ex.getMessage();

			if (msg.indexOf("Unknown database ") != -1) {
				//DB nie je vytvorena, pokus sa vytvorit (ak mas prava)

				String origDBName = setupForm.getDbName();
				setupForm.setDbName("mysql");

				try {
					if (setupForm.isDbUseSuperuser()) {
						con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbSuperuserUsername(), setupForm.getDbSuperuserPassword());
					} else {
						if (con != null) con.close();
						con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbUsername(), setupForm.getDbPassword());
					}

					PreparedStatement ps = con.prepareStatement("CREATE DATABASE " + origDBName);
					ps.execute();
					ps.close();

					con.close();

					setupForm.setDbName(origDBName);

					con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbUsername(), setupForm.getDbPassword());
					con.close();

					dbConnectOK = true;
				} catch (Exception e) {
					connErrMsg += msg;
					sk.iway.iwcm.Logger.error(e);
				}
				setupForm.setDbName(origDBName);
			} else if (msg.indexOf("Cannot open database requested") != -1) {
				//DB nie je vytvorena, pokus sa vytvorit (ak mas prava)

				String origDBName = setupForm.getDbName();
				setupForm.setDbName("master");

				try {
					if (setupForm.isDbUseSuperuser()) {
						if (con != null) con.close();
						con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbSuperuserUsername(), setupForm.getDbSuperuserPassword());
					} else {
						if (con != null) con.close();
						con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbUsername(), setupForm.getDbPassword());
					}

					PreparedStatement ps = con.prepareStatement("CREATE DATABASE " + origDBName);
					ps.execute();
					ps.close();

					con.close();

					setupForm.setDbName(origDBName);

					con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbUsername(), setupForm.getDbPassword());
					con.close();

					dbConnectOK = true;
				} catch (Exception e) {
					connErrMsg += msg;
					sk.iway.iwcm.Logger.error(e);
				}
				setupForm.setDbName(origDBName);
			} else {
				connErrMsg += msg;
				System.out.println("------ DB FAIL ------\n");
				ex.printStackTrace(System.err);
			}
		}

		System.out.println("dbConnectOK="+dbConnectOK+" driver="+setupForm.getDbDriver());

		if (dbConnectOK) {
			//resetni DBPool
			DBPool.getInstance(true);

			String dbCreateErrMsg = null;
			if ("org.mariadb.jdbc.Driver".equals(setupForm.getDbDriver())) {
				//napln databazu
				dbCreateErrMsg = UpdateDatabase.fillEmptyDatabaseMySQL();
			} else if ("oracle.jdbc.driver.OracleDriver".equals(setupForm.getDbDriver())) {
				//napln databazu
				dbCreateErrMsg = UpdateDatabase.fillEmptyDatabaseOracle();
			} else if ("org.postgresql.Driver".equals(setupForm.getDbDriver())) {
				//napln databazu
				String schema = "webjet_cms";
				if (Tools.isNotEmpty(setupForm.getDbParameters())) {
					String[] params = Tools.getTokens(setupForm.getDbParameters(), "&");
					for (String param : params) {
						if (param.startsWith("currentSchema=")) {
							schema = param.substring(14);
							break;
						}
					}
				}
				dbCreateErrMsg = UpdateDatabase.fillEmptyDatabasePgSQL(schema);
			} else {
				//	napln databazu
				dbCreateErrMsg = UpdateDatabase.fillEmptyDatabaseMSSQL();
			}

			if (Tools.isNotEmpty(dbCreateErrMsg)) {
				setModelWithErr(model, setupForm, false, null, dbCreateErrMsg);
				return FORWARD;
			}

			//uloz konfiguracne hodnoty
			Connection db_conn = null;
			try {
				db_conn = DBPool.getConnection();

				Enumeration<String> e = request.getParameterNames();
				String name;
				String value;
				while (e.hasMoreElements()) {
					name = e.nextElement();
					if (name.startsWith("conf_")) {
						value = request.getParameter(name);
						name = name.substring(5);
						if (Tools.isNotEmpty(value))
							saveConf(name, value, db_conn);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			} finally {
				try {
					if (db_conn != null) db_conn.close();
				} catch (Exception ex2) {}
			}

			//nastav character coding vo web.xml a skopiruj to z runtime
			String webXML = FileTools.readFileContent("/WEB-INF/web-runtime.xml");

			if (webXML != null && webXML.length() > 100 && (data == null || data.length() < 100)) {
				webXML = Tools.replace(webXML, "<param-value>windows-1250</param-value>", "<param-value>"+setupForm.getEncoding()+"</param-value>");
				if ("net.sourceforge.jtds.jdbc.Driver".equals(setupForm.getDbDriver()))
					webXML = Tools.replace(webXML, "<param-value>TOMCAT_MYSQL</param-value>", "<param-value>RESIN_MSSQL</param-value>");

				//	prekopiruj web.xml
				FileTools.saveFileContent("/WEB-INF/web.xml", webXML);
			}

			//restartni server
			InitServlet.restart();

			//setModelForSave(model, setupForm);

			setModel(model, null, true, true);

			return SAVED;
		} else {
			setModelWithErr(model, setupForm, true, connErrMsg, null);
			return FORWARD;
		}
	}

	/**
	 * Ulozenie suboru poolman.xml
	 * @param sForm
	 */
	private static void savePoolman(SetupFormBean sForm)
	{
		StringBuilder poolman = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n")
				.append("\r\n")
				.append("<poolman>\r\n")
				.append("  <datasource>\r\n")
				.append("      <dbname>iwcm</dbname>\r\n")
				.append("      <driver>").append(sForm.getDbDriver()).append("</driver>\r\n");

		poolman.append("      <url>").append(getDBURLString(sForm)).append("</url>\r\n");

		poolman.append("\r\n")
				.append("      <username>"+sForm.getDbUsername()+"</username>\r\n")
				.append("      <password>"+sForm.getDbPassword()+"</password>\r\n")
				.append("\r\n")
				.append("      <initialConnections>2</initialConnections>\r\n")
				.append("      <minimumSize>0</minimumSize>\r\n")
				.append("      <maximumSize>60</maximumSize>\r\n")
				.append("\r\n")
				.append("      <connectionTimeout>600</connectionTimeout>\r\n")
				.append("      <userTimeout>120</userTimeout>\r\n")
				.append("\r\n")
				.append("  </datasource>\r\n")
				.append("\r\n")
				.append("</poolman>\r\n");

		FileTools.saveFileContent("/WEB-INF/classes/poolman.xml", poolman.toString());
	}

	/**
	 * Pripravi URL string pre connection (bez username a password)
	 * @param sForm
	 * @return
	 */
	private static String getDBURLString(SetupFormBean sForm) {
		String url = null;
		String port = "";
		if (Tools.isNotEmpty(sForm.getDbPort()))
			port = ":"+sForm.getDbPort();

		if ("com.mysql.jdbc.Driver".equals(sForm.getDbDriver())) {
			url = "jdbc:mysql://"+sForm.getDbDomain()+port+"/"+sForm.getDbName()+"?"+sForm.getDbParameters();
		} else if ("org.mariadb.jdbc.Driver".equals(sForm.getDbDriver())) {
			url = "jdbc:mariadb://"+sForm.getDbDomain()+port+"/"+sForm.getDbName()+"?"+sForm.getDbParameters();
		} else if ("net.sourceforge.jtds.jdbc.Driver".equals(sForm.getDbDriver())) {
			url = "jdbc:jtds:sqlserver://"+sForm.getDbDomain()+port+"/"+sForm.getDbName()+";"+sForm.getDbParameters();
		} else if ("oracle.jdbc.driver.OracleDriver".equals(sForm.getDbDriver())) {
			url = "jdbc:oracle:thin:@"+sForm.getDbDomain()+port;
			if (Tools.isNotEmpty(sForm.getDbName()))
				url += ":"+sForm.getDbName();

			if (Tools.isNotEmpty(sForm.getDbParameters()))
				url += "/"+sForm.getDbParameters();
		} else if ("org.postgresql.Driver".equals(sForm.getDbDriver())) {
			String params = "currentSchema=webjet_cms";
			if (Tools.isNotEmpty(sForm.getDbParameters())) params = sForm.getDbParameters();
			url = "jdbc:postgresql://"+sForm.getDbDomain()+port+"/"+sForm.getDbName()+"?"+params;
		}
		return(url);
	}

	/**
	 * Ulozenie hodnoty do konfiguracnej tabulky
	 * @param name
	 * @param value
	 * @param db_conn
	 * @throws SQLException
	 */
	private static void saveConf(String name, String value, Connection db_conn) throws SQLException {
		PreparedStatement ps = db_conn.prepareStatement("DELETE FROM "+ConfDB.CONF_TABLE_NAME+" WHERE name=?");
		ps.setString(1, name);
		ps.execute();
		ps.close();

		ps = db_conn.prepareStatement("INSERT INTO "+ConfDB.CONF_TABLE_NAME+" (name, value) VALUES (?, ?)");
		ps.setString(1, name);
		ps.setString(2, value);
		ps.execute();
		ps.close();
	}

	private static void setModel(Model model, SetupFormBean setupForm, Boolean disableLanguageSelect, boolean isSave) {
		//Informing FE what key to use, when creating page
		if(isSave)
			// page /admin/setup/setup
			model.addAttribute("isSetupSave", true);
		else
		// page /admin/setup/setup_saved
			model.addAttribute("isSetup", true);

		//Object that will be used in filling setup form
		if(setupForm != null) model.addAttribute("setupForm", setupForm);
		//Style file content for page
		model.addAttribute("cmpCss", FileTools.readFileContent("/components/cmp.css"));
		//If true, user will not see select to change language
		model.addAttribute("disableLng", disableLanguageSelect);
	}

	private static void setModelWithErr(Model model, SetupFormBean setupForm, Boolean conErr, String conErrMsg, String createErrMsg) {
		setModel(model, setupForm, true, false);

		//Will show con error msg in page
		model.addAttribute("dbConnFail", conErr);
		//This is content of con err msg
		model.addAttribute("dbErrMsg", conErrMsg);
		//Separe crate rr message, will be shown if != null
		model.addAttribute("dbCreateErrMsg", createErrMsg);
	}
}