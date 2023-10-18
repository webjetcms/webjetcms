<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.*,sk.iway.iwcm.*,java.sql.*,org.apache.commons.beanutils.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true" perms="cmp_diskusia"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
//nastavi forum_rating v tabulke users podla poctu prispevkov vo fore

Connection db_conn = null;
PreparedStatement ps = null;
ResultSet rs = null;
try
{
   List users = DB.getDynaList("SELECT user_id, count(forum_id) as rank FROM document_forum WHERE user_id > 0"+sk.iway.iwcm.common.CloudToolsForCore.getDomainIdSqlWhere(true)+" AND deleted=0 GROUP BY user_id ORDER BY rank desc");

	db_conn = DBPool.getConnection();

	Iterator iter = users.iterator();
	BasicDynaBean db;
	int userId, rank;
	while (iter.hasNext())
	{
		db = (BasicDynaBean)iter.next();

		userId = Tools.getIntValue(""+db.get("user_id"), -1);
		rank = Tools.getIntValue(""+db.get("rank"), -1);

		out.println(userId+": "+rank+"<br>");

		ps = db_conn.prepareStatement("UPDATE users SET forum_rank=? WHERE user_id=?");
		ps.setInt(1, rank);
		ps.setInt(2, userId);
		ps.execute();
		ps.close();
	}

	out.println("-- RATING RANK --<br>");

	users = DB.getDynaList("SELECT count(doc_id) AS rank, user_id FROM rating GROUP BY user_id ORDER BY rank desc");

	iter = users.iterator();
	while (iter.hasNext())
	{
		db = (BasicDynaBean)iter.next();

		userId = Tools.getIntValue(""+db.get("user_id"), -1);
		rank = Tools.getIntValue(""+db.get("rank"), -1);

		out.println(userId+": "+rank+"<br>");

		ps = db_conn.prepareStatement("UPDATE users SET rating_rank=? WHERE user_id=?");
		ps.setInt(1, rank);
		ps.setInt(2, userId);
		ps.execute();
		ps.close();
	}

	//fixni pocty prispevkov vo fore



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
%>
<%@ include file="/admin/layout_bottom.jsp" %>