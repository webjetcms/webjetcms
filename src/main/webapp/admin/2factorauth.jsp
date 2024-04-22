<%@page import="sk.iway.iwcm.system.context.ContextFilter"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>

<%

GroupsDB groupsDB = GroupsDB.getInstance();

boolean hasCustomSettings = false;
String customSettingsPath = "/components/"+Constants.getInstallName()+"/admin/admin_user_settings.jsp";
IwcmFile settingsFile = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(customSettingsPath));
int userId = Tools.getIntValue(Tools.getRequestParameter(request, "userid"), -1);

%>

<%
request.setAttribute("cmpName", "users.gauth");
%>
<%@ include file="layout_top_dialog.jsp" %>

<script type="text/javascript" src="/admin/scripts/qrcode.js"></script>
<script type="text/javascript">

	resizeDialog(990, 680);

</script>

<style>
#btnCancel{
	display:none;
}
.disItems
{
	width: 250px;
	height:345px;
}
</style>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
table.sort_table td { vertical-align: top;}
</style>

<script type="text/javascript" src="/components/form/check_form.js"></script>

<div class="padding10" style="min-width: 800px; min-height: 400px;">

		<br>

		<p><iwcm:text key="user.gauth.instructions"/></p>

		<table cellspacing="0" cellpadding="1">
            <tr>
                <td nowrap="nowrap"><label><input type="checkbox" id="gauthCheckbox"/> <iwcm:text key="user.gauth.enable"/></label></td>
            </tr><tr>
                <td nowrap="nowrap"><div id="qrImage"></div></td>
            </tr><tr>
                <td nowrap="nowrap" id="instructions"></td>
            </tr><tr>
                <td nowrap="nowrap" id="scratchCodeCell" style="display: none"><iwcm:text key="user.gauth.scratchCode"/> <span id="scratchCode"></span></td>
            </tr>
		</table>
        <span id="secret"></span>

        <p id="okToConfirm" style="display: none">
            <strong><iwcm:text key="user.gauth.clickOkToConfirm"/></strong>
        </p>


</div>

<script type="text/javascript" src="/components/_common/javascript/jquery.cookie.js"></script>
<script type="text/javascript">

//var tabPaneUsers = new WebFXTabPane(document.getElementById("tab-pane-useredit") ,true);

function Ok()
{
    $.ajax({
        url: "/admin/users/2factorauth",
		method : "POST",
		data : {
            "secret" : $("#secret").text(),
            "__token": "<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(session, true)%>"
        },
        success: function (response) {
            if (response == ""){
                window.close();
            } else {
                window.alert(response);
            }
        },
        error: function (xhr, ajaxOptions, thrownError) {
            alert(xhr.status);
            alert(thrownError);
        }
    });
}

$(document).ready(function()
{
    $("#scratchCodeCell").hide();
    $("#secret").hide();
    $.ajax({
        url: "/admin/users/2factorauth",
    }).done(function(msg) {
        if (msg == ""){
            $("#uniform-gauthCheckbox span").removeClass("checked");
        }else{
            $("#secret").text(msg);
            $("#uniform-gauthCheckbox span").addClass("checked");
        }
    });

   $("#gauthCheckbox").change(function() {

       if($("#secret").text() == ""){//!$("#uniform-gauthCheckbox span").hasClass("checked")){

           $.ajax({
               url: "/admin/users/2factorauthNew",
               method: "GET",
           }).done(function(msg) {
			   data = jQuery.parseJSON(msg);

			   $("#qrImage").show();
               var qrcode = new QRCode(document.getElementById("qrImage"), {
                    text: data.url,
                    width: 300,
                    height: 300,
                    colorDark : "#000000",
                    colorLight : "#ffffff",
                    correctLevel : QRCode.CorrectLevel.H
                });

			   //not used anymore $("#scratchCodeCell").show();
			   $("#scratchCode").text(data.scratch);
			   $("#secret").text(data.secret);
               $("#uniform-gauthCheckbox span").addClass("checked");

               $("#instructions").html("<iwcm:text key='user.gauth.instructions2'/> "+data.secret);

               $("#okToConfirm").show();
           });
	   }else{
           $("#uniform-gauthCheckbox span").removeClass("checked");
           $("#qrImage").hide();
           $("#scratchCodeCell").hide();
           $("#scratchCode").text("");
           $("#secret").text("");
           $("#instructions").html("");

	   }


 		});


});
</script>

<%@ include file="layout_bottom_dialog.jsp" %>
