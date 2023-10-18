<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/><%
//presmerovanie ak bola zobrazena URL pred prihlasenim
String afterLogonRedirect = (String)session.getAttribute("adminAfterLogonRedirect");
System.out.println("adminAfterLogonRedirect="+afterLogonRedirect);
session.removeAttribute("adminAfterLogonRedirect");
if (Tools.isNotEmpty(afterLogonRedirect) &&
            afterLogonRedirect.indexOf("welcome.jsp")==-1 && afterLogonRedirect.indexOf("refresher")==-1 &&
            afterLogonRedirect.indexOf("FCKeditor")==-1 && afterLogonRedirect.indexOf("logon.jsp")==-1 &&
            afterLogonRedirect.indexOf("ajax")==-1 && afterLogonRedirect.indexOf("/admin/todo/")==-1 &&
            afterLogonRedirect.indexOf("combine.jsp")==-1 && afterLogonRedirect.indexOf("edituser.do")==-1 &&
            afterLogonRedirect.equals("/admin/index.jsp")==false && afterLogonRedirect.equals("/admin/")==false &&
            afterLogonRedirect.equals("/admin/v9")==false && afterLogonRedirect.equals("/admin/v9/")==false)
{
    response.setStatus(301);
    response.setHeader("Location", afterLogonRedirect);
    response.setHeader("Pragma","No-Cache");
    response.setDateHeader("Expires",0);
    response.setHeader("Cache-Control","no-Cache");
    return;
}

response.sendRedirect("/admin/v9/");
%>