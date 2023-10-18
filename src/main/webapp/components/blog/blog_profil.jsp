<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.doc.*, sk.iway.iwcm.users.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

int groupId = Integer.parseInt((String)request.getAttribute("group_id"));
GroupsDB groupsDB = GroupsDB.getInstance();

String userIdString = groupsDB.getPropertyRecursive(groupId, "fieldA");
int userId = -1;
if(Tools.isNotEmpty(userIdString))
{
	try
	{
		userId = Integer.parseInt(userIdString);
	}
	catch(Exception e)
	{
		out.println("Chyba: Hodnota v poli A nie je číslo!");
	}
}

if(userId < 0) return;

UserDetails user = UsersDB.getUser(userId);
if(user == null) return;
%>
<table>
	<tr><td colspan="2"><h2><%=user.getFullName() %></h2></td></tr>
	<tr><td colspan="2"><%=user.getPhoto() %></td></tr>
	<tr><td>Kontakt: </td><td><%=user.getEmail() %>, <%=user.getPhone() %></td></tr>
	<tr><td>Rating rank: </td><td><%=user.getRatingRank() %></td></tr>
</table>