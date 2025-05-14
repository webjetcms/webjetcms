<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.PageParams"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Tools" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<iwcm:checkLogon admin="true" perms="cmp_form"/>
<% request.setAttribute("cmpName", "invisible-captcha");

    String jspFileName = Tools.getRequestParameter(request, "jspFileName");


%>
<jsp:include page="/components/top.jsp"/>

<%
    String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

    if (Tools.isNotEmpty(paramPageParams)) {
        request.setAttribute("includePageParams", paramPageParams);
    }

    PageParams pageParams = new PageParams(request);
%>

<script>
    function getIncludeText()
    {
        return "!INCLUDE(/components/form/captcha.jsp)!";
    }

    function Ok() {
        oEditor.FCK.InsertHtml(getIncludeText());
        return true;
    }
    function openPopupEdit(nazov,hodnota)
    {
        var options = "toolbar=no,scrollbars=no,resizable=yes,width=600,height=500";
        popupwindow=window.open('/admin/conf_editor_popup.jsp?prefix='+escape(nazov)+'&value='+escape(hodnota), "ConfDBeditor", options);
        if (window.focus)
        {
            popupwindow.focus();
        }
    }

</script>
<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px;">
    <form name="textForm" style="padding: 10px; margin: 0px;">

        <div class="form-group clearfix">
            <div class="row">
                <div class="col-sm-6"><strong>ReCaptcha Site Key:</strong> <%=Tools.getStringValue(Constants.getString("reCaptchaSiteKey"), "")%> </div>
                <div class="col-sm-2"><a class="btn green" href="javascript:openPopupEdit('reCaptchaSiteKey','undefined')"> <iwcm:text key="button.setup"/></a></div>
            </div>
        </div>
        <div class="form-group clearfix">
            <div class="row">
                <div class="col-sm-6"><strong>ReCaptcha Secret:</strong> <%=Tools.getStringValue(Constants.getString("reCaptchaSecret"),"")%></div>
                <div class="col-sm-3"><a class="btn green" href="javascript:openPopupEdit('reCaptchaSecret','undefined')"> <iwcm:text key="button.setup"/></a></div>
            </div>
        </div>
    </form>
</div>
<jsp:include page="/components/bottom.jsp"/>
