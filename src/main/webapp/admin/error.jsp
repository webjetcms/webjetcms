<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<script type="text/javascript">
function Ok()
{
	window.close();	
}
</script>

<div class="tab-pane toggle_content" id="tab-pane-1" style="height: 300px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">

<h1><iwcm:text key="components.chat.error"/></h1>

<span class="error">
<iway:request name="err_msg"/>
</span>

	</div>
</div>
