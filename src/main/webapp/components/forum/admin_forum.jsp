<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,java.util.*,sk.iway.iwcm.*" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_diskusia"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

if ("yes".equals(request.getParameter("forumiframe")))
{
	//sme v iframe, tam diskusiu znova nevypiseme...
	return;
}

PageParams pageParams = new PageParams(request);
String type = pageParams.getValue("type", "iframe");

if ("open".equals(type) || "open".equals(request.getParameter("forum")))
{
	pageContext.include("admin_forum_open.jsp");
	return;
}

int docId = 0;
try
{
	docId = Integer.parseInt(request.getParameter("docid"));
}
catch (Exception ex)
{
	return;
}

List forum = ForumDB.getForumFieldsForDoc(request, docId, true, 0, true, true);
pageContext.setAttribute("forum", forum);
%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<script type="text/javascript">
function popupNewForum(parent, docId)
{
	var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
	editwindow=window.open("/components/forum/new_admin.jsp?parent="+parent+"&docid="+docId,'forumNew',options);
	if (!editwindow.opener) editwindow.opener = self;
	if (window.focus) {editwindow.focus()}
}
</script>
<LINK href="/components/forum/forum-admin.css" type="text/css" rel=stylesheet>
<table border=0 cellspacing=0 cellpadding=0 class="forumTable">
<tr>
  <td class="forumTitle">
	  <iwcm:text key="forum.title"/>:
  </td>
  <td align="right">
	  <a href="javascript:popupNewForum(0, <%=docId%>);"><iwcm:text key="forum.new"/></a>
  </td>
</tr>
<tr>
  <td colspan=2>
	<% if (forum.size()==0) { %>
		<div class="forumBlank">
			<iwcm:text key="forum.empty"/>
		</div>
	<% } else { %>
			<table border=0 cellspacing=0 cellpadding=0 class="forumCloseTable">
				<tr class="forumCloseTableHeader">
					<td><iwcm:text key="forum.author"/></td>
					<td><iwcm:text key="forum.subject"/></td>
					<td><iwcm:text key="forum.date"/></td>
				</tr>
			<logic:iterate name="forum" id="field" type="sk.iway.iwcm.forum.ForumBean" indexId="index">
				<tr>
					<td>
						<logic:notEmpty name="field" property="autorEmail">
							<a href="mailto:<bean:write name="field" property="autorEmail"/>"><bean:write name="field" property="autorFullName"/></a>
						</logic:notEmpty>
						<logic:empty name="field" property="autorEmail">
							<bean:write name="field" property="autorFullName"/>
						</logic:empty>
					</td>
					<td>
						<bean:write name="field" property="prefix" filter="false"/><b><bean:write name="field" property="subject"/></b></div>
					</td>
					<td><bean:write name="field" property="questionDateDisplayDate"/> <bean:write name="field" property="questionDateDisplayTime"/></td>
				</tr>
			</logic:iterate>
			</table>
	<% } %>
  </td>
</tr>
<% if (forum.size()>0) { %>
<tr>
  <td class="forumBottom" colspan=2 align="center">
	  <%
	  String url = "/showdoc.do?forum=open&type="+type;
	  Enumeration params = request.getParameterNames();
	  String name, value;
	  while (params.hasMoreElements())
	  {
		  name = (String)params.nextElement();
		  if ("forum".equals(name)==false)
		  {
			  url += "&"+name+"="+ResponseUtils.filter(request.getParameter(name));
		  }
	  }
	  %>
	  <a href='<%=url%>'><iwcm:text key="forum.showall"/></a>&nbsp;
	  <a href="javascript:popupNewForum(0, <%=docId%>);"><iwcm:text key="forum.new"/></a>
  </td>
</tr>
<% } %>
</table>