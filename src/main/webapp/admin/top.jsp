<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<iwcm:checkLogon admin="true"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>

<html>
<HEAD>
<TITLE>:: Web JET admin ::</TITLE>
<%
   response.setHeader("Pragma","No-Cache");
   response.setDateHeader("Expires",0);
   response.setHeader("Cache-Control","no-Cache");
%>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
<LINK rel="stylesheet" href="css/style.css">
<script language="JavaScript" src="scripts/common.jsp"></script>
<SCRIPT LANGUAGE="JavaScript" src="scripts/modalDialog.js"></SCRIPT>

</HEAD>

<BODY bgcolor="white" onLoad="Cas();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">

<TABLE WIDTH="100%" BORDER=0 CELLPADDING=0 CELLSPACING=0>
	<TR>
		<TD><a href="/admin/welcome.jsp" target="mainFrame"><IMG SRC="images/hmskin/top_left_logo.gif" WIDTH="322" HEIGHT="52" border="0"></a></TD>
		<TD background="images/hmskin/top_right_bg.gif" width="99%" valign="top">
		   <table border=0 cellspacing=0 cellpadding=0 width="98%">
		      <tr>
		      	<td align="left" style="padding-top: 8px;">
		      	   :: Web JET admin :: v<%=InitServlet.getActualVersionLong()%>
		      	</td>
		         <td align="right" style="padding-top: 8px;">
		            <span ID="tdTime">&nbsp;</span>
		         </td>
		      </tr>
		      <tr>
		         <td colspan="2" align="right" style="padding-top: 8px;" class="hmskinTopInfo">
		            <img src="images/hmskin/icon_logged_user.gif" width="16" height="16" align="absmiddle">
		            <iwcm:text key="top.logged_user"/>: <% if (Tools.isEmpty(Constants.getString("NTLMDomainController"))) { %><a href="javascript:openPopupDialogFromLeftMenu('/admin/edituser.do');" target="mainFrame"><% } %><bean:write name="iwcm_useriwcm" property="fullName"/></a>
		            &nbsp;&nbsp;&nbsp;		            
		            <a href="javascript:m_click_help()"><img src="images/hmskin/icon_help.gif" width="19" height="20" align="absmiddle" border=0> <iwcm:text key="menu.top.help"/></a>
		            &nbsp;&nbsp;&nbsp;
		            <a href="logoff.do" target="_top"><img src="images/hmskin/icon_logoff.gif" width="16" height="16" align="absmiddle" border=0> <iwcm:text key="menu.logout"/></a>
		         </td>
		      </tr>
		   </table>
      </TD>
	</TR>
</TABLE>
<script language="JavaScript">
<%
   java.util.Date d_server = new java.util.Date();
   int i_d_server = (int) (d_server.getTime() / 1000);
   out.println("t_server = " + i_d_server);
%>
lokalny_start = new Date().getTime() // pocet ms od 1.1. 1970 (lokal)
var timeServerOffset = (new Date().getTime()) - <%=Tools.getNow()%>;
window.status = "time offset: " + timeServerOffset;
</script>

<script type="text/vbscript" src="/admin/FCKeditor/editor/plugins/WJSpellCheck/word_script.jsp"></script>

</body>
</html>