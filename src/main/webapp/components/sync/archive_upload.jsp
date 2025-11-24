<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_sync"/>
<%
request.setAttribute("dialogTitleKey", "components.import_web_pages.xml.dialogTitle");
request.setAttribute("dialogDescKey", "components.import_web_pages.xml.dialogDesc");
request.setAttribute("cmpName", "syncDir");
request.setAttribute("disableAutoResize", "true");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%><script>
if (window.name && window.name=="componentIframe")
{
	document.write("<LINK rel='stylesheet' href='/components/iframe.css'>");
}
else
{
	document.write("<LINK rel='stylesheet' href='/admin/css/style.css'>");
}
var helpLink = "";

//resize window to 70% to window size
var newWidth = Math.max(window.screen.width * 0.9, 800);
var newHeight = Math.max(window.screen.height * 0.7, 600);
if (newWidth > 2000) newWidth = 2000;
window.resizeTo(newWidth, newHeight);

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
	window.location.href='/components/sync/sync_dir.jsp?localGroupId=<%=Tools.getParameterNotNull(request, "localGroupId")%>&remoteGroupId=<%=Tools.getParameterNotNull(request, "remoteGroupId")%>&syncDir=${syncDir}&compareBy=${compareBy}&btnLoadData=btnLoadData';
	//-->
	</script>
	<%
}
%>
<div class="padding10">

	<iwcm:stripForm action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.stripes.SyncArchiveActionBean" id="form" class="row me-3">
		<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.stripes.SyncArchiveActionBean"/>
		<input type="hidden" name="localGroupId" value="<%=Tools.getParameterNotNull(request, "localGroupId")%>" />
		<input type="hidden" name="remoteGroupId" value="<%=Tools.getParameterNotNull(request, "remoteGroupId")%>" />

		<div class="mb-3">
			<label class="form-label"><iwcm:text key="components.syncDir.archive_file"/></label>
			<stripes:file name="archive" size="50" class="form-control"/>
		</div>

		<div class="mb-3">
			<label class="form-label"><iwcm:text key="components.syncDirAction.comapre_by.title"/></label>
			<select name="compareBy" class="form-select">
				<option value="nameOrUrl"><iwcm:text key="components.syncDirAction.comapre_by.name_or_url"/></option>
				<option value="url"><iwcm:text key="components.syncDirAction.comapre_by.url"/></option>
				<option value="none"><iwcm:text key="components.syncDirAction.comapre_by.nothing"/></option>
				<option value="fieldA"><iwcm:text key="templates.temps-list.object-a"/></option>
				<option value="fieldB"><iwcm:text key="templates.temps-list.object-b"/></option>
				<option value="fieldC"><iwcm:text key="templates.temps-list.object-c"/></option>
			</select>
		</div>

		<input type="submit" id="sync" name="sync" value="sync"  style="display: none;"/>
	</iwcm:stripForm>

	<div id="proccesingMsg" hidden>
		<h2 style='color:green;'><iwcm:text key="components.syncDir.processing_file"/></h2>
	</div>

	<script type="text/javascript">
	function Ok()
	{
		document.getElementById("proccesingMsg").hidden = false;
		document.getElementById("sync").click();
	}
	</script>

</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
