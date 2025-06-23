<%@page import="sk.iway.iwcm.users.UsersDB"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="windows-1250"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%

  String lng = PageLng.getUserLng(request);
  pageContext.setAttribute("lng", lng);

  Identity user = UsersDB.getCurrentUser(session);
  if(user != null)
  {

%>

<a class="logout btn" href="/logoff.do?forward=/">
	<img src="/thumb<%if((user.getPhotoOriginal()).equals("")){out.print("/images/gallery/user/admin.jpg");}else {out.print(user.getPhotoOriginal());} %>?w=36&h=36&ip=5" alt="<%=user.getFullName()%>">
	<%=user.getFullName()%><br />
	<span>Log out</span>
</a>


<%}else{%>
   <a class="btn btn-primary" href="<%=Constants.getString("aceintegration_logon_link")%>"><iwcm:text key="logon.logon"/></a>
<%}%>