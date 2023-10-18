<%@page import="java.util.Map"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.sql.*"%>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld"%>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld"%>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld"%>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld"%>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld"%>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld"%>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp"%>
<%@page import="java.util.Calendar"%>
<%@page import="sk.iway.iwcm.doc.DebugTimer"%>
<%@page import="sk.iway.iwcm.stat.StatNewDB"%>
<%@page import="java.util.Hashtable"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Iterator"%>
<%@page import="sk.iway.iwcm.components.banner.BannerDB"%>
<%@page import="java.text.SimpleDateFormat"%>
<script language="JavaScript">
if (window.name && window.name=="componentIframe")
{
	document.write("<LINK rel='stylesheet' href='/components/iframe.css'>");
}
else
{
	document.write("<LINK rel='stylesheet' href='/admin/css/style.css'>");
}
var helpLink = "";
</script>

<h1>Stat database conversion (table banner_stat_views ->
banner_stat_views_day )</h1>
<p>This may take from several minutes to several hours depending on
database size, please be patient. You can close this window, process
will continue on background.</p>


<%!

class BannerView
{
int bannerId=-1;
Date insertDate=null;
int views=0;
}
%>

<%
SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");


out.flush();


DebugTimer dt = new DebugTimer("db_convert.jsp");

Map<Integer,Integer> banners=new Hashtable();
Map<String,BannerView> bannerViews=new Hashtable();

Connection db_conn = null;
PreparedStatement ps = null;
ResultSet rs = null;
int counter=0;
try
{
	db_conn = DBPool.getConnection();
	ps = db_conn.prepareStatement("select banner_id from banner_banners");
	rs = ps.executeQuery();
	out.println("adding banners to hashtable. <br>");
	out.flush();
	while (rs.next())
	{
		banners.put(rs.getInt("banner_id"),rs.getInt("banner_id"));
	}
	rs.close();
	ps.close();

	out.println("Data readed. ("+dt.getLastDiff()+" ms<br/>");
	out.flush();
	ps = db_conn.prepareStatement("select * from banner_stat_views", java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
	if (Constants.DB_TYPE==Constants.DB_MYSQL) ps.setFetchSize(Integer.MIN_VALUE);
   else ps.setFetchSize(1);
	rs = ps.executeQuery();

	out.println("Summing banners views. <br>");
	out.flush();
	while (rs.next())
	{
		if ((banners.get(rs.getInt("banner_id")))==null)
		{
			 continue;
		}else
		{

		BannerView bv=new BannerView();
		BannerView tmp=null;
		bv.bannerId=rs.getInt("banner_id");
		bv.insertDate=new Date(rs.getTimestamp("insert_date").getTime());
		bv.views=rs.getInt("views");
		StringBuffer buff=new StringBuffer();
		buff.append(bv.bannerId);
		buff.append("+");
		buff.append(sdf.format(bv.insertDate));

		tmp=bannerViews.get(buff.toString());
		if (tmp!=null){
			bv.views+=tmp.views;
		}else {
		}
		bannerViews.put(buff.toString(),bv);

		}
		counter++;
		if (counter%50000==0){
			out.println(counter + " rows converted ("+ dt.getLastDiff() +" ms)<br/>");
			out.flush();
		}

	}

   Iterator<BannerView> it=bannerViews.values().iterator();

  out.println("Updating the database. <br>");
	out.flush();
  int i=0;
   while (it.hasNext())
   {
   	BannerView bv=it.next();
    BannerDB.statAddViewExt(bv.bannerId,bv.views,bv.insertDate);
   	i++;
   }

   out.println("Inserted/updated "+i+" rows . <br>");

	db_conn.close();
	rs = null;
	ps = null;
	db_conn = null;
}
catch (Exception ex)
{
	out.println("<span style='color: red;'>ERROR: "+ex.getMessage()+"</span>");
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

%>

<hr>
Convert done.


<%@ include file="/admin/layout_bottom.jsp"%>
