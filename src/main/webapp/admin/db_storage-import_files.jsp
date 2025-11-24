<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Constants" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/admin/layout_top.jsp" %>

<%@page import="sk.iway.iwcm.PathFilter"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.io.IwcmFsDB, java.io.File"%>
<%@ page import="sk.iway.iwcm.Tools" %>
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
<%
Prop prop = Prop.getInstance(request);

if (Tools.getRequestParameter(request, "virtual_path")!=null && Constants.getBoolean("iwfs_useDB")==true)
{
	File f=new File(sk.iway.iwcm.Tools.getRealPath(Tools.getRequestParameter(request, "virtual_path")));
	IwcmFsDB.writeDirectoryToDB(f,true);
	out.println("Import dokončený.");
	System.gc();
}

%>
<H3>Import suborov z disku do db storage</H3>

<br><br>
<form action="<%=PathFilter.getOrigPath(request)%>">
<input type="text" value="/images/" name="virtual_path" >
<input type="submit" value="Importovať" >
</form>


<%@ include file="/admin/layout_bottom.jsp" %>
