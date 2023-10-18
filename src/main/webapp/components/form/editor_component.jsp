<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%
    request.setAttribute("cmpName", "form");
    request.setAttribute("iconLink", "/components/app-date/editoricon.png");

    pageContext.include("/components/form/editor_component_invisible_captcha.jsp");
%>
<%-- LPA - tento kod nic nerobi, tak som to dal natvrdo.
<% request.setAttribute("cmpName", "form");
    request.setAttribute("iconLink", "/components/app-date/editoricon.png");

    String jspFileName = Tools.getRequestParameter(request, "jspFileName");
    boolean isInvisible = "invisible".equals(Constants.getString("captchaType");
    if(isInvisible && jspFileName != null && jspFileName.contains("captcha.jsp")) {
        pageContext.include("/components/form/editor_component_invisible_captcha.jsp");
        return;
    }


%>
<jsp:include page="/components/top.jsp"/>

<%
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
//out.println("paramPageParams="+paramPageParams);
%>
<jsp:include page="/components/bottom.jsp"/>
--%>
