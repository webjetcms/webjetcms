<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.editor.*, java.sql.*, sk.iway.iwcm.users.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<h1>Pregenerovanie data_asc stlpca v databaze, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<%
if ("fix".equals(request.getParameter("act"))) {
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);

	Identity user = UsersDB.getCurrentUser(request);
	if (user == null || user.isAdmin()==false) return;

	int maxId = (new SimpleQuery()).forInt("SELECT max(doc_id) AS id FROM documents");

	int start = 0;
	int PAGE_SIZE = 100;

	int doc_id = -1;

	Connection db_conn = null;
	ResultSet rs = null;
	PreparedStatement ps = null, ps2 = null;
	String sql2 = "UPDATE documents SET data_asc=? where doc_id=?";

	DocDB docDB = DocDB.getInstance();

	while (start <= maxId)
	{
		try
		{
			db_conn = DBPool.getConnection();

			String sql = "SELECT doc_id, data FROM documents WHERE doc_id>="+start+" AND doc_id<"+(start+PAGE_SIZE);

			ps = db_conn.prepareStatement(sql);

			rs = ps.executeQuery();
			int stop = 0;
			while (rs.next())
			{
				String data = DB.getDbString(rs, "data");
				doc_id = rs.getInt("doc_id");

				DocDetails doc = docDB.getDoc(doc_id);
				EditorForm ef = new EditorForm(doc);

				data = EditorDB.getDataAsc(data, ef, true, request);

				ps2 = db_conn.prepareStatement(sql2);

				DB.setClob(ps2, 1, data);
				ps2.setInt(2, doc_id);
				ps2.executeUpdate();
				ps2.close();
				/*if(stop == 2) break;
				stop++;*/
				out.println("Doc ID: " + doc_id + " Data: "+data.length());
			}

			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
		ps = null;
			db_conn = null;

			out.println("<br/><br/> "+start+"/"+maxId);
		}
		catch (Exception ex)
		{
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
		start += PAGE_SIZE;
	}
}

//out.println(data);
%>