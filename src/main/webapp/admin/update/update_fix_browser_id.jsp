<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>

<%@page import="sk.iway.iwcm.system.UpdateDatabase"%><script language="JavaScript">
var helpLink = "";
</script>
<h1>Fixing browser id, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>

<%
out.flush();
if ("fix".equals(request.getParameter("act"))) UpdateDatabase.fixBrowserId(true);
%>


<%@ include file="/admin/layout_bottom.jsp" %>