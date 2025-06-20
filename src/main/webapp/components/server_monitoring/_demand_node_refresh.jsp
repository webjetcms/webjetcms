<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.system.monitoring.MonitoringDB"%>
<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<%@page import="sk.iway.iwcm.system.cluster.ClusterDB"%>
<iwcm:menu name="cmp_server_monitoring">
<%
	String node = Tools.getRequestParameter(request, "node");
	ClusterDB.addRefreshClusterMonitoring(node, MonitoringDB.class);
%>
<b><iwcm:text key="components.monitoring.wait_for_refresh" param1="${param.node}" /></b>
</iwcm:menu>
