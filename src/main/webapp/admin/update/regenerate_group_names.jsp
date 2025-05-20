<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.DBPool,sk.iway.iwcm.Tools" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>
<%@page import="sk.iway.iwcm.common.DocTools"%>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<script language="JavaScript">
var helpLink = "";
</script>
<h1>Pregeneruje zoznam URL adries adresarov podla nazvu adresara, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<%
//tento script pregeneruje zoznam URL adries adresarov podla nazvu adresara
//ak sa niekde pouzivali povodne EN nazvy a kvoli SEO sa to chce optimalizovat
if ("fix".equals(request.getParameter("act"))) {
   Connection db_conn = null;
   PreparedStatement ps = null;
   ResultSet rs = null;
   try
   {
      out.println("<table border='1'><tr><th>id</th><th>uld url</th><th>title</th><th>new url</th></tr>");

      db_conn = DBPool.getConnection();
      ps = db_conn.prepareStatement("SELECT * FROM groups ORDER BY group_name", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = ps.executeQuery();
      while (rs.next())
      {
         String urlDirName = rs.getString("url_dir_name");
         if (Tools.isEmpty(urlDirName) || "-".equals(urlDirName) || urlDirName.startsWith("/")) continue;

         String groupName = rs.getString("group_name");

         String newUrlDirName = groupName;
         newUrlDirName = sk.iway.iwcm.DB.internationalToEnglish(newUrlDirName).toLowerCase();
         newUrlDirName = DocTools.removeCharsDir(newUrlDirName, true);

         out.println("<tr><td>"+rs.getInt("group_id")+"</td><td>"+Tools.escapeHtml(urlDirName)+"</td><td>"+Tools.escapeHtml(groupName)+"</td><td>"+Tools.escapeHtml(newUrlDirName)+"</td></tr>\n");

         if ("true".equals(Tools.getRequestParameter(request, "set")))
         {
            rs.updateString("url_dir_name", newUrlDirName);
            rs.updateRow();
         }
      }
      rs.close();
      ps.close();
      db_conn.close();
      rs = null;
      ps = null;
      db_conn = null;

      out.println("</table>");

      if ("true".equals(Tools.getRequestParameter(request, "set")))
      {
         GroupsDB.getInstance(true);
      }
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
}
%>

<%@ include file="/admin/layout_bottom.jsp" %>
