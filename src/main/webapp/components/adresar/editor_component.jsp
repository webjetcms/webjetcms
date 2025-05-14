<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*, sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<% request.setAttribute("cmpName", "adresar"); %>

<%@page import="sk.iway.iwcm.users.UserGroupsDB"%>
<%@page import="sk.iway.iwcm.users.UserGroupDetails"%>
<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<jsp:include page="/components/top.jsp"/>

<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>
<script type='text/javascript'>

function Ok()
{
	var form = document.menuSubscribeForm;
	var skupiny = form.skupiny.value;
	var showFilter = form.showFilter.checked;
	var parametre = "";

	if(skupiny != "")	parametre += ", groupIds="+skupiny;

	if(showFilter == false)	parametre += ", showFilter=false";
	else parametre += ", showFilter=true";

	oEditor.FCK.InsertHtml("!INCLUDE(/components/adresar/main.jsp"+parametre+")!");
	return true;
} // End function
</script>
  <form name="menuSubscribeForm" >
	  <table border="0" cellspacing="0" cellpadding="2">
	  	 <tr>
	        <td nowrap="nowrap"><iwcm:text key="components.adresar.skupiny"/>:</td>
	        <td>
	        	<select name="skupiny">
					<%
					UserGroupsDB ugDB = UserGroupsDB.getInstance();
					for (UserGroupDetails ugd : ugDB.getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS))
					{
						out.println("<option value='"+ugd.getUserGroupId()+"'>"+ugd.getUserGroupName()+"</option>");
					}
					%>
				</select>
	        </td>
	     </tr>
		 <tr>
			<td><iwcm:text key="components.adresar.zobraz_filter"/>:</td>
			<td><input type="checkbox" name="showFilter" value="" <%if (pageParams.getBooleanValue("showFilter", true)) out.print("checked='checked'");%>></td>
		</tr>
	</table>
  </form>

<script type="text/javascript">
<% if (Tools.isNotEmpty(pageParams.getValue("groupIds", ""))) {%>
	document.menuSubscribeForm.skupiny.value = "<%=ResponseUtils.filter(pageParams.getValue("groupIds", ""))%>";
<%}%>
</script>
<jsp:include page="/components/bottom.jsp"/>
