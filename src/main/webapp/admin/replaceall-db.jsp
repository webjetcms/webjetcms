<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.tags.support_logic.ResponseUtils,sk.iway.iwcm.doc.*,java.util.*,java.sql.*"%>
<%@ page import="sk.iway.iwcm.DBPool" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<iwcm:checkLogon admin="true" perms="users.edit_admins"/>
<%@ include file="layout_top.jsp" %>
<%!

public void replaceText(Connection db_conn, String oldText, String newText, String field, JspWriter out) throws Exception
{
	String sql = "UPDATE documents SET "+field+"=replace("+field+", ?, ?)";
	PreparedStatement ps = db_conn.prepareStatement(sql);
	ps.setString(1, oldText);
	ps.setString(2, newText);
	int pocet = ps.executeUpdate();
	ps.close();
	out.println("Text "+ ResponseUtils.filter(oldText) +" nahradeny "+pocet+"x za " + ResponseUtils.filter(newText)+" v "+field+"<br>");
}

%>
<%
String oldText = Tools.getRequestParameterUnsafe(request, "oldText");
String newText = Tools.getRequestParameterUnsafe(request, "newText");

if (oldText == null)
{
   oldText = "";
   newText = "";
}
else
{
   Connection db_conn = null;

	try
	{
		db_conn = DBPool.getConnection();

		replaceText(db_conn, oldText, newText, "data", out);
		replaceText(db_conn, oldText, newText, "data_asc", out);
		replaceText(db_conn, oldText, newText, "title", out);
		replaceText(db_conn, oldText, newText, "navbar", out);
		replaceText(db_conn, oldText, newText, "html_head", out);
		replaceText(db_conn, oldText, newText, "html_data", out);
		replaceText(db_conn, oldText, newText, "perex_image", out);
		replaceText(db_conn, oldText, newText, "perex_place", out);
		replaceText(db_conn, oldText, newText, "external_link", out);
		replaceText(db_conn, oldText, newText, "virtual_path", out);


		db_conn.close();
		db_conn = null;

		DocDB.getInstance();
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
		}
		catch (Exception ex2)
		{
		}
	}
}

%>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-refresh"></i><iwcm:text key="components.user.perms.replaceAll"/></h1>
</div>
<div class="content-wrapper">

	<form name="replaceForm" action="replaceall-db.jsp" method="post">
		Starý text:
		<textarea name='oldText' class="form-control" rows=8 cols=80><%=ResponseUtils.filter(oldText)%></textarea>
		<br>
		Nový text:
		<textarea name='newText' class="form-control" rows=8 cols=80><%=ResponseUtils.filter(newText)%></textarea>
		<br>
		<input type="submit" class="btn btn-primary" name="replace" value="Nahradiť">
	</form>
</div>

<%@ include file="layout_bottom.jsp" %>
