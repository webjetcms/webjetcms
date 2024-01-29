<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.sql.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@page import="sk.iway.iwcm.forum.ForumDB,sk.iway.iwcm.components.forum.jpa.DocForumEntity"%>
<%@page import="java.util.List"%>

<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<iwcm:checkLogon admin="true" perms="cmp_blog"/>
<%@ include file="/admin/layout_top.jsp" %>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	String filterBloggerName = Tools.getStringValue(request.getParameter("filterBloggerName"), "");

	String action = request.getParameter("act");
	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

	if("delete".equals(action) && request.getParameter("forumId") != null && user != null)
	{
		int forumId = Tools.getIntValue(request.getParameter("forumId"), -1);
		if (forumId > 0)
		{
			DocForumEntity toBeDeletedMessage = ForumDB.getForumBean(request, forumId);
			boolean del = ForumDB.deleteMessage(forumId, toBeDeletedMessage.getDocId(), user);
			System.out.println("ForumID: " + forumId + "   result: " + del);
		}
	}

	List<Object> params = new ArrayList<Object>();

	StringBuilder sql = new StringBuilder();
	sql.append("SELECT forum_id FROM document_forum WHERE doc_id IN (SELECT doc_id FROM documents WHERE author_id = ?) AND deleted = 0 ");

	params.add(user.getUserId());

	if (Tools.isNotEmpty(filterBloggerName))
	{
		sql.append(" AND author_name LIKE ?");
		params.add("%" + filterBloggerName + "%");
	}

	sql.append(" ORDER BY question_date DESC");

	List<DocForumEntity> messages = new ArrayList<DocForumEntity>();
	List<Long> messageIds = new SimpleQuery().forList(sql.toString(), params.toArray());
	for(Long messageId : messageIds)
		messages.add(ForumDB.getForumBean(request, messageId.intValue()));

	request.setAttribute("messages", messages);
%>


<script type="text/javascript">
<!--
	helpLink = "components/blog.jsp&book=components";

	function deleteOK(url)
	{
		if(confirm('<iwcm:text key="components.blog.comments.do_you_really_want_to_delete"/>'))
			location.href = url;
	}
//-->
</script>

<div class="row title">
	<h1 class="page-title"><i class="icon-book-open"></i><iwcm:text key="components.blog.title"/><i class="fa fa-angle-right"></i><iwcm:text key="components.blog.forum.title" /></h1>
</div>

<div class="webjet5hidden">
	<div class="box_tab box_tab_thin">
		<ul class="tab_menu">
			<li class="open">
				<div class="first">&nbsp;</div>
				<a class="activeTab" href="#" id="tabLink1" onclick="showHideTab('1');">
					<iwcm:text key="components.filter"/>
				</a>
			</li>
		</ul>
	</div>

	<div class="box_toggle">
		<div class="toggle_content">
			<div id="tabMenu1">

				<form name="blogFilterForm" action="/components/blog/blog_comments.jsp" class="zobrazenie">
					<fieldset>
						<p>
							<label>
								<iwcm:text key="components.blog.forum.author"/>:
								<input type="text" class="poleKratke" value="<%=filterBloggerName %>" name="filterBloggerName" />
							</label>

							<input type="submit" class="button50" value="<iwcm:text key="components.tips.view"/>" />
							<input type="hidden" name="filterForm" value="" />
						</p>
					</fieldset>
				</form>

			</div>
		</div>
	</div>
</div>

<logic:empty name="messages">
	<iwcm:text key="components.blog.forum.no_messages" />
</logic:empty>

<logic:notEmpty name="messages">

	<display:table name="messages" export="true" pagesize="30" class="sort_table" cellspacing="0" cellpadding="0" uid="msg">
		<%  DocForumEntity message = (DocForumEntity)msg; %>

		<display:column titleKey="components.blog.forum.author" sortable="true" >
			<%=message.getAuthorName() %>
		</display:column>

		<display:column titleKey="components.blog.forum.article" sortable="true">
			<a href="<%="/admin/editor.do?docid=" + message.getDocId() %>">
				<%=DocDB.getInstance().getDoc(message.getDocId()).getTitle()%>
			</a>
		</display:column>

		<display:column titleKey="components.table.column.tools">
			<a href="#" onclick="deleteOK('<%="/components/blog/blog_comments.jsp?forumId=" + message.getForumId() + "&act=delete" %>');return false;" title='<iwcm:text key="button.delete"/>' class="iconDelete">&nbsp;</a>
		</display:column>

		<display:column titleKey="components.blog.forum.message">
			<%=message.getQuestion() %>
		</display:column>

		<display:column titleKey="gallery.card.send_date" property="questionDate" sortable="true" decorator="sk.iway.displaytag.DateTimeSecondsDecorator" />

	</display:table>

</logic:notEmpty>

<%@ include file="/admin/layout_bottom.jsp" %>