<%@page import="sk.iway.iwcm.gallery.GalleryDB"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.users.UserGroupsDB"%>
<%@page import="sk.iway.iwcm.users.UserGroupDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="sk.iway.iwcm.*" %><%@
		taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
		taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
		taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
		taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
		taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.users.UserDetails"%><%

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

// PageParams pageParams = new PageParams(request);
// //hodnoty filtra
// String filterValues = pageParams.getValue("filterValues",null);
// if(Tools.isEmpty(filterValues))
// {
// 	//nastavi nejake zakladne hodnoty
// 	filterValues = "first_name|last_name|email|phone|company";
// }
%>
<%

	List<UserDetails> usersList = (List<UserDetails>) request.getAttribute("users");
%>

<% for(UserDetails contact:usersList) {%>

<div class="col-lg-3 col-sm-6">
	<p>
		<img src="/thumb<%if((contact.getPhotoOriginal()).equals("")){out.print("/images/gallery/user/admin.jpg");}else {out.print(GalleryDB.getImagePathOriginal(contact.getPhotoOriginal()));} %>?w=160&h=160&ip=5" alt="<%=contact.getFullName()%>">
	</p>
	<p data-aos="fade-up" data-aos-duration="600"><strong><%=contact.getFullName()%></strong>
		<br><%=contact.getPosition()%>
	</p>
</div>

<%} %>