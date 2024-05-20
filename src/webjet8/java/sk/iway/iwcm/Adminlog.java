package sk.iway.iwcm;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.adminlog.AdminlogNotifyManager;

/**
 *  Adminlog.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: thaber $
 *@version      $Revision: 1.25 $
 *@created      Date: 7.2.2005 16:35:22
 *@modified     $Date: 2010/02/24 15:06:13 $
 */
public class Adminlog
{
	public static final int TYPE_HELP_LAST_SEEN = 10;
	public static final int TYPE_FILE_UPLOAD = 20;
	public static final int TYPE_SAVEDOC = 30;
	public static final int TYPE_MEDIA = 31;
	public static final int TYPE_MEDIA_GROUP = 32;

	public static final int TYPE_INIT = 40;
	//public static final int TYPE_AUDIT_NOTIFY = 41;

	public static final int TYPE_GROUP = 50;
	public static final int TYPE_IMPORTXLS = 60;
	public static final int TYPE_DMAIL_AUTOSENDER = 70;
	public static final int TYPE_DMAIL_DOMAINLIMITS = 71;
	public static final int TYPE_DMAIL = 72;
	public static final int TYPE_DMAIL_BLACKLIST = 73;
	public static final int TYPE_USER_LOGOFF = 79;
	public static final int TYPE_USER_LOGON = 80;
	public static final int TYPE_USER_EDIT = 81;
	public static final int TYPE_USER_INSERT = 82;
	public static final int TYPE_USER_DELETE = 83;
	public static final int TYPE_USER_AUTHORIZE = 84;
	public static final int TYPE_USER_UPDATE = 85;
	public static final int TYPE_USER_SAVE = 86;
	public static final int TYPE_USER_GROUP_UPDATE = 87;
	public static final int TYPE_USER_GROUP_DELETE = 88;
	public static final int TYPE_USER_GROUP_INSERT = 89;
	public static final int TYPE_TEMPLATE_INSERT = 90;
	public static final int TYPE_TEMPLATE_UPDATE = 91;
	public static final int TYPE_TEMPLATE_DELETE = 92;
	public static final int TYPE_INQUIRY = 95;
	public static final int TYPE_GALLERY = 93;
	public static final int TYPE_SE_SITEMAP = 100;
	public static final int TYPE_FORM_EXPORT = 110;
	public static final int TYPE_FORM_DELETE = 111;
	public static final int TYPE_FORM_ARCHIVE = 112;
   public static final int TYPE_FORM_VIEW = 113;
	public static final int TYPE_FORM_REGEXP = 114;
   public static final int TYPE_FORMMAIL = 120;
	public static final int TYPE_SENDMAIL = 130;
	public static final int TYPE_JSPERROR = 140;
	public static final int TYPE_SQLERROR = 150;
	public static final int TYPE_XSS = 160;
	public static final int TYPE_XSRF = 165;
	public static final int TYPE_RUNTIME_ERROR = 170;
	public static final int TYPE_HELPDESK = 200;
	public static final int TYPE_DATA_DELETING = 220;
	public static final int TYPE_CRON = 230;
	public static final int TYPE_USER_CHANGE_PASSWORD = 94;
	/*
	public static final int TYPE_COMPONENT_CREATE = 500;
	public static final int TYPE_COMPONENT_DELETE = 510;
	public static final int TYPE_COMPONENT_UPDATE = 520;
	*/
	public static final int TYPE_PEREX_GROUP_CREATE = 530;
	public static final int TYPE_PEREX_GROUP_DELETE = 540;
	public static final int TYPE_PEREX_GROUP_UPDATE = 550;
	public static final int TYPE_CONF_UPDATE = 560;
	public static final int TYPE_CONF_DELETE = 570;
	public static final int TYPE_PROP_UPDATE = 580;
	public static final int TYPE_PROP_DELETE = 590;
	public static final int TYPE_BANNER_CREATE = 600;
	public static final int TYPE_BANNER_DELETE = 610;
	public static final int TYPE_BANNER_UPDATE = 620;
	public static final int TYPE_CALENDAR_CREATE = 630;
	public static final int TYPE_CALENDAR_DELETE = 640;
	public static final int TYPE_CALENDAR_UPDATE = 650;
	public static final int TYPE_QA_CREATE = 660;
	public static final int TYPE_QA_DELETE = 670;
	public static final int TYPE_QA_UPDATE = 680;
	public static final int TYPE_TIP_CREATE = 690;
	public static final int TYPE_TIP_DELETE = 700;
	public static final int TYPE_TIP_UPDATE = 710;
	public static final int TYPE_INQUIRY_CREATE = 720;
	public static final int TYPE_INQUIRY_DELETE = 730;
	public static final int TYPE_INQUIRY_UPDATE = 740;
	public static final int TYPE_PROXY_CREATE = 750;
	public static final int TYPE_PROXY_DELETE = 760;
	public static final int TYPE_PROXY_UPDATE = 770;
	public static final int TYPE_REDIRECT_CREATE = 780;
	public static final int TYPE_REDIRECT_DELETE = 790;
	public static final int TYPE_REDIRECT_UPDATE = 800;
	public static final int TYPE_PAGE_DELETE = 810;
	public static final int TYPE_PAGE_UPDATE = 820;
	public static final int TYPE_FORUM_SAVE = 2000;
	public static final int TYPE_USER_PERM_GROUP_CREATE = 830;
	public static final int TYPE_USER_PERM_GROUP_UPDATE = 840;
	public static final int TYPE_USER_PERM_GROUP_DELETE = 850;
	public static final int TYPE_FORUM_DELETE = 860;
	public static final int TYPE_FORUM_UNDELETE = 861;
	public static final int TYPE_FORUM_CLOSE = 860;
	public static final int TYPE_FORUM_CREATE = 870;
	public static final int TYPE_FORUM_UPDATE = 880;
	public static final int TYPE_BASKET_CREATE = 890;
	public static final int TYPE_BASKET_DELETE = 900;
	public static final int TYPE_BASKET_UPDATE = 910;
	public static final int TYPE_FILE_SAVE = 920;
	public static final int TYPE_FILE_DELETE = 930;
	public static final int TYPE_FILE_EDIT = 935;
	public static final int TYPE_FILE_CREATE = 936;

	public static final int TYPE_PERSISTENT_CACHE = 300;
	public static final int TYPE_INSERT_SCRIPT = 301;
	public static final int TYPE_ADMINLOG_NOTIFY = 302;
	public static final int TYPE_TEMPLATE_GROUP = 303;
	public static final int TYPE_TOOLTIP = 304;

	public static final int TYPE_RESERVATION = 305;
	public static final int TYPE_RESERVATION_OBJECT = 306;
	public static final int TYPE_RESERVATION_PRICE = 307;
	public static final int TYPE_RESERVATION_TIMES = 308;
	public static final int TYPE_DOC_ATTRIBUTES = 309;
	public static final int TYPE_SEO = 310;
	public static final int TYPE_RESTAURANT_MENU = 311;
	public static final int TYPE_QUIZ = 312;

	/*
	public static final int TYPE_HEAT_MAP_CLEAN = 940;
	public static final int TYPE_REG_ALARM = 970;
	*/
	public static final int TYPE_CLIENT_SPECIFIC = 99999;
	public static final int TYPE_UPDATEDB = 980;
	/*
	public static final int TYPE_CONTACT_CREATE = 990;
	public static final int TYPE_CONTACT_UPDATE = 991;
	public static final int TYPE_CONTACT_DELETE = 992;
	*/
	public static final int TYPE_INVENTORY = 1000;
	public static final int TYPE_IMPORT_WEBJET = 1010;
	public static final int TYPE_EXPORT_WEBJET = 1020;
	public static final int TYPE_EXPORT=1030;
	public static final int TYPE_PAYMENT_GATEWAY=1040;
	public static final int TYPE_WEB_SERVICES=1050;
    public static final int TYPE_GDPR_FORMS_DELETE = 1070;
    public static final int TYPE_GDPR_USERS_DELETE = 1071;
    public static final int TYPE_GDPR_BASKET_INVOICES_DELETE = 1072;
    public static final int TYPE_GDPR_EMAILS_DELETE = 1073;
	 public static final int TYPE_GDPR_REGEXP = 1074;
	 public static final int TYPE_GDPR_DELETE = 1075;
	 public static final int TYPE_GDPR_COOKIES = 1076;

	//POZOR NAD hodnotu 10 000 je mozne pridavat len client specific polia, ktore sa zobrazia len ak sa v nazve nachadza install name
	public static final int TYPE_CLONE_CLOUD_STRUCTURE = 10030;
	public static final int TYPE_SUCCESSFULLY_DELETED_CLOUD_STRUCTURE = 10040;
	public static final int TYPE_DELETE_ERROR_CLOUD_STRUCTURE = 10050;
	public static final int TYPE_REQUEST_TO_ORDER_DOMAIN_CLOUD = 10060;
	public static final int TYPE_ORDERED_DOMAIN_CLOUD = 10070;
	public static final int TYPE_REQUEST_TO_CONNECT_DOMAIN_CLOUD = 10080;
	public static final int TYPE_CONNECTED_DOMAIN_CLOUD = 10090;
	public static final int TYPE_USER_RENAME_DOMAIN_CLOUD = 10100;

	public static final int TYPE_COOKIE_ACCEPTED = 1060;
	public static final int TYPE_COOKIE_REJECTED = 1061;

	public static final int TYPE_RESPONSE_HEADER = 1062;

	//toto musi byt public aby to vedel ziskat adminlog.jsp
	private static final Integer[] TYPY_ARRAY;

	static
	{
		//zozbiera zoznam hore do pola TYPY_ARRAY
		// MBO: to uz to sakra nemohol byt ENUM???
		List<Integer> values = new ArrayList<>();
		Field[] fields =	Adminlog.class.getFields();
		for (Field field : fields)
		{
			try
			{
				if (field.getType() != int.class)
					continue;

				int value = field.getInt(null);

				if (value > 10000 && value != TYPE_CLIENT_SPECIFIC)
				{
					if (field.getName().toLowerCase().indexOf(Constants.getInstallName())==-1) continue;
				}

				values.add(value);
				// this code gather all the types and creates corresponding text.properties keys
				//System.out.println(String.format("components.adminlog.%d=%s", value, field.getName().replace("TYPE_", "")));
			}
			catch (IllegalArgumentException|IllegalAccessException e) {sk.iway.iwcm.Logger.error(e);}
		}
		Collections.sort(values);
		TYPY_ARRAY = values.toArray(new Integer[0]);
	}

	private Adminlog() {
		//private constructor
	}

	/**
	 * Zaloguje hlasku do admin logu
	 * @param logType - typ zaznamu (podla TYPE_xxx alebo custom > 99999)
	 * @param description - popis zaznamu
	 * @param subId1 - custom int typ1 (napr. primarny kluc objektu, ktoreho sa tyka zmena - docid, forumid,...)
	 * @param subId2 - custom int typ2
	 */
	public static void add(int logType, String description, int subId1, int subId2)
	{
		add(logType, SetCharacterEncodingFilter.getCurrentRequestBean(), description, subId1, subId2, new Timestamp(Tools.getNow()));
	}

	public static void add(int logType, String description, Long subId1, Long subId2)
	{
		int isub1 = 0;
		int isub2 = 0;
		if (subId1 != null) isub1 = subId1.intValue();
		if (subId2 != null) isub2 = subId2.intValue();
		add(logType, SetCharacterEncodingFilter.getCurrentRequestBean(), description, isub1, isub2, new Timestamp(Tools.getNow()));
	}

	/**
	 * Zaloguje hlasku do admin logu
	 * @param logType - typ zaznamu (podla TYPE_xxx alebo custom > 99999)
	 * @param description - popis zaznamu
	 * @param subId1 - custom int typ1 (napr. primarny kluc objektu, ktoreho sa tyka zmena - docid, forumid,...)
	 * @param subId2 - custom int typ2
	 * @param timestamp - timestamp zaznamu v DB
	 */
	public static void add(int logType, String description, int subId1, int subId2, Timestamp timestamp)
	{
		add(logType, SetCharacterEncodingFilter.getCurrentRequestBean(), description, subId1, subId2, timestamp);
	}



	/**
	 * Pridanie zaznamu do adminlogu AK SA POUZIVATEL ZMENIL OD ZACIATKU REQUESTU
	 * @param logType - typ zaznamu (podla TYPE_xxx alebo custom > 99999)
	 * @param userId - id pouzivatela (pouzije sa ak je request null)
	 * @param description - popis zmeny
	 * @param subId1 - custom int typ1 (napr. primarny kluc objektu, ktoreho sa tyka zmena - docid, forumid,...)
	 * @param subId2 - custom int typ2
	 * @param timestamp - timestamp zaznamu v DB
	 */
	public static void add(int logType, int userId, String description, int subId1, int subId2, Timestamp timestamp)
	{
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean == null) requestBean = new RequestBean();
		if (userId > 0) requestBean.setUserId(userId);
		add(logType, requestBean, description, subId1, subId2, timestamp);
	}

	/**
	 * Pridanie zaznamu do adminlogu AK SA POUZIVATEL ZMENIL OD ZACIATKU REQUESTU
	 * @param logType - typ zaznamu (podla TYPE_xxx alebo custom > 99999)
	 * @param userId - id pouzivatela (pouzije sa ak je request null)
	 * @param description - popis zmeny
	 * @param subId1 - custom int typ1 (napr. primarny kluc objektu, ktoreho sa tyka zmena - docid, forumid,...)
	 * @param subId2 - custom int typ2
	 */
	public static void add(int logType, int userId, String description, int subId1, int subId2)
	{
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean == null) requestBean = new RequestBean();
		if (userId > 0) requestBean.setUserId(userId);
		add(logType, requestBean, description, subId1, subId2, new Timestamp(Tools.getNow()));
	}

	/**
	 * Pridanie zaznamu do adminlogu, zaznam sa zapise anonymne (bez IP adresy, user-agent a referer)
	 * @param logType - typ zaznamu (podla TYPE_xxx alebo custom > 99999)
	 * @param description - popis zmeny
	 * @param subId1 - custom int typ1 (napr. primarny kluc objektu, ktoreho sa tyka zmena - docid, forumid,...)
	 * @param subId2 - custom int typ2
	 * @param anonymous -
	 */
	public static void addAnonymously(int logType, String description, int subId1, int subId2) {
		RequestBean requestBeanAnonym = new RequestBean();
		requestBeanAnonym.setUserId(-1);
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null) {
			requestBeanAnonym.setDomain(requestBean.getDomain());
			requestBeanAnonym.setUrl(requestBean.getUrl());
			requestBeanAnonym.setRemoteIP("0.0.0.0");
			requestBeanAnonym.setRemoteHost("0.0.0.0");

		}
		add(logType, requestBeanAnonym, description, subId1, subId2, new Timestamp(Tools.getNow()));
	}

//	public static void addWithDifferentUser(int logType, int userId, String description, int subId1, int subId2)
//	{
//		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
//		requestBean.setUserId(userId);
//		add(logType, requestBean, description, subId1, subId2);
//	}

//	private static void add(int logType, RequestBean requestBean, String description, int subId1, int subId2)
//	{
//		add(logType, requestBean, description, subId1, subId2, new Timestamp(Tools.getNow()));
//	}

	private static void add(int logType, RequestBean requestBean, String descriptionParam, int subId1, int subId2, Timestamp timestamp)
	{
		StringBuilder localhostAppend = null;
		if (requestBean == null || Tools.isEmpty(requestBean.getRemoteIP())) {
			if (requestBean == null) requestBean = new RequestBean();

			try {
				localhostAppend = new StringBuilder();
				//it's probably not HTTP request call, set IP address of server
				String serverName = InetAddress.getLocalHost().getHostName();
				InetAddress[] ipecky = InetAddress.getAllByName(serverName);

				int i;
				for (i = 0; i < ipecky.length; i++)
				{
					if (i==0) {
						requestBean.setRemoteIP(ipecky[i].getHostAddress());
						requestBean.setRemoteHost(ipecky[i].getHostName());
					}
					localhostAppend.append("ServerIP[").append(i + 1).append("]: ").append(ipecky[i].getHostAddress()).append(' ').append(ipecky[i].getHostName()).append('\n');
				}

				if (Constants.getServletContext()!=null) localhostAppend.append("Web root: ").append(Constants.getServletContext().getRealPath("/")).append('\n');
				localhostAppend.append("user: ").append(System.getProperty("user.name")).append('\n');

			} catch (Exception ex) {
				//
			}
		}

		//Timestamp timestamp = new Timestamp(Tools.getNow());

		String description = descriptionParam;

		if (Tools.isEmpty(description) || logType==Adminlog.TYPE_FORMMAIL)
		{
			if (Tools.isNotEmpty(requestBean.getErrorsString())) description = requestBean.getErrorsString();

			StringBuilder parameters = new StringBuilder();

			for (Map.Entry<String, String[]> entry : requestBean.getAllParameters().entrySet())
			{
				for (String value : entry.getValue())
				{
					if (parameters.length()>0) parameters.append("\n");
					parameters.append(entry.getKey());
					parameters.append(": ");
					parameters.append(value);
				}
			}

			if (parameters.length()>0)
			{
				description = description + "\nparameters:\n"+parameters.toString();
			}
		}


		boolean writeToAuditLog = true;

		try
		{
			if (logType==TYPE_JSPERROR && description.indexOf("Cannot create a session after the response has been committed")!=-1) writeToAuditLog = false;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		if (writeToAuditLog)
		{
			try
			{
				//debug
				Logger.debug(Adminlog.class, description + " subId1="+subId1+" subId2="+subId2);

				Connection db_conn = DBPool.getConnection();
				try
				{
					PreparedStatement ps = db_conn.prepareStatement("INSERT INTO " + ConfDB.ADMINLOG_TABLE_NAME+" (log_type, user_id, ip, hostname, create_date, description, sub_id1, sub_id2) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
					try
					{
						int index = 1;
						ps.setInt(index++, logType);
						ps.setInt(index++, requestBean.getUserId());
						ps.setString(index++, requestBean.getRemoteIP());
						ps.setString(index++, requestBean.getRemoteHost());

						ps.setTimestamp(index++, timestamp);

						String descriptionLocal = DB.prepareString(description, 60000);

						String nodeName = Constants.getString("clusterMyNodeName");
                        descriptionLocal += "\n";
						if (Tools.isNotEmpty(nodeName))
						{
                            descriptionLocal += "\nnode:"+nodeName;
						}
						if (Tools.isNotEmpty(requestBean.getUrl()))
						{
                            descriptionLocal += "\nURI: "+requestBean.getUrl();
							if (Tools.isNotEmpty(requestBean.getQueryString()) && requestBean.getUrl().equals("/admin/logon.do")==false) descriptionLocal += "?"+requestBean.getQueryString();
						}
						if (Tools.isNotEmpty(requestBean.getDomain()))
						{
                            descriptionLocal += "\nDomain: "+requestBean.getDomain();
						}
						if (Tools.isNotEmpty(requestBean.getUserAgent()))
						{
                            descriptionLocal += "\nUser-Agent: "+requestBean.getUserAgent();
						}
						if (localhostAppend!=null && localhostAppend.length()>0)
						{
							descriptionLocal += "\n"+localhostAppend.toString();
						}

						DB.setClob(ps, index++, DB.prepareString(descriptionLocal, 65000));

						ps.setInt(index++, subId1);
						ps.setInt(index++, subId2);

						ps.execute();
					}
					finally { ps.close(); }
				}
				finally { db_conn.close(); }
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		if (Tools.isNotEmpty(Constants.getString("adminlogCustomLogger")))
		{
			try
			{
				@SuppressWarnings("rawtypes")
				Class loggerClass = Class.forName(Constants.getString("adminlogCustomLogger"));
				if (loggerClass!=null && AdminlogCustomLogger.class.isAssignableFrom(loggerClass))
				{
					@SuppressWarnings("unchecked")
					AdminlogCustomLogger logger = (AdminlogCustomLogger)loggerClass.getDeclaredConstructor().newInstance();
					//sem musi ist descriptionParam, pretoze description uz obsahuje aj parametre (ak je prazdny description pridaju sa, to v TB handlujeme zvlast)
					logger.addLog(logType, requestBean, descriptionParam, timestamp);
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		try
		{
			//Logovanie do suboru
			AdminlogFile.write(timestamp, requestBean, logType, description);
			//AdminlogTB.write(timestamp, requestBean, logType, description);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		try
		{
			//Poslanie notifikacie
			AdminlogNotifyManager.sendNotification(logType, requestBean, timestamp, description, writeToAuditLog); // posle notifikacny email, ak je nastaveny
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vrati datum posledneho zaznamu daneho typu pre daneho pouzivatela
	 * @param logType - typ zaznamu
	 * @param userId - id pouzivatela
	 * @return
	 */
	public static long getLastDate(int logType, int userId)
	{
		long lastDate = 0;
		try
		{
			Connection db_conn = DBPool.getConnection();
			try
			{
				PreparedStatement ps = db_conn.prepareStatement("SELECT max(create_date) as create_date FROM "+ConfDB.ADMINLOG_TABLE_NAME+" WHERE user_id=? AND log_type=?");
				try
				{
					ps.setInt(1, userId);
					ps.setInt(2, logType);
					ResultSet rs = ps.executeQuery();
					try
					{
						if (rs.next())
						{
							Timestamp ts = rs.getTimestamp("create_date");
							if (ts != null) lastDate = ts.getTime();
						}
					}
					finally { rs.close(); }
				}
				finally { ps.close(); }
			}
			finally { db_conn.close(); }
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(lastDate);
	}

	/**
	 * Vrati zoznam zmenenych atributov POJO objektu
	 * @param o1
	 * @param o1Original
	 * @return
	 */
	public static String getPojoZmeny(Object o1, Object o1Original)
	{
		Map<String, String> properties = new HashMap<>();
		try
		{
			BeanUtils.populate(o1, properties);
		}
		catch (Exception ex)
		{

		}

		return null;
	}

	public static List<AdminlogBean> searchAdminlog(int[] logTypes, int logTypeFrom, int logTypeTo, int userId, long createdFrom, long createdTo, String description, int subId1, int subId2, String ip, String hostname)
	{
		return searchAdminlog(logTypes, logTypeFrom, logTypeTo, userId, createdFrom, createdTo, description, subId1, subId2, ip, hostname, -1);
	}

	/**
	 * Vyhladavanie v adminlogu
	 * @param logTypes - pole log_type ideciek, alebo prazdne pole
	 * @param logTypeFrom - log type od, alebo -1
	 * @param logTypeTo - log type do, alebo -1
	 * @param userId - id pouzivatela, alebo 0
	 * @param createdFrom - datum zaciatku logu, alebo -1
	 * @param createdTo - datum konca logu, alebo -1
	 * @param description, podretazec z pola description, alebo null
	 * @param subId1 - sub_id1, alebo -1
	 * @param subId2 - sub_id2, alebo -1
	 * @param ip - podretazec ip adresy pocitaca, alebo null
	 * @param hostname - podretazec hostname pocitaca, alebo null
	 * @param logIdGreaterThan - id zaznamu v databaze od ktoreho sa budu vysledky vyhladavat. Je to kladne cislo alebo -1
	 * @return
	 */
	public static List<AdminlogBean> searchAdminlog(int[] logTypes, int logTypeFrom, int logTypeTo, int userId, long createdFrom, long createdTo, String description, int subId1, int subId2, String ip, String hostname, int logIdGreaterThan)
	{
		List<AdminlogBean> logy = new ArrayList<>();

		try
		{
			StringBuilder sql = new StringBuilder("SELECT * FROM ").append(ConfDB.ADMINLOG_TABLE_NAME);
			StringBuilder where = new StringBuilder();
			if (logTypes.length>0)
			{
				where.append(" AND log_type IN (");
				for (int i=0; i<logTypes.length; i++)
				{
					if (i>0) where.append(',');
					where.append(logTypes[i]);
				}
				where.append(')');
			}
			if (logTypeFrom != -1)
			{
				where.append(" AND log_type>=").append(logTypeFrom);
			}
			if (logTypeTo != -1)
			{
				where.append(" AND log_type<=").append(logTypeTo);
			}
			if (userId != 0)
			{
				if (userId<0)
				{
					userId = -userId;
					where.append(" AND user_id<>").append(userId);
				}
				else
				{
					where.append(" AND user_id=").append(userId);
				}
			}
			if (createdFrom > 0)
			{
				where.append(" AND create_date>=?");
			}
			if (createdTo > 0)
			{
				where.append(" AND create_date<=?");
			}
			if (Tools.isNotEmpty(description))
			{
				if (description.charAt(0)=='!' && description.length()>2)
				{
					description = description.substring(1);
					where.append(" AND description NOT LIKE ?");
				}
				else if (description.startsWith("NOT") && description.length()>3)
                {
                    description = description.substring(3).trim();
                    where.append(" AND description NOT LIKE ?");
                }
				else
				{
					where.append(" AND description LIKE ?");
				}
			}
			if (subId1 != -1)
			{
				where.append(" AND sub_id1=").append(subId1);
			}
			if (subId2 != -1)
			{
				where.append(" AND sub_id2=").append(subId2);
			}
			if (Tools.isNotEmpty(ip))
			{
				if (ip.charAt(0)=='!' && ip.length()>2)
				{
					ip = ip.substring(1);
					where.append(" AND ip NOT LIKE ?");
				}
				else
				{
					where.append(" AND ip LIKE ?");
				}
			}
			if (Tools.isNotEmpty(hostname))
			{
				if (hostname.charAt(0)=='!' && hostname.length()>2)
				{
					hostname = hostname.substring(1);
					where.append(" AND hostname NOT LIKE ?");
				}
				else
				{
					where.append(" AND hostname LIKE ?");
				}
			}
			if (logIdGreaterThan != -1)
			{
				where.append(" AND log_id>=").append(logIdGreaterThan);
			}


			if (Tools.isNotEmpty(where))
			{
				//odstranime prve AND
				sql.append(" WHERE ").append(where.substring(4).trim());
			}

			sql.append(" ORDER BY log_id DESC");

			Logger.debug(Adminlog.class, "adminlog sql: " + sql);

			Connection db_conn = DBPool.getConnection();
			try
			{
				PreparedStatement ps = db_conn.prepareStatement(sql.toString());
				try
				{
					int psCounter = 1;
					if (createdFrom > 0)
					{
						ps.setTimestamp(psCounter++, new Timestamp(createdFrom));
					}
					if (createdTo > 0)
					{
						ps.setTimestamp(psCounter++, new Timestamp(createdTo));
					}
					if (Tools.isNotEmpty(description))
					{
						ps.setString(psCounter++, "%"+description+"%");
					}
					if (Tools.isNotEmpty(ip))
					{
						ps.setString(psCounter++, "%"+ip+"%");
					}
					if (Tools.isNotEmpty(hostname))
					{
						ps.setString(psCounter++, "%"+hostname+"%");
					}

					ResultSet rs = ps.executeQuery();
					try
					{
						AdminlogBean ab;
						while (rs.next())
						{
							ab = new AdminlogBean(rs);
							logy.add(ab);
						}
					}
					finally { rs.close(); }
				}
				finally { ps.close(); }
			}
			finally { db_conn.close(); }
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return(logy);
	}

	public static List<AdminlogBean> getLastEvents(int size)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<AdminlogBean> logy = new ArrayList<>();
		String ADMINLOG_FIELDS_NODATA = "SELECT * FROM ";
		String ADMINLOG_ORDER_BY_DOCS_ID = " ORDER BY log_id DESC";
		try
		{
			StringBuilder sql = null;
			if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				sql = new StringBuilder("SELECT TOP ").append(size).append(" * FROM ").append(ConfDB.ADMINLOG_TABLE_NAME);
				sql.append(ADMINLOG_ORDER_BY_DOCS_ID);
			}
			else if (Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				sql = new StringBuilder(ADMINLOG_FIELDS_NODATA).append(ConfDB.ADMINLOG_TABLE_NAME);
				sql.append(ADMINLOG_ORDER_BY_DOCS_ID);
				sql.append(" LIMIT ").append(size);
			}
			else
			{
				//oracle
				sql = new StringBuilder(ADMINLOG_FIELDS_NODATA).append(ConfDB.ADMINLOG_TABLE_NAME);
				sql.append(" WHERE rownum < ").append(size);
				sql.append(ADMINLOG_ORDER_BY_DOCS_ID);
			}
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			rs = ps.executeQuery();
			try
			{
				AdminlogBean ab;
				while (rs.next())
				{
					ab = new AdminlogBean(rs);
					logy.add(ab);
				}
			}
			finally
			{
				rs.close();
				ps.close();
				db_conn.close();
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return logy;
	}

	/**
	 * Returns all types of audit
	 * @return
	 */
	public static Integer[] getTypes() {
		return TYPY_ARRAY;
	}
}
