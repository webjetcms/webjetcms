<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.users.UserGroupsDB"%>
<%@page import="sk.iway.iwcm.users.UserGroupDetails"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.components.adresar.AdresarDB"%>
<%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
if(session.getAttribute("adresarPageParams") == null)
	session.setAttribute("adresarPageParams",pageParams);
//ci ma zobrazit aj filter
boolean showFilter = pageParams.getBooleanValue("showFilter",true);
//cislo skupiny uzivatelov
String userGroupIds =  pageParams.getValue("groupIds",null);
if(Tools.isEmpty(userGroupIds))
{
	%><iwcm:text key="components.adresar.group.nenasiel"/><%
	return;
}
//hodnoty filtra
String filterValues = pageParams.getValue("filterValues",null);
if(Tools.isEmpty(filterValues))
{
	//nastavi nejake zakladne hodnoty
	filterValues = "first_name|last_name|email|phone|company";
}
if (Tools.isNotEmpty(request.getParameter("hladaj")))
{
	pageContext.include("/sk/iway/iwcm/components/adresar/Adresar.action");
	if(request.getAttribute("users") != null)
	{
		request.setAttribute("users",request.getAttribute("users"));		
	}
}
else
{
	List<UserDetails> users = null;
	users = AdresarDB.listUsers(filterValues, null, request);
	request.setAttribute("users", users);
}
if(showFilter)
	pageContext.include("/components/adresar/filter.jsp");
pageContext.include("/components/aceintegration/adresar/list.jsp");
%>