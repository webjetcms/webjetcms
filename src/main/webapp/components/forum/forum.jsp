<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.util.*,sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@ page import="sk.iway.iwcm.doc.DocDB"%>
<%
	session.setAttribute("forum-shown", Boolean.TRUE);

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	if ("yes".equals(request.getParameter("forumiframe")))
	{
		//sme v iframe, tam diskusiu znova nevypiseme...
		return;
	}

	PageParams pageParams = new PageParams(request);

	int docId = Tools.getIntValue(request.getParameter("docid"),0);
	if(docId == 0)
		docId = pageParams.getIntValue("docid",0);

	if(docId == 0)
		return;

	//disable forum for blog main page
	if (pageParams.getBooleanValue("isBlog", false)==true)
	{
		DocDetails doc = (DocDetails)request.getAttribute("docDetailsOriginal");
		if (doc == null) doc = (DocDetails)request.getAttribute("docDetails");
		GroupDetails group = (GroupDetails)request.getAttribute("pageGroupDetails");
		//out.println(group.getDefaultDocId() + " " + doc.getDocId());
		if (group != null && doc != null && group.getDefaultDocId()==doc.getDocId()) return;
	}

	boolean isAjaxCall = request.getAttribute("docDetails")==null;
	if(isAjaxCall)
	{
		pageParams = (PageParams) session.getAttribute("forumNormalPageParams");
		if (pageParams == null)
		{
			//nemame session, vrat nieco zmysluplne
			%><iwcm:text key="forum.errorLoadingData"/><%
			return;
		}
	}
	session.setAttribute("forumNormalPageParams",pageParams);

	String type = pageParams.getValue("type", "iframe");
	boolean sortAscending = pageParams.getBooleanValue("sortAscending", true);
	String pageSize = pageParams.getValue("pageSize", null);
	String noPaging = pageParams.getValue("noPaging", "true");
	if (pageSize != null)
		noPaging = "false";

	if ("mb_open".equals(type))
	{
		ForumGroupEntity fgb = ForumDB.getForum(docId);
		if (fgb != null && fgb.isMessageBoard())
		{
			request.setAttribute("useTitleImage", pageParams.getValue("useTitleImage", ""));
			request.setAttribute("useDelTimeLimit", pageParams.getValue("useDelTimeLimit", "false"));
			request.setAttribute("delMinutes", pageParams.getValue("delMinutes", "30"));
			request.setAttribute("pageSize", pageParams.getValue("pageSize", "10"));
			pageContext.include("mb_topics.jsp");
		}
		return;
	}


	if ("open".equals(type) || "open".equals(request.getParameter("forum")))
	{
		pageContext.include("forum-open.jsp");
		return;
	}

	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

	List forum = null;
	if (user != null && user.isAdmin())
	{
		forum = ForumDB.getForumFieldsForDoc(request, docId, true, 0, sortAscending, true);
	}
	else
	{
		forum = ForumDB.getForumFieldsForDoc(request, docId, true, 0, sortAscending, false);
	}
	boolean active = ForumDB.isActive(docId);
	pageContext.setAttribute("forum", forum);
%>
<%@page import="org.apache.struts.util.ResponseUtils"%>

<%
if(!isAjaxCall)
{
	//toto treba vyrendrovat len ak sa to nevola z ajaxu, aby sa to nekaskadovalo dnu do forumContentDiv
%>

	<style type="text/css" media="all">@import url("/components/forum/forum.css");</style>

	<!-- // dialogove okno s funkciami -->
	<jsp:include page="/components/dialog.jsp" />

	<% if (user != null && user.isAdmin()) { %>
	<style type="text/css">
		tr.trDeleted td, tr.trDeleted td a, tr.trDeleted td, tr.trDeleted td strong { color: red !important; }
	</style>
	<% } %>

	<div id="forumContentDiv">
<%
}
%>

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
  <td align="right">
	  <a href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=0&type=normal&docid=<%=docId%>');"><iwcm:text key="forum.new"/></a>
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
			<table border="0" cellspacing="0" cellpadding="0" class="forumCloseTable">
				<tr class="forumCloseTableHeader">
					<td><iwcm:text key="forum.author"/></td>
					<td><iwcm:text key="forum.subject"/></td>
					<td><iwcm:text key="forum.date"/></td>
				</tr>
			<logic:iterate name="forum" id="field" type="DocForumEntity" indexId="index">
				<tr<% if (field.isDeleted()) out.print(" class='trDeleted'"); %>>
					<td>
						<logic:notEmpty name="field" property="authorEmail">
							<a href="mailto:<bean:write name="field" property="authorEmail"/>"><bean:write name="field" property="authorName"/></a>
						</logic:notEmpty>
						<logic:empty name="field" property="authorEmail">
							<bean:write name="field" property="authorName"/>
						</logic:empty>
					</td>
					<td>
						<bean:write name="field" property="prefix" filter="false"/><strong><bean:write name="field" property="subject"/></strong></div>
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
  <td class="forumBottom" colspan="2" align="center">
	  <%
	  String url = "/showdoc.do?docid="+docId + "&amp;forum=open&amp;type=" + type;


	  if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML)
		  url = DocDB.getInstance().getDocLink(docId)+"?forum=open&amp;type="+type;
	  Enumeration params = request.getParameterNames();
	  String name;
	  while (params.hasMoreElements())
	  {
		  name = ResponseUtils.filter((String)params.nextElement());
		  if ("forum".equals(name) || "docid".equals(name)) continue;

		  url += "&amp;"+name+"="+ResponseUtils.filter(request.getParameter(name));
	  }

	  if ("true".equals(noPaging))
		  url += "&amp;noPaging";
	  else if (pageSize != null)
		  url += "&amp;pageSize=" + Tools.getIntValue(pageSize, 25);

	  %>
	  <a href='<%=url%>'><iwcm:text key="forum.showall"/></a>&nbsp;
	 <%if (active) {%>
	  <a href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=0&type=normal&docid=<%=docId%>');"><iwcm:text key="forum.new"/></a>
	 <%}%>
  </td>
</tr>
<% } %>
</table>
<%
if(!isAjaxCall)
{
%>
</div>
<%
}
%>