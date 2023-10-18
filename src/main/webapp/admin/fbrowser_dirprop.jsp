<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="org.apache.commons.codec.binary.Base64"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.filebrowser.*,java.io.*,sk.iway.iwcm.users.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ include file="/admin/layout_top_popup.jsp" %>

<script type="text/javascript">
<logic:present parameter="refresh">
	refresh();
</logic:present>

function refresh() {
	if (window.parent)
	{
		if (typeof window.parent.fbrowserDone == 'function') {
			window.parent.fbrowserDone();
		}
		else {
			window.parent.location.href="/admin/fbrowser.browse.do?rnd=<%=Tools.getNow()%>";
		}
	}
}

function viewUsage()
{
	window.location.href="/admin/fbrowser/fileprop/?prop=yes&usage=true&dir=<%=Tools.getRequestParameter(request, "dir")%>&file=";
}

function reindexDir()
{
	document.reindexForm.dir.value = getDir();
	document.reindexForm.submit();
}

</script>
<style type="text/css">
.loginDocIdBox,
.buttonsBox {display: none;}
.loginDocIdBox {margin-top: 10px;}
fieldset {margin-bottom: 10px;}
</style>

<html>
    <head>
    </head>
	<body>
		<logic:present parameter="saveok">
			<script type="text/javascript">
				//needs wj2023 window.top.WJ.notifySuccess("", "<iwcm:text key="components.page_update_info.save_ok"/>", 10);
			</script>
			<div class="alert alert-success" role="alert">
				<iwcm:text key="components.page_update_info.save_ok"/>
			</div>
		</logic:present>
		<form:form method="post" modelAttribute="fbrowserEditForm" action="/admin/fbrowser/dirprop/" id="fbrowserEditForm" name="fbrowserEditForm" style="margin:0px; padding: 0px;">
			<div id="userGroupsList1">
				<fieldset>
					<table>
						<tr>
							<td><form:label path="dir">
								<iwcm:text key="fbrowse.dirprop.dir_name"/>: </form:label>
							</td>
							<td><form:input path="dir" disabled="true"/></td>
							<td><form:input path="dir" hidden="hidden"/></td>
						</tr>
						<iwcm:menu name="fileIndexer">
							<tr>
								<td colspan=2><form:checkbox path="indexFulltext"/>
									<iwcm:text key="fbrowse.dirprop.index_fulltext"/>
								</td>
							</tr>
						</iwcm:menu>
					</table>
				</fieldset>

				<fieldset>
					<h2><iwcm:text key="editor.group.permissions"/></h2>
					<%String userGroupId;%>
					<logic:iterate name="userGroupsList" id="ugl" type="sk.iway.iwcm.users.UserGroupDetails">
					<%
						userGroupId = "" + ugl.getUserGroupId();
						if (ugl.getUserGroupType()==UserGroupDetails.TYPE_PERMS)
						{
					%>
					<label>
						<form:checkbox path="passwordProtected" value="<%=userGroupId%>"/>
						<jsp:getProperty name="ugl" property="userGroupName"/>
					</label>
					<br>
					<% } %>
					</logic:iterate>
					<div class="loginDocIdBox">
						<div class="row">
							<div class="col-xs-12">
								<label for="logonDocId"><iwcm:text key="groupedit.logonPageDocId"/>:</label>
							</div>
							<div class="col-xs-3">
								<form:input styleClass="form-control" styleId="logonDocId" path="logonDocId" maxlength="10" size="4"/>
							</div>
							<div class="col-xs-3">
								<input type="button" class="btn btn-default" value="<iwcm:text key="groupedit.select"/>" name="bSelLogonPae" onClick='popup("/admin/user_adddoc.jsp", 450, 340);'>
							</div>
						</div>
					</div>
				</fieldset>

				<div class="buttonsBox">
					<input type="submit" name="bSubmit" class="btn btn-primary bSubmit"><iwcm:text key="fbrowse.dirprop.save_button"/></button>

					<form:hidden path="origDir"/>
				</div>
			</div>
		</form:form>
	</body>
</html>

<form action="/admin/fbrowser/fulltext-index/index/" name="reindexForm" style="display: none">
	<input type="hidden" name="dir" value="">
</form>

<script>

function getDir() {
	return '<%=Tools.getRequestParameter(request, "dir")%>';
}
</script>

<%@ include file="/admin/layout_bottom_popup.jsp" %>
