<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*,sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.util.*,sk.iway.iwcm.*, sk.iway.iwcm.users.*" %>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%!

public boolean isAdmin(Identity user, ForumGroupEntity fgb)
{
	if (user == null) return false;
	if (user.isAdmin()) return true;

	String ids = fgb.getAdminGroups();
	if(ids != null )
	{
		StringTokenizer st = new StringTokenizer(ids,"+,;");
		while(st.hasMoreTokens())
		{
			if(user.isInUserGroup(Tools.getIntValue(st.nextToken(), -1))) return true;
		}
	}
	return false;
}

%>
<%
session.setAttribute("forum-shown", Boolean.TRUE);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
DocDB docDB = DocDB.getInstance();

List<DocForumEntity> topics = Collections.emptyList();

PageParams pageParams = new PageParams(request);

boolean isAjaxCall = request.getAttribute("docDetails")==null;
if(isAjaxCall)
{
	pageParams = (PageParams) session.getAttribute("forumMBPageParams");
	if (pageParams == null)
	{
		//nemame session, vrat nieco zmysluplne
		%><iwcm:text key="forum.errorLoadingData"/><%
		return;
	}
}
session.setAttribute("forumMBPageParams",pageParams);


boolean useDelTimeLimit =  pageParams.getBooleanValue("useDelTimeLimit", false);
int delMinutes = pageParams.getIntValue("delMinutes", 30);
int pageSize = pageParams.getIntValue("pageSize", 10);
int pageLinksNum = pageParams.getIntValue("pageLinksNum", 5);
int allowedUserGroup = pageParams.getIntValue("allowedUserGroup", -1);
int profileDocId = pageParams.getIntValue("profileDocId", -1);
int searchPostsDocId = pageParams.getIntValue("searchPostsDocId", -1);
boolean sortAscending = pageParams.getBooleanValue("sortAscending", true);
boolean showSearchBox = pageParams.getBooleanValue("showSearchBox", true);
String type = pageParams.getValue("type", "topics");
ForumSortBy sortTopicsBy = ForumSortBy.valueOf(pageParams.getValue("sortTopicsBy", ForumSortBy.LastPost.name()));

boolean rootGroup = pageParams.getBooleanValue("rootGroup", false);
request.removeAttribute("rootGroup");
if("true".equals(request.getParameter("rootGroup")) || rootGroup==true)
{
	request.setAttribute("rootGroup", "true");
	rootGroup = true;
}

int docId = Tools.getIntValue(request.getParameter("docid"), -1);
int rootForumId = Tools.getIntValue(request.getParameter("rootForumId"),-1);
if(isAjaxCall)
{
	DocDetails docDetails = docDB.getDoc(docId);
	if(docDetails!=null)
	{
		request.setAttribute("pageGroupDetails",docDetails.getGroup());
		request.setAttribute("doc_title",docDetails.getTitle());
		request.setAttribute("perex_data",docDetails.getPerex());
	}
}
if (request.getParameter("rootForumId")==null)
{
   //vydedukuj
   GroupDetails actualGroup = (GroupDetails)request.getAttribute("pageGroupDetails");

   //Fix - when creating new page, actualGroup is null (doest not exist yet)
   //User we not see previu of app but at least it wont throw exception
   if(actualGroup == null) return;

   GroupDetails parentGroup = GroupsDB.getInstance().getGroup(actualGroup.getParentGroupId());
   if (parentGroup != null) rootForumId = parentGroup.getDefaultDocId();
}
if (rootGroup) rootForumId = docId;

request.setAttribute("useDelTimeLimit", pageParams.getValue("useDelTimeLimit", "false"));
request.setAttribute("delMinutes", pageParams.getValue("delMinutes", "30"));
request.setAttribute("pageSize", pageParams.getValue("pageSize", "10"));
request.setAttribute("pageLinksNum", ""+pageLinksNum);
request.setAttribute("allowedUserGroup", ""+allowedUserGroup);
request.setAttribute("profileDocId", ""+profileDocId);
request.setAttribute("searchPostsDocId", ""+searchPostsDocId);


boolean active = false;
boolean del = false;
boolean advertisementType = false;

int parentId = Tools.getIntValue(request.getParameter("pId"), -1);
Identity user = (Identity) session.getAttribute(Constants.USER_KEY);

if(request.getParameter("words")!=null)
{
	pageContext.include("forum_mb_search.jsp");
	return;

}
if ("search".equals(type))
{
	pageContext.include("forum_mb_search.jsp");
	return;
}
else if ("user_posts".equals(type)||"user_posts".equals(request.getParameter("type")))
{
	pageContext.include("forum_mb_search_user_posts.jsp");
	return;
}
else if ("view_profile".equals(type)||"view_profile".equals(request.getParameter("type")))
{
	pageContext.include("forum_mb_view_user_profile.jsp");
	return;
}

int iMod = 1;
String action = request.getParameter("act");
if("delete".equals(action) && request.getParameter("forumId") != null && user != null)
{
	int forumId = Tools.getIntValue(request.getParameter("forumId"), -1);
	if (forumId > 0)
	{
		ForumGroupEntity forumGroupBean = ForumDB.getForum(docId);
		if (useDelTimeLimit && forumGroupBean.isAdmin(user)==false)
		{
			DocForumEntity fb = ForumDB.getForumBean(request, forumId, sortAscending);
			//porovnam cas. hranicu pre moznost vymazania prispevku a aktualny cas,
			//cas pridania prispevku + pocet min pre povolenie vymazania(prehodeny na milisec.)
			if(fb!=null && fb.getQuestionDate()!=null)
			{
				long delTimeLimitMsec = fb.getQuestionDate().getTime() + (delMinutes*60*1000);
				if(delTimeLimitMsec > Tools.getNow())
					del = ForumDB.deleteMessage(forumId, docId, user);
				else
					request.setAttribute("delTimeLimitExpired","true");
			}
		}
		else
		{
			del = ForumDB.deleteMessage(forumId, docId, user);
		}
	}
}

String offset = "0";
String end = ""+(pageSize);

ForumGroupEntity forumGroupBean = null;

if (docId > 0 && parentId > 0)
{
	request.setAttribute("pageSize", ""+pageSize);
	pageContext.include("forum_mb_open.jsp");
	return;
}
else if (docId > 0)
{
	forumGroupBean = ForumDB.getForum(docId);
	advertisementType = forumGroupBean.isAdvertisementType();
	if (advertisementType && forumGroupBean.isAdmin(user)) advertisementType = false;

	if(forumGroupBean.isAdmin(user))
		topics = ForumDB.getForumTopics(docId, false, true);
	else
		topics = ForumDB.getForumTopics(docId, true, false);
	if (topics.size() > 0)
		request.setAttribute("topics", topics);

	DocForumEntity fb = ForumDB.getForumStat(docId);
	if (fb != null)
	{
		//out.println("<strong>FORUM STAT - last post:</strong> " +fb.getLastPost()+ "&nbsp;&nbsp;&nbsp;<strong>total messages:</strong> " +fb.getStatReplies()+ "<br><br>");
	}
	active = forumGroupBean.isActive();
}

int count = 0;

boolean canPostNewTopic = true;
if (advertisementType || active==false) canPostNewTopic = false;
if (forumGroupBean.isAdmin(user)) canPostNewTopic = true;
%>

<%
if(!isAjaxCall)
{
	//toto treba vyrendrovat len ak sa to nevola z ajaxu, aby sa to nekaskadovalo dnu do forumContentDiv
%>

	<!-- // dialogove okno s funkciami -->
	<jsp:include page="/components/dialog.jsp" />

	<style type="text/css" media="screen">
		@import "/components/forum/forum_mb.css";
	</style>

	<script type="text/javascript">
	<!--
	function popupNewForum(parent, docId)
	{
		var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
		//editwindow=window.open("/addforum.do?parent="+parent+"&docid="+docId,'forumNew',options);
		editwindow=window.open("/components/forum/new.jsp?parent="+parent+"&type=mb&rootForumId=<%=rootForumId%>&docid="+docId,'forumNew',options);
		if (!editwindow.opener) editwindow.opener = self;
		if (window.focus) {editwindow.focus()}
	}

	function delMessage(forumId)
	 {
		 if(window.confirm('<iwcm:text key="components.forum.delete_message_prompt"/> ?') == true)
	     {
	       document.actionForm.act.value="delete";
	       document.actionForm.forumId.value = forumId;
	       document.actionForm.pId.value = <%=parentId%>;
	       document.actionForm.submit();
	     }
	 }
	 -->
	 </script>

	<form method="get" action='/showdoc.do' name="actionForm">
	<p>
	    <input type="hidden" name="act" />
	    <input type="hidden" name="docid" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("docid"))%>" />
		 <input type="hidden" name="forumId" />
		 <input type="hidden" name="pId" />
	     </p>
	</form>

	<div id="forumContentDiv">
<%
}
%>
<!-- HLAVNE FORUM*********************************************************************************************** -->
<iwcm:notPresent name="rootGroup">
<h1 class="maintitle"><bean:write name="doc_title"/></h1>
<p class="mainSubtitle"><bean:write name="perex_data" filter="false"/></p>
</iwcm:notPresent>

<iwcm:present name="rootGroup">
	<%if(showSearchBox==true){%>
	<div class="forumSearchBox row mobile-fix">
		<div class="col-md-4 offset-md-8 col-xs-12 pull-right pr-0">
			<form id="forum-search" class="form-search pull-right ng-pristine ng-valid" name="forumQuickSearch" action="<%=docDB.getDocLink(docId)%>">
				<div class="input-group">
					<input class="form-control input-medium search form-control" name="words" />
					<span class="input-group-btn">
						<input type="hidden" name="rootForumId" value="<%=rootForumId%>" />
						<a class="btn btn-primary btn-default" href="javascript:document.forumQuickSearch.submit();"><iwcm:text key="components.forum.search.btn" /></a>
					</span>
				</div>
			</form>
		</div>
	</div>
	<%}%>

	<div class="table-responsive">
		<table class="forumline table table-striped table-bordered">
			<thead class="topiclist">
				<tr class="forumHead">
					<th class="thCornerL forum-name"><i class="fa fa-sitemap"></i> <iwcm:text key="forum.title"/></th>
					<th class="thTop topics"><i class="fa fa-comments-o"></i> <iwcm:text key="components.forum.topics_number"/></th>
					<th class="thTop posts"><i class="fa fa-pencil-square-o"></i> <iwcm:text key="components.forum.posts_number"/></th>
					<th class="thCornerR lastpost"><i class="fa fa-history"></i> <iwcm:text key="components.forum.last_post"/></th>
				</tr>
			</thead>
			<%

				GroupsDB groupsDB = GroupsDB.getInstance();
				List groups = groupsDB.getGroups(docDB.getDoc(docId).getGroupId());
				for(int i=0;i<groups.size();i++) {
					GroupDetails gd = (GroupDetails)groups.get(i);
			%>
			<thead class="topiclist">
				<tr>
					<th class="catLeft" colspan="4"><span class="cattitle"><%=gd.getGroupName()%></span></th>
				</tr>
			</thead>
			<%
				List docs = docDB.getDocByGroup(gd.getGroupId(), DocDB.ORDER_PRIORITY, true, -1, -1, false);
				for(int j=0;j<docs.size();j++)
				{
					DocDetails dd = (DocDetails)docs.get(j);
					ForumGroupEntity fgb = ForumDB.getForum(dd.getDocId());
					String[] last = ForumDB.getForumLastPostDate(dd.getDocId());
					String icon = "folder_big.gif";
					if (fgb.isActive()==false) icon = "folder_locked_big.gif";
			%>
			<tr>
				<td class="row1 forum-name">
					<span class="pull-left forum-icon"> </span>
					<span class="forumlink"> <a class="forumtitle" href="<%=docDB.getDocLink(dd.getDocId())%>"><%=dd.getTitle()%></a><br /></span>
					<span class="genmed"><%=dd.getPerex()%><br /></span>
				</td>
				<td class="row2 topics"><span class="badge"><%=ForumDB.getForumTopicsCount(dd.getDocId())%></span></td>
				<td class="row2 posts"><span class="badge"><%=ForumDB.getForumPostsCount(dd.getDocId())%></span></td>
				<td class="row2 lastpost">
			   		<%if(last!=null) {%>
					<span>
					<%int lastUserId=Tools.getIntValue(last[2],-1);
						if(lastUserId>0){
						%>
						<a href="<%=Tools.addParameterToUrl(docDB.getDocLink(dd.getDocId()),"uId",String.valueOf(lastUserId))%>&amp;type=view_profile"><%=last[1]%></a>
						<%}else out.print(last[1]);%>
						<br/>
						<%=last[0]%>
					</span>
					<%}else out.print("&nbsp;");%>
				</td>
			</tr>
			<%
					}
				}
			%>
	   </table>
	</div>
</iwcm:present>
<!-- KONIEC HLAVNE FORUM*********************************************************************************** -->

<iwcm:present name="delTimeLimitExpired">

<br>
 <div align="center" >
		<iwcm:text key="components.forum.del_time_limit_expired"/>
 </div>
 <%request.removeAttribute("delTimeLimitExpired");%>
</iwcm:present>
<iwcm:notPresent name="rootGroup">
 <%if(advertisementType){%>
    <span class="forumAdvertisement"><iwcm:text key="components.forum.advertisement_type_message"/></span>
 <%} if(!active){%>
	 <span class="forumClosed"><iwcm:text key="components.forum.forum_closed"/></span>
 <%}%>
	<div class="row mobile-fix">
		<div class="col-md-2 col-xs-12">
			<% if (canPostNewTopic) { %>
			<a class="btn btn-primary btn-sm btn-forum-new-topic" href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=<%=parentId%>&type=mb&rootForumId=<%=rootForumId%>&docid=<%=docId%>&language=<%=lng%>');" title="<iwcm:text key="components.forum.new_topic"/>">
				<!--span class="btn-label"><i class="fa fa-pencil-square-o"></i></span-->
				<iwcm:text key="components.forum.new_topic"/>
			</a>
			<% } %>
		</div>

		<div class="col-md-6 col-xs-12">
			<nav aria-label="breadcrumb">
				<ol class="breadcrumb">
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(rootForumId)%>"><iwcm:text key="components.forum.show_topics"/></a></li>
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(docId)%>"><bean:write name="doc_title"/></a></li>
				</ol>
			</nav>
		</div>

		<%if(showSearchBox==true){%>
			<div class="col-md-4 col-xs-12 pr-0">
				<form id="forum-search" class="form-search pull-right ng-pristine ng-valid" name="forumQuickSearch" action="<%=docDB.getDocLink(docId)%>">
					<div class="input-group">
						<input class="input-medium search form-control" name="words" />
						<span class="input-group-btn">
							<input type="hidden" name="rootForumId" value="<%=rootForumId%>" />
							<a class="btn btn-primary btn-default" href="javascript:document.forumQuickSearch.submit();"><iwcm:text key="components.forum.search.btn" /></a>
						</span>
					</div>
				</form>
			</div>
		<%}%>

		<% if(topics != null && topics.size() > 0)
			{
				request.setAttribute("pagingList", topics);
				request.setAttribute("pagingListKey", "components.forum.number_of_topics");
				pageContext.include("/components/forum/paging_component.jsp");
			}
		%>

	</div>

<iwcm:present name="topics">
<!--  ******** KOMPONENTA STRANKOVANIA ********  -->
<%
	if(request.getAttribute("offset") != null)
		offset = request.getAttribute("offset").toString();
	if(request.getAttribute("end") != null)
		end = request.getAttribute("end").toString();
 %>
<!--  ***** KONIEC KOMPONENTY STRANKOVANIA ***** -->
<div class="table-responsive">
	<table class="forumline table table-striped table-bordered">
		<thead class="topiclist">
		<tr class="forumHead">
			<th class="thCornerL forum-name"><iwcm:text key="components.forum.forum_topics"/></th>
			<th class="thTop posts"><iwcm:text key="components.forum.bb.replies"/></th>
			<th class="thTop autor"><iwcm:text key="components.forum.bb.author"/></th>
			<th class="thTop views"><iwcm:text key="components.forum.bb.views"/></th>
			<th class="thCornerR lastpost"><iwcm:text key="components.forum.last_post"/></th>
		</tr>
		</thead>
		<logic:iterate offset="<%= offset%>" length="<%= end%>" name="topics" id="t" type="DocForumEntity" indexId="index">

			<% if ((iMod % 2) == 0) {
				  out.println("<tr class='even'>");
			   }
			   else
			   {
				  out.println("<tr class='odd'>");
				}
				iMod++;
				System.out.println("t.isDeleted(): "+t.isDeleted());
				if(t != null && t.isDeleted())
					System.out.println(" >>> OK");
			%>

			   <td class="row1 forum-name">
			   	<%
					String flag = ForumDB.getForumBean(request, t.getForumId()).getFlag();
					%>

					<% if(!t.getDeleted() && t.getForumGroupEntity().getActive() && t.getConfirmed()) { %> <img src="/components/forum/images/folder_big.gif" style="border:0px;" align="absbottom"/> <% } %>
					<% if(t.getDeleted() || !t.getConfirmed()) { %> <img src="/components/forum/images/icon_del.gif" style="border:0px;" align="absbottom"/> <% } %>
					<% if(!t.getForumGroupEntity().getActive()) { %> <img src="/components/forum/images/folder_locked_big.gif" style="border:0px;" align="absbottom"/> <% } %>
			      <%	if (t.canDelete(user, delMinutes)) { %><a href="javascript:delMessage('<%=t.getForumId()%>');"><img src="/components/forum/images/icon_trash.gif" style="border:0px;" align="absbottom" title='<iwcm:text key="components.forum.delete_message"/>'></a>&nbsp;<%	} %>
  			      <span class="topictitle"><%
			        if("O".equals(flag)) {%><b><iwcm:text key="components.forum.announcement"/>: </b><%}
					  else if("D".equals(flag)) {	%><b><iwcm:text key="components.forum.sticky"/>: </b><%}
					%><a href='<%=Tools.addParametersToUrl(docDB.getDocLink(Tools.getIntValue(request.getParameter("docid"), -1)), "pId="+t.getForumId())%>' ><%if(t != null && (t.isDeleted() || !t.getForumGroupEntity().getActive() || !t.isConfirmed())) out.print("<span style=\"color: red;\">");%><bean:write name="t" property="subject"/><%if(t != null && (t.isDeleted() || !t.getForumGroupEntity().getActive() || !t.isConfirmed())) out.print("</span>");%></a></span>
            </td>
			   <td class="row2 posts"><span class="postdetails badge"><bean:write name="t" property="statReplies"/></span></td>
			   <td class="row3 autor">
			      <span class="name">
			      <% if (t.getUserId()>0) { %>
				   <a href="<%=Tools.addParameterToUrl(docDB.getDocLink(t.getDocId()),"uId",String.valueOf(t.getUserId()))%>&amp;type=view_profile"><bean:write name="t" property="authorName"/></a>
				   <% } else { %>
				   <bean:write name="t" property="authorName"/>
				   <% } %>
			      </span>
			   </td>
			   <td class="row2 views"><span class="postdetails badge"><bean:write name="t" property="statViews"/></span></td>
			   <td class="row3 lastpost"><span class="postdetails"><bean:write name="t" property="lastPost"/></span></td>

			</tr>
		</logic:iterate>
	</table>
</div>
	<div class="row mobile-fix">
		<% if(topics != null && topics.size() > 0)
			{
				request.setAttribute("pagingList", topics);
				request.setAttribute("pagingListKey", "components.forum.number_of_topics");
				pageContext.include("/components/forum/paging_component.jsp");
			}
		%>
	</div>
</iwcm:present>
</iwcm:notPresent>
<%request.removeAttribute("delTimeLimitExpired");%>
<%if(!isAjaxCall){%>
</div>
<%}%>