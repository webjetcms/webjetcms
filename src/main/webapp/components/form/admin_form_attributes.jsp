<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@page import="sk.iway.iwcm.Identity"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Tools" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_form"/>
<%
java.util.Map<String, String> attributes = new FormAttributeDB().load(Tools.getRequestParameter(request, "formname"));

Identity user = UsersDB.getCurrentUser(request);

if (Tools.isEmpty(attributes.get("recipients")) && user != null)
{
	attributes.put("recipients", user.getEmail());
}

request.setAttribute("formAttributes", attributes);
%>
<%@page import="sk.iway.iwcm.form.FormAttributeDB"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>

<style type="text/css">

table.formAttributes input, table.formAttributes select, table.formAttributes textarea
{
    background-color: #fff;
    border: 1px solid #c9cccf;
    border-top-color: #aeb3b9;
    padding: 4px 6px;
    outline: none;
    box-sizing: border-box;
    border-radius: 3px;
    box-shadow: 0 1px 2px rgba(0,0,0,.15) inset;
}

table.formAttributes td
{
	padding: 2px;
}

</style>
<script type="text/javascript">

var lastPressed = null;

//called from user_addoc.jsp
function setPage(document)
{
	var virtualPath = ""
	$.ajax({
		type: "GET",
		dataType: "json",
		url: "/admin/_docid_to_virtual_path.jsp?docid="+document[0],
		async: false,
		success: function(text){virtualPath = eval(text).virtualPath}
	})
	if (lastPressed == $('#forward_button').get(0))
		$("input[name='attribute_forward']").val(virtualPath)
	else if (lastPressed == $('#forward_fail_button').get(0))
		$("input[name='attribute_forwardFail']").val(virtualPath)
	else if (lastPressed == $('#use_form_doc_id_button').get(0))
		$("input[name='attribute_useFormDocId']").val(document[0])
	else if (lastPressed == $('#use_form_mail_doc_id_button').get(0))
		$("input[name='attribute_useFormMailDocId']").val(document[0])
	else
		$("input[name='attribute_formmail_sendUserInfoDocId']").val(document[0])
}

function saveAttributes(that)
{
	var formName = that.getContentElement("info", "wjFormName").getValue();
	var postData = {'form_name' : formName };
	$(":input[name^='attribute_']").each(function(){
		postData[$(this).attr('name')] = $(this).val()
	})

	$(":input[name^='attribute_'][type='checkbox']:not(:checked)").each(function(){
		postData[$(this).attr('name')] = ""
	})

	console.log(postData);

	$.ajax({
		url: '/components/form/_save_form_attributes.jsp',
		data : postData,
		async: false,
		method : "POST"
	})
}

</script>

<table cellpadding="3" class="formAttributes">
		<tr>
			<td><iwcm:text key="editor.form.recipients" /></td>
			<td><input type="text" id="attribute_recipients_id" name="attribute_recipients" class="email" size="40" value="${formAttributes['recipients']}" title="<iwcm:text key="editor.form.recipients"/>" /></td>
		</tr>

		<tr>
			<td><iwcm:text key="editor.form.cc_emails" /></td>
			<td><input type="text" name="attribute_ccEmails" class="email" size="40" value="${formAttributes['ccEmails']}" title="<iwcm:text key="editor.form.help.cc_emails"/>" /></td>
		</tr>

		<tr><td><iwcm:text key="editor.form.bcc_emails" /></td>
		<td><input type="text" name="attribute_bccEmails" class="email" size="40" value="${formAttributes['bccEmails']}" title="<iwcm:text key="editor.form.help.bcc_emails"/>" />
		</td></tr>

		<tr><td><iwcm:text key="editor.form.reply_to_emails" /></td>
		<td><input type="text" name="attribute_replyTo" class="email" size="40" value="${formAttributes['replyTo']}" title="<iwcm:text key="editor.form.reply_to_emails"/>" />
		</td></tr>

		<tr><td><iwcm:text key="editor.form.subject" /></td>
		<td><input type="text" name="attribute_subject" class="" value="${formAttributes['subject']}" title="<iwcm:text key="editor.form.help.subject"/>" />
		</td></tr>

		<tr><td><iwcm:text key="editor.form.savedb" /></td>
		<td><input type="text" name="attribute_savedb" class="" value="${formAttributes['savedb']}" title="<iwcm:text key="editor.form.help.savedb"/>" />
		</td></tr>

		<tr><td><iwcm:text key="editor.form.forward" /></td>
		<td><input type="text" size="40" name="attribute_forward" class="" value="${formAttributes['forward']}" title="<iwcm:text key="editor.form.help.forward"/>" />
			<input type="button" class="btn btn-sm btn-outline-secondary button70" value="<iwcm:text key="editor.form.choose"/>"  id="forward_button"
				 onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
		</td></tr>

		<tr><td><iwcm:text key="editor.form.forward_fail" /></td>
		<td><input type="text" size="40" name="attribute_forwardFail" class="" value="${formAttributes['forwardFail']}" title="<iwcm:text key="editor.form.help.forward_fail"/>" />
			<input type="button" class="btn btn-sm btn-outline-secondary button70" value='<iwcm:text key="editor.form.choose" />'  id="forward_fail_button"
				 onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
		</td></tr>

		<tr><td><iwcm:text key="editor.form.forward_type" /></td>
		<td><select name="attribute_forwardType" title="<iwcm:text key="editor.form.help.forward_type"/>">
			<option value=""></option>
			<option value="forward" <%="forward".equals(attributes.get("forwardType")) ? "selected" : "" %>>forward</option>
			<option value="addParams" <%="addParams".equals(attributes.get("forwardType")) ? "selected" : "" %> >addParams</option>
		</select>
		</td></tr>

		<tr><td><iwcm:text key="editor.form.use_form_doc_id" /></td>
		<td><input type="text" size="4"  name="attribute_useFormDocId" class="" value="${formAttributes['useFormDocId']}" title="<iwcm:text key="editor.form.help.use_form_doc_id"/>" />
			<input type="button" class="btn btn-sm btn-outline-secondary button70" value="<iwcm:text key="editor.form.choose"/>"  id="use_form_doc_id_button"
				 onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
		</td></tr>

		<tr><td><iwcm:text key="editor.form.use_form_mail_doc_id" /></td>
		<td><input type="text" size="4" name="attribute_useFormMailDocId" class="" value="${formAttributes['useFormMailDocId']}" title="<iwcm:text key="editor.form.help.use_form_mail_doc_id"/>" />
			<input type="button" class="btn btn-sm btn-outline-secondary button70" value="<iwcm:text key="editor.form.choose"/>"  id="use_form_mail_doc_id_button"
				 onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.force_text_plain"/>"><iwcm:text key="editor.form.force_text_plain" /></label></td>
		<td><input type="checkbox" name="attribute_forceTextPlain" class="inputcheckbox" value="true" <%="true".equalsIgnoreCase(attributes.get("forceTextPlain")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.force_text_plain"/>"/>
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.form_mail_encoding"/>"><iwcm:text key="editor.form.form_mail_encoding" /></label></td>
			<td><input type="checkbox" name="attribute_formMailEncoding" value="ASCII" <%="ASCII".equals(attributes.get("formMailEncoding")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.form_mail_encoding"/>" />
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.is_pdf"/>"><iwcm:text key="editor.form.is_pdf" /></label></td>
			<td><input type="checkbox" name="attribute_isPdfVersion" class="inputcheckbox" value="true" <%="true".equalsIgnoreCase(attributes.get("isPdfVersion")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.is_pdf"/>"/>
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.allow_only_one_submit"/>"><iwcm:text key="editor.form.allow_only_one_submit" /></label></td>
			<td><input type="checkbox" name="attribute_formmail_allowOnlyOneSubmit" class="inputcheckbox" value="true" <%="true".equalsIgnoreCase(attributes.get("formmail_allowOnlyOneSubmit")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.allow_only_one_submit"/>" />
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.overwrite_old_forms"/>"><iwcm:text key="editor.form.overwrite_old_forms" /></label></td>
		<td><input type="checkbox" name="attribute_formmail_overwriteOldForms" class=inputcheckbox" value="true" <%="true".equalsIgnoreCase(attributes.get("formmail_overwriteOldForms")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.overwrite_old_forms"/>" />
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.message_as_attach"/>"><iwcm:text key="editor.form.message_as_attach" /></label></td>
		<td><input type="checkbox" name="attribute_messageAsAttach" id="messageAsAttachId" class=inputcheckbox" value="true" onclick="showFileName(this);" <%="true".equalsIgnoreCase(attributes.get("messageAsAttach")) ? " checked " : "" %> title="<iwcm:text key="editor.form.message_as_attach"/>" />
		</td></tr>

        <tr>
            <td>
                <label for="attribute-doubleOptIn" title="<iwcm:text key="editor.form.help.doubleOptIn"/>"><iwcm:text key="editor.form.doubleOptIn" /></label>
            </td>
            <td>
                <input type="checkbox" name="attribute_doubleOptIn" id="attribute-doubleOptIn" class=inputcheckbox" value="true" <%="true".equalsIgnoreCase(attributes.get("doubleOptIn")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.doubleOptIn"/>" />
            </td>
        </tr>

		<tr><td class="attFileName"><label title="<iwcm:text key="editor.form.message_as_attach_file_name"/>"><iwcm:text key="editor.form.message_as_attach_file_name" /></label></td>
		<td class="attFileName"><input type="text" name="attribute_messageAsAttachFileName" class="" value="<%=attributes.get("messageAsAttachFileName") == null ? "priloha.html" : attributes.get("messageAsAttachFileName")%>" />
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.send_user_info_doc_id"/>"><iwcm:text key="editor.form.send_user_info_doc_id" /></label></td>
			<td><input type="docid" size="4"  name="attribute_formmail_sendUserInfoDocId" class="inputtext" value="${formAttributes['formmail_sendUserInfoDocId']}" title="<iwcm:text key="editor.form.help.send_user_info_doc_id"/>" />
		   <input type="button" class="btn btn-sm btn-outline-secondary button70" value='<iwcm:text key="editor.form.choose"/>'  id="info_docid_button" onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.fields_emaiL_header"/>"><iwcm:text key="editor.form.fields_email_header" /></label></td>
			<td><input type="text" name="attribute_fieldsEmailHeader" class="" value="${formAttributes['fieldsEmailHeader']}" title="<iwcm:text key="editor.form.help.fields_email_header"/>" />
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.source"/>"><iwcm:text key="editor.form.source" /></label></td>
			<td><input type="text" name="attribute_source" class="" value="${formAttributes['source']}" title="<iwcm:text key="editor.form.help.source"/>" />
		</td></tr>

		<tr><td><label title="<iwcm:text key="editor.form.help.afterSendInterceptor"/>"><iwcm:text key="editor.form.afterSendInterceptor" /></label></td>
		<td><input type="text" name="attribute_afterSendInterceptor" class="" size="40" id="attribute-afterSendInterceptor" value="${formAttributes['afterSendInterceptor']}" title="<iwcm:text key="editor.form.help.afterSendInterceptor"/>" />
		</td></tr>

		<iwcm:menu name="cmp_crypto">
			<tr><td><label for="attribute_encryptKey"><iwcm:text key="components.form.encryptionKey" /></label></td>
			<td>
				<textarea name="attribute_encryptKey" class="form-control" id="attribute_encryptKey" style="height: 190px; width: 99%; font-size: 10px !important;" placeholder="encrypt_key-..."><%=attributes.get("encryptKey") != null ? ResponseUtils.filter(attributes.get("encryptKey")) : ""%></textarea>
				<div class="comment">
					<iwcm:text key="components.form.enterEncryptionKey"/>
				</div>
			</td></tr>
		</iwcm:menu>
</table>
<script type="text/javascript">
showFileName(document.getElementById("messageAsAttachId"));
function showFileName(chck)
{
	//TODO wj8
	//if(chck.checked) $(".attFileName").show();
	//else $(".attFileName").hide();
}
$("table.formAttributes input").on("keyup keydown", function(e) {
	//console.log("keyup, e=", e.keyCode);
	if (e.keyCode>=37 && e.keyCode<=40) e.stopPropagation();
	else if (e.keyCode == 9 || e.keyCode == 13) e.stopPropagation();
});
</script>
<%
String recipients = attributes.get("recipients");
if (Tools.isNotEmpty(recipients)) {
%>
	<script type="text/javascript">
	$("#attribute_recipients_id").val("<%=ResponseUtils.filter(recipients) %>");
	</script>
<% } %>
<script type="text/javascript">
	$('#attribute-afterSendInterceptor').autocomplete({
        source: "/components/form/admin_form_attributes_interceptor_ajax.jsp",
        minLength: 2

	});
</script>
