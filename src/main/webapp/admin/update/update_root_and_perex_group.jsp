<%@page import="java.util.List"%><%@page import="java.util.Arrays"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="sk.iway.iwcm.doc.DebugTimer"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.sql.Connection"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>
<%!
public static void setRootGroupInDocuments(JspWriter out)
{
	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
		DebugTimer dt = new DebugTimer("setRootGroupInDocuments");
		Map<Integer, Integer> docGroupIdMap = new HashMap<Integer, Integer>();
		db_conn = DBPool.getConnection();
		ps = db_conn.prepareStatement("SELECT doc_id, group_id FROM documents");
		dt.diff("execute sql: SELECT doc_id, group_id FROM documents");
		out.print("ziskavam doc_id, group_id z documents...");out.flush();
		rs = ps.executeQuery();
		int groupId;
		while (rs.next())
		{
			groupId = rs.getInt("group_id");
			if(groupId > 0) docGroupIdMap.put(rs.getInt("doc_id"), groupId);
		}
		rs.close();
		ps.close();
		rs = null;
		ps = null;
		dt.diff("after select");
		out.print("pocet zaznamov: "+docGroupIdMap.size()+"<br/>");out.flush();
		Map.Entry<Integer, Integer> docIdGroupId;
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> parentGroups = null;
		int[] root_group_l = new int[3];
		int ind = 0;
		out.print("zacinam aktualizovat root_group_l1 - l3 ... cakaj<br/>");out.flush();
		int count = 0;
		for (Iterator<Map.Entry<Integer, Integer>> iterator = docGroupIdMap.entrySet().iterator(); iterator.hasNext();)
		{
			Arrays.fill(root_group_l, 0);
			docIdGroupId = iterator.next();
			parentGroups = groupsDB.getParentGroups(docIdGroupId.getValue());
			ind = 0;
			if(parentGroups != null && parentGroups.size() > 0)
			{
				for(int i=parentGroups.size()-1; i >= 0; i--)
				{
					root_group_l[ind++] = parentGroups.get(i).getGroupId();
					if(ind == 3) break;
				}
				ps = db_conn.prepareStatement("UPDATE documents SET root_group_l1 = ?, root_group_l2 = ?, root_group_l3 = ? WHERE doc_id = ?");
				if(root_group_l[0] > 0) ps.setInt(1, root_group_l[0]);
				else ps.setObject(1, null);
				if(root_group_l[1] > 0) ps.setInt(2, root_group_l[1]);
				else ps.setObject(2, null);
				if(root_group_l[2] > 0) ps.setInt(3, root_group_l[2]);
				else ps.setObject(3, null);
				ps.setInt(4, docIdGroupId.getKey().intValue());
				ps.execute();
				ps.close();
				ps = null;

				count++;
				if(count % 1000 == 0)
				{
					out.print("aktualizujem riadok "+count+"<br/>");
					out.flush();
				}
			}

		}
		out.print("aktualizovanych celkovo <strong>"+count+"</strong> zaznamov<br/>");
		out.flush();
		db_conn.close();
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

public void movePerexFromDocToPerexGroupDoc(JspWriter out)
{
	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
		DebugTimer dt = new DebugTimer("movePerexFromDocToPerexGroupDoc");
		Map<Integer, String> docPerexMap = new HashMap<Integer, String>();
		db_conn = DBPool.getConnection();
		ps = db_conn.prepareStatement("SELECT doc_id, perex_group FROM documents");
		dt.diff("execute sql: SELECT doc_id, perex_group FROM documents");
		out.print("ziskavam doc_id, perex_group z documents...");out.flush();
		rs = ps.executeQuery();
		String perexGroup;
		while (rs.next())
		{
			perexGroup = DB.getDbString(rs, "perex_group");
			if(Tools.isNotEmpty(perexGroup)) docPerexMap.put(rs.getInt("doc_id"), perexGroup);
		}
		rs.close();
		ps.close();
		rs = null;
		ps = null;
		dt.diff("after select");
		out.print("pocet zaznamov: "+docPerexMap.size()+"<br/>");out.flush();
		String[] pgArray = null;
		out.print("zacinam aktualizovat perex_group_doc ... cakaj<br/>");out.flush();
		int count = 0;
		for (Iterator iterator = docPerexMap.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry<Integer, String> docIdPerex = (Map.Entry<Integer, String>) iterator.next();
			pgArray = Tools.getTokens(docIdPerex.getValue(), ",");
			for(String pg : pgArray)
			{
				if(Tools.getIntValue(pg, -1) > 0)
				{
					ps = db_conn.prepareStatement("INSERT INTO perex_group_doc (doc_id, perex_group_id) VALUES (?,?)");
					ps.setInt(1, docIdPerex.getKey().intValue());
					ps.setInt(2, Tools.getIntValue(pg, 0));
					ps.execute();
					ps.close();
					ps = null;
				}
			}
			count++;
			if(count % 1000 == 0)
			{
				out.print("aktualizujem riadok "+count+"<br/>");
				out.flush();
			}
		}
		out.print("aktualizovanych celkovo <strong>"+count+"</strong> zaznamov<br/>");
		out.flush();
		db_conn.close();
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
%>
<%
if("true".equals(Tools.getRequestParameter(request, "start")))
{
	out.println("<strong>... START ...</strong><br/><br/>");
	out.println("<strong>ZACIATOK aktualizacie root_group_l1 - l3 v tabulke documents</strong><br/><br/>");out.flush();
	setRootGroupInDocuments(out);
	out.println("<br/><strong>KONIEC aktualizacie root_group_l1 - l3 v tabulke documents</strong><br/><br/>");
	out.println("<strong>ZACIATOK naplnania tabulky perex_group_doc</strong><br/><br/>");out.flush();
	movePerexFromDocToPerexGroupDoc(out);
	out.println("<br/><strong>KONIEC naplnania tabulky perex_group_doc</strong><br/><br/>");
	out.println("<strong>... END ...</strong><br/><br/>");out.flush();
}
else
{
	%>
	<p>
	Komponenta aktualizuje root_group_l1 - l3 v tabulke documents a naplnania tabulku perex_group_doc<br/>
	<a href="?start=true">Spustit aktualizaciu</a>
	</p>
	<%
}
%>
<%@ include file="/admin/layout_bottom.jsp" %>
