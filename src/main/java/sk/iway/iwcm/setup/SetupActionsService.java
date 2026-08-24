package sk.iway.iwcm.setup;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.text.StringEscapeUtils;
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
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.UpdateDatabase;

/**
 * SetupAction.java - priprava formularu pre nastavenie systemu
 */
@SuppressWarnings("java:S106")
public class SetupActionsService {

	private static final String FORWARD = "/admin/setup/setup";
	private static final String SAVED = "/admin/setup/setup_saved";
	private static final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";
	private static final String MSSQL_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
	private static final String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";
	private static final String ORACLE_DRIVER_NEW = "oracle.jdbc.OracleDriver";
	private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
	private static final String POOLMAN_PATH = "/WEB-INF/classes/poolman.xml";
	private static final Pattern POSTGRESQL_SCHEMA_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

	private SetupActionsService() {
		// Private constructor to hide the implicit public one.
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

		if (InitServlet.isWebjetInitialized()) {
			return null;
		}

		String data = readPoolman();

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
											sForm.setDbParameters(getSafeDbParameters(driver, schemaName.substring(i + 1)));
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

		String language = request.getParameter("language");
		if (language != null && language.length() == 2) {
			sForm.setConf_defaultLanguage(language);
		}

		setModel(model, sForm, false, false);

		return FORWARD;
	}

	public static String setupSaveAction(SetupFormBean setupForm, Model model, HttpServletRequest request, HttpServletResponse response) {
		if (InitServlet.isWebjetInitialized()){
			System.out.println("WebJET is allready initialized");
			return SAVED;
		}

		String data = readPoolman();

		boolean createPoolman = data == null || data.length() < 30;
		String postgresqlSchema = null;
		if (POSTGRESQL_DRIVER.equals(setupForm.getDbDriver())) {
			try {
				postgresqlSchema = getPostgresqlSchema(setupForm.getDbParameters());
			} catch (IllegalArgumentException ex) {
				setModelWithErr(model, setupForm, false, null, ex.getMessage());
				return FORWARD;
			}
		}

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
			String password = getEnvPassword(setupForm.getDbPassword());
			if (Tools.isEmpty(userName)) userName = null;
			if (Tools.isEmpty(password)) password = null;

			con = DriverManager.getConnection(getDBURLString(setupForm), userName, password);
			validateDatabaseCharacterSet(con, setupForm.getDbDriver());
			con.close();
			dbConnectOK = true;
		}
		catch (Exception ex) {
			try {
				if (con != null) con.close();
			} catch (Exception e) {}

			String msg = ex.getMessage();

			if (isDatabaseMissing(ex, setupForm.getDbDriver())) {
				//DB nie je vytvorena, pokus sa vytvorit (ak mas prava)

				String origDBName = setupForm.getDbName();
				setupForm.setDbName("mysql");
				if (MSSQL_DRIVER.equals(setupForm.getDbDriver()))
					setupForm.setDbName("master");
				else if (POSTGRESQL_DRIVER.equals(setupForm.getDbDriver()))
					setupForm.setDbName("postgres");

				try {
					if (setupForm.isDbUseSuperuser()) {
						con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbSuperuserUsername(), getEnvPassword(setupForm.getDbSuperuserPassword()));
					} else {
						if (con != null) con.close();
						con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbUsername(), getEnvPassword(setupForm.getDbPassword()));
					}

					String createDatabaseSql = getCreateDatabaseSql(setupForm.getDbDriver(), origDBName);

					try (PreparedStatement ps = con.prepareStatement(createDatabaseSql)) {
						ps.execute();
					}

					con.close();

					setupForm.setDbName(origDBName);

					con = DriverManager.getConnection(getDBURLString(setupForm), setupForm.getDbUsername(), getEnvPassword(setupForm.getDbPassword()));
					validateDatabaseCharacterSet(con, setupForm.getDbDriver());
					con.close();

					dbConnectOK = true;
				} catch (Exception e) {
					connErrMsg += Tools.isNotEmpty(e.getMessage()) ? e.getMessage() : msg;
					sk.iway.iwcm.Logger.error(e);
					try {
						if (con != null) con.close();
					} catch (Exception closeException) {
						sk.iway.iwcm.Logger.error(closeException);
					}
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
			// Persist only connection details that have already been validated.
			boolean generatedPoolman = false;
			if (createPoolman) {
				boolean poolmanExisted = IwcmFile.fromVirtualPath(POOLMAN_PATH).exists();
				if (savePoolman(setupForm) == false) {
					String error = rollbackGeneratedPoolman(poolmanExisted == false,
						"Unable to save poolman.xml. Check filesystem permissions and retry setup.");
					setModelWithErr(model, setupForm, false, null, error);
					return FORWARD;
				}
				generatedPoolman = poolmanExisted == false;
			}

			String dbCreateErrMsg = null;
			try {
				//resetni DBPool
				DBPool.getInstance(true);

				if (MARIADB_DRIVER.equals(setupForm.getDbDriver())) {
					//napln databazu
					dbCreateErrMsg = UpdateDatabase.fillEmptyDatabaseMySQL();
				} else if (isOracleDriver(setupForm.getDbDriver())) {
					//napln databazu
					dbCreateErrMsg = UpdateDatabase.fillEmptyDatabaseOracle();
				} else if (POSTGRESQL_DRIVER.equals(setupForm.getDbDriver())) {
					//napln databazu
					dbCreateErrMsg = UpdateDatabase.fillEmptyDatabasePgSQL(postgresqlSchema);
				} else {
					//	napln databazu
					dbCreateErrMsg = UpdateDatabase.fillEmptyDatabaseMSSQL();
				}
			} catch (Exception ex) {
				sk.iway.iwcm.Logger.error(ex);
				dbCreateErrMsg = "Unable to initialize the database. Check the server log and retry setup.";
			}

			if (Tools.isNotEmpty(dbCreateErrMsg)) {
				dbCreateErrMsg = rollbackGeneratedPoolman(generatedPoolman, dbCreateErrMsg);
				setModelWithErr(model, setupForm, false, null, dbCreateErrMsg);
				return FORWARD;
			}

			String configurationError = saveConfigurationValues(request);
			if (Tools.isNotEmpty(configurationError)) {
				configurationError = rollbackGeneratedPoolman(generatedPoolman, configurationError);
				setModelWithErr(model, setupForm, false, null, configurationError);
				return FORWARD;
			}

			//setModelForSave(model, setupForm);

			setModel(model, null, true, true);
			SetupCompletionState.markCompleted(request);
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}

			return SAVED;
		} else {
			setModelWithErr(model, setupForm, true, connErrMsg, null);
			return FORWARD;
		}
	}

	private static boolean isDatabaseMissing(Exception exception, String dbDriver) {
		if (isOracleDriver(dbDriver)) return false;
		if (exception instanceof SQLException sqlException && "3D000".equals(sqlException.getSQLState())) return true;

		String message = exception.getMessage();
		if (Tools.isEmpty(message)) return false;
		if (MARIADB_DRIVER.equals(dbDriver)) return message.contains("Unknown database");
		if (MSSQL_DRIVER.equals(dbDriver)) return message.contains("Cannot open database");
		return POSTGRESQL_DRIVER.equals(dbDriver) && message.contains("does not exist");
	}

	static String getCreateDatabaseSql(String dbDriver, String dbName) {
		String quotedDbName = quoteDatabaseName(dbDriver, dbName);
		if (POSTGRESQL_DRIVER.equals(dbDriver)) {
			return "CREATE DATABASE " + quotedDbName + " WITH TEMPLATE template0 ENCODING 'UTF8'";
		}
		if (MARIADB_DRIVER.equals(dbDriver)) {
			return "CREATE DATABASE " + quotedDbName
				+ " DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci";
		}
		if (MSSQL_DRIVER.equals(dbDriver)) {
			// WebJET uses NVARCHAR/NTEXT on MS SQL. The _SC collation adds correct
			// supplementary-character handling and remains compatible with SQL Server 2012+.
			return "CREATE DATABASE " + quotedDbName + " COLLATE Latin1_General_CI_AI";
		}
		throw new IllegalArgumentException("Automatic database creation is not supported for driver: " + dbDriver);
	}

	private static String quoteDatabaseName(String dbDriver, String dbName) {
		if (Tools.isEmpty(dbName) || dbName.indexOf('\0') >= 0) {
			throw new IllegalArgumentException("Invalid database name");
		}
		if (MARIADB_DRIVER.equals(dbDriver)) {
			return "`" + dbName.replace("`", "``") + "`";
		}
		if (MSSQL_DRIVER.equals(dbDriver)) {
			return "[" + dbName.replace("]", "]]") + "]";
		}
		return "\"" + dbName.replace("\"", "\"\"") + "\"";
	}

	static void validateDatabaseCharacterSet(Connection connection, String dbDriver) throws SQLException {
		if (isOracleDriver(dbDriver) == false) return;

		String characterSet = null;
		String nationalCharacterSet = null;
		String sql = "SELECT parameter, value FROM nls_database_parameters "
			+ "WHERE parameter IN ('NLS_CHARACTERSET', 'NLS_NCHAR_CHARACTERSET')";
		try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				String parameter = rs.getString("parameter");
				if ("NLS_CHARACTERSET".equals(parameter)) characterSet = rs.getString("value");
				else if ("NLS_NCHAR_CHARACTERSET".equals(parameter)) nationalCharacterSet = rs.getString("value");
			}
		}

		if ("AL32UTF8".equalsIgnoreCase(characterSet) == false) {
			throw new SQLException("Oracle database must use NLS_CHARACTERSET=AL32UTF8, current value is " + characterSet);
		}
		if ("AL16UTF16".equalsIgnoreCase(nationalCharacterSet) == false) {
			throw new SQLException("Oracle database must use NLS_NCHAR_CHARACTERSET=AL16UTF16, current value is "
				+ nationalCharacterSet);
		}
	}

	private static boolean isOracleDriver(String dbDriver) {
		return ORACLE_DRIVER.equals(dbDriver) || ORACLE_DRIVER_NEW.equals(dbDriver);
	}

	static String getSafeDbParameters(String dbDriver, String parameters) {
		String allowedParameter;
		if (POSTGRESQL_DRIVER.equals(dbDriver)) allowedParameter = "currentSchema";
		else if (MSSQL_DRIVER.equals(dbDriver)) allowedParameter = "encoding";
		else return "";

		if (Tools.isEmpty(parameters)) return "";
		for (String parameter : parameters.split("[&;]")) {
			int delimiter = parameter.indexOf('=');
			if (delimiter < 1) continue;
			String name = parameter.substring(0, delimiter).trim();
			String value = parameter.substring(delimiter + 1).trim();
			if (allowedParameter.equalsIgnoreCase(name) && Tools.isNotEmpty(value)) {
				return allowedParameter + "=" + value.replace("\r", "").replace("\n", "");
			}
		}
		return "";
	}

	static String getPostgresqlSchema(String parameters) {
		String schema = "webjet_cms";
		if (Tools.isNotEmpty(parameters)) {
			for (String parameter : parameters.split("[&;]")) {
				int delimiter = parameter.indexOf('=');
				if (delimiter < 0 || "currentSchema".equalsIgnoreCase(parameter.substring(0, delimiter).trim()) == false) {
					continue;
				}
				schema = parameter.substring(delimiter + 1).trim();
				break;
			}
		}
		if (POSTGRESQL_SCHEMA_PATTERN.matcher(schema).matches() == false) {
			throw new IllegalArgumentException(
				"PostgreSQL currentSchema must contain only letters, digits, and underscores and must not start with a digit."
			);
		}
		return schema;
	}

	/**
	 * Ulozenie suboru poolman.xml
	 * @param sForm
	 */
	static boolean savePoolman(SetupFormBean sForm)
	{
		StringBuilder poolman = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n")
				.append("\r\n")
				.append("<poolman>\r\n")
				.append("  <datasource>\r\n")
				.append("      <dbname>iwcm</dbname>\r\n")
				.append("      <driver>").append(escapeXml(sForm.getDbDriver())).append("</driver>\r\n");

		poolman.append("      <url>").append(escapeXml(getDBURLString(sForm))).append("</url>\r\n");

		poolman.append("\r\n")
				.append("      <username>").append(escapeXml(sForm.getDbUsername())).append("</username>\r\n")
				.append("      <password>").append(escapeXml(sForm.getDbPassword())).append("</password>\r\n")
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

		return FileTools.saveFileContent(POOLMAN_PATH, poolman.toString());
	}

	static String rollbackGeneratedPoolman(boolean generatedPoolman, String errorMessage) {
		if (generatedPoolman == false) return errorMessage;
		try {
			IwcmFile poolmanFile = IwcmFile.fromVirtualPath(POOLMAN_PATH);
			if (poolmanFile.exists() == false || poolmanFile.delete()) return errorMessage;
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}
		return errorMessage + " The generated poolman.xml could not be removed; remove it manually before retrying.";
	}

	private static String escapeXml(String value) {
		return StringEscapeUtils.escapeXml11(value == null ? "" : value);
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
	private static String saveConfigurationValues(HttpServletRequest request) {
		try (Connection connection = DBPool.getConnection()) {
			if (connection == null) throw new SQLException("DBPool returned no connection");
			saveConfigurationValues(request, connection);
			return null;
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			return "Unable to save WebJET configuration. Check the server log and retry setup.";
		}
	}

	static void saveConfigurationValues(HttpServletRequest request, Connection connection) throws SQLException {
		boolean originalAutoCommit = connection.getAutoCommit();
		Throwable failure = null;
		try {
			if (originalAutoCommit) connection.setAutoCommit(false);
			Enumeration<String> parameterNames = request.getParameterNames();
			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();
				if (parameterName.startsWith("conf_")) {
					String value = request.getParameter(parameterName);
					if (Tools.isNotEmpty(value)) {
						saveConf(parameterName.substring(5), value, connection);
					}
				}
			}
			connection.commit();
		} catch (SQLException | RuntimeException ex) {
			failure = ex;
			try {
				connection.rollback();
			} catch (SQLException rollbackException) {
				ex.addSuppressed(rollbackException);
			}
			throw ex;
		} finally {
			if (originalAutoCommit) {
				try {
					connection.setAutoCommit(true);
				} catch (SQLException autoCommitException) {
					if (failure != null) failure.addSuppressed(autoCommitException);
					else throw autoCommitException;
				}
			}
		}
	}

	private static void saveConf(String name, String value, Connection db_conn) throws SQLException {
		try (PreparedStatement ps = db_conn.prepareStatement(
			"UPDATE "+ConfDB.CONF_TABLE_NAME+" SET value=? WHERE name=?")) {
			ps.setString(1, value);
			ps.setString(2, name);
			if (ps.executeUpdate() > 0) return;
		}

		try (PreparedStatement ps = db_conn.prepareStatement(
			"INSERT INTO "+ConfDB.CONF_TABLE_NAME+" (name, value) VALUES (?, ?)")) {
			ps.setString(1, name);
			ps.setString(2, value);
			ps.executeUpdate();
		}
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
		setupForm.setDbPassword("");
		setupForm.setDbSuperuserPassword("");
		setModel(model, setupForm, true, false);

		//Will show con error msg in page
		model.addAttribute("dbConnFail", conErr);
		//This is content of con err msg
		model.addAttribute("dbErrMsg", conErrMsg);
		//Separe crate rr message, will be shown if != null
		model.addAttribute("dbCreateErrMsg", createErrMsg);
	}

	private static String getEnvPassword(String password) {
		//if password is in form ${WEBJET_DB_PASS} try to get it using getSystemProperty
		if (password != null && password.startsWith("${") && password.endsWith("}")) {
			String envName = password.substring(2, password.length()-1);
			String envValue = getSystemProperty(envName);
			if (Tools.isNotEmpty(envValue)) {
				password = envValue;
			}
		}
		return password;
	}

	private static String getSystemProperty(String name) {
		String value = System.getProperty(name);
		if (Tools.isNotEmpty(value)) return value;
		value = System.getenv(name);
		if (Tools.isNotEmpty(value)) return value;
		return "";
	}
}
