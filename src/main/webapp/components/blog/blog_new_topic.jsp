<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.editor.EditorDB"%>
<%@page import="sk.iway.iwcm.editor.EditorForm"%>
<iwcm:checkLogon admin="true" perms="cmp_blog"/>
<%
	request.setAttribute("cmpName", "blog.add_topic");
%>

<%@ include file="/admin/layout_top_dialog.jsp" %>

<script src="/components/form/check_form.js" type="text/javascript"></script>
<%=Tools.insertJQuery(request)%>
<script type="text/javascript">
<!--
	resizeDialog(400, 300);
	document.body.style.cursor = "default";

	window.opener.location.reload();
	
	function Ok()
	{
		//musi to ist takto, inak by sa nezavolal check form
		document.getElementById('submit').click();
	}
//-->
</script>

<div class="padding10">

<%
	
	//PageParams pageParams = new PageParams(request);
	String state = "";
	try
	{
		//--------------------SPRACOVANIE UZ ODOSLANEHO FORMULARU---------------------------
		if (request.getParameter("submit") != null)
		{
			//------------POZRIEME SA, CI NAHODOU UZ TAKYTO TOPIC NEEXISTUJE--------------------
			String topic = request.getParameter("topic");
			int authorId = ((Identity) session.getAttribute(Constants.USER_KEY)).getUserId();
			boolean NO_RECURSIVE = false;
			String editableGroup = UsersDB.getUser(authorId).getEditableGroups().split(",")[0];
			int parentGroupId = Integer.valueOf(editableGroup);
			boolean topicAlreadyExists = GroupsDB.getInstance().getGroup("topic", parentGroupId) != null;
			if (topicAlreadyExists)
				state = Prop.getInstance(request).getText("components.blog.topics.already_exists");
			else
			{
				//-----------OK, NEEXISTUJE, MOZEME JU VYTVORIT, SPOLU S NEWS KOMPONENTOU VO VNUTRI---------------------
				GroupDetails newTopicFolder = new GroupDetails();
					newTopicFolder.setParentGroupId(parentGroupId);
					newTopicFolder.setGroupName(topic);
					newTopicFolder.setTempId( GroupsDB.getInstance().getGroup(parentGroupId).getTempId() );
					newTopicFolder.setMenuType(GroupDetails.MENU_TYPE_ONLYDEFAULT);
				GroupsDB.getInstance().setGroup(newTopicFolder);
				//news komponenta pre default skupinu
				EditorForm newTopicNews = EditorDB.getEditorForm(request, -1, -1, newTopicFolder.getGroupId());
					newTopicNews.setTitle( Prop.getInstance(request).getText("components.blog.articles_list")+" "+topic);
					newTopicNews.setNavbar(Prop.getInstance(request).getText("components.blog.articles_list")+" "+topic);
					newTopicNews.setData("!INCLUDE(/components/blog/blog_news.jsp, groupIds="
										+ newTopicNews.getGroupId()
										+ ", orderType=date, asc=no, publishType=all, noPerexCheck=yes, paging=yes, pageSize=10, cols=1, image=none, perex=no, date=no, place=no, expandGroupIds=yes)!");
					newTopicNews.setAvailable(true);
					newTopicNews.setSearchable(true);
					newTopicNews.setCacheable(false);
					newTopicNews.setPublish("1");
					newTopicNews.setAuthorId(authorId);
				EditorDB.saveEditorForm(newTopicNews, request);
				//a defautlna stranka, nieco ako hello world
				EditorForm newTopicDefaultDoc = EditorDB.getEditorForm(request,-1,-1,newTopicFolder.getGroupId());
					newTopicDefaultDoc.setTitle( Prop.getInstance(request).getText("components.blog.default_page_title") );
					newTopicDefaultDoc.setData( Prop.getInstance(request).getText("components.blog.default_page_text") );
					newTopicDefaultDoc.setAuthorId( authorId );
					newTopicDefaultDoc.setAvailable(true);
					newTopicDefaultDoc.setSearchable(true);
					newTopicDefaultDoc.setCacheable(false);
					newTopicDefaultDoc.setPublish("1");
				EditorDB.saveEditorForm( newTopicDefaultDoc,request);
				
				EditorDB.cleanSessionData(request);
				
				state = Prop.getInstance(request).getText("components.blog.topics.topic_founded");
			}
		}
	}
	catch(NumberFormatException e)
	{
		state = Prop.getInstance(request).getText("components.blog.topics.not_blogger");
	}
	//------------------------KONIEC SPRACOVANIA FORMULARU----------------------------
%>

<form action="blog_new_topic.jsp" method="post" name="addUserForm">
	<table>
		<tr>
			<td><label for="topicId"><iwcm:text key="components.blog.topics.topic_name" />:</label></td>
			<td><input type="text" class="required safeChars" id="topicId" size="30" name="topic" value="<%=request.getParameter("topic") != null ? org.apache.struts.util.ResponseUtils.filter(request.getParameter("topic")) : ""%>"/></td>
		</tr>		
		<input type="submit" id="submit" name="submit" style="display:none;" />
	</table>
</form>

<span style="color: red;font-weight: bold;"><%=state%></span>

</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>