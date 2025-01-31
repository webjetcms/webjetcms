<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.system.stripes.CSRF" %>
<%@ page import="sk.iway.iwcm.editor.appstore.AppManager" %>
<%@ page import="java.util.Map" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms='<%=Constants.getString("webpagesFunctionsPerms")%>'/>
<%@ include file="/admin/layout_top_popup.jsp" %><%
    String componentName = Tools.getStringValue(Tools.getRequestParameter(request, "componentName"), "");
%>

<style type="text/css">
body { overflow: hidden; }
</style>

<input type="hidden" id="componentCode" value="" size="90"/>

<iframe id="editorComponent" name="editorComponent" src="/admin/iframe_blank.jsp" width="100%" height="100%" frameborder="0" scrolling="auto"></iframe>

<form id="componentForm" name="componentForm" method="post" action="" target="editorComponent" style="display: none">
    <input type="hidden" name="pageParams" id="pageParams"/>
    <input type="hidden" name="jspFileName" id="jspFileName"/>
    <input type="hidden" name="title" id="title"/>
</form>

<script type="text/javascript">

    var editor = window.parent.getCkEditorInstance();
    var actuallyEditedElement = editor.document.$.getElementById("actuallyEditedComponent");
    var element = null;
    var insert = true;

    var docId = -1;
    var groupId = -1;
    var title = null;

    function getCkEditorInstanceElfinder() {
        return editor;
    }

    function getPageNavbar() {
        try {
            var navbar = undefined;
            if (window.parent.$) navbar = window.parent.$("#DTE_Field_navbar").val();
            if (typeof navbar == "undefined") {
                try {
                    navbar = window.parent?.parent?.$("#DTE_Field_navbar").val(); //page builder - insert image
                } catch (e) { }
            }
            if (typeof navbar == "undefined") {
                try {
                    navbar = window.parent?.opener?.parent?.$("#DTE_Field_navbar").val(); //page builder - background of element
                } catch (e) { }
            }

            var ckInstance = getCkEditorInstanceElfinder();
            if (typeof navbar == "undefined") navbar = ckInstance.element.$.form.navbar?.value;
            if (typeof navbar == "undefined") navbar = ckInstance.element.$.form.title?.value;
            //console.log("returning navbar=", navbar);
            return navbar;
        } catch (e) {
            console.log(e);
        }
        return "";
    }

    try {
        docId = parseInt(editor.element.$.form.docId.value);
        groupId = parseInt(editor.element.$.form.groupId.value);
        title = getPageNavbar();
    } catch (e) {
    }

    //console.log("docId=", docId, "groupId=", groupId, "title=", title, "form=", editor.element.$.form);

    function webjetOpenComponentEdit(html) {
        //console.log("webjetOpenComponentEdit, html=", html);
        $('.cke_dialog_title', window.parent.document).show();
        //window.alert("webjetOpenComponentEdit, html=" + html);

        var pageParams = "",
            jspFileName = "",
            componentName = "<%=componentName%>";

        //replace JSP paths to Spring APP class names
        var replaces = {};
        <%
        Map<String, String> classToJspReplaces = AppManager.getClassToJspReplaces(request);
        for (String key : classToJspReplaces.keySet()) {
            String value = classToJspReplaces.get(key);
            %>
            replaces["<%=key%>"] = "<%=value%>";
            <%
        }
        %>

        //parse original JSP fileName
        var originalJspFileName = "";
        var originalComponentName = componentName;
        if (html != null && html.indexOf("!INCLUDE") == 0) {
            var compFileNameStart = html.indexOf("(");
            if (compFileNameStart !== -1) {
                var jspIndex = html.indexOf(".jsp");
                if (jspIndex > compFileNameStart) {
                    //skus najst presnejsie meno
                    originalJspFileName = html.substring(compFileNameStart + 1, jspIndex+4);
                }
            }
        }

        if (html != null) {
            for (var key in replaces) {
                html = html.replace(key, replaces[key]);
            }
        }

        if (html != null && html.indexOf("!INCLUDE") == 0) {
            var comFileName = "/comp";
            comFileName += "onents/";
            var compFileNameStart = html.indexOf(comFileName);
            if (compFileNameStart !== -1) {
                var jspIndex = html.indexOf(".jsp");
                if (jspIndex > compFileNameStart) {
                    //skus najst presnejsie meno
                    var htmlPart = html.substring(compFileNameStart + 12, jspIndex);
                    jspIndex = htmlPart.lastIndexOf("/");
                    if (jspIndex > 0) componentName = htmlPart.substring(0, jspIndex);
                } else {
                    //pouzijeme len zakladne meno komponenty /compo nents/meno/
                    var compFileNameEnd = html.indexOf("/", compFileNameStart + 13);
                    if (compFileNameEnd > compFileNameStart) componentName = html.substring(compFileNameStart + 12, compFileNameEnd);
                }

                var ciarka = html.indexOf(","),
                    koniec = html.indexOf(")!");

                if (ciarka > 0 && koniec > ciarka) {
                    //pageParams = encodeURIComponent(html.substring(ciarka+1, koniec));
                    pageParams = html.substring(ciarka + 1, koniec);
                    //jspFileName = encodeURIComponent(html.substring(0, ciarka));
                    var zaciatok = 0;
                    if (html.indexOf("!INCLUDE(") === 0) zaciatok = 9;
                    jspFileName = html.substring(zaciatok, ciarka);
                }
                else {
                    jspFileName = html;
                }
            } else {
                var include = html;
                if (include.indexOf("!INCLUDE(") !== -1) {
                    include = include.substring(9);
                }

                if (include.indexOf(")!") !== -1) {
                    include = include.substring(0, include.indexOf(")!"));
                }

                var c = include,
                    parameters = "";
                if (include.indexOf(',') !== -1) {
                    c = $.trim(include.substring(0, include.indexOf(',')));
                    parameters = $.trim(include.substring(include.indexOf(',') + 1));
                }

                parameters = parameters.replace(/\&/gi, "&amp;");
                parameters = parameters.replace(/\&amp;amp;quot;/gi, "&amp;quot;")
                parameters = parameters.replace(/\&amp;nbsp;/gi, "&nbsp;")
                //console.log("CURRENT PARAMETERS=", parameters);

                var src = '/admin/v9/webpages/component?id=1&showOnlyEditor=true';
                var iframe = $('#editorComponent');
                iframe
                    .after($('<input type="hidden" id="className" />').val(c))
                    .after($('<input type="hidden" id="parameters" />').val(parameters))
                    .after($('<input type="hidden" id="docId" />').val(docId))
                    .after($('<input type="hidden" id="groupId" />').val(groupId))
                    .after($('<input type="hidden" id="title" />').val(title))
                    .after($('<input type="hidden" id="originalComponentName" />').val(originalComponentName))
                    .after($('<input type="hidden" id="originalJspFileName" />').val(originalJspFileName));

                iframe.attr('src', src);
                return;
            }
        }

        //console.log("componentName=", componentName);

        if (componentName !== "") {
            //$("#editorComponent").attr("src", "/components/"+componentName+"/editor_component.jsp?pageParams="+pageParams+"&jspFileName="+jspFileName+"&docId="+docId+"&groupId="+groupId);
            //"/components/gallery/editor_component.jsp?pageParams="+encodeURIComponent(pageParams));
            //FCKDialog.OpenDialog( 'WJDialog_Components', 'Components', '/components/'+componentName+'/editor_component.jsp?pageParams='+pageParams+'&jspFileName='+jspFileName, 400, 330 ) ;

            $("#componentForm").attr("action", "/components/" + componentName + "/editor_component.jsp?docId=" + docId + "&groupId=" + groupId);
            $("#pageParams").val(pageParams);
            $("#jspFileName").val(jspFileName);
            $("#title").val(title);
            $("#componentForm").submit();
        } else {
            $("#editorComponent").attr("src", "/admin/appstore/appstore.jsp?docId=" + docId + "&groupId=" + groupId + "&title="+encodeURIComponent(title));
        }
    }

    //console.log("webjetcomponent.js, actuallyEditedElement 1="+actuallyEditedElement);
    var data = null;

    if (actuallyEditedElement != null) {
        var CKEDITOR = window.parent.parent.CKEDITOR;
        if (CKEDITOR == undefined) CKEDITOR = window.parent.CKEDITOR;

        element = new CKEDITOR.dom.element(actuallyEditedElement);

        //console.log("DATA="+element.data("cke-realelement"));

        if (element != null && element.data("cke-realelement") != null) {
            data = decodeURIComponent(element.data("cke-realelement"));
            //window.alert("data="+data);
            //console.log("DATA decoded="+data);

            var index = data.indexOf("<article>");
            if (index == -1) index = data.indexOf("<ARTICLE>");
            if (index >= 0) {
                data = data.substring(index + 9, data.length - 10);
                $("#componentCode").val(data);

                insert = false;

                //console.log("DATA set="+data);
            }
        }
    }

    webjetOpenComponentEdit(data);

    function insertHtml(html) {
        $("#componentCode").val(html);
        setComponentData();
    }

    function setComponentData() {
        var data = $("#componentCode").val();

        if (insert == false && element != null) {
            data = encodeURIComponent("<article>" + data + "</article>");

            //update
            element.data("cke-realelement", data);
            element.setAttribute("src", "/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp?a=1");
            element.setAttribute("id", "");
        } else {
            //window.alert("Inserting as html: "+data);

            //editor.insertHtml("<p>"+data+"</p>", "html");
            if (data != "") {
                try {
                    if (data.indexOf("!INCLUDE") == 0) editor.wjInsertUpdateComponent(data);
                    else editor.wjInsertHtml(data);
                } catch (e) {
                    //v try catch kvoli IE ktore niekedy pada na nemozno ziskat scrollIntoView
                }
            }
        }

        setTimeout(function () {
            try {
                ckEditorInstance.focus();
            } catch (e) {
            }
        }, 300);
    }

    function OkClick() {
        //window.alert("OK click");

        //zavolaj inner frame a ziskaj z neho include kod
        try {
            var okClicked = $("#editorComponent").get(0).contentWindow.Ok();
            if (!okClicked) return false;
        } catch (e) {
        }

        setComponentData();

        return true;
    }

    function Cancel() {
        try {
            if (window.parent.parent.CKEDITOR.dialog.getCurrent()==null) {
                //sme vnoreny priamo v editore v admin casti v PB rezime
                window.parent.CKEDITOR.dialog.getCurrent().hide();
                return;
            }
        } catch (e) {}
        window.parent.parent.CKEDITOR.dialog.getCurrent().hide();
    }

    var FCK =
        {
            InsertHtml: function (html) {
                $("#componentCode").val(html);
            },

            GetXHTML: function () {
                editor.getData();
            }
        };

    var fckEditorSimulation = new Object();
    fckEditorSimulation.ckeditor = editor;
    fckEditorSimulation.FCK = FCK;
    fckEditorSimulation.FCK.LinkedField = editor.element.$;

    function InnerDialogLoaded() {
        return fckEditorSimulation;
    }


</script>

<%@ include file="/admin/layout_bottom_popup.jsp" %>
