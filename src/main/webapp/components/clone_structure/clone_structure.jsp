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

<style type="text/css">
	label {
		text-align: right;
		width: 100%;
		padding-right: 16px;
		max-width: 380px;
	}
</style>

<div class="padding10">

	<form name="pathForm" action="/apps/clone_structure/admin/clone/" method="post" ><%=org.apache.struts.taglib.html.FormTag.renderToken(session)%>
		<table>
			<tr>
				<td><label for="srcGroupId1"><iwcm:text key="admin.clone.source_dir_id"/></label></td>
				<td>
					<div class="input-group">
						<input type="text" class="form-control" name="srcGroupId" id="srcGroupId1" size="10"  required>
						<div class="input-group-append">
							<button name="groupSelect" onclick='popup("/admin/grouptree.jsp?fcnName=setGroup", 300, 450);' class="btn btn-outline-secondary"><i class="ti ti-focus-2"></i></button>
						</div>
					<div>
				</td>
			</tr>
			<tr>
				<td><label for="destGroupId1"><iwcm:text key="admin.clone.destination_dir_id"/></label></td>
				<td>
					<div class="input-group">
						<input type="text" class="form-control" name="destGroupId" id="destGroupId1" size="10" required >
						<div class="input-group-append">
							<button name="groupSelect2" onclick='popup("/admin/grouptree.jsp?fcnName=setGroup2", 300, 450);' class="btn btn-outline-secondary"><i class="ti ti-focus-2"></i></button>
						</div>
					</div>
				</td>
			</tr>
			<tr>
				<td><label><iwcm:text key="admin.clone.keepMirroring"/></label></td>
				<td>
					<input type="checkbox" name="keepMirroring" value="true">
				</td>
			</tr>
			<tr>
				<td><label><iwcm:text key="admin.clone.keepVirtualPath"/></label></td>
				<td>
					<input type="checkbox" name="keepVirtualPath" value="true">
				</td>
			</tr>
		</table>
	</form>
</div>

<jsp:include page="/admin/layout_bottom_dialog.jsp" />