<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String editorData = pageParams.getValue("editorData", "[]");
boolean isJSON = pageParams.getBooleanValue("isJSON", true);

Identity user = UsersDB.getCurrentUser(request);
if (user == null || user.isAdmin()==false) return;
%>
<%=Tools.insertJQuery(request) %>
<%=Tools.insertJQueryUI(pageContext, "sortable") %>
<style>
    .editorWrapper .item {
        border:1px solid #000;
        background:#fff;
        margin:10px;
        padding:20px;
    }

    .editorWrapper .imageDiv {
    	width:100px;
    	height:100px;
    	background-image:url(/components/news/admin_imgplaceholder.png?w=100&h=100&ip=5);
    }
</style>
<script type="text/javascript" src="/components/json_editor/editor.js"></script>
<script type="text/javascript">
$(function() {
    // Editor Configuration
    // For fields configuration
    var editorItemFields = {
        title: {
            title: "Title",
            type: "text",
            classes: "mojPrvyTest",
            addCleaner: true
        },
        description: {
            title: "Description",
            type: "textArea",
            classes: "DalsiaClass"
        },
        image: {
            title: "Image",
            type: "image",
            classes: ""
        },
        facebookUrl: {
            title: "Facebook",
            type: "text",
            classes: ""
        },
        somethingElse: {
            title: "Something else",
            type: "text",
            classes: ""
        }
    };

    // Form fields to edit
    var editorItemsToUse = [
        "title",
        "description",
        "image",
        "somethingElse"
    ];

    <% if(isJSON) { %>
        var inputData = JSON.parse('<%=editorData %>');
    <% } else { %>
        var inputData = <%=editorData %>;
    <% } %>

    var editorItemsList = new EditorItemsList(inputData, editorItemsToUse);
    var renderer = new EditorRenderer("#editorWrapper", editorItemsList, editorItemFields, editorItemsToUse, "EditorItemFieldRenderer");


    renderer.setAfterRenderCallback(function(self) {
        $(".removeItem").on('click', function(e) {
            e.preventDefault();
            $(this).closest('.item').remove();
            self.editorItemsList.setDataFromDom($(self.selector + " .item"));
        });
    });

    $('#saveEditor').on('click', function(e) {
        e.preventDefault();
        editorItemsList.setDataFromDom($("#editorWrapper .item"));
        console.log(editorItemsList.getData());
        console.log(JSON.stringify(editorItemsList.getData()));
        // tu implementuj do includovanie
    });

    $('#addItem').on('click', function(e) {
        e.preventDefault();
        editorItemsList.setDataFromDom($("#editorWrapper .item"));
        editorItemsList.addNewItem();
        renderer.render();
    });

    renderer.render();
});
</script>
<h1>JSON EDITOR</h1>

<div id="editorWrapper" class="editorWrapper">

</div>

<input type="button" id="addItem" value="Add item">
<input type="button" id="saveEditor" value="Save">
