<%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*,
		sk.iway.iwcm.sync.export.ExportManager"
%><%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld"
%><%@ taglib prefix="iway" uri="/WEB-INF/iway.tld"
%><%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><iwcm:checkLogon admin="true" perms="cmp_sync"/><%

	Encoding.setResponseEnc(request, response, "text/html");

%><%@ include file="/admin/layout_top_popup.jsp" %>

<div style="position: fixed; top: 0px; width: 97%; height: 50px; background-color: white; border-bottom: 1px solid #ccc;">
	<h1><iwcm:text key="components.syncDir.creating"/></h1>

	<div id="waitDiv" style="text-align:center; width: 99%;">
		<img src="/admin/images/loading-anim.gif" alt="" />
	</div>
</div>

<div style="height: 50px;"></div>

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
<style type="text/css">
	body { padding: 10px !important; }
</style>
<%

	ExportManager export = ExportManager.create(request, session);
	export.setOut(out);
	export.exportGroup(Tools.getIntValue(Tools.getRequestParameter(request, "remoteGroupId"), -1));

%>

<h2><iwcm:text key="admin.archive.done"/></h2>

<a href="<c:out value='${zipfile}'/>"><c:out value='${zipfile}'/></a>

<br/><br/>
<script type='text/javascript'>window.scrollBy(0,1000);</script>

<%@ include file="/admin/layout_bottom_popup.jsp" %>
