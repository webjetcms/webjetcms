package sk.iway.iwcm;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.helpers.Util;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Logger.java - logger pre vypis vsetkeho mozneho
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 23.10.2005 22:02:57
 *@modified     $Date: 2010/02/09 08:28:56 $
 */
@SuppressWarnings("java:S1845")
public class Logger
{
	public static final Level OFF = Level.OFF;
	public static final Level ERROR = Level.ERROR;
	public static final Level WARN = Level.WARN;
	public static final Level INFO = Level.INFO;
	public static final Level DEBUG = Level.DEBUG;
	public static final Level TRACE = Level.TRACE;
	public static final Level ALL = Level.ALL;

	private static String installName = "webjet";

	protected Logger() {
		//utility class
	}

	public static void println(Class<?> c, String message)
	{
		info(c, message);
	}

	/**
	 * @deprecated - use info
	 */
	@Deprecated
	//uz sa neda pouzit, log4j print nepodporuje, je potrebne pouzit println
	public static void print(Class<?> c, String message)
	{
		info(c, message);
	}

	/*
	public static void printBeforeInitialization(Class<?> c, String message)
	{

		HashMap<String, String> map = new HashMap<>();

		map.put("%X{installName}", getInstallName());
		if (c!=null) map.put("%c", Constants.getBoolean("logShowClassName") ? c.getName() : "");
		else map.put("%c", "");
		map.put("%p", LogManager.getRootLogger().getLevel().toString());
		map.put("%X{userId}", String.valueOf(getUserId()));
		map.put("%m", message);
		map.put("%n", "\n");
		map.put("%d{" + getDateTimeFormat() + "}", LocalDateTime.now().format(DateTimeFormatter.ofPattern(getDateTimeFormat())));

		String result = "[%X{installName}][%c][%p][%X{userId}] %d{" + getDateTimeFormat() + "} - %m%n";

		for (Map.Entry<String, String> entry : map.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			result = Tools.replace(result, key, value);
		}

		System.out.print("[Logger.print]" + result);
	}*/

	public static String getDateTimeFormat() {
		return "yyyy-MM-dd HH:mm:ss,SSS";
	}

	public static void println(Object o, String message)
	{
		info(o, message);
	}

	/**
	 * @deprecated - use info
	 */
	@Deprecated
	//uz sa neda pouzit, log4j print nepodporuje, je potrebne pouzit println
	public static void print(Object o, String message)
	{
		info(o, message);
	}

	/**
	 * @deprecated - use error
	 */
	@Deprecated
	//nahradene metodou error
	public static void printlnError(Object o, Exception e)
	{
		error(o, e.getMessage());
	}

	/**
	 * @deprecated - use error
	 */
	@Deprecated
	//nahradene metodou error
	public static void printlnError(Object o, String message)
	{
		error(o, message);
	}

	/**
	 * @deprecated - use error
	 */
	@Deprecated
	//uz sa neda pouzit, log4j print nepodporuje, je potrebne pouzit println
	public static void printError(Object o, String message)
	{
		error(o, message);
	}

	/**
	 * @deprecated - use error
	 */
	@Deprecated
	//je potrebne pouzit error
	public static void printlnError(Class<?> c, String message)
	{
		error(c, message);
	}

	public static void printf(Class<?> source, String format, Object...args)
	{
		info(source, String.format(format, args));
	}

	/**
	 * @deprecated - use error
	 */
	@Deprecated
	//je potrebne pouzit error
	public static void printError(Class<?> c, String message)
	{
		error(c, message);
	}

	public static void printfError(Class<?> source, String format, Object...args)
	{
		error(source, String.format(format, args));
	}

	public static boolean isLevel(Level level) {
		return getLevel(null).isGreaterOrEqual(level);
	}

	public static Level getLevel(String className) {
		org.slf4j.Logger logger = getLogger(className);

		if (logger.isTraceEnabled()
				&& logger.isDebugEnabled()
				&& logger.isInfoEnabled()
				&& logger.isWarnEnabled()
				&& logger.isErrorEnabled()) {
			return Level.ALL;
		}

		if (logger.isTraceEnabled()) {
			return Level.TRACE;
		}
		if (logger.isDebugEnabled()) {
			return Level.DEBUG;
		}
		if (logger.isInfoEnabled()) {
			return Level.INFO;
		}
		if (logger.isWarnEnabled()) {
			return Level.WARN;
		}
		if (logger.isErrorEnabled()) {
			return Level.ERROR;
		}
		return Level.OFF;
	}

	public static int getLogLevel()
	{
		return getLevel(null).toInt();
	}

	public static void setWJLogLevel(String level)
	{
		if ("normal".equals(level))
		{
			setLevel(Constants.getString("logWebjetPackages"), Level.INFO);
		}
		else // if ("normal".equals(level))
		{
			setLevel(Constants.getString("logWebjetPackages"), Level.toLevel(level));
		}
	}

	public static void setWJLogLevel(Level level)
	{
		setLevel(Constants.getString("logWebjetPackages"), level);
	}

	public static void setWJLogLevels(Map<String, Level> levels) {
		if (levels == null) {
			return;
		}

		for (Map.Entry<String, Level> levelEntry : levels.entrySet()) {
		    try {
                Logger.setLevel(levelEntry.getKey(), levelEntry.getValue());
                Logger.debug(Logger.class, String.format("setWJLogLevels - package: %s, level: %s", levelEntry.getKey(), levelEntry.getValue()));
            }
            catch (Exception e) {
                Logger.debug(Logger.class, String.format("setWJLogLevels - Nepodarilo sa nastavit WJlogLevel pre package: %s, level: %s", levelEntry.getKey(), levelEntry.getValue()));
		        sk.iway.iwcm.Logger.error(e);
            }
		}
	}

	public static Map<String, Level> getLogLevelsMap(String logLevels) {
		Map<String, Level> result = new HashMap<>();
		if (Tools.isNotEmpty(logLevels)) {
			List<String> logLevelsList = Tools.getStringListValue(Tools.getTokens(logLevels, "\n"));
			for (String logLevel : logLevelsList) {
				String[] tokens = Tools.getTokens(logLevel, "=", true);
				if (tokens.length == 2) {
					result.put(tokens[0], Level.toLevel(tokens[1]));
                    Logger.debug(Logger.class, String.format("getLogLevelsMap - package: %s, level: %s", tokens[0], tokens[1]));
				}
				else {
					Logger.debug(Logger.class, String.format("getLogLevelsMap - Nepodarilo sa vyparsovat package a level zo stringu: %s", logLevel));
				}
			}
		}

		//to audit DB leaks we need this
		if (logLevels.contains("com.zaxxer.hikari.pool.ProxyLeakTask")==false) {
			result.put("com.zaxxer.hikari.pool.ProxyLeakTask", Level.WARN);
		}

		return result;
	}

	/**
	 * Nastavi uroven logovania pre zadane packs (ciarkou oddeleny zoznam packagov)
	 * @param packs sk.iway,cz.webactive,com.mypackage
	 * @param level log4jlevel
	 */
	public static void setLevel(String packs, Level level) {
		try {
			String[] packArr = Tools.getTokens(packs, ",");
			for (String pack : packArr)
			{
				ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) getLogger(pack);
				logger.setLevel(level);
			}
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	public static void setLevel(Level level) {
		try {
			ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME); //NOSONAR
			LoggerContext lc = root.getLoggerContext();
			for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {
				log.setLevel(level);
			}
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	protected static org.slf4j.Logger getLogger(Class<?> className) {
		org.slf4j.Logger logger;
		if (className == null) {
			logger = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME); //NOSONAR
		}
		else {
			logger = LoggerFactory.getLogger(className);
		}

		setMDC();

		return logger;
	}

	protected static org.slf4j.Logger getLogger(Object o)
	{
		String className = "sk.iway.iwcm.Logger";

		String str = getObjectClassName(o);
		if (str!=null && looksLikeClassName(str))
		{
			//ak to vyzera ako fqdn, pouzijeme to ako className
			className = str;
		}

		org.slf4j.Logger logger = LoggerFactory.getLogger(className);
		setMDC();

		return logger;
	}

	private static String appendObjectName(Object o, String message)
	{
		String str = getObjectClassName(o);
		if (str==null || looksLikeClassName(str)==false)
		{
			//pridame do message aj str, aby sa nam vypisalo meno JSP suboru alebo co to vlastne je
			return str+": "+message;
		}
		return message;
	}

	private static String getObjectClassName(Object o) {
		if (o == null) {
			return null;
		}

		if (o instanceof String) {
			//str moze byt vo formate sk.iway.iwcm.Tools.class alebo aj /components/pokus.jsp
			return  (String) o;
		}

		return o.getClass().getName();
	}

	protected static boolean looksLikeClassName(String str)
	{
		if (str.startsWith("sk.") || str.startsWith("org.") || str.startsWith("net.") || str.startsWith("com.") || str.startsWith("cz."))
		{
			return true;
		}
		//iterate over logWebjetPackages
		String[] packages = Constants.getArray("logWebjetPackages");
		for (String pack : packages)
		{
			if (str.startsWith(pack))
			{
				return true;
			}
		}
		//iterate over springAddPackages
		packages = Constants.getArray("springAddPackages");
		for (String pack : packages)
		{
			if (str.startsWith(pack))
			{
				return true;
			}
		}
		//iterate over logLevels
		String[] logLevels = Constants.getArray("logLevels");
		for (String logLevel : logLevels)
		{
			String[] tokens = Tools.getTokens(logLevel, "=", true);
			if (tokens.length == 2 && str.startsWith(tokens[0]))
			{
				return true;
			}
		}
		return false;
	}

	private static void setMDC() {
		int userId = getUserId();
		String installName = getInstallName();

		MDC.put("userId", "" + userId);
		MDC.put("installName", installName);
		MDC.put("node", Constants.getString("clusterMyNodeName"));
		MDC.put("nodeType", Constants.getString("clusterMyNodeType"));
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null) {
			String uri = rb.getUrl();
			if (Tools.isNotEmpty(rb.getQueryString()) && rb.getUrl().equals("/admin/logon.do")==false) uri += "?"+rb.getQueryString();
			MDC.put("URI", uri);
			MDC.put("Domain", rb.getDomain());
			MDC.put("User-Agent", rb.getUserAgent());
			MDC.put("IP", rb.getRemoteIP());
			MDC.put("Host", rb.getRemoteHost());
		}
	}

	private static int getUserId() {
		int userId = 0;
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null && rb.getUserId() > 0) {
			userId = rb.getUserId();
		}

		return userId;
	}

	public static void setInstallName(String installName) {
		Logger.installName = installName;
	}

	public static String getInstallName()
	{
		String logInstallName = Constants.getString("logInstallName");
		if (Tools.isNotEmpty(logInstallName)) return logInstallName;

		return Logger.installName;
	}

	public static void info(String message) {
		Class<?> c = Util.getCallingClass();
		info(c, message);
	}

	public static void info(Class<?> c, String message)
	{
		getLogger(c).info(message);
	}
	public static void info(Class<?> c, String message, Object... params)
	{
		getLogger(c).info(message, params);
	}

	public static void info(Object o, String message)
	{
		org.slf4j.Logger l = getLogger(o);
		if (l.isInfoEnabled()) l.info(appendObjectName(o, message));
	}
	public static void info(Object o, String message, Object... params)
	{
		org.slf4j.Logger l = getLogger(o);
		if (l.isInfoEnabled()) l.info(appendObjectName(o, message), params);
	}

	public static void debug(String message) {
		Class<?> c = Util.getCallingClass();
		debug(c, message);
	}

	public static void debug(Class<?> c, String message)
	{
		getLogger(c).debug(message);
	}
	public static void debug(Class<?> c, String message, Object... params)
	{
		getLogger(c).debug(message, params);
	}

	//Tato metoda sa vola typicky s name ako String, ale nejde to spravit inak kedze druha je Class<> a to nevie kompiler spravit
   	public static void debug(Object o, String message)
	{
		org.slf4j.Logger l = getLogger(o);
		if (l.isDebugEnabled()) l.debug(appendObjectName(o, message));
	}
	public static void debug(Object o, String message, Object... params)
	{
		org.slf4j.Logger l = getLogger(o);
		if (l.isDebugEnabled()) l.debug(appendObjectName(o, message), params);
	}

	public static void warn(String message) {
		Class<?> c = Util.getCallingClass();
		warn(c, message);
	}

	public static void warn(Class<?> c, String message) {
		getLogger(c).warn(message);
	}
	public static void warn(Class<?> c, String message, Object... params) {
		getLogger(c).warn(message);
	}

	public static void warn(Object o, String message){
		getLogger(o).warn(message);
	}
	public static void warn(Object o, String message, Object... params){
		getLogger(o).warn(message);
	}

	public static void error(Class<?> c, String message)
	{
		getLogger(c).error(message);
	}
	public static void error(Class<?> c, String message, Object... params)
	{
		getLogger(c).error(message, params);
	}

	public static void error(Throwable t) {
		Class<?> c = Util.getCallingClass();
		getLogger(c).error(t.getMessage(), t);
	}

	public static void error(Exception e) {
		Class<?> c = Util.getCallingClass();
		error(c, e);
	}

	public static void error(Class<?> c, Exception e)
	{
		getLogger(c).error(e.getMessage(), e);
	}
	public static void error(Class<?> c, Exception e, String message, Object... params)
	{
		getLogger(c).error(message, e, params);
	}

	public static void error(Class<?> c, String message, Throwable t) {
		getLogger(c).error(message, t);
	}
	public static void error(Class<?> c, String message, Throwable t, Object... params) {
		getLogger(c).error(message, t, params);
	}

	public static void error(Object o, String message)
	{
		org.slf4j.Logger l = getLogger(o);
		if (l.isErrorEnabled()) l.error(appendObjectName(o, message));
	}
	public static void error(Object o, String message, Object... params)
	{
		org.slf4j.Logger l = getLogger(o);
		if (l.isErrorEnabled()) l.error(appendObjectName(o, message), params);
	}

	/**
	 * @deprecated - use error
	 */
	@Deprecated
	public static void fatal(Class<?> c, String message) {
		getLogger(c).error(message);
	}

	public static void printfDebug(Class<?> source, String format, Object...args)
	{
		debug(source, String.format(format, args));
	}

	public static void debugMemInfo()
	{
		if(!isLevel(Level.DEBUG)) {
			return;
		}

		Runtime rt = Runtime.getRuntime();
	   	long free = rt.freeMemory();
	   	long total = rt.totalMemory();
	   	long used = total - free;
	   	Logger.debug(InitServlet.class, "MEM: F="+(free/1024/1024)+" T=" + (total/1024/1024) + " U="+(used/1024/1024));
	}

	/**
	 * Vypise do logu vsetky hodnoty HTTP parametrov
	 * @param request
	 */
	public static void debugAllParameters(HttpServletRequest request)
	{
		if(!isLevel(Level.DEBUG)) {
			return;
		}

		try
		{
			Enumeration<String> e = request.getParameterNames();
			while (e.hasMoreElements())
			{
				String name = e.nextElement();
				String[] values = request.getParameterValues(name);
				for (String value : values)
				{
					Logger.debug(Logger.class, "debugAllParameters: name="+name+" value="+value);
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vypise do logu vsetky HTTP hlavicky
	 * @param request
	 */
	public static void debugAllHeaders(HttpServletRequest request)
	{
		if(!isLevel(Level.DEBUG)) {
			return;
		}

		try
		{
			Enumeration<String> e = request.getHeaderNames();
			while (e.hasMoreElements())
			{
				String name = e.nextElement();
				Enumeration<String> h = request.getHeaders(name);
				while (h.hasMoreElements())
				{
					String value = h.nextElement();
					Logger.debug(Logger.class, "debugAllHeaders: name="+name+" value="+value);
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Zaloguje do debugu SQL query z JPA
	 * @param info
	 * @param query
	 */
	@SuppressWarnings("rawtypes")
	public static void debugQuery(String info, Query query)
	{
		if(!isLevel(Level.DEBUG)) {
			return;
		}

		try
		{
			DatabaseQuery databaseQuery = ((EJBQueryImpl) query).getDatabaseQuery();
			String sqlString = databaseQuery.getSQLString();
			Logger.debug(DatabaseQuery.class, info + ":\n" + sqlString);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	public static String getStackTrace(Exception ex)
	{
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));

		return sw.toString();
	}

	public static void debug(Map<String, List<String>> requestProperties) {
		System.out.println("RequestProperties:"); //NOSONAR
		for (Map.Entry<String, List<String>> entry : requestProperties.entrySet())
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); //NOSONAR
	}
}
