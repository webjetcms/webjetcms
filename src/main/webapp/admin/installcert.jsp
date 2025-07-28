<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/admin/layout_top.jsp" %>
<%@page import="sk.iway.iwcm.system.InstallCert"%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<script language="JavaScript">
var helpLink = "";
</script>
<%
String host = Tools.getRequestParameter(request, "host");
int port = Tools.getIntValue(Tools.getRequestParameter(request, "port"), 443);
String passphrase = Tools.getRequestParameter(request, "passphrase");
String line = Tools.getRequestParameter(request, "line");
if (Tools.isNotEmpty(host) && Tools.isNotEmpty(passphrase))
{
	String log = "";
	try
	{
	log = InstallCert.install(host, port, passphrase.toCharArray(), line);
	} catch (Exception ex) {}
	log = ResponseUtils.filter(log);
	log = Tools.replace(log, "\n", "<br/>");
	out.println(log);
}

if (host==null) host = "";
if (passphrase==null) passphrase = "";
if (line==null) line = "q";
%>

<form action="installcert.jsp" method="post">
	<table border="0">
		<tr>
			<td>host:</td>
			<td><input type="text" name="host" value="<%=host%>"/></td>
		</tr>
		<tr>
			<td>port:</td>
			<td><input type="text" name="port" value="<%=port%>"/></td>
		</tr>
		<tr>
			<td>passphrase:</td>
			<td><input type="text" name="passphrase" value="<%=passphrase%>"/></td>
		</tr>
		<tr>
			<td>line:</td>
			<td><input type="text" name="line" value="<%=line%>"/></td>
		</tr>
		<tr>
			<td></td>
			<td><input type="submit"/></td>
		</tr>
	</table>
</form>

<%@ include file="/admin/layout_bottom.jsp" %>
