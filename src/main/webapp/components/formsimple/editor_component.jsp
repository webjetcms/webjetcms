<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"
	import="org.apache.struts.util.ResponseUtils,sk.iway.iwcm.Identity"%>
<%@ page import="sk.iway.iwcm.PageParams" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.common.DocTools" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.form.FormAttributeDB" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.json.JSONObject" %>
<%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%>
	<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld"%>
	<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld"%><%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld"%>
	<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld"%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
	<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	request.setAttribute("cmpName", "formsimple");
	Prop prop = Prop.getInstance(request);

	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams)) {
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String jspFileName = "/components/formsimple/form.jsp";

	//JSON editor
	String editorData = pageParams.getValue("editorData", "W10=");

	Identity user = UsersDB.getCurrentUser(request);
	String formName = prop.getText("components.formsimple.title");
	int docId = Tools.getIntValue(request.getParameter("docId"), -1);
	if (docId > 0)
	{
		DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
		if (doc != null) formName = doc.getTitle();
	}
	formName = pageParams.getValue("formName", formName);
    boolean rowView = pageParams.getBooleanValue("rowView", false);

	request.setAttribute("descKey", "");

	java.util.Map<String, String> attributes = new FormAttributeDB().load(DocTools.removeChars(formName, true));
%>

<jsp:include page="/components/top.jsp"/>

<%-- JSON Editor Script --%>
<%@include file="/components/_common/cleditor/jquery.cleditor.js.jsp" %>
<%--
<link type="text/css" rel="stylesheet" href="/components/_common/wysiwyg/wysiwyg.css" />
--%>
<link type="text/css" rel="stylesheet" media="screen" href="/components/json_editor/editor_style.css" />
<script type="text/javascript" src="/components/json_editor/editor.js"></script>
<script type="text/javascript" src="/components/json_editor/editor_functions.js"></script>
<%-- END of JSON Editor Script --%>


<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />

<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px !important; }
	td.main { padding: 0px; }
	.editorWrapper .editorLeft { width: 40%; }
	.editorWrapper .editorRight { margin-left: 40%; margin-right: 10px; }
	.editorWrapper .item { min-height: 105px; max-height: 500px; transition-property: height, min-height, max-height, padding, margin, right, bottom; transition-duration: 0.5s; }
	.editorWrapper .item.collapsed { height: auto; max-height: 37px; min-height: 37px; padding: 2px; margin: 4px 0; }
	.editorWrapper .item.collapsed .removeItem { display: none; }
	.editorWrapper .item.collapsed .moveItem { bottom: -10px;right: 2px; }
	.jsonEditorHidden { display: none; }
    .requiredField {
        position: absolute;
        top: 55px;
    }
    #tabMenu1 .form-group label { margin-top: 10px; }

	#editorWrapper form.collapsed .cleditorToolbar {
		display: none;
	}
</style>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>

<script type='text/javascript'>
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
	else if (lastPressed == $('#use_form_mail_doc_id_button').get(0))
		$("input[name='attribute_useFormMailDocId']").val(document[0])
	else
		$("input[name='attribute_formmail_sendUserInfoDocId']").val(document[0])
}
</script>

<script type='text/javascript'>
//<![CDATA[

var oEditor = window.parent.InnerDialogLoaded();
var FCK		= oEditor.FCK;

var fieldTypeHidenFields = {};
<%
	String keyPrefix = "components.formsimple.hide.";
	List<Map.Entry<String, String>> keys = Prop.sortByValue(prop.getTextStartingWith(keyPrefix));

	for (Map.Entry<String, String> label : keys)
	{
	   String key = label.getKey().substring(keyPrefix.length());
	   %>fieldTypeHidenFields["<%=key%>"]="<%=label.getValue()%>";<%
	}
%>

function fieldTypeOnChange(select)
{
    var value = select.value;
    var fieldsToHide = fieldTypeHidenFields[value];
    var itemNo = select.form.name.substring(select.form.name.lastIndexOf("_")+1);

    $("#item_"+itemNo+" div.propertyWrapper").removeClass("jsonEditorHidden");
    if (fieldsToHide)
	{
        //console.log(fieldsToHide);
		var fields = fieldsToHide.split(",");
		for (var i=0; i<fields.length; i++)
		{
		    var divId = "#"+fields[i]+"_"+itemNo;
		    //console.log("Hiding "+divId);
		    $(divId).addClass("jsonEditorHidden");
		}
	}
}
<%
JSONArray list = new JSONArray();
keyPrefix = "components.formsimple.label.";
keys = Prop.sortByValue(prop.getTextStartingWith(keyPrefix));

for (Map.Entry<String, String> label : keys)
{
   String key = label.getKey().substring(keyPrefix.length());
   int i = key.indexOf(".");
   if (i>0) key = key.substring(0, i);

   JSONObject object = new JSONObject();
   object.put("title", label.getValue());
   object.put("value", key);
   list.put(object);
}
%>
//Editor Configuration
// For fields configuration
var editorItemFields = {
    fieldType: {
      	title: "<iwcm:text key="components.formsimple.fieldType"/>",
		type: "select",
		dataType: "string",
		classes: "editorLeft",
		options: <%= list.toString(4) %>,
        onchange: "fieldTypeOnChange"
	},
    required: {
        title: "<iwcm:text key="components.formsimple.required" />",
        type: "checkboxRight",
        classes: "editorLeft requiredField"
    },
    label: {
        title: "<iwcm:text key="components.formsimple.label" />",
        comment: "<iwcm:text key="components.formsimple.labelComment" />",
        type: "textAreaWysiwyg",
        dataType: "string",
        classes: "editorRight"
    },
    value: {
        title: "<iwcm:text key="components.formsimple.value" />",
        comment: "<iwcm:text key="components.formsimple.valueComment" />",
        type: "text",
        dataType: "string",
        classes: "editorRight"
    },
    placeholder: {
        title: "<iwcm:text key="components.formsimple.placeholder" />",
        comment: "<iwcm:text key="components.formsimple.placeholderComment" />",
        type: "text",
        dataType: "string",
        classes: "editorRight"
    },
    tooltip: {
        title: "<iwcm:text key="components.formsimple.tooltip" />",
        comment: "<iwcm:text key="components.formsimple.tooltipComment" />",
        type: "textAreaWysiwyg",
        dataType: "string",
        classes: "editorRight"
    }
};

// Form fields to edit
var editorItemsToUse = [
    "fieldType",
    "required",
    "label",
    "value",
    "placeholder",
    "tooltip"
];

var inputData = EditorItemsList.decodeJSONData('<%=editorData %>');

//console.log("inputData=", inputData);

var editorItemsList = new EditorItemsList(inputData, editorItemsToUse);
var renderer = new EditorRenderer("#editorWrapper", editorItemsList, editorItemFields, editorItemsToUse, "EditorItemFieldRenderer");

//--------------

$(document).ready(function()
{
    //showHideTab('2');
    <%if(!"true".equals(Tools.getCookieValue(request.getCookies(),"gdprFormApprowed","false"))){%>
    if(confirm('<%=prop.getText("components.forms.alert.gdpr")%>'))
    {
        var today = new Date();
        var expire = new Date();
        expire.setTime(today.getTime() + 3600000*24*7);// 7 dni
        document.cookie = 'gdprFormApprowed=true;expires='+expire.toGMTString()+';path=/;';
    }
    else
    {
        window.parent.Cancel();
    }
    <%}%>
});

function getIncludeText()
{
	// set Data From JSON Editor
	editorItemsList.setDataFromDom($("#editorWrapper .item"));

	//console.log(editorItemsList.getData());

	//--------Custom text styles------
    var formName = $("#formName").val();

	var jspFileName = "<%=jspFileName%>";

    var rowView = $("#rowView").is(":checked");

	var includeText = "!INCLUDE(" + jspFileName +
        	", formName=\"" + formName + "\"" +
            ", rowView=\"" + rowView + "\"" +
			", editorData="+ EditorItemsList.encodeJSONData(editorItemsList.getData())+
			")!";

	//console.log(editorItemsList.getData());
    //console.log("getIncludeText="+includeText);

	return includeText;
}

function saveAttributes()
{
    var formName = $("#formName").val();
    var postData = {'form_name' : formName };

    $(":input[name^='attribute_']").each(function(){
        postData[$(this).attr('name')] = $(this).val()
    })

    $(":input[name^='attribute_'][type='checkbox']:not(:checked)").each(function(){
        postData[$(this).attr('name')] = ""
    })

	postData["fixFormName"] = "true";
    postData["attribute_subject"] = formName;

    $.ajax({
        url: '/components/form/_save_form_attributes.jsp',
        data : postData,
        method : "POST"
    }).done(function() {

        window.parent.setComponentData();
        window.parent.Cancel();

	});
}

function gdprConfirmation()
{
    var data = {hasUserApproved:'true',formName:$("#formName").val()}
    $.ajax({
        type: "POST",
        url: "/components/form/admin_gdpr_check-ajax.jsp",
        data: data,
        success: function(data){
            data = $.trim(data);
            if(data.indexOf('true') == 0)
            {
                //return true;
                saveAndExit();
                //saveAttributes(attribues);//this
            }
            else
            {
                if(confirm('<%=prop.getText("components.forms.alert.gdpr")%>'))
                {
                    var data = {addUserApprove:'true',formName:$("#formName").val()}
                    $.ajax({
                        type: 'POST',
                        url: "/components/form/admin_gdpr_check-ajax.jsp",
                        data: data,
                        success: function(data) {
                            data = $.trim(data);
                            if (data.indexOf('true') == 0) {
                                saveAndExit();
                                //saveAttributes(attribues);//this

                            }
                        }
                    });
                }
                else
                {
                    return false;
                }
            }
        }
    });
}

function saveAndExit() {
    oEditor.FCK.InsertHtml(getIncludeText());

    //uloz atributy
    saveAttributes();

    return false;
}

function Ok()
{
    try {
        checkForm.showClassicErrorMessage = true;
        var check = checkForm.recheckAjax(document.userForm);
        if (false != check) {
            //updateTextareas();

            oEditor.FCK.InsertHtml(getIncludeText());
            //uloz atributy
            saveAttributes();
        }
    }
    catch(ex) {
        console.log(ex);
	}

    //vratime false, pretoze okno sa zatvori az po ulozeni atributov
    return false;
} // End function

var wysiwygs = null;
function loadWysiwygs() {

    if (wysiwygs != null) {
        return;
    }

    var cleditorOptions = {
        width: "100%",
        height: 58,
        controls: "bold italic underline color",
        bodyStyle: "font: 11px  Arial, Helvetica, sans-serif;"
    };

    wysiwygs = $("#editorWrapper textarea.wysiwyg").filter(function (i, v) {
        return $(this).data('cleditor') != true;
    }).cleditor(cleditorOptions).data("cleditor", true);

    setTimeout(function () {
        $(".ui-sortable").on("sortstop", function (event, ui) {
            ui.item.find('textarea.wysiwyg').each(function () {
                $(this).cleditor(cleditorOptions).get(0).refresh();
            });
        }).data("sortend", true);
    });


    $('#addItem').click(function () {
        $("#editorWrapper textarea.wysiwyg").each(function () {
            if (typeof $(this).cleditor == 'undefined') {
                $(this).cleditor(cleditorOptions);
            }
            else {
                $(this).cleditor(cleditorOptions).get(0).refresh();
            }
        });
    });

    $('textarea.wysiwyg').each(function () {
        var editor = $(this).cleditor()[0];
        $(editor.doc).bind("keyup", function (e) {
            var code = $(editor.doc.body).html()
            var regexReplace = [
                //[/\s*style\s*=\s*("|')([^"']+)("|')/ig, ''],
                //[/<([^>])>\s*<\s*\/[^>]>/mgi,  ''],
                [/&nbsp;/gi, " "],
                [/<\/div>/gi, ""],
                [/<p><br><\/p>/gi, ""],
                [/<\!--<p-->/gi, ""],
                [/<\!--<\/p-->/gi, ""],
                [/<p[^>]*>/gi, ''],
                [/<\/p>/gi, '<br>'],
                [/<br>$/gi, ""]
            ];

            //console.log(code);

            $.each(regexReplace, function (index, item) {
                code = code.replace(item[0], item[1]);
            });


            //console.log(editor.$area)
            $(editor.$area).val(code);

            //console.log(code);
            //console.log(code.replace(/<[^>]+>/g, ''));
            //console.log('--- clean');

            $(editor.$frame).bind('blur', function(){
              	alert(0);
			})

			// editor.updateTextArea(code);
            return true;
        });
    });
}

/*
function updateTextareas() {
    if (wysiwygs != null) {
        wysiwygs.each(function () {
            $(this).get(0).updateTextArea();
        });
    }

    function removeLastElement(value, element) {
        if (element == null) {
            return value;
		}

        value = $.trim(value);
		var index = value.indexOf(element);

        if (index == -1) {
            return value;
        }

        console.log(value)

        if (index == value.length - element.length) {
            value = value.slice(0, element.length);
		}

		console.log(value)

        return value;
	}
}
*/

	//]]>
</script>

<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="datatable.tab.basic" /></a></li>
		<li class="last"><a href="#" onclick="showHideTab('2'); loadWysiwygs()" id="tabLink2"><iwcm:text key="datatable.tab.advanced" /></a></li>
		<li class="last"><a href="#" onclick="showHideTab('3'); loadWysiwygs()" id="tabLink3"><iwcm:text key="components.news.items" /></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">

		<p>
			<iwcm:text key="components.formsimple.desc"/>
		</p>

		<div class="form-group clearfix" style="margin-top: 24px;">
			<div class="col-xs-4"><label for="formName"><iwcm:text key="components.formsimple.formName" /></label></div>
			<div class="col-xs-8"><input type="text" class="form-control required" name="formName" id="formName" value="<%=ResponseUtils.filter(formName)%>"/></div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"><label for="attribute_recipients"><iwcm:text key="components.formsimple.recipients" /></label></div>
			<div class="col-xs-8">
				<input type="text" name="attribute_recipients" class="form-control required" id="attribute_recipients" value="<%=attributes.get("recipients") != null ? ResponseUtils.filter(attributes.get("recipients")) : user.getEmail()%>"/>
				<div class="comment">
					<iwcm:text key="components.formsimple.recipientsComment"/>
				</div>

			</div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"></div>
			<div class="col-xs-8">
                <label><input type="checkbox" id="rowView" name="rowView" class="inputcheckbox" value="true" <%=rowView ? " checked " : "" %>/> <iwcm:text key="components.formsimple.rowView" /></label>
                <div class="comment">
                    <iwcm:text key="components.formsimple.rowViewHelp"/>
                </div>
            </div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"><label for="attribute_emailTextBefore"><iwcm:text key="components.formsimple.textBefore" /></label></div>
			<div class="col-xs-8">
				<textarea name="attribute_emailTextBefore" class="form-control" id="attribute_emailTextBefore"><%=attributes.get("emailTextBefore") != null ? ResponseUtils.filter(attributes.get("emailTextBefore")) : ""%></textarea>
				<div class="comment">
					<iwcm:text key="components.formsimple.textBefore.comment"/>
				</div>
			</div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"><label for="attribute_emailTextAfter"><iwcm:text key="components.formsimple.textAfter" /></label></div>
			<div class="col-xs-8">
				<textarea name="attribute_emailTextAfter" class="form-control" id="attribute_emailTextAfter"><%=attributes.get("emailTextAfter") != null ? ResponseUtils.filter(attributes.get("emailTextAfter")) : ""%></textarea>
				<div class="comment">
					<iwcm:text key="components.formsimple.textAfter.comment"/>
				</div>
			</div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"></div>
			<div class="col-xs-8">
                <label><input type="checkbox" name="attribute_forceTextPlain" class="inputcheckbox" value="true" <%="true".equalsIgnoreCase(attributes.get("forceTextPlain")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.force_text_plain"/>"/> <iwcm:text key="editor.form.force_text_plain" /></label>
                <div class="comment">
                    <iwcm:text key="editor.form.help.force_text_plain"/>
                </div>
            </div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"></div>
			<div class="col-xs-8">
				<label><input type="checkbox" name="attribute_addTechInfo" class="inputcheckbox" value="true" <%="true".equalsIgnoreCase(attributes.get("addTechInfo")) ? " checked " : "" %> title="<iwcm:text key="editor.form.help.addTechInfo"/>"/> <iwcm:text key="editor.form.addTechInfo" /></label>
				<div class="comment">
					<iwcm:text key="editor.form.help.addTechInfo"/>
				</div>
			</div>
		</div>

	</div>
    <div class="tab-page" id="tabMenu2">

        <div class="form-group clearfix">
			<div class="col-xs-4"> <iwcm:text key="editor.form.cc_emails" /> </div>
			<div class="col-xs-8"> <input type="text" name="attribute_ccEmails" class="email" size="40" value="<%=attributes.get("ccEmails") != null ? ResponseUtils.filter(attributes.get("ccEmails")) : ""%>" title="<iwcm:text key="editor.form.help.cc_emails"/>" /></div>
		</div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.bcc_emails" /> </div>
            <div class="col-xs-8"> <input type="text" name="attribute_bccEmails" class="email" size="40" value="<%=attributes.get("bccEmails") != null ? ResponseUtils.filter(attributes.get("bccEmails")) : ""%>" title="<iwcm:text key="editor.form.help.bcc_emails"/>" /> </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.reply_to_emails" /> </div>
            <div class="col-xs-8"> <input type="text" name="attribute_replyTo" class="email" size="40" value="<%=attributes.get("replyTo") != null ? ResponseUtils.filter(attributes.get("replyTo")) : ""%>" title="<iwcm:text key="editor.form.reply_to_emails"/>" /> </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.subject" /> </div>
            <div class="col-xs-8"> <input type="text" name="attribute_subject" class="" value="<%=attributes.get("subject") != null ? ResponseUtils.filter(attributes.get("subject")) : ""%>" title="<iwcm:text key="editor.form.help.subject"/>" /> </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.forward" /> </div>
            <div class="col-xs-8">
                <input type="text" size="40" name="attribute_forward" class="" value="<%=attributes.get("forward") != null ? ResponseUtils.filter(attributes.get("forward")) : ""%>" title="<iwcm:text key="editor.form.help.forward"/>" />
                <input type="button" class="button70" value="<iwcm:text key="editor.form.choose"/>"  id="forward_button" onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
            </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.forward_fail" /> </div>
            <div class="col-xs-8">
                <input type="text" size="40" name="attribute_forwardFail" class="" value="<%=attributes.get("forwardFail") != null ? ResponseUtils.filter(attributes.get("forwardFail")) : ""%>" title="<iwcm:text key="editor.form.help.forward_fail"/>" />
                <input type="button" class="button70" value='<iwcm:text key="editor.form.choose" />'  id="forward_fail_button" onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
            </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.forward_type" /> </div>
            <div class="col-xs-8">
                <select name="attribute_forwardType" title="<iwcm:text key="editor.form.help.forward_type"/>">
                    <option value=""></option>
                    <option value="forward" <%="forward".equals(attributes.get("forwardType")) ? "selected" : "" %>>forward</option>
                    <option value="addParams" <%="addParams".equals(attributes.get("forwardType")) ? "selected" : "" %> >addParams</option>
                </select>
            </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.use_form_mail_doc_id" /> </div>
            <div class="col-xs-8">
                <input type="text" size="5" name="attribute_useFormMailDocId" class="" value="<%=attributes.get("useFormMailDocId") != null ? ResponseUtils.filter(attributes.get("useFormMailDocId")) : ""%>" title="<iwcm:text key="editor.form.help.use_form_mail_doc_id"/>" />
                <input type="button" class="button70" value="<iwcm:text key="editor.form.choose"/>"  id="use_form_mail_doc_id_button" onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
            </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.send_user_info_doc_id" /> </div>
            <div class="col-xs-8">
                <input type="text" size="5"  name="attribute_formmail_sendUserInfoDocId" class="inputtext" value="<%=attributes.get("formmail_sendUserInfoDocId") != null ? ResponseUtils.filter(attributes.get("formmail_sendUserInfoDocId")) : ""%>" title="<iwcm:text key="editor.form.help.send_user_info_doc_id"/>" />
                <input type="button" class="button70" value='<iwcm:text key="editor.form.choose"/>'  id="info_docid_button" onclick='lastPressed=this; popupFromDialog("/admin/user_adddoc.jsp", 450, 340);'/>
            </div>
        </div>

        <div class="form-group clearfix">
            <div class="col-xs-4"> <iwcm:text key="editor.form.afterSendInterceptor" /> </div>
            <div class="col-xs-8"> <input type="text" name="attribute_afterSendInterceptor" class="" size="40" id="attribute-afterSendInterceptor" value="<%=attributes.get("afterSendInterceptor") != null ? ResponseUtils.filter(attributes.get("afterSendInterceptor")) : ""%>" title="<iwcm:text key="editor.form.help.afterSendInterceptor"/>" /> </div>
        </div>

        <iwcm:menu name="cmp_crypto">
            <div class="form-group clearfix">
                <div class="col-xs-4"><label for="attribute_encryptKey"><iwcm:text key="components.form.encryptionKey" /></label></div>
                <div class="col-xs-8">
                    <textarea name="attribute_encryptKey" class="form-control" id="attribute_encryptKey" style="height: 190px; font-size: 10px !important;" placeholder="encrypt_key-..."><%=attributes.get("encryptKey") != null ? ResponseUtils.filter(attributes.get("encryptKey")) : ""%></textarea>
                    <div class="comment">
                        <iwcm:text key="formsimple.encryptionKey.text_a" /> <a href="javascript:openPopupDialogFromLeftMenu('/components/crypto/admin/keymanagement');"><iwcm:text key="formsimple.encryptionKey.text_b" /></a>
                    </div>
                </div>
            </div>
		</iwcm:menu>
    </div>

	<div class="tab-page" id="tabMenu3">
		<div id="editorWrapper" class="editorWrapper collapsable"
				data-collapse="<iwcm:text key="components.json_editor.collapse" />"
				data-expand="<iwcm:text key="components.json_editor.expand" />">
		</div>

		<p>
			<input type="button" id="addItem" class="button50 button50grey" value="<iwcm:text key="components.slider.addItem"/>">
		</p>
		<p>&nbsp;</p>

	</div>
</div>

<%--
<script type="text/javascript" src="/components/_common/wysiwyg/wysiwyg.jsp"></script>
--%>
<jsp:include page="/components/bottom.jsp" />
