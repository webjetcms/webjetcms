<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.util.*,sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
boolean sortAscending = pageParams.getBooleanValue("sortAscending", true);

int docId = Tools.getDocId(request);

List forum = ForumDB.getForumFieldsForDoc(request, docId, true, 0, sortAscending);
boolean active = ForumDB.isActive(docId);
pageContext.setAttribute("forum", forum);

boolean isAdmin = false;
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user!=null && user.isAdmin())
{	
	//otestuj, ci ma pravo na tento adresar
	int group_id = Tools.getIntValue((String)request.getAttribute("group_id"), -1);
	//System.out.println("group_id="+group_id);
	GroupsDB groupsDB = GroupsDB.getInstance();
	String accessible_groups = "";
	if (user.getEditableGroups() != null && user.getEditableGroups().length() > 0)
	{
		//testni ci sa v ceste nachadza aktualna grupa
		String parentGroups = "," + groupsDB.getParents(group_id)+",";
		//iteruj cez moje povolene grupy a skus ich hladat
		StringTokenizer st = new StringTokenizer(user.getEditableGroups(), ",");
		String id;
		int i_id;
		while (st.hasMoreTokens())
		{
			id = st.nextToken().trim();
			try
			{
				i_id = Integer.parseInt(id);
				//ziskaj navbar a pridaj ho
				accessible_groups = accessible_groups+groupsDB.getNavbarNoHref(i_id)+"<br>";
				if (id.length() > 0)
				{
					//skus ho najst v parent grupach
					if (parentGroups.indexOf("," + id + ",") != -1)
					{
						//hura, ma pristup
						isAdmin = true;
					}
				}
			}
			catch (Exception ex)
			{
			}
		}
	}
	else
	{
		isAdmin = true;
	}
}
%>
<script type="text/javascript">
	<!--
		document.write('<style type="text/css" media="all">@import url("/components/forum/forum.css");</style>');
		
		function popupNewForum(parent, docId){
			var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
			editwindow=window.open("/components/forum/new.jsp?parent="+parent+"&docid="+docId,'forumNew',options);
			if (!editwindow.opener) editwindow.opener = self;
			if (window.focus) {editwindow.focus()}
			}
	-->
</script>


<table border="0" cellspacing="0" cellpadding="0" class="forumTable">
<%if (!active) {%>
 <tr>
	<td colspan="3" align="center"><b><iwcm:text key="components.forum.forum_closed"/>!</b></td> 
 </tr>
  <tr>
	<td>&nbsp;</td> 
 </tr>
<%}%>
<tr>
  <td class="forumTitle">
	  <iwcm:text key="forum.title"/>
  </td>
 <%if (active) {%>
  <td style="text-align:right;">
	  <a href="javascript:popupNewForum(0, <%=docId%>);"><iwcm:text key="forum.new"/></a>
  </td>
 <%}%>
</tr>
<tr>
  <td colspan="2">
	<% if (forum.size()==0) { %>
		<div class="forumBlank">
			<iwcm:text key="forum.empty"/>
		</div>
	<% } else { %>
		<table border="0" cellspacing="0" cellpadding="0" class="forumOpenTable">
		<logic:iterate name="forum" id="field" type="DocForumEntity" indexId="index">
			<tr>
				<td style="padding-left:<%=(20 * field.getLevel())%>px;" class="forumOpenTableHeader">
						<b><bean:write name="field" property="subject"/></b><br />
						<iwcm:text key="forum.author"/>:
						<logic:notEmpty name="field" property="authorEmail">
							<a href="mailto:<bean:write name="field" property="authorEmail"/>"><bean:write name="field" property="authorName"/></a>
						</logic:notEmpty>
						<logic:empty name="field" property="authorEmail">
							<bean:write name="field" property="authorName"/>
						</logic:empty>
				</td>
				<td align="right" class="forumOpenTableHeader">
						<bean:write name="field" property="questionDateDisplayDate"/> <bean:write name="field" property="questionDateDisplayTime"/><br />						
					 <%if (active) {%>	
						<a href="javascript:popupNewForum(<bean:write name="field" property="forumId"/>, <%=docId%>);">[<iwcm:text key="forum.reply"/>]</a>
					 <%}%>
				</td>
			</tr>
			<tr>
				<td colspan="2" style="padding-left:<%=((20 * field.getLevel())+5)%>px;" class="forumOpenQuestion">
					<jsp:getProperty name="field" property="question"/>
				</td>
			</tr>
		</logic:iterate>
		</table>
	<% } %>
  </td>
</tr>
</table>
