<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.i18n.*" %>

<%@page import="sk.iway.iwcm.system.translation.TranslationService"%>
<%@page import="org.json.JSONObject"%>

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

	JSONObject translationInfo = TranslationService.getTranslationInfo();
	String engineName;
	Long freeCharacters;
	if(translationInfo == null) {
		engineName = "---";
		freeCharacters = -1L;
	} else {
		engineName = translationInfo.getString("engineName");
		freeCharacters = translationInfo.getLong("numberOfFreeCharacters");
	}
%>

<jsp:include page="/admin/layout_top_dialog.jsp" />

<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>
<script type="text/javascript">
	let undoSyncIsRunning = false;

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

	function setGroup3(returnValue)
	{
		if(returnValue.length > 15)
		{
			var groupId = returnValue.substr(0,15);
			$("#undoSyncGroupId").val(groupId);
		}
	}

	function cancelSync() {

		$("#undo_wait").show();
		$("#undo_succ").hide();
		$("#undo_fail").hide();

		undoSyncIsRunning = true;

		$.ajax({
			url: "/apps/clone_structure/admin/cancel_sync",
			method : "POST",
			data : {
				"rootGroupId" : $("#undoSyncGroupId").val(),
			},
			success: function (response) {
				console.log("OK");
				$("#undo_wait").hide();
				$("#undo_succ").show();
				undoSyncIsRunning = false;
			},
			error: function (xhr, ajaxOptions, thrownError) {
				$("#undo_wait").hide();
				$("#undo_fail").show();
				undoSyncIsRunning = false;
				if(thrownError != undefined && thrownError != null && thrownError.length > 0) {
					alert(thrownError)
				}
			}
		});
	}

	function Ok()
	{
		if(undoSyncIsRunning == true) {
			alert('<iwcm:text key="components.clone-structure.undo-sync-running-err"/>');
			return;
		}

		if(pathForm.srcGroupId.value == "" || pathForm.destGroupId.value == "")
		{
			alert('<iwcm:text key="components.clone-structure.required-fields-err"/>');
		} else {
			document.pathForm.submit();
		}
	}
</script>

<style type="text/css">
	label {
		text-align: right;
		width: 100%;
		padding-right: 16px;
		max-width: 380px;
	}

	.custom-section {
		background: #edeff6;
		margin: 15px 0;
		padding: 20px;
		border-radius: 10px;
	}

	#btnUndoSync {
		background-color: white;
		color: #13151b;
		border-color: #868ea5;
		border-width: 1px;
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

	<div id="tranlator-info" class="custom-section">
		<%if(translationInfo != null) {%>
			<h2 style="color: #00be9f;"><iwcm:text key="components.translation_engine.found"/></h2>
			<table>
				<tr>
					<td><iwcm:text key="components.translation_engine.name"/>:</td>
					<td><b><%=engineName%></b></td>
				</tr>
				<tr>
					<td><iwcm:text key="components.translation_engine.free_characters"/>:</td>
					<td><%=freeCharacters%></td>
				</tr>
			</table>

			<%if((long) freeCharacters < 1) {%>
				<p style="color: #ff4b58"><iwcm:text key="components.translation_engine.no_free_characters.msg"/></p>
			<% } %>
		<% } else { %>
			<h2 style="color: #ff4b58"><iwcm:text key="components.translation_engine.not_found"/></h2>
			<p><iwcm:text key="components.translation_engine.not_found.msg"/></p>
		<% } %>

	</div>

	<div class="custom-section">

		<h3 style="width:100%"><iwcm:text key="components.clone-structure.undo-sync-group"/></h3>

		<div style="display: flex; padding-bottom: 5px;">
			<div class="input-group" style="max-width: 160px;">
				<input type="text" class="form-control" name="undoSyncGroupId" id="undoSyncGroupId" size="10">
				<div class="input-group-append">
					<button name="groupSelect" onclick='popup("/admin/grouptree.jsp?fcnName=setGroup3", 300, 450);' class="btn btn-outline-secondary"><i class="ti ti-focus-2"></i></button>
				</div>
			</div>
			<input id="btnUndoSync" class="btn btn-sm btn-outline-secondary" style="margin-left: 16px;" type="button" value="<iwcm:text key="components.clone-structure.undo-sync-button"/>" onclick="cancelSync();"/>
		</div>
		<div>
			<p id="undo_wait" style="width:100%; display: none;"><iwcm:text key="components.clone-structure.undo-sync-wait"/></p>
			<p id="undo_succ" style="width:100%; color: #00be9f; display: none;"><iwcm:text key="components.clone-structure.undo-sync-succesfull"/></p>
			<p id="undo_fail" style="width:100%; color: #ff4b58; display: none;"><iwcm:text key="components.clone-structure.undo-sync-fail"/></p>
		</div>
	</div>
</div>

<jsp:include page="/admin/layout_bottom_dialog.jsp" />