<%@page import="java.util.Map"%><%@page import="java.util.List"%><%@page import="sk.iway.iwcm.doc.DebugTimer"%>
<%@page import="sk.iway.iwcm.gallery.GalleryBean"%>
<%@page import="sk.iway.iwcm.gallery.GalleryDB"%>
<%@page import="sk.iway.iwcm.stat.Column"%>
<%@page import="sk.iway.iwcm.stat.StatDB"%>
<%@page import="sk.iway.iwcm.stat.StatNewDB"%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="java.io.IOException"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.sql.PreparedStatement"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.sql.ResultSet" %><%@page import="java.sql.Timestamp"%>
<%@ page import="sk.iway.iwcm.common.CloudToolsForCore" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate|users.edit_admins"/><%!

public void printLine(StringBuffer log, JspWriter out, String text) throws IOException
{
	log.append(text);
	log.append("<br/>");
	System.out.println(text);
	out.println(text+"<br/>");
}

%>

<%@ include file="/admin/layout_top.jsp" %>

<h1>Aktualizacia pre WebJET CMS</h1>
<%
if ("fix".equals(request.getParameter("act"))==false) {
	%><p><a href="?act=fix">Spustiť</a></p><%
	return;
}
%>

<%@ include file="/admin/update/delete_old_files.jsp" %>

<%
Identity user = UsersDB.getCurrentUser(request);


if (user.isDisabledItem("modUpdate") && user.isDisabledItem("users.edit_admins"))
{
	out.println("Permission denied");
	return;
}

String email = user.getEmail();
String serverName = Tools.getBaseHref(request);

StringBuffer log = new StringBuffer();

//updatni fileName field pre vyhladavanie
sk.iway.iwcm.doc.DocDB.updateFileNameField(-1);

if (Constants.getBoolean("galleryWebJET7Converted")==false)
{
	ConfDB.setName("galleryWebJET7Converted", "true");

	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	/* Convert Gallery START*/
	printLine(log, out, "<br />Converting gallery");
	int counterDate=0;
	int counterPriority=0;
	int counterNew=0;

	Collection<IwcmFile> fileList = GalleryDB.getImagesFromDir("/images/gallery", true);
	Map<String,GalleryBean> fromDb = GalleryDB.getGalleryBeanTable("/images/gallery", "_sk", true);

	GalleryBean gb = null;
	int priority =0;

	for(IwcmFile file : fileList){
		if((gb = fromDb.get(file.getVirtualPath()))!= null){
			if(gb.getUploadDateNull() == null){
				//gb.setUploadDate(new Date(file.lastModified()));
				GalleryDB.updateImageItem(gb.getImageId(),"upload",Long.toString(file.lastModified()),gb.getImagePath(),gb.getImageName(),"");
				counterDate++;
			}
			if(gb.getSortPriority() == 0){
				priority = GalleryDB.getUpdatePriority(file.getVirtualParent());
				//gb.setSortPriority(GalleryDB.getNewPriority(file.getVirtualParent()));
				counterPriority++;
				GalleryDB.updateImageItem(gb.getImageId(),"priority",Integer.toString(priority),gb.getImagePath(),gb.getImageName(),"");
			}
		}
		else{
			//vytvorit novy zaznam v DB
			try
			{
				String dir = file.getVirtualParent();
				String name = file.getName();

				GalleryBean gBean =new GalleryBean();

				String sql = "INSERT INTO gallery (image_path,image_name,s_description_sk,l_description_sk,s_description_en,l_description_en,s_description_cz,l_description_cz,s_description_de,l_description_de,"+
					"s_description_pl,l_description_pl,s_description_hu,l_description_hu,s_description_ru,l_description_ru,s_description_esp,l_description_esp,s_description_cho,l_description_cho,author,allowed_domains, perex_group, upload_datetime,sort_priority,domain_id)"+
					" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

				db_conn = DBPool.getConnection(DBPool.getDBName(request));
				ps = db_conn.prepareStatement(sql);
				int parameterIndex = 1;
				ps.setString(parameterIndex++, dir);
				ps.setString(parameterIndex++, name);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				DB.setClob(ps, parameterIndex++, null);
				ps.setString(parameterIndex++, "");
				ps.setString(parameterIndex++, null);
				ps.setString(parameterIndex++, null);
				ps.setTimestamp(parameterIndex++, new Timestamp(file.lastModified()));
				priority = GalleryDB.getUpdatePriority(dir);
				ps.setInt(parameterIndex++,priority);
				ps.setInt(parameterIndex++, CloudToolsForCore.getDomainId());

				ps.execute();
				ps.close();
				counterNew++;

			}
			catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
			finally{
				try{
					if (rs != null) rs.close();
					if (ps != null) ps.close();
					if (db_conn != null) db_conn.close();
				}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
			}

		}
	}


	printLine(log, out, "Updated dates: " + counterDate);
	printLine(log, out, "Updated priorities: " + counterPriority);
	printLine(log, out, "New DB records: " + counterNew);
	printLine(log, out, "Gallery converted");
	/* Convert Gallery END*/
}
String logMessage;
if (Constants.getBoolean("statEnableTablePartitioning")==false)
{

	ConfDB.setName("statEnableTablePartitioning", "true");

	printLine(log, out, "<h1>Converting statistics (table partitioning)</h1>");
	printLine(log, out, "<h2>Stat database conversion (table stat_views)</h2>");
	printLine(log, out, "<p>This may take from several minutes to several hours depending on database size, please be patient. You can close this window, process will continue on background.</p>");

	out.flush();

	//zapis konfiguracnu hodnotu
	ConfDB.setName("statEnableTablePartitioning", "true");

	Connection db_conn = null;
	Connection db_conn2 = null;
	PreparedStatement ps = null;
	PreparedStatement ps2 = null;
	ResultSet rs = null;
	int counter = 0;
	DebugTimer dt = new DebugTimer("admin_db_convert.jsp");

	Map allreadyCreatedSuffixes = new Hashtable();

	int maxViewId = DB.queryForInt("SELECT MAX(view_id) AS view_id FROM stat_views");
	int actualViewId = 0;
	int viewIdStep = 200000;

	while (actualViewId <= maxViewId)
	{
		try
		{
		   db_conn = DBPool.getConnection();
		   db_conn2 = DBPool.getConnection();

		   //ORDER BY view_id ASC
		   ps = db_conn.prepareStatement("SELECT * FROM stat_views WHERE view_id>=? AND view_id<?", java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
		   ps.setInt(1, actualViewId);
		   ps.setInt(2, actualViewId+viewIdStep);

		   logMessage = "<strong>Getting stat_views list from: "+actualViewId+" to: "+(actualViewId+viewIdStep)+" total: "+maxViewId+"</strong>";
		   printLine(log, out, logMessage);
		   out.flush();

		   actualViewId += viewIdStep;

		   ps.setFetchSize(100);
		   rs = ps.executeQuery();

		   printLine(log, out, "Data readed. "+dt.getLastDiff()+" ms");

		   String lastSuffix = null;
		   Calendar cal = Calendar.getInstance();
		   Timestamp viewTime;

		   //od kedy sa skonvertuje statistika
		   long convertFrom = 0;
		   //convertFrom = DB.getTimestamp("1.1.2007", "0:00");

		   while (rs.next())
		   {
		   	counter++;
		   	try
		   	{
			   	viewTime = rs.getTimestamp("view_time");

			   	if (counter%50000==0)
					{
				   	printLine(log, out, Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows converted ("+ dt.getLastDiff() +" ms)");
						out.flush();
					}

			   	if (viewTime.getTime() < convertFrom) continue;

					cal.setTimeInMillis(viewTime.getTime());
					String suffix = "_"+cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);
					if (suffix.equals(lastSuffix)==false)
					{
						printLine(log, out, Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows converted ("+ dt.getLastDiff() +" ms)");

						printLine(log, out, "<hr>Suffix: "+suffix+"<hr>");
						out.flush();

						lastSuffix = suffix;
						if (ps2!=null) ps2.close();

						if (allreadyCreatedSuffixes.get(suffix)==null)
						{
							StatNewDB.createStatTable("stat_views", suffix);
							allreadyCreatedSuffixes.put(suffix, suffix);
						}

						String insertSql = "INSERT INTO stat_views"+suffix+" ("+
									"browser_id, session_id, doc_id, last_doc_id, view_time, group_id, last_group_id"+
									") VALUES (?, ?, ?, ?, ?, ?, ?)";
						ps2 = db_conn2.prepareStatement(insertSql);
					}

					ps2.setLong(1, rs.getLong("browser_id"));
					ps2.setLong(2, rs.getLong("session_id"));
					ps2.setInt(3, rs.getInt("doc_id"));
					ps2.setInt(4, rs.getInt("last_doc_id"));
					ps2.setTimestamp(5, viewTime);
					ps2.setInt(6, rs.getInt("group_id"));
					ps2.setInt(7, rs.getInt("last_group_id"));
					ps2.execute();
		   		}
		   		catch (Exception e2)
		   		{

		   		}
		   }
		   rs.close();
		   ps.close();
		   if (ps2!=null) ps2.close();
			db_conn.close();
			if (db_conn2!=null) db_conn2.close();
		   rs = null;
		   ps = null;
		   ps2 = null;
			db_conn = null;
			db_conn2 = null;
		}
		catch (Exception ex)
		{
		   if (ex.getMessage()!=null) printLine(log, out, "<span style='color: red;'>ERROR: "+ex.getMessage()+"</span>");
		   sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
		   try
		   {
		      if (rs!=null) rs.close();
		      if (ps!=null) ps.close();
		      if (ps2!=null) ps2.close();
		      if (db_conn!=null) db_conn.close();
		      if (db_conn2!=null) db_conn2.close();
		   }
		   catch (Exception ex2)
		   {

		   }
		}
		printLine(log, out, Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows converted ("+ dt.getLastDiff() +" ms)");
	}
	%>

	<hr>
<% }

//je to vsetko priamo tu, pretoze pri jsp:include sa stavalo ze timeoutla session a nedokoncila sa konverzia
if (Constants.getBoolean("statWebJET7Converted")==false)
{
	ConfDB.setName("statWebJET7Converted", "true");

	printLine(log, out, "<h1>Stat database merge</h1>");
	printLine(log, out, "<p>This may take from several minutes to several hours depending on database size, please be patient. You can close this window, process will continue on background.</p>");

	out.flush();

	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	int counter = 0;
	DebugTimer dt = new DebugTimer("admin_db_merge.jsp");

	Map<String, List<Column>> browserDataTable = new Hashtable<String, List<Column>>();
	try
	{
	   db_conn = DBPool.getConnection();

	   printLine(log, out, "Reading data from stat_browser into list");

	   //ORDER BY view_id ASC
	   ps = db_conn.prepareStatement("SELECT * FROM stat_browser", java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
	   ps.setFetchSize(100);
	   rs = ps.executeQuery();

	   printLine(log, out, "Data readed. "+dt.getLastDiff()+" ms");

	   String lastKey = null;
	   List<Column> columnList = null;
	   while (rs.next())
	   {
	   	counter++;
	   	if (counter%10000==0)
			{
	   		logMessage = Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows readed, tableSize="+browserDataTable.size()+" ("+ dt.getLastDiff() +" ms)";

	   		printLine(log, out, logMessage);
				out.flush();
			}

	   	String key = rs.getInt("year")+"-"+rs.getInt("week")+"-"+rs.getInt("group_id");
	   	if (key.equals(lastKey)==false || columnList == null)
	   	{
	   		columnList = browserDataTable.get(key);
	   		lastKey = key;
	   	}
	   	if (columnList==null)
	   	{
	   		columnList = new ArrayList<Column>();
	   		browserDataTable.put(key, columnList);
	   	}

	   	Column column = new Column(0,4,0,0,0);

	   	column.setIntColumn(StatDB.getStatKeyId(rs.getString("browser_id")), 0);
	   	column.setIntColumn(StatDB.getStatKeyId(rs.getString("platform")), 1);
	   	column.setIntColumn(StatDB.getStatKeyId(rs.getString("subplatform")), 2);
	   	column.setIntColumn(rs.getInt("views"), 3);

	   	columnList.add(column);
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
		printLine(log, out, "<span style='color: red;'>ERROR: "+ex.getMessage()+"</span>");
	   sk.iway.iwcm.Logger.error(ex);
	}
	finally
	{
	   try
	   {
	      if (rs!=null) rs.close();
	      if (ps!=null) ps.close();
	      if (db_conn!=null) db_conn.close();
	   }
	   catch (Exception ex2)
	   {

	   }
	}

	ps = null;
	rs = null;
	db_conn = null;

	logMessage = Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows readed, tableSize="+browserDataTable.size()+" ("+ dt.getLastDiff() +" ms)";
	printLine(log, out, logMessage);
	out.flush();

	Map<String, List<Column>> countryDataTable = new Hashtable<String, List<Column>>();
	try
	{
	   db_conn = DBPool.getConnection();

	   printLine(log, out, "Reading data from stat_country into list");

	   //ORDER BY view_id ASC
	   ps = db_conn.prepareStatement("SELECT * FROM stat_country", java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
	   ps.setFetchSize(100);
	   rs = ps.executeQuery();

	   printLine(log, out, "Data readed. "+dt.getLastDiff()+" ms");

	   String lastKey = null;
	   List<Column> columnList = null;
	   counter = 0;
	   while (rs.next())
	   {
	   	counter++;
	   	if (counter%10000==0)
			{
	   		logMessage = Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows readed, tableSize="+countryDataTable.size()+" ("+ dt.getLastDiff() +" ms)";

	   		printLine(log, out, logMessage);
				out.flush();
			}

	   	String key = rs.getInt("year")+"-"+rs.getInt("week")+"-"+rs.getInt("group_id");
	   	if (key.equals(lastKey)==false || columnList == null)
	   	{
	   		columnList = countryDataTable.get(key);
	   		lastKey = key;
	   	}
	   	if (columnList==null)
	   	{
	   		columnList = new ArrayList<Column>();
	   		countryDataTable.put(key, columnList);
	   	}

	   	Column column = new Column(1,1,0,0,0);

	   	column.setColumn(DB.internationalToEnglish(DB.prepareString(rs.getString("country_code"), 4).toLowerCase()), 0);
	   	column.setIntColumn(rs.getInt("views"), 0);

	   	columnList.add(column);
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
		printLine(log, out, "<span style='color: red;'>ERROR: "+ex.getMessage()+"</span>");
	   sk.iway.iwcm.Logger.error(ex);
	}
	finally
	{
	   try
	   {
	      if (rs!=null) rs.close();
	      if (ps!=null) ps.close();
	      if (db_conn!=null) db_conn.close();
	   }
	   catch (Exception ex2)
	   {

	   }
	}

	ps = null;
	rs = null;
	db_conn = null;

	logMessage = Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows readed, tableSize="+countryDataTable.size()+" ("+ dt.getLastDiff() +" ms)";
	printLine(log, out, logMessage);
	out.flush();

	//ok, mozeme iterovat zaznamy cez stat_views tabulky a zapisat tam nejake tie ciselka ;-)
	Calendar cal = Calendar.getInstance();
	cal.add(Calendar.YEAR, 1);
	long to = cal.getTimeInMillis();
	cal.set(Calendar.YEAR, 2000);
	cal.set(Calendar.DATE, 1);
	cal.set(Calendar.MONTH, Calendar.JANUARY);
	long from = cal.getTimeInMillis();

	String suffixes[] = StatNewDB.getTableSuffix(from, to);
	counter = 0;
	for (int s=0; s<suffixes.length; s++)
	{
		try
		{
			int minViewId = DB.queryForInt("SELECT MIN(view_id) AS view_id FROM stat_views"+suffixes[s]);
			int maxViewId = DB.queryForInt("SELECT MAX(view_id) AS view_id FROM stat_views"+suffixes[s]);
			int actualViewId = minViewId;
			int viewIdStep = 200000;

			while (actualViewId <= maxViewId)
			{
			   db_conn = DBPool.getConnection();
			   ps = db_conn.prepareStatement("SELECT view_id, view_time, group_id, browser_ua_id, platform_id, subplatform_id, country FROM stat_views"+suffixes[s]+" WHERE view_id>=? AND view_id<? ORDER BY view_id", java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			   ps.setInt(1, actualViewId);
			   ps.setInt(2, actualViewId+viewIdStep);

			   logMessage = "Getting list from: "+actualViewId+" to: "+(actualViewId+viewIdStep)+" in "+suffixes[s];
			   printLine(log, out, logMessage);
			   out.flush();

			   actualViewId += viewIdStep;

			   rs = ps.executeQuery();
			   String lastKey = null;
			   List<Column> browserColumnList = null;
			   List<Column> countryColumnList = null;
			   int logRows = 5000;

			   String updateSql = "UPDATE stat_views"+suffixes[s]+" SET browser_ua_id=?, platform_id=?, subplatform_id=?, country=? WHERE view_id=?";
			   if (Constants.DB_TYPE==Constants.DB_MYSQL) updateSql = "UPDATE LOW_PRIORITY IGNORE stat_views"+suffixes[s]+" SET browser_ua_id=?, platform_id=?, subplatform_id=?, country=? WHERE view_id=?";

			   PreparedStatement psUpdate = db_conn.prepareStatement(updateSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			   while (rs.next())
			   {
			   	counter++;

			   	Timestamp t = rs.getTimestamp("view_time");

					if (t != null)
					{
						if (counter%logRows==0)
						{
				   		long lastDiff = dt.getLastDiff();
				   		long rowsPerSecond = Math.round((double)logRows / ((double)lastDiff / 1000D));
				   		logMessage = Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows updated, row date="+Tools.formatDate(t.getTime())+" ("+ lastDiff +" ms, "+rowsPerSecond+" rows/s)";

				   		printLine(log, out, logMessage);
							out.flush();
						}


						int groupId = rs.getInt("group_id");

						cal.setFirstDayOfWeek(Calendar.MONDAY);
						cal.setTimeInMillis(t.getTime());
						int year = cal.get(Calendar.YEAR);
						int week = cal.get(Calendar.WEEK_OF_YEAR);

						String key = year+"-"+week+"-"+groupId;
						if (browserColumnList == null || countryColumnList==null || key.equals(lastKey)==false)
						{
							browserColumnList = browserDataTable.get(key);
							countryColumnList = countryDataTable.get(key);
				   		lastKey = key;
						}

						int browserUaId = rs.getInt("browser_ua_id");
						int platformId = rs.getInt("platform_id");
						int subplatformId = rs.getInt("subplatform_id");
						boolean hasUpdate = false;
				   	if (browserColumnList!=null)
				   	{
				   		//vyber prvy volny zaznam
				   		for (Column col : browserColumnList)
				   		{
				   			if (col.getIntColumn(3)>0)
				   			{
				   				browserUaId = col.getIntColumn(0);
				   				platformId = col.getIntColumn(1);
				   				subplatformId = col.getIntColumn(2);

				   				hasUpdate = true;

				   				col.setIntColumn(col.getIntColumn(3)-1, 3);

				   				break;
				   			}
				   		}
				   	}

				   	String country = rs.getString("country");
				   	if (countryColumnList!=null)
				   	{
				   		//vyber prvy volny zaznam
				   		for (Column col : countryColumnList)
				   		{
				   			if (col.getIntColumn(0)>0)
				   			{
				   				country = col.getColumn(0);

				   				hasUpdate = true;

				   				col.setIntColumn(col.getIntColumn(0)-1, 0);

				   				break;
				   			}
				   		}
				   	}
				   	if (hasUpdate)
				   	{
				   		psUpdate.setInt(1, browserUaId);
				   		psUpdate.setInt(2, platformId);
				   		psUpdate.setInt(3, subplatformId);
				   		psUpdate.setString(4, country);
				   		psUpdate.setInt(5, rs.getInt("view_id"));
				   		psUpdate.execute();
				   	}
					}
			   }

			   rs.close();
			   ps.close();
			   psUpdate.close();
				db_conn.close();
			   rs = null;
			   ps = null;
				db_conn = null;
			}
		}
		catch (Exception ex)
		{
		   if (ex.getMessage().contains("exist")) {
			//stat table doesnt exists, it's ok
		   } else {
			sk.iway.iwcm.Logger.error(ex);
		   }
		}
		finally
		{
		   try
		   {
		      if (rs!=null) rs.close();
		      if (ps!=null) ps.close();
		      if (db_conn!=null) db_conn.close();
		   }
		   catch (Exception ex2)
		   {

		   }
		}
	}

	logMessage = Tools.formatDateTime(Tools.getNow()) + " " + counter + " rows updated ("+ dt.getLastDiff() +" ms)";

	printLine(log, out, logMessage);
	out.flush();

	printLine(log, out, "<hr>Convert done.");
}
else
{
	printLine(log, out, "Database is allready converted, skipping");
}

printLine(log, out, "Po skonceni konverzie databazy statistik je mozne zmazat tabulky:");
printLine(log, out, "DROP TABLE stat_browser;");
printLine(log, out, "DROP TABLE stat_country;");
printLine(log, out, "DROP TABLE stat_site_days;");
printLine(log, out, "DROP TABLE stat_site_hours;");
printLine(log, out, "DROP TABLE stat_doc;");
printLine(log, out, "DROP TABLE stat_views;");

if (Tools.isNotEmpty(email))
{
	SendMail.send(email, email, email, "Statistics database conversion " + serverName, log.toString());
}
%>

<br/>
Pre zvysenie bezpecnosti odporucame zmigrovat databazu hesiel z obojsmernej sifry na pouzivanie hashov - <a target="_blank" href='/admin/update/update_passwords.jsp?act=fix'>/admin/update/update_passwords.jsp</a>.
Tato operacia je nezvratna, zmeni sa aj sposob ziskania zabudnuteho hesla - neposle sa existujuce heslo ale linka na zmenu hesla na email adresu daneho pouzivatela.

<%@ include file="/admin/layout_bottom.jsp" %>