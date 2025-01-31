<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.Identity"%>
<%@page import="sk.iway.iwcm.LabelValueDetails"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.users.UserGroupsDB"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="java.util.ArrayList"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.HashMap,java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %><iwcm:checkLogon admin="true" perms="menuFbrowser|menuWebpages"/><%!
//POZOR toto je treba menit aj v editor_menubar.jsp - je to duplicitne
public static List<LabelValueDetails> getFieldList(String type)
{
	String values[] = Tools.getTokens(type, "|");
	List<LabelValueDetails> list = new ArrayList<LabelValueDetails>();
	if (type.startsWith("|"))
	{
		LabelValueDetails lvd = new LabelValueDetails("", "");
		list.add(lvd);
	}
	for (String value : values)
	{
		LabelValueDetails lvd = new LabelValueDetails(value, value);
		list.add(lvd);
	}
	return list;
}

//toto uz nie
public static void renderSelect(List<LabelValueDetails> list, String actualValue, JspWriter out) throws Exception
{
	for (LabelValueDetails lvd : list)
	{
	    String value = ResponseUtils.filter(lvd.getLabel());
		out.print("<option title=\"" + value + "\" value=\""+value+"\"");
		if (lvd.getValue().equals(actualValue)) out.print(" selected='selected'");
		out.println(">"+value+"</option>");
	}
}
%>
<%
Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(Constants.getServletContext(), request);

String[] files = request.getParameterValues("files");

if (files.length == 0) {
	out.print("No files");
	return;
}

List<Integer> ids = new ArrayList<>();
Map<String, DocDetails> documents = new HashMap<>();
DocDB docDB = DocDB.getInstance();
for (String file : files) {
	String url = file+".html";
	int docId = docDB.getDocIdFromURLImpl(url, null);
	DocDetails fileDoc = docDB.getDoc(docId);

	if (fileDoc != null) {
		documents.put(url, fileDoc);
		ids.add(fileDoc.getDocId());
	}
}

UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
String[] fields = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};

request.setAttribute("ids", ids);
request.setAttribute("fields", fields);
request.setAttribute("documents", documents);
request.setAttribute("files", files);
request.setAttribute("userGroupsList", userGroupsDB.getUserGroups());
%>
<link rel="stylesheet" type="text/css" href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" />
<style type="text/css">
/* v dialogovom okne by aj tak nefungovalo */
.noperms-editor_edit_perex { display: none; }
.tab-pane {padding: 20px 0 0 0;}
</style>
<form class="permissionsForm form-horizontal" name="editorForm" action="/" method="POST">
	<input type="hidden" class="ids" name="ids" value="<%= Tools.join((ArrayList) ids, ",") %>" />

	<div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
		<c:forEach items="${documents}" var="entry" varStatus="status">
			<% DocDetails document = ((Entry<String, DocDetails>) pageContext.getAttribute("entry")).getValue(); %>
			<c:set var="document" value="${entry.value}" />
			<c:set var="id" value="${document.docId}" />

			<div class="panel panel-default">
				<div class="panel-heading" role="tab" id="headingOne">
					<h4 class="panel-title">
						<a role="button" data-bs-toggle="collapse" data-parent="#accordion${id}" href="#accordion${id}" aria-expanded="false" aria-controls="accordion${id}">
							${entry.key}
						</a>
					</h4>
				</div>
				<div id="accordion${id}" class="panel-collapse collapse" role="tabpanel" aria-labelledby="">
					<div class="panel-body">
						<input type="hidden" class="groupId" name="groupId[${id}]" value="${document.groupId}"/>
						<input type="hidden" class="fileName" name="fileName[${id}]" value="${entry.key}"/>

						<ul class="nav nav-tabs" role="tablist">
						    <li role="presentation" class="nav-item">
								<a class="active" href="#permissions${id}" aria-controls="permissions${id}" role="tab" data-bs-toggle="tab">Klasifikačné metadáta</a>
							</li>
							<li role="presentation" class="nav-item">
								<a href="#accesible${id}" aria-controls="accesible${id}" role="tab" data-bs-toggle="tab"><iwcm:text key="editor.group.permissions"/></a>
							</li>
						</ul>

						<!-- Tab panes -->
						<div class="tab-content">
							<div role="tabpanel" class="tab-pane active" id="permissions${id}">
								<table class="table">
									<%
									for (String pismeno : fields)
									{
										String label = prop.getText("editor.field_"+pismeno.toLowerCase());
										String type = prop.getText("editor.field_" + pismeno.toLowerCase() + ".type");
									%>
										<tr<%if ("none".equals(type)) out.print(" style='display: none;'"); %> id="fieldTr<%=pismeno%>">
											<td><%= label %>: </td>
											<td id="fieldTd<%=pismeno%>">
												<% if (type.indexOf("|")!=-1)
												   {
												%>
													<select id="fieldInput<%=pismeno%>" name="<%=("field"+pismeno)%>[${id}]">
														<% renderSelect(getFieldList(type), (String)BeanUtils.getProperty(document, "field"+pismeno), out); %>
													</select>
												<% } else { %>
													<input id="fieldInput<%=pismeno%>" type="text" name="<%=("field"+pismeno)%>[${id}]" size="50" maxlength="255"<%
													String value = (String)BeanUtils.getProperty(document, "field"+pismeno);
													if (Tools.isNotEmpty(value)) out.print(" value=\""+ResponseUtils.filter(value)+"\"");
													%>/>
												<% } %>
											</td>
										</tr>
									<% } %>
								</table>
							</div>
							<div role="tabpanel" class="tab-pane" id="accesible${id}">
								<table border="0" cellpadding="1" cellspacing="0" style="width: 100%;">
									<tr>
										<td style="width: 40%; padding-bottom: 10px;" valign="top"><iwcm:text key="editor.search"/>: <br /> <input type="text" name="passwordProtectedSearch[${id}]" onkeyup="perexGroupSearchChange(this, 'passwordProtected[${id}]', 'passwordProtected[${id}]')" style="width: 100%;"></td>
										<td>&nbsp;</td>
										<td style="width: 40%;" valign="top"><iwcm:text key="editor.passwordProtected.selectedGroups"/>:</td>
									</tr>
									<tr>
										<td valign="top">
											<select name="passwordProtectedFake[${id}]Left" multiple="true" size="12" style="width: 100%; font-size: 12px;"></select>
										</td>
										<td style="text-align: center;">
											<input type="button" name="bPerexGroupsMoveLeft" onClick="moveLeft(this.form, 'passwordProtected[${id}]', 'passwordProtectedFake[${id}]');" title="<iwcm:text key="editor.perex_group.unselect"/>" value="<-" class="btn btn-default">
											<input type="button" name="bPerexGroupsMoveRight" onClick="moveRight(this.form, 'passwordProtected[${id}]', 'passwordProtectedFake[${id}]');" title="<iwcm:text key="editor.perex_group.select"/>" value="->" class="btn btn-default">
										</td>
										<td valign="top">
											<select name="passwordProtectedFake[${id}]Right" multiple="true" size="12" style="width: 100%; font-size: 12px;"></select>
										</td>
									</tr>
								</table>

								<select class="passwordProtected" name="passwordProtected[${id}]" multiple="true" size="6" style="display: none;">
									<c:forEach items="${userGroupsList}" var="userGroup">
										<option title="${userGroup.userGroupName}<c:if test="${not empty userGroup.userGroupComment}"> - ${userGroup.userGroupComment}</c:if>" value="${userGroup.userGroupId}" <c:if test="${document.isInUserGroup(userGroup.userGroupId)}"> selected="selected"</c:if>>${userGroup.userGroupName}<c:if test="${not empty userGroup.userGroupComment}"> - ${userGroup.userGroupComment}</c:if></option>
									</c:forEach>
								</select>
							</div>
						</div>
					</div>
				</div>
			</div>
		</c:forEach>
	</div>
</form>
<script src="//code.jquery.com/jquery-1.12.4.min.js" integrity="sha256-ZosEbRLbNQzLpnKIkEdrPv7lOy9C27hHQ+Xp8a4MxAQ=" crossorigin="anonymous"></script>
<script type="text/javascript" src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<script type="text/javascript" src="/admin/scripts/common.jsp?v=1485941358223&lng=sk"></script>
<script type="text/javascript">
	$(function(){
		if ($('.panel-collapse').length == 1) {
			$('.panel-collapse').addClass('in');
		}

		<c:forEach var="id" items="${ids}">
			initializeDisabledItems(document.editorForm, 'passwordProtected[${id}]', 'passwordProtectedFake[${id}]');
		</c:forEach>

		$('#btnOk').closest('table').hide();
	});


	function Ok() {
		var data = $('.permissionsForm').serializeArray();

		checkForm(function(){
			$.ajax({
				url: '/components/elfinder/metadata/admin_metadata_save_ajax.jsp',
				data: data,
				success: function(response) {
					var errors = [];

					$.each(response, function(i, v){
						if (!v.success) {
							errors.push(v.errors);
						}
					});

					if (errors.length > 0) {
						alert(response.error);
						return;
					}

					if (typeof parent.fbrowserDone != "undefined") {
						parent.fbrowserDone(false);
					}
				},
				error: function(a1, a2, a3){
					console.log(a1);
					console.log(a2);
					console.log(a3);
				}
			});
		});
	}

	function checkForm(success) {
		clearErrors();
		<%--<%= Tools.isNotEmpty(user.getSignature()) && user.getSignature().indexOf("Interway")!=-1 ? "return true;" : "" %>--%>

		//preskocit ak ma pravo pre 'WEB Stranky - Neobmedzene nahravanie suborov'
		<%--<%= user != null && user.isDisabledItem("editor_unlimited_upload") == false ? "return true;" : "" %>--%>

		var form = $('.permissionsForm');
		var errors = [];
		var errorElements = [];

		$('.panel-group .panel').each(function(){
			var panel = $(this);
			var fileName = panel.find('.fileName').val();

			if (fileName &&
				fileName.indexOf("/files")!=0 &&
				panel.find('.disabledItemsRight').val() &&
				panel.find('.disabledItemsRight').val().length == 0){
					errors.push("<iwcm:text key="vub.editor.zvolte_klucove_slova"/>");
			}
		});

		if (errors.length > 0) {
			window.alert(errors.join("\n"));
			console.log(errorElements);
			$.each(errorElements, function(){
				$(this).closest('.form-group').addClass('has-error');
			});
		}
		else {
			clearErrors();
			success();
		}
	}

	function clearErrors()
	{
		var form = $('.permissionsForm');
		form.find('.has-error').removeClass('has-error');
	}

	function vubMamZvolenuNejakuSkupinu(panel) {
		return panel.find('.passwordProtected option:selected').length > 0;
	}

	function vubIsNumber(num) {
		return !isNaN(parseFloat(num)) && isFinite(num) && num > 0 && num < 9999;
	}
</script>
<%@ include file="/admin/layout_bottom_dialog.jsp"%>