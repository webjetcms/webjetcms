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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_sync"/>
<%
request.setAttribute("dialogTitleKey", "components.import_web_pages.xml.dialogTitle");
request.setAttribute("dialogDescKey", "components.import_web_pages.xml.dialogDesc");
request.setAttribute("cmpName", "syncDir");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%><script language="JavaScript">
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
Prop prop = Prop.getInstance(Constants.getServletContext(), request);

if (Tools.getRequestParameter(request, "sync")!=null)
{
	pageContext.include("/SyncArchive.action");
	%>
	<script type="text/javascript">
	<!--
	//window.alert("Sync dir: ${syncDir}");
	window.location.href='/components/sync/sync_dir.jsp?localGroupId=${param.localGroupId}&remoteGroupId=${param.remoteGroupId}&syncDir=${syncDir}&btnLoadData=btnLoadData';
	//-->
	</script>
	<%
}
%>
<div class="padding10">

	<iwcm:text key="components.syncDir.archive_file"/>
	<iwcm:stripForm action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.stripes.SyncArchiveActionBean" id="form">
	<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.stripes.SyncArchiveActionBean"/>
	<input type="hidden" name="localGroupId" value="${param.localGroupId}" />
	<input type="hidden" name="remoteGroupId" value="${param.remoteGroupId}" />
	<stripes:file name="archive" size="50"/>

	<input type="submit" id="sync" name="sync" value="sync"  style="display: none;"/>
	</iwcm:stripForm>
	<script type="text/javascript">
	function Ok()
	{
	document.getElementById("sync").click();
	}
	</script>

</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
