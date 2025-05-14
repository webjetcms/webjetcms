<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.users.*, java.util.*, sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
request.setAttribute("cmpName", "user");
UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
List userGroups = userGroupsDB.getUserGroups();
if (userGroups != null && userGroups.size() > 0)
{
	request.setAttribute("groups", userGroups);
}

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
String jspFileName = request.getParameter("jspFileName");
if(Tools.isNotEmpty(jspFileName)){
	int slash = jspFileName.lastIndexOf("/");
	int dot = jspFileName.lastIndexOf(".");

	if(slash > 0 && dot < jspFileName.length()) jspFileName = jspFileName.substring(slash+1, dot);	//ziskam len nazov komponenty bez koncovky
}
if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);


String defaultRequired = ",login,password,password2,firstName,lastName,email,";
List requiredFields = new ArrayList();
requiredFields.add("login");
requiredFields.add("password");
requiredFields.add("password2");
requiredFields.add("title");
requiredFields.add("firstName");
requiredFields.add("lastName");
requiredFields.add("company");
requiredFields.add("email");
requiredFields.add("dateOfBirth");
requiredFields.add("signature");
requiredFields.add("adress");
requiredFields.add("city");
requiredFields.add("zip");
requiredFields.add("country");
requiredFields.add("phone");
request.setAttribute("requiredFields", requiredFields);

String defaultShow = ",login,password,password2,firstName,lastName,email,sexMale,adress,city,zip,country,phone";
List showFields = new ArrayList();
showFields.add("login");
showFields.add("password");
showFields.add("password2");
showFields.add("title");
showFields.add("firstName");
showFields.add("lastName");
showFields.add("company");
showFields.add("email");
showFields.add("dateOfBirth");
showFields.add("sexMale");
showFields.add("userImage");
showFields.add("signature");
showFields.add("adress");
showFields.add("city");
showFields.add("zip");
showFields.add("country");
showFields.add("phone");
request.setAttribute("showFields", showFields);

%>
<jsp:include page="/components/top.jsp"/>

<script type="text/javascript">
var docIdSelector = 0;

function Ok()
{
	try
	{

		var options = document.textForm.field.options;
		var htmlCode = document.textForm.field.value;
		for (var i=0; i < options.length; i++)
		{
			value = options[i].value;
			if (options[i].selected)
			{
				if (value == "!INCLUDE(/components/user/newuser.jsp)!")
				{
					successDocId = document.div1Form.successDocId.value;
					groupIds = checkboxesToList(document.div1Form.userGroups);
					infoemail = document.div1Form.infoemail.value;
					htmlCode = "!INCLUDE(/components/user/newuser.jsp, groupIds="+groupIds+
						", groupIdsEditable=\""+document.div1Form.groupIdsEditable.value+"\""+
						", show="+checkboxesToList(document.div1Form.showFields)+", required="+
						checkboxesToList(document.div1Form.requiredFields)+", emailUnique="+
						document.div1Form.emailUnique.checked+", successDocId="+successDocId+
						", infoemail="+infoemail+", requireEmailVerification="+
						document.div1Form.requireEmailVerification.checked+", notAuthorizedEmailDocId="+
						document.div1Form.notAuthorizedEmailDocId.value+", loginNewUser="+
						document.div1Form.loginNewUser.checked+", useAjax="+document.div1Form.useAjax.checked+
						", useCustomFields="+document.div1Form.useCustomFields.checked+
						(!document.div1Form.useCustomFieldA.checked ? "" : ', fieldALabel="'+document.div1Form.fieldAName.value+'"')+
						(!document.div1Form.useCustomFieldB.checked ? "" : ', fieldBLabel="'+document.div1Form.fieldBName.value+'"')+
						(!document.div1Form.useCustomFieldC.checked ? "" : ', fieldCLabel="'+document.div1Form.fieldCName.value+'"')+
						(!document.div1Form.useCustomFieldD.checked ? "" : ', fieldDLabel="'+document.div1Form.fieldDName.value+'"')+
						(!document.div1Form.useCustomFieldE.checked ? "" : ', fieldELabel="'+document.div1Form.fieldEName.value+'"')+
						")!";
				}
				if(value == "!INCLUDE(/components/user/logon.jsp)!"){
					groupIds = checkboxesToList(document.div2Form.userGroups);
					htmlCode = "!INCLUDE(/components/user/logon.jsp, ";
					if(groupIds != "") {
						groupIds = groupIds.replace(/\+/g, ',');
						htmlCode += "regToUserGroups=\""+groupIds+"\", ";
					}
					htmlCode += "facebook=\""+document.div2Form.facebook.checked+
					"\", google=\""+document.div2Form.google.checked+"\"";
					if($('#facebook').is(':checked')){
						htmlCode += ", facebook_key=\""+$('#facebook_key').val()+"\", facebook_secret=\""+$('#facebook_secret').val()+"\"";
					}
					if($('#google').is(':checked')){
						htmlCode += ", google_key=\""+$('#google_key').val()+"\", google_secret=\""+$('#google_secret').val()+"\"";
					}
					htmlCode += ")!";
				}
			}
		}
	}
	catch (e)
	{
		//console.log(e);
	}

	oEditor.FCK.InsertHtml(htmlCode);
	return true ;
} // End function

if (isFck)
{
}
else
{
	resizeDialog(450, 500);
}

function showDiv()
{
	var value;
	var options = document.textForm.field.options;
	for (var i=0; i < options.length; i++)
	{
		value = options[i].value;
		if (options[i].selected)
		{
			if (value == "!INCLUDE(/components/user/newuser.jsp)!")
			{
				el = document.getElementById("div_1");
				if (el != null)	el.style.display = "block";
				el = document.getElementById("div_2");
				if (el != null)	el.style.display = "none";
			}
			else if(value == "!INCLUDE(/components/user/logon.jsp)!")
			{
				el = document.getElementById("div_2");
				if (el != null)	el.style.display = "block";
				el = document.getElementById("div_1");
				if (el != null)	el.style.display = "none";
			}
			else
			{
				el = document.getElementById("div_1");
				if (el != null)	el.style.display = "none";
				el = document.getElementById("div_2");
				if (el != null)	el.style.display = "none";
			}
		}
	}
}

function addGroupId()
{
	var value = document.div1Form.groupIds.value;
	if (value == "")
	{
		document.div1Form.groupIds.value = document.div1Form.userGroups.value;
	}
	else
	{
		document.div1Form.groupIds.value = value + "+" + document.div1Form.userGroups.value;
	}
}

var lastStandardDocIdField = null;
function setStandardDoc(docId)
{
	if (lastStandardDocIdField!=null)
	{
		lastStandardDocIdField.value = docId;
	}
}

function showHelpGroup(event)
{
	document.getElementById("groupIdsHelpDiv").style.display = "none";
	var div = document.getElementById("groupIdsHelpDiv");
	div.style.position = "absolute";
	div.style.top = (event.clientY - 60)+"px";
	div.style.left = (event.clientX - 100)+"px";
	div.style.display = 'inline';
	setTimeout('document.getElementById("groupIdsHelpDiv").style.display = "none";', 7000);
}

function showHelpGoogle(event)
{
	document.getElementById("googleHelpDiv").style.display = "none";
	var div = document.getElementById("googleHelpDiv");
	div.style.position = "absolute";
	div.style.top = (event.clientY - 60)+"px";
	div.style.left = (event.clientX - 100)+"px";
	div.style.display = 'inline';
	setTimeout('document.getElementById("googleHelpDiv").style.display = "none";', 7000);
}

function showHelpFacebook(event)
{
	document.getElementById("facebookHelpDiv").style.display = "none";
	var div = document.getElementById("facebookHelpDiv");
	div.style.position = "absolute";
	div.style.top = (event.clientY - 60)+"px";
	div.style.left = (event.clientX - 100)+"px";
	div.style.display = 'inline';
	setTimeout('document.getElementById("facebookHelpDiv").style.display = "none";', 7000);
}

function showFacebook(){
	if($('#facebook').is(':checked')){
		$('#div_2_1').show("slow");
	} else {
		$('#div_2_1').hide("slow");
	}
}

function showGoogle(){
	if($('#google').is(':checked')){
		$('#div_2_2').show("slow");
	} else {
		$('#div_2_2').hide("slow");
	}
}

function showCustomFields(){
	document.getElementById('customFieldsDiv').style.display = document.div1Form.useCustomFields.checked ? 'block' :'none';
}

</script>

<style type="text/css">
	.leftCol {
		text-align: right;
		padding-top: 5px;
	}
	#tabMenu1 {
		margin-top: 15px;
	}
</style>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block; width:800px; ">


		<div id="groupIdsHelpDiv" style="display: none; width: 300px; heigth: 50px; border: 2px solid; background-color: beige; z-index: 10;" >
			<iwcm:text key="components.user.loguser_groups_ids"/>
		</div>

		<div id="googleHelpDiv" style="display: none; width: 300px; heigth: 50px; border: 2px solid; background-color: beige;" >
			<iwcm:text key="components.user.license_google_help"/>
		</div>

		<div id="facebookHelpDiv" style="display: none; width: 300px; heigth: 50px; border: 2px solid; background-color: beige;" >
			<iwcm:text key="components.user.license_facebook_help"/>
		</div>

		<form name=textForm>
			<div class="col-sm-12">
				<div class="col-sm-12 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.user.field"/>
					</div>
					<div class="col-sm-8">
						<select name="field" onchange="javascript: showDiv();">
							<option value="!INCLUDE(/components/user/newuser.jsp)!"><iwcm:text key="components.user.reg_form"/></option>
							<option value="!INCLUDE(/components/user/logon.jsp)!"><iwcm:text key="components.user.logon_form"/></option>
							<option value="<a href='/components/user/authorize.jsp?userId=!LOGGED_USER_ID!&hash=!AUTHORIZE_HASH!'>/components/user/authorize.jsp?userId=!LOGGED_USER_ID!&hash=!AUTHORIZE_HASH!</a>"><iwcm:text key="components.user.authorize_link"/></option>
							<option value="!INCLUDE(/components/user/forget_password.jsp)!"><iwcm:text key="logon.mail.lost_password"/></option>
				            <option value="!LOGGED_USER_FIRSTNAME!"><iwcm:text key="components.user.name"/></option>
				            <option value="!LOGGED_USER_LASTNAME!"><iwcm:text key="reguser.lastname"/></option>
				            <option value="!LOGGED_USER_NAME!"><iwcm:text key="components.user.full_name"/></option>
				            <option value="!LOGGED_USER_LOGIN!"><iwcm:text key="components.user.login"/></option>
				            <option value="!LOGGED_USER_PASSWORD!"><iwcm:text key="components.user.password"/></option>
				            <option value="!LOGGED_USER_PASSWORD2!"><iwcm:text key="components.user.password2"/></option>
				            <option value="!LOGGED_USER_EMAIL!"><iwcm:text key="components.user.email"/></option>
				            <option value="!LOGGED_USER_PHONE!"><iwcm:text key="components.user.phone"/></option>
				            <option value="!LOGGED_USER_COMPANY!"><iwcm:text key="components.user.company"/></option>
				            <option value="!LOGGED_USER_ADDRESS!"><iwcm:text key="components.user.address"/></option>
				            <option value="!LOGGED_USER_CITY!"><iwcm:text key="components.user.city"/></option>
				            <option value="!LOGGED_USER_COUNTRY!"><iwcm:text key="components.user.country"/></option>
				            <option value="!LOGGED_USER_ZIP!"><iwcm:text key="components.user.zip"/></option>
				            <option value="!LOGGED_USER_ID!"><iwcm:text key="components.user.id"/></option>
				            <option value="!LOGGED_USER_FIELDA!"><iwcm:text key="components.user.use_custom_field_A"/></option>
				            <option value="!LOGGED_USER_FIELDB!"><iwcm:text key="components.user.use_custom_field_B"/></option>
				            <option value="!LOGGED_USER_FIELDC!"><iwcm:text key="components.user.use_custom_field_C"/></option>
				            <option value="!LOGGED_USER_FIELDD!"><iwcm:text key="components.user.use_custom_field_D"/></option>
				            <option value="!LOGGED_USER_FIELDE!"><iwcm:text key="components.user.use_custom_field_E"/></option>
							<option value="!LOGGED_USER_GROUPS!"><iwcm:text key="user.admin.editUserGroups"/></option>
			         </select>
		     	</div>
				</div>

			</div>
		</form>

		<style type="text/css">
		.scroll_checkboxes { height: 100px; width: 250px; background-color: white; padding: 5px; overflow: auto; border: 1px solid #ccc }
		</style>
		<span id="div_1">
			<div class="col-sm-12 form-group">
				<iwcm:text key="components.user.reguser_intro_text"/>
			</div>
		  	<form name="div1Form">
		  		<div class="form-group" >
			  		<div class="col-sm-4">
			  			<iwcm:present name="groups">
			  	 			<iwcm:text key="components.user.group_id"/>:
							<div class="scroll_checkboxes">
								<iwcm:iterate id="g" name="groups" type="sk.iway.iwcm.users.UserGroupDetails">
								 <label><input type="checkbox" name="userGroups" style="margin-right:5px;" value='<%=g.getUserGroupId()%>' <%if (ResponseUtils.filter(pageParams.getValue("groupIds", "")).indexOf(((Integer)g.getUserGroupId()).toString()) != -1)
									out.print("checked='checked'");%>/><%=g.getUserGroupName()%></label><br>
							  </iwcm:iterate>
							</div>
				  	 	</iwcm:present>
			  		</div>
			  		<div class="col-sm-4">
			  			<iwcm:text key="components.user.show_fields"/>:
							<div class="scroll_checkboxes">
								<iwcm:iterate id="f" name="showFields" type="java.lang.String">
								 <label><input type="checkbox" name="showFields" style="margin-right:5px;" value='<%=f%>'<%
								 if(Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("show", ""))))
								 {
									 if(ResponseUtils.filter(pageParams.getValue("show", "")).indexOf(f) != -1) out.print(" checked='checked'");
								 }
								 else
								 {
									 if (defaultShow.indexOf(","+f+",")!=-1) out.print(" checked='checked'");
								 }
								 %>/><iwcm:text key='<%="components.user.newuser."+f%>'/></label><br>
							   </iwcm:iterate>
							</div>
			  		</div>
			  		<div class="col-sm-4">
			  			<iwcm:text key="components.user.required_fields"/>:
							<div class="scroll_checkboxes">
								<iwcm:iterate id="f" name="requiredFields" type="java.lang.String">
								 <label><input type="checkbox" name="requiredFields" style="margin-right:5px;" value='<%=f%>'<%
								 if(Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("show", ""))))
								 {
									 if(ResponseUtils.filter(pageParams.getValue("required", "")).indexOf(f) != -1) out.print(" checked='checked'");
								 }
								 else
								 {
									 if (defaultRequired.indexOf(","+f+",")!=-1 || ResponseUtils.filter(pageParams.getValue("required", "")).indexOf(f) != -1) out.print(" checked='checked'");
								 }
								 %>/><iwcm:text key='<%="components.user.newuser."+f%>'/></label><br>
							   </iwcm:iterate>
							</div>
					</div>
				</div>
				<div class="col-sm-12 form-group" style="margin-top: 8px">
					<iwcm:text key="components.user.editableGroupIds"/>
					<input type="text" name="groupIdsEditable" value="<%=ResponseUtils.filter(pageParams.getValue("groupIdsEditable", ""))%>"/>
				</div>
				<div class="col-sm-12 form-group" >
					<input type="checkbox" name="emailUnique" value="true" <%if (pageParams.getBooleanValue("emailUnique", false))
						out.print("checked='checked'");%>/> <iwcm:text key="components.user.email_must_be_unique"/>
				</div>
				<div class="col-sm-12 form-group">
					<iwcm:text key="components.newuser.success_docid"/>
					<div>
						<input type="text" name="successDocId" size="5" maxlength="6" value='<%=ResponseUtils.filter(pageParams.getValue("successDocId", ""))%>'/>
						<input type="button" value='<iwcm:text key="editor.form.sl.update"/>' onClick='lastStandardDocIdField=this.form.successDocId;popupFromDialog("/admin/user_adddoc.jsp", 450, 340);' />
					</div>
				</div>
				<div class="col-sm-12 form-group">
					<iwcm:text key="components.user.infoemail"/>
					<input type="text" name="infoemail" value="<%=ResponseUtils.filter(pageParams.getValue("infoemail", ""))%>" style="width: 250px;" />
				</div>
				<div class="col-sm-12 form-group">
					<input type="checkbox" name="requireEmailVerification" value="true" <%if (pageParams.getBooleanValue("requireEmailVerification", false))
						out.print("checked='checked'");%>/>
					<iwcm:text key="components.user.require_email_verification"/>
				</div>
				<div class="col-sm-12 form-group">
					<iwcm:text key="components.newuser.not_authorized_email_docid"/>
					<div>
			  	 		<input type="text" name="notAuthorizedEmailDocId" size="5" maxlength="6" value="<%=ResponseUtils.filter(pageParams.getValue("notAuthorizedEmailDocId", ""))%>"/>
						<input type="button" value='<iwcm:text key="editor.form.sl.update"/>' onClick='lastStandardDocIdField=this.form.notAuthorizedEmailDocId;popupFromDialog("/admin/user_adddoc.jsp", 450, 340);' />
					</div>
				</div>
				<div class="col-sm-12 form-group">
					<input type="checkbox" name="loginNewUser" value="true" <%if (pageParams.getBooleanValue("loginNewUser", false))
						out.print("checked='checked'");%>/> <iwcm:text key="components.user.login_new_user"/>
				</div>
				<div class="col-sm-12 form-group">
					<input type="checkbox" name="useAjax" value="true" <%if (pageParams.getBooleanValue("useAjax", true))
						out.print("checked='checked'");%>/> <iwcm:text key="components.user.send_using_ajax"/>
				</div>
				<div class="col-sm-12 form-group">
					<input type="checkbox" name="useCustomFields" value="useCustomFields" <%if (pageParams.getBooleanValue("useCustomFields", true))
						out.print("checked='checked'");%> onclick="showCustomFields();"  />
		  	 		<iwcm:text key="components.user.use_custom_fields"/>
		  	 	</div>
		  	 	<div id="customFieldsDiv" class="col-sm-8 col-sm-offset-2" style="display: block;">
		  	 		<div class="form-row">
		  	 			<div class="col-sm-4">
		  	 				<label><input type="checkbox" name="useCustomFieldA" value="useCustomFieldA" <%if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("fieldALabel", ""))))
							out.print("checked='checked'");%>/> <iwcm:text key="components.user.use_custom_field_A"/></label>
		  	 			</div>
		  	 			<div class="col-sm-8">
		  	 				<label><iwcm:text key="components.user.custom_field_A_label"/>: <input type="text" name="fieldAName" value='<%=ResponseUtils.filter(pageParams.getValue("fieldALabel", ""))%>'/></label>
		  	 			</div>
		  	 		</div>
		  	 		<div class="form-row">
		  	 			<div class="col-sm-4">
		  	 				<label><input type="checkbox" name="useCustomFieldB" value="useCustomFieldB" <%if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("fieldBLabel", ""))))
							out.print("checked='checked'");%>/> <iwcm:text key="components.user.use_custom_field_B"/></label>
		  	 			</div>
		  	 			<div class="col-sm-8">
		  	 				<label><iwcm:text key="components.user.custom_field_B_label"/>: <input type="text" name="fieldBName" value='<%=ResponseUtils.filter(pageParams.getValue("fieldBLabel", ""))%>'/></label>
		  	 			</div>
		  	 		</div>
		  	 		<div class="form-row">
		  	 			<div class="col-sm-4">
		  	 				<label><input type="checkbox" name="useCustomFieldC" value="useCustomFieldC" <%if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("fieldCLabel", ""))))
							out.print("checked='checked'");%>/> <iwcm:text key="components.user.use_custom_field_C"/></label>
						</div>
		  	 			<div class="col-sm-8">
		  	 				<label><iwcm:text key="components.user.custom_field_C_label"/>: <input type="text" name="fieldCName" value='<%=ResponseUtils.filter(pageParams.getValue("fieldCLabel", ""))%>'/></label>
		  	 			</div>
		  	 		</div>
		  	 		<div class="form-row">
		  	 			<div class="col-sm-4">
		  	 				<label><input type="checkbox" name="useCustomFieldD" value="useCustomFieldD" <%if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("fieldDLabel", ""))))
						out.print("checked='checked'");%>/> <iwcm:text key="components.user.use_custom_field_D"/></label>
						</div>
		  	 			<div class="col-sm-8">
							<label><iwcm:text key="components.user.custom_field_D_label"/>: <input type="text" name="fieldDName" value='<%=ResponseUtils.filter(pageParams.getValue("fieldDLabel", ""))%>'/></label>
						</div>
		  	 		</div>
		  	 		<div class="form-row">
		  	 			<div class="col-sm-4">
		  	 				<label><input type="checkbox" name="useCustomFieldE" value="useCustomFieldE" <%if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("fieldELabel", ""))))
						out.print("checked='checked'");%>/> <iwcm:text key="components.user.use_custom_field_E"/></label>
						</div>
		  	 			<div class="col-sm-8">
		  	 				<label><iwcm:text key="components.user.custom_field_E_label"/>: <input type="text" name="fieldEName" value='<%=ResponseUtils.filter(pageParams.getValue("fieldELabel", ""))%>'/></label>
		  	 			</div>
		  	 		</div>
		  	 	</div>
			</form>
		</span>
		<span id="div_2" style="display:none">
		  <form name="div2Form">
		  	<div class="col-sm-12">
		  		<div class="col-sm-10 col-sm-offset-1" style="display:none">
		  			<strong><iwcm:text key="components.user.socialAuth"/></strong><br>

		  			<div>
		  				<input type="checkbox" name="facebook" id="facebook" value="true" <%if (pageParams.getBooleanValue("facebook", false))
						out.print("checked='checked'");%> onclick="showFacebook()"/> Facebook <br>
	  	 			</div>
	  	 			<div id="div_2_1" class="col-sm-12" style="display:none">
  	 					<div class="form-group col-sm-12">
  	 						<div class="col-sm-5 leftCol">Facebook Consumer Key:</div>
  	 						<div class="col-sm-7">
		  	 					<input type="text" name="facebook_key" id="facebook_key" value="<%=ResponseUtils.filter(pageParams.getValue("facebook_key", ""))%>" style="width: 200px;" />
		  	 					<img src="question.gif" onclick="showHelpFacebook(event);"/>
		  	 				</div>
	  	 				</div>
		  	 			<div class="form-group">
		  	 				<div class="col-sm-5 leftCol">Facebook Consumer Secret:</div>
		  	 				<div class="col-sm-7">
		  	 					<input type="text" name="facebook_secret" id="facebook_secret" value="<%=ResponseUtils.filter(pageParams.getValue("facebook_secret", ""))%>" style="width: 200px;" />
		  	 					<br>
		  	 					<a href="http://www.facebook.com/developers/apps.php" target="about:blank"> <iwcm:text key="components.user.generator_link" /></a>
		  	 				</div>
		  	 			</div>
	  	 			</div>
	  	 			<div>
	  	 				<input type="checkbox" name="google" id="google" value="true" <%if (pageParams.getBooleanValue("google", false))
						out.print("checked='checked'");%> onclick="showGoogle()"/> Google <br>
		  	 		</div>
		  	 		<div id="div_2_2" class="col-sm-12" style="display:none">
	  	 				<div class="form-group col-sm-12">
	  	 					<div class="col-sm-5 leftCol">Google Consumer Key:</div>
	  	 					<div class="col-sm-7">
	  	 						<input type="text" name="google_key" id="google_key" value="<%=ResponseUtils.filter(pageParams.getValue("google_key", ""))%>" style="width: 200px;" />
	  	 						<img src="question.gif" onclick="showHelpGoogle(event);"/>
	  	 					</div>
	  	 				</div>
	  	 				<div class="form-group"></div>
	  	 					<div class="col-sm-5 leftCol">Google Consumer Secret:</div>
	  	 					<div class="col-sm-7">
		  	 					<input type="text" name="google_secret" id="google_secret" value="<%=ResponseUtils.filter(pageParams.getValue("google_secret", ""))%>" style="width: 200px;" />
		  	 					<br>
		  	 					<a href="http://code.google.com/apis/accounts/docs/RegistrationForWebAppsAuto.html" target="about:blank"> <iwcm:text key="components.user.generator_link" /></a>
	  						</div>
	  	 				</div>
		  	 		</div>
		  		</div>
		  		<div class="col-sm-6 col-sm-offset-3 form-group" style="margin-top:20px;">
		  			<iwcm:present name="groups">
		  	 			<iwcm:text key="components.user.group_id"/>
		  	 			<img src="question.gif" onclick="showHelpGroup(event);"/>
						<br><br>
						<div class="scroll_checkboxes" style="height=250px">
							<iwcm:iterate id="g" name="groups" type="sk.iway.iwcm.users.UserGroupDetails">
							 <label><input type="checkbox" name="userGroups" value='<%=g.getUserGroupId()%>' <%if (ResponseUtils.filter(pageParams.getValue("regToUserGroups", "")).indexOf(((Integer)g.getUserGroupId()).toString()) != -1)
								out.print("checked='checked'");%>/><%=g.getUserGroupName()%></label><br>
						  </iwcm:iterate>
						</div>
		  	 		</iwcm:present>
		  		</div>
		  	</div>
		  </form>
		</span>
	</div>
</div>

<script type="text/javascript">
<% if (Tools.isNotEmpty(jspFileName)) {%>
	document.textForm.field.value = "!INCLUDE(/components/user/<%=jspFileName%>.jsp)!";
	showDiv();
<%}%>
showCustomFields();
showGoogle();
showFacebook();
</script>

<jsp:include page="/components/bottom.jsp"/>
