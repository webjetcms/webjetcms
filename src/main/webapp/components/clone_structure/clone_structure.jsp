<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.i18n.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<iwcm:menu notName="cmp_clone_structure">
<%
	response.sendRedirect("/admin/403.jsp");
	if (1==1) return;
%>
</iwcm:menu>

<%
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
	request.setAttribute("iconLink", "");
	request.setAttribute("dialogTitle", prop.getText("admin.clone.dialogTitle"));
	request.setAttribute("dialogDesc", prop.getText("admin.clone.dialogDesc")+ ".");
%>

<jsp:include page="/admin/layout_top_dialog.jsp" />

<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>
<script type="text/javascript">
	function setGroup(returnValue)
	{
		if(returnValue.length > 15)
		{
			var srcGroupId = returnValue.substr(0,15);
			pathForm.srcGroupId.value = srcGroupId;
		}
	}

	function setGroup2(returnValue)
	{
		if(returnValue.length > 15)
		{
			var destGroupId = returnValue.substr(0,15);
			pathForm.destGroupId.value = destGroupId;
		}
	}

	function Ok()
	{
	   document.pathForm.submit();
	}
</script>

<div class="padding10">
	<form name="pathForm" action="/admin/clone.do" method="post" ><%=org.apache.struts.taglib.html.FormTag.renderToken(session)%>
		<table>
			<tr>
				<td><label for="srcGroupId1"><b><iwcm:text key="admin.clone.source_dir_id"/></b></label></td>
				<td>
					<input type="text" name="srcGroupId" id="srcGroupId1" size="3" >&nbsp;
					<input type="button" name="groupSelect" value="<iwcm:text key="button.select"/>" onclick='popup("/admin/grouptree.jsp?fcnName=setGroup", 300, 450);' class="button50" />
				</td>
			</tr>
			<tr>
				<td><label for="srcTempLangId"><iwcm:text key="admin.clone.source_lang_name"/></label></td>
				<td><input type="text" name="srcTempLang" id="srcTempLangId"></td>
			</tr>
		 	<tr>
				<td>&nbsp;</td>
			</tr>
			<tr>
				<td><label for="destGroupId1"><b><iwcm:text key="admin.clone.destination_dir_id"/></b></label></td>
				<td>
					<input type="text" name="destGroupId" id="destGroupId1" size="3">&nbsp;
					<input type="button" name="groupSelect2" value="<iwcm:text key="button.select"/>" onclick='popup("/admin/grouptree.jsp?fcnName=setGroup2", 300, 450);' class="button50" />
				</td>
			</tr>

			<tr>
				<td><label for="destTempLangId"><iwcm:text key="admin.clone.destination_lang_name"/></label></td>
			 	<td><input type="text" name="destTempLang" id="destTempLangId" /></td>
			</tr>
		</table>
	</form>

	<iwcm:text key="components.clone.clone_lng_desc"/>
</div>

<jsp:include page="/admin/layout_bottom_dialog.jsp" />