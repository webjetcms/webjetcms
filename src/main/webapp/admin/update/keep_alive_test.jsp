<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/admin/layout_top.jsp" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<script language="JavaScript">
var helpLink = "";
</script>
<h1>Test drzania spojenia s admin castou, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<%
if ("fix".equals(request.getParameter("act"))) {
	int i = 0;
	for (i=0; i<60; i++)
	{
		out.println(i+"<br/>");
		out.println(Tools.formatDateTimeSeconds(Tools.getNow())+"<br/>");
		out.flush();

		try
		{
			Thread.sleep(1000);
		}
		catch (Exception ex) {}
	}
}

%>
<br/>
DONE
<br/>


<%@ include file="/admin/layout_bottom.jsp" %>