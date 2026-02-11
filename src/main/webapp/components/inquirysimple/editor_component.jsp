<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="java.util.Map" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.common.DocTools" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.UUID" %>
<%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%>
	<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld"%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
	<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	request.setAttribute("cmpName", "inquirysimple");
	Prop prop = Prop.getInstance(request);

	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams)) {
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String jspFileName = "/components/inquirysimple/inquiry.jsp";

	//JSON editor
	String editorData = pageParams.getValue("editorData", "W10=");

	Identity user = UsersDB.getCurrentUser(request);
	String name = prop.getText("components.inquirysimple.title");
	int docId = Tools.getIntValue(request.getParameter("docId"), -1);
	if (docId > 0)
	{
		DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
		if (doc != null) name = doc.getTitle();
	}
	name = pageParams.getValue("name", name);
	String formId = pageParams.getValue("formId", UUID.randomUUID().toString());
	String active = pageParams.getValue("active", "true");
	String multiAnswer = pageParams.getValue("multiAnswer", "false");
%>

<jsp:include page="/components/top.jsp"/>

<%-- JSON Editor Script --%>
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
    #tabMenu1 .form-group label { margin-top: 10px; }
</style>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type="text/javascript" src="/components/quiz/js/jquery.sha1.js"></script>

<script type='text/javascript'>
//<![CDATA[

var oEditor = window.parent.InnerDialogLoaded();
var FCK		= oEditor.FCK;

var fieldTypeHidenFields = {};
<%
	String keyPrefix = "components.inquirysimple.hide.";
	List<Map.Entry<String, String>> keys = Prop.sortByValue(prop.getTextStartingWith(keyPrefix));

	for (Map.Entry<String, String> label : keys)
	{
	   String key = label.getKey().substring(keyPrefix.length());
	   %>fieldTypeHidenFields["<%=key%>"]="<%=label.getValue()%>";<%
	}
%>


//Editor Configuration
// For fields configuration
var editorItemFields = {
	id: {
	  	type: "hidden",
		dataType: "String"
	},
    question: {
        title: "<iwcm:text key="components.inquirysimple.question" />",
        type: "text",
        dataType: "string"
    }
};

// Form fields to edit
var editorItemsToUse = [
    "id",
    "question",
	"image"
];

var inputData = EditorItemsList.decodeJSONData('<%=editorData %>');

var editorItemsList = new EditorItemsList(inputData, editorItemsToUse);
var renderer = new EditorRenderer("#editorWrapper", editorItemsList, editorItemFields, editorItemsToUse, "EditorItemFieldRenderer");

function getIncludeText()
{
    // set Ids where is empty
    $('#editorWrapper input[name="id"]').each(function(order, obj) {
        var val = $(obj).val();
        if(val == null || val == undefined || val == "") {
            // generate new id
            var newId = $.sha1($("#name").val() + new Date() + order);
            $(obj).val(newId);
        }
    });

	// set Data From JSON Editor
	editorItemsList.setDataFromDom($("#editorWrapper .item"));

	//--------Custom text styles------
	var formId = '<%=formId %>';
    var name = $("#name").val();
    var active = $("#active").is(":checked") + "";
    var multiAnswer = $("#multiAnswer").is(":checked") + "";

	var jspFileName = "<%=jspFileName%>";

	var includeText = "!INCLUDE(" + jspFileName +
        	", formId=\"" + formId + "\"" +
        	", name=\"" + name + "\"" +
        	", active=" + active +
        	", multiAnswer=" + multiAnswer +
			", editorData="+ EditorItemsList.encodeJSONData(editorItemsList.getData())+
			")!";
	return includeText;
}

function saveAttributes()
{


	window.parent.setComponentData();
	window.parent.Cancel();
}

function saveAndExit() {
    oEditor.FCK.InsertHtml(getIncludeText());

    //uloz atributy
    saveAttributes();

    return false;
}

function Ok()
{
    checkForm.showClassicErrorMessage = true;
    var check = checkForm.recheckAjax(document.userForm);
    if (false != check) {
        oEditor.FCK.InsertHtml(getIncludeText());
        //uloz atributy
        saveAttributes();
    }

    //vratime false, pretoze okno sa zatvori az po ulozeni atributov
    return false;
} // End function

	//]]>
</script>

<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.news.styleAndSettings" /></a></li>
		<li class="last"><a href="#" onclick="showHideTab('2');" id="tabLink2"><iwcm:text key="inquiry.answer2" /></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content" style="max-height: 400px; width: 780px;">
	<div class="tab-page" id="tabMenu1" style="display: block;  width: 780px; height:400px">

		<div class="form-group clearfix" style="margin-top: 24px;">
			<div class="col-xs-4"><label for="name"><iwcm:text key="components.inquirysimple.name" /></label></div>
			<div class="col-xs-8"><input type="text" class="form-control required" name="name" id="name" value="<%=ResponseUtils.filter(name)%>"/></div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"></div>
			<div class="col-xs-8">
				<label for="active"><input type="checkbox" name="active" id="active" class="inputcheckbox" <%="true".equalsIgnoreCase(active) ? "checked" : ""%> title="<iwcm:text key="components.inquirysimple.active"/>"/> <iwcm:text key="components.inquirysimple.active" /></label>
				<div class="comment">
					<iwcm:text key="components.inquirysimple.activeComment"/>
				</div>

			</div>
		</div>

		<div class="form-group clearfix">
			<div class="col-xs-4"></div>
			<div class="col-xs-8">
				<label for="multiAnswer"><input type="checkbox" name="multiAnswer" id="multiAnswer" class="inputcheckbox" <%="true".equalsIgnoreCase(multiAnswer) ? "checked" : ""%> title="<iwcm:text key="components.inquirysimple.multiAnswer"/>"/> <iwcm:text key="components.inquirysimple.multiAnswer" /></label>
				<div class="comment">
					<iwcm:text key="components.inquirysimple.multiAnswerComment"/>
				</div>
			</div>
		</div>

	</div>
	<div class="tab-page" id="tabMenu2">


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

<jsp:include page="/components/bottom.jsp" />
