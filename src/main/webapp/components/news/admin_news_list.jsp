
<%@page import="sk.iway.iwcm.doc.groups.GroupsController"%>
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.users.SettingsAdminWebpagesTable" %>

<%@
		taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
		taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
		taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
		taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
		taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_news"/>
<%@ include file="/admin/layout_top.jsp" %>

<script>
	var link = "/apps/news/admin/";
	var qs = window.location.search;
	if (qs) {
		link += qs;
	}
	window.location.href=link;
</script>

<%@ include file="/admin/layout_bottom.jsp" %>