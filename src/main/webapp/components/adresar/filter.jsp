<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.adresar.AdresarDB"%>
<%!
private String getLabel(String val, Prop prop)
{
	String result = val;
	if(Tools.isNotEmpty(val))
	{
		if("first_name".equals(val))
			return prop.getText("reguser.firstname");
		if("last_name".equals(val))
			return prop.getText("reguser.lastname");
		if("email".equals(val))
			return prop.getText("reguser.email");
		if("phone".equals(val))
			return prop.getText("reguser.phone");
		if("company".equals(val))
			return prop.getText("reguser.company");
		if("field_a".equals(val))
			return prop.getText("user.fieldA");
		if("field_b".equals(val))
			return prop.getText("user.fieldB");
		if("field_c".equals(val))
			return prop.getText("user.fieldC");
		if("field_d".equals(val))
			return prop.getText("user.fieldD");
		if("field_e".equals(val))
			return prop.getText("user.fieldE");
	}

	return result;
}
%>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);
PageParams pageParams = new PageParams(request);
//hodnoty filtra
String filterValues = pageParams.getValue("filterValues",null);
if(Tools.isEmpty(filterValues))
{
	//nastavi nejake zakladne hodnoty
	filterValues = "last_name|first_name|email|phone|company";
}
%>
<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.adresar.AdresarAction"/>

<div class="adresarFilter">
	<iwcm:stripForm id="filterForm" name="filterForm" method="GET" action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.components.adresar.AdresarAction">
	<stripes:errors/>
		<iwcm:text key="components.adresar.vyhladavanie.kontaktov"/>:
		<stripes:text name="vyraz"/>
		<%
		String filterList[] = Tools.getTokens(filterValues, "|");
		if(filterList != null && filterList.length > 0)
		{
			%>
			<stripes:select name="filter">
			<%
			for(String val : filterList)
			{
				%><stripes:option value="<%=val.trim()%>"><%=getLabel(val.trim(), prop)%></stripes:option><%
			}
			%></stripes:select><%
		}
		%>
		<input type="hidden" name="hladaj" value="true"/>
		<input type="submit" value="<iwcm:text key="components.adresar.hladaj"/>"/>
		<c:if test="${not empty param.forward}">
			<input type="hidden" name="forward" value="<%=Tools.getParameterNotNull(request, "forward")%>" />
		</c:if>
	</iwcm:stripForm>
</div>