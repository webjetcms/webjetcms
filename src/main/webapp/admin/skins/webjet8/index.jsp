<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.stat.StatWriteBuffer"%>
<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="java.io.File"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.components.welcome.WelcomeDataBackTime"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/><%

{
	//moznost presmerovania po prihlaseni sa do adminu
	String adminAfterLogonRedirect = Constants.getString("adminAfterLogonRedirect");
	System.out.println("adminAfterLogonRedirect="+adminAfterLogonRedirect);
	if (Tools.isNotEmpty(adminAfterLogonRedirect) && adminAfterLogonRedirect.startsWith("/") && "true".equals(session.getAttribute("logon_ok_admin")))
	{
		session.removeAttribute("logon_ok_admin");
		response.setStatus(301);
		response.setHeader("Location", adminAfterLogonRedirect);
		response.setHeader("Pragma","No-Cache");
		response.setDateHeader("Expires",0);
		response.setHeader("Cache-Control","no-Cache");
		return;
	}
}

{
	//presmerovanie ak bola zobrazena URL pred prihlasenim
	String afterLogonRedirect = (String)session.getAttribute("adminAfterLogonRedirect");
	System.out.println("adminAfterLogonRedirect="+afterLogonRedirect);
	if (Tools.isNotEmpty(afterLogonRedirect) &&
				afterLogonRedirect.indexOf("welcome.jsp")==-1 && afterLogonRedirect.indexOf("refresher")==-1 &&
				afterLogonRedirect.indexOf("FCKeditor")==-1 && afterLogonRedirect.indexOf("logon.jsp")==-1 &&
				afterLogonRedirect.indexOf("ajax")==-1 && afterLogonRedirect.indexOf("/admin/todo/")==-1 &&
				afterLogonRedirect.indexOf("combine.jsp")==-1 && afterLogonRedirect.indexOf("edituser.do")==-1 &&
				afterLogonRedirect.equals("/admin/index.jsp")==false && afterLogonRedirect.equals("/admin/")==false)
	{
		session.removeAttribute("adminAfterLogonRedirect");
		response.setStatus(301);
		response.setHeader("Location", afterLogonRedirect);
		response.setHeader("Pragma","No-Cache");
		response.setDateHeader("Expires",0);
		response.setHeader("Cache-Control","no-Cache");
		return;
	}
}

response.sendRedirect("/admin/v9/");
%>
