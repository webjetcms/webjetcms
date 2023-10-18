<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" type="text/css" href="/components/_common/cleditor/jquery.cleditor.css" />
<script type="text/javascript" src="/components/_common/javascript/jquery.browser.js"></script>
<script type="text/javascript" src="/components/_common/cleditor/jquery.cleditor.js"></script>
<script type="text/javascript" src="/components/_common/cleditor/jquery.cleditor.icon.js"></script>
<script type="text/javascript">
	$.cleditor.messages = []

	$.cleditor.messages['selection_required'] = '<iwcm:text key="components.cleditor.messages.selection_required" />';
	$.cleditor.messages['cutcopypaste'] = '<iwcm:text key="components.cleditor.messages.cutcopypaste_disabled_by_browser" />';
	$.cleditor.messages['urlenter'] = '<iwcm:text key="components.cleditor.messages.urlenter" />';
	$.cleditor.messages['submit'] = '<iwcm:text key="components.cleditor.messages.submit" />';
	$.cleditor.messages['pastetext'] = '<iwcm:text key="components.cleditor.messages.pastetext" />';

	$.cleditor.title = []
	$.cleditor.title['bold'] = '<iwcm:text key="components.cleditor.title.bold" />';


</script>