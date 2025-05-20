<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@page import="sk.iway.iwcm.system.ConfDB"%><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
//konverzia dat pre WebJET6
ConfDB.setName("disableRemoveAbandoned", "true");

 %>

<h1>Converting banner statistics, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<%out.flush();
if ("fix".equals(request.getParameter("act"))) {
    %>
    <jsp:include page="/components/banner/db_convert.jsp"/>
    <%
    ConfDB.setName("disableRemoveAbandoned", "false");
}
%>

<h1>CONVERSION DONE</h1>

<%@ include file="/admin/layout_bottom.jsp" %>