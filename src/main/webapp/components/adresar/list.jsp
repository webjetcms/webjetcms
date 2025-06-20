<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.users.UserDetails"%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
//hodnoty filtra
String filterValues = pageParams.getValue("filterValues",null);
if(Tools.isEmpty(filterValues))
{
	//nastavi nejake zakladne hodnoty
	filterValues = "first_name|last_name|email|phone|company";
}
UserDetails user = null;
%>
<display:table class="tabulkaStandard" requestURI="<%=PathFilter.getOrigPath(request)%>" cellspacing="0" export="true" cellpadding="0" name="users" id="userTmp" pagesize="20">
	<%
	if(userTmp != null)
	{
		user = (UserDetails)userTmp;
		%>
		<display:column titleKey="reguser.firstname" sortProperty="lastName" sortable="true">
			<a href="javascript:wjPopup('/components/adresar/detail.jsp?id=<%=user.getUserId()%>',600,400)"><%
			out.print(user.getLastName());
			if (Tools.isNotEmpty(user.getFirstName())) out.print(" "+user.getFirstName());
			if (Constants.getBoolean("fullNameIncludeTitle") && Tools.isNotEmpty(user.getTitle())) out.print(", "+user.getTitle());
			%></a>
		</display:column>
		<display:column titleKey="reguser.phone" property="phone" sortable="true"/>
		<display:column titleKey="reguser.email" sortProperty="email" sortable="true">
			<a href="mailto:<iwcm:beanWrite name="userTmp" property="email"/>"><iwcm:beanWrite name="userTmp" property="email"/></a>
		</display:column>
		<%
		String filterList[] = Tools.getTokens(filterValues, "|");
		if(filterList != null && filterList.length > 0)
		{
			for(String val : filterList)
			{
				if(Tools.isNotEmpty(val) && val.startsWith("field_"))
				{
					String lastChar = val.substring(val.length()-1).toUpperCase();
					%>
					<display:column titleKey='<%="user.field"+lastChar%>' property='<%="field"+lastChar%>' sortable="true"/>
					<%
				}
			}
		}
		%>
	<%
	}
	%>
</display:table>
