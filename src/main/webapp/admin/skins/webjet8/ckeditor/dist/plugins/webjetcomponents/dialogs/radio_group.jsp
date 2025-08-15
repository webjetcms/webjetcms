<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%

request.setAttribute("cmpName", "form");
request.setAttribute("titleKey", "components.forms.radio_group.title");
request.setAttribute("descKey", "");

int matrixRows = Tools.getIntValue(Tools.getRequestParameter(request, "matrixRows"), 5);
%>
<jsp:include page="/components/top.jsp"/>
<style type="text/css">
	.checkbox label {margin-left: 20px;}
	.checkbox label input {margin-top: 2px;}
</style>
<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs" style="background-color:transparent;">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="editor.form.rbgroup.settings"/></a></li>
		<li class=""><a href="#" onclick="showHideTab('2');" id="tabLink2"><iwcm:text key="components.gallery.preview"/></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<form class="settingsForm" name="settingsForm">
			<div class="col-sm-12" style="padding-top: 15px;">
				<div class="form-group">
					<label for="groupIds" class="control-label"><iwcm:text key="components.forms.radio_group.size"/>:</label>

					<div class="input-group">
						<input style="margin-right: 10px;" type="text" name="matrixRows" value="<%=matrixRows %>" size="3">
						<label style="margin-right: 10px;"><input type="radio" name="orientation" value="rows" <% if ("columns".equals(Tools.getRequestParameter(request, "orientation"))==false) out.print("checked='checked'"); %> id="orientationRows"/> <iwcm:text key="editor.form.rbgroup.rows"/></label>
						<label style="margin-right: 10px;"><input type="radio" name="orientation" value="columns" <% if ("columns".equals(Tools.getRequestParameter(request, "orientation"))) out.print("checked='checked'"); %>/> <iwcm:text key="editor.form.rbmatrix.cols"/></label>
						<span id="helpBlock" class="help-block"><iwcm:text key="editor.form.rbgroup.sendNotice"/></span>
					</div>
				</div>

				<div class="form-group">
					<label for="groupIds" class="control-label"><iwcm:text key="editor.form.rbgroup.inputType"/></label>
					<div class="input-group">
						<label style="margin-right: 10px;"><input type="radio" name="inputType" value="radio" <% if ("checkbox".equals(Tools.getRequestParameter(request, "inputType"))==false) out.print("checked='checked'"); %> id="inputTypeRadio"/> <iwcm:text key="editor.form.rbgroup.inputType.radio"/></label>
						<label style="margin-right: 10px;"><input type="radio" name="inputType" value="checkbox" <% if ("checkbox".equals(Tools.getRequestParameter(request, "inputType"))) out.print("checked='checked'"); %>/> <iwcm:text key="editor.form.rbgroup.inputType.checkbox"/></label>
					</div>
				</div>

				<div class="row">
					<div class="col-sm-12" style="margin-bottom: 15px;">
						<div class="checkbox">
					    	<label>
					    		<input type="checkbox" name="required" value="yes" checked='checked'> <iwcm:text key="editor.form.tf.required"/>
					    	</label>
						</div>
					</div>
				</div>


				<% if ("checkbox".equals(Tools.getRequestParameter(request, "inputType"))) { %>
				<div class="form-group">
					<div class="col-sm-6">
						<div class="input-group">
							<iwcm:text key="editor.form.rbgroup.required_selected_values"/>: <input type="text" name="countCondFrom" value="" size="3" maxlength="3"/> - <input type="text" name="countCondTo" value="" size="3" maxlength="3"/>
						</div>
					</div>
				</div>
				<% } %>

				<div class="row">
					<div class="col-sm-12">
						<input type="submit" value="<iwcm:text key="editor.form.rbmatrix.set"/>" class="button"/>
					</div>
				</div>

			</div>
		</form>
	</div>

	<div class="tab-page" id="tabMenu2" style="max-width: 780px; overflow: auto; padding-right: 0px;">
		<form class="rbmatrixForm" name="rbmatrixForm">
			<div class="col-sm-12" style="padding-top: 15px;">
				<div class="form-group">
					<label for="groupIds" class="control-label"><iwcm:text key="editor.form.rbgroup.fieldNamePefix"/>:</label>
					<div class="input-group">
						<input type="text" name="prefix" value="Otázka 1" size="40"/>
						<span id="helpBlock" class="help-block"><iwcm:text key="editor.form.rbgroup.fieldNamePrefixNotice"/></span>
					</div>
				</div>

				<div class="form-group">
					<label for="groupIds" class="control-label"></label>
					<div class="table-responsive">
							<table class="table">
							<%
							if ("columns".equals(Tools.getRequestParameter(request, "orientation")))
							{
								%><tr><%
								for (int row=1; row<=matrixRows; row++) { %>
									<td><input type="text" id="rowHeader<%=row %>" name="rowHeader<%=row %>" value="<%=row %>" size="10" style='text-align: center;'/></td>
								<% }%>
								</tr>
								<tr>
								<%
								for (int row=1; row<=matrixRows; row++) { %>
									<td style="text-align: center;">
										<% if ("checkbox".equals(Tools.getRequestParameter(request, "inputType"))) { %>
											<input type="checkbox" id="radio_<%=row %>" name="radio_<%=row %>" value="<%=row %>"/>
										<% } else { %>
											<input type="radio" id="radio_<%=row %>" name="radio_<%=row %>" value="<%=row %>"/>
										<% } %>
									</td>
								<%
								}
								%></tr><%
							}
							else
							{
								for (int row=1; row<=matrixRows; row++) { %>
								<tr>
									<td style="text-align: center;">
										<% if ("checkbox".equals(Tools.getRequestParameter(request, "inputType"))) { %>
											<input type="checkbox" id="radio_<%=row %>" name="radio_<%=row %>" value="<%=row %>"/>
										<% } else { %>
											<input type="radio" id="radio_<%=row %>" name="radio_<%=row %>" value="<%=row %>"/>
										<% } %>
									</td>
									<td><input type="text" id="rowHeader<%=row %>" name="rowHeader<%=row %>" value="<iwcm:text key="editor.form.rbgroup.name"/> <%=row %>" size="40"/></td>
								</tr>
								<% }
							}
							%>
						</table>
					</div>
				</div>
			</div>
		</form>
	</div>

</div>

<script type="text/javascript" src="/components/form/check_form.js"></script>
<script type="text/javascript">
var htmlCode = '';

$(function(){
	if (window.location.search != '') {
		showHideTab('2');
	}
});

function OkClick()
{
	//vygeneruj HTML kod
	var f = document.rbmatrixForm;
	var prefix = removeChars(internationalToEnglish(f.prefix.value));

	htmlCode = "<table id='"+prefix+"_table'>\n";

	var rbName = removeChars(internationalToEnglish(prefix+"_rb"));

	var addClass = "";
	if (document.settingsForm.required.checked) addClass="required";

	if (document.settingsForm.countCondFrom && document.settingsForm.countCondFrom.value!="" && document.settingsForm.countCondTo.value!="")
	{
		addClass+=" countCond-"+document.settingsForm.countCondFrom.value+"-"+document.settingsForm.countCondTo.value;
	}

	if (addClass!="") addClass=" class='"+addClass+"'";

	var labelClass = "formCheckbox";
	if (document.getElementById("inputTypeRadio").checked) labelClass = "formRadio";

	if (document.getElementById("orientationRows").checked)
	{
		for (row=1; row<=<%=matrixRows%>; row++)
		{
			var rbId = rbName + "_" + removeChars(internationalToEnglish(document.getElementById("rowHeader"+row).value));

			htmlCode += "<tr>\n";
			htmlCode += "<td style='text-align: center;'>";
			if (document.getElementById("inputTypeRadio").checked)
			{
				htmlCode += "<input type='radio' name='"+rbName+"' id='"+rbId+"' value='"+document.getElementById("rowHeader"+row).value+"'"+addClass+"/>";
			}
			else
			{
				rbName = removeChars(internationalToEnglish(prefix+"_cb"));
				htmlCode += "<input type='checkbox' name='"+rbName+"' id='"+rbId+"' value='"+document.getElementById("rowHeader"+row).value+"'"+addClass+"/>";
			}
			htmlCode += "</td>\n";
			htmlCode += "<td><label for='"+rbId+"' class='"+labelClass+"'>"+document.getElementById("rowHeader"+row).value+"</label></td>\n";
			htmlCode += "\<tr>\n";
		}
	}
	else
	{
		htmlCode += "<tr>\n";
		for (row=1; row<=<%=matrixRows%>; row++)
		{
			if (document.getElementById("inputTypeRadio").checked==false) rbName = removeChars(internationalToEnglish(prefix+"_cb"));
			var rbId = rbName + "_" + removeChars(internationalToEnglish(document.getElementById("rowHeader"+row).value));

			htmlCode += "<td><label for='"+rbId+"' class='"+labelClass+"'>"+document.getElementById("rowHeader"+row).value+"</label></td>\n";
		}
		htmlCode += "\<tr>\n";
		htmlCode += "<tr>\n";
		for (row=1; row<=<%=matrixRows%>; row++)
		{
			htmlCode += "<td style='text-align: center;'>";
			if (document.getElementById("inputTypeRadio").checked)
			{
				var rbId = rbName + "_" + removeChars(internationalToEnglish(document.getElementById("rowHeader"+row).value));
				htmlCode += "<input type='radio' name='"+rbName+"' id='"+rbId+"' value='"+document.getElementById("rowHeader"+row).value+"'"+addClass+"/>";
			}
			else
			{
				rbName = removeChars(internationalToEnglish(prefix+"_cb"));
				var rbId = rbName + "_" + removeChars(internationalToEnglish(document.getElementById("rowHeader"+row).value));
				htmlCode += "<input type='checkbox' name='"+rbName+"' id='"+rbId+"' value='"+document.getElementById("rowHeader"+row).value+"'"+addClass+"/>";
			}
			htmlCode += "</td>\n";
		}
		htmlCode += "\<tr>\n";
	}
	htmlCode += "</table>";

	//window.alert(htmlCode);

	//FCK.InsertHtml(htmlCode);
	//FCK.WJToogleBordersCommand_thisRef.RefreshBorders();

	return (true);
}

</script>
<jsp:include page="/components/bottom.jsp"/>
