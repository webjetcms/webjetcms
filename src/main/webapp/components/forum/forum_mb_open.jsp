<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.util.*,sk.iway.iwcm.*, sk.iway.iwcm.users.*,sk.iway.iwcm.doc.*" %>
<%@page import="org.apache.commons.codec.binary.Base64"%><%@ page import="sk.iway.iwcm.i18n.Prop" %><%@ page import="sk.iway.iwcm.components.forum.jpa.DocForumEntity" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
DocDB docDB = DocDB.getInstance();
Prop prop = Prop.getInstance(lng);

PageParams pageParams = new PageParams(request);

boolean isAjaxCall = request.getAttribute("docDetails")==null;
if(isAjaxCall)
{
	pageParams = (PageParams) session.getAttribute("forumMBOpenPageParams");
	if (pageParams == null)
	{
		//nemame session, vrat nieco zmysluplne
		%><iwcm:text key="forum.errorLoadingData"/><%
		return;
	}
}
session.setAttribute("forumMBOpenPageParams",pageParams);


boolean sortAscending = pageParams.getBooleanValue("sortAscending", true);
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
   GroupDetails parentGroup = GroupsDB.getInstance().getGroup(actualGroup.getParentGroupId());
   if (parentGroup != null) rootForumId = parentGroup.getDefaultDocId();

}

int pageSize = pageParams.getIntValue("pageSize", 10);
if(request.getAttribute("pageSize") != null)
	pageSize = sk.iway.iwcm.Tools.getIntValue((String)request.getAttribute("pageSize"), 10);

request.setAttribute("pageSize", String.valueOf(pageSize));

int pageNum = sk.iway.iwcm.Tools.getIntValue(request.getParameter("pageNum"), 1);

int profileDocId = 4;
if(request.getAttribute("profileDocId") != null) profileDocId = sk.iway.iwcm.Tools.getIntValue((String)request.getAttribute("profileDocId"), -1);

boolean active = ForumDB.isActive(docId);
int parentId = Tools.getIntValue(request.getParameter("pId"), 0);
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
Hashtable<String, String> emoticons = new Hashtable<>();
String outStr = "";
String fTitle = "";
UserDetails uDet = null;

DocForumEntity baseForumBean = new DocForumEntity();
ForumGroupEntity forumGroupBean = ForumDB.getForum(docId);

try
{
	if (parentId > 0)
	{
		//listovanie v podtemach, testujem parameter inc, ak je +1 beriem nasled. polozku v zozname
		//ak je -1, beriem predchadzajucu
		DocForumEntity fb;
		boolean notFound = true;
		int listIndex = 0;

		baseForumBean = ForumDB.getForumBean(request, parentId, sortAscending);
		if (baseForumBean != null)
		{
			//!! - IMPORTANT check
			//In case of deleted or not confirmed (or not approved) forum we will not show forum unless user is logged in and admin
			if((baseForumBean.getDeleted() || !baseForumBean.getConfirmed()) && (user == null || !user.isAdmin())) {
				%> <p><span class="forumClosed"><iwcm:text key="components.forum.not_exist_error"/>!</span></p> <%
				return;
			};

			if(active) {
				active = baseForumBean.isActive();
				fTitle = baseForumBean.getSubject();
				pageContext.setAttribute("forumActive", "");
			} else pageContext.setAttribute("forumClosed", "");
		}
		else pageContext.setAttribute("forumClosed", "");
	}

	//definovanie emotikonov, ktore sa nahradia
	emoticons.put(":-)", "<img src='/components/emoticon/w_regular_smile.gif' style='border:0px'>");
	emoticons.put(":-(", "<img src='/components/emoticon/w_sad_smile.gif' style='border:0px'>");
	//odstranime aj prazdne odstavce
	emoticons.put("<p>&nbsp;</p>", "");

}
catch(Exception ex)
{
	sk.iway.iwcm.Logger.error(ex);
}

int iMod = 1;
List forum = null;
String offset = "0";
String end = ""+(pageSize);
if (docId > 0 && parentId > 0)
{
	forum = ForumDB.getForumFieldsForDoc(request, docId, true, parentId, sortAscending, false);
	//pageContext.setAttribute("forum", forum);
	ForumDB.updateForumStatViews(parentId);

	if(forum.size() > 0)
		pageContext.setAttribute("forum", forum);
	else
		pageContext.setAttribute("emptyForum", "true");
}

boolean canPostNewTopic = true;
boolean isAdmin = false;
if (active==false || (baseForumBean != null && (baseForumBean.getDeleted() || !baseForumBean.getConfirmed()))) canPostNewTopic = false;
if (forumGroupBean.isAdmin(user))
{
	canPostNewTopic = true;
	isAdmin = true;
}

//nacitaj povolenia na upload
LabelValueDetails uploadLimits = null;
if (user != null) uploadLimits = ForumDB.getUploadLimits(docId, request);
%>
<%@page import="sk.iway.spirit.MediaDB"%>
<%@page import="sk.iway.spirit.model.Media"%>

<!-- // dialogove okno s funkciami -->
<jsp:include page="/components/dialog.jsp" />

<%
if(!isAjaxCall)
{
	//toto treba vyrendrovat len ak sa to nevola z ajaxu, aby sa to nekaskadovalo dnu do forumContentDiv
%>

	<script type="text/javascript">
	<!--
	function popupNewForum(parent, docId, isCite)
	{
		var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
		//editwindow=window.open("/addforum.do?parent="+parent+"&docid="+docId,'forumNew',options);
		editwindow=window.open("/components/forum/new.jsp?parent="+parent+"&type=mb_open&rootForumId=<%=rootForumId%>&docid="+docId+"&isCite="+isCite,'forumNew',options);
		if (!editwindow.opener) editwindow.opener = self;
		if (window.focus) {editwindow.focus()}
	}

	function popupNewUpload(forumId, docId)
	{
		var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
		editwindow=window.open("/components/forum/new_file.jsp?forumId="+forumId+"&docid="+docId,'forumNew',options);
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

	<style type="text/css" media="screen">
		@import "/components/forum/forum_mb.css";
	</style>

	<% if (isAdmin) { %>
	<style type="text/css">
	tr.trDeleted td, tr.trDeleted td a, tr.trDeleted td strong, tr.trDeleted td p, tr.trDeleted td span { color: red !important }
	</style>
	<% } %>

	<form method="get" action="<%=PathFilter.getOrigPath(request) %>" name="actionForm" style="display: none;">
		<div>
	    <input type="hidden" name="act" />
	    <input type="hidden" name="docid" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("docid"))%>" />
		 <input type="hidden" name="forumId" />
		 <input type="hidden" name="pId" />
		 <input type="hidden" name="pageNum" value="<%=pageNum%>" />
		</div>
	</form>

	<div id="forumContentDiv">
<%
}
%>

<iwcm:present name="forumClosed">
	<p><span class="forumClosed"><iwcm:text key="components.forum.forum_closed"/>!</span></p>
</iwcm:present>

<iwcm:present name="delTimeLimitExpired">
  <div align="center" >
		<p><iwcm:text key="components.forum.del_time_limit_expired"/></p>
 </div>
 <%request.removeAttribute("delTimeLimitExpired");%>
</iwcm:present>

<h1 class="maintitle"><%=fTitle%></h1>
	<div class="row mobile-fix">
		<div class="col-md-2 col-xs-12">
			<% if (canPostNewTopic) { %>
			<a class="btn btn-primary btn-sm btn-forum-new-reply" href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=<%=parentId%>&parent2=<%=parentId%>&type=mb_open&rootForumId=<%=rootForumId%>&docid=<%=docId%>&isCite=false&pageNum=<%=pageNum%>');">
		 		<iwcm:text key="components.forum.post.reply"/>
			</a>
			<% } %>
		</div>

		<div class="col-md-3 col-xs-12">
			<nav aria-label="breadcrumb">
				<ol class="breadcrumb">
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(rootForumId)%>"><iwcm:text key="components.forum.show_topics"/></a></li>
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(docId)%>"><iwcm:beanWrite name="doc_title"/></a></li>
				</ol>
			</nav>
		</div>

		<% if(forum != null && forum.size() > 0)
			{
				request.setAttribute("pagingList", forum);
		%>
				<jsp:include page="paging_component.jsp" />
		<%	}%>

	</div>

<iwcm:present name="emptyForum">
<table cellspacing="0" cellpadding="0" style="width:100%;" class="forumTableOpen" >
	<tr>
	  <td colspan=2>
			<div class="forumBlank"><iwcm:text key="forum.empty"/></div>
	  </td>
	</tr>
</table>
</iwcm:present>

<!--  ******** KOMPONENTA STRANKOVANIA ********  -->
<%
	if(request.getAttribute("offset") != null)
		offset = request.getAttribute("offset").toString();
	if(request.getAttribute("end") != null)
		end = request.getAttribute("end").toString();
 %>
<!--  ***** KONIEC KOMPONENTY STRANKOVANIA ***** -->

<iwcm:present name="forum">

	<!--table class="forumline table table-striped table-bordered">
		<thead class="topiclist">
			<tr class="hlavicka">
				<th class="thLeft" style="width:150px;"><iwcm:text key="forum.author"/></th>
				<th class="thRight"><iwcm:text key="components.forum.message_text"/></th>
			</tr>
		</thead>
	</table-->
	<iwcm:iterate offset="<%= offset%>" length="<%= end%>" name="forum" id="field" type="DocForumEntity" indexId="index">

	<%
		String trClass = "";
	    if (field.isDeleted()) trClass = " trDeleted";
		if ((iMod % 2) == 0) {
		  out.println("<div class='row2 even"+trClass+"'>");
	   }
	   else
	   {
		  out.println("<div class='row1 odd"+trClass+"'>");
		}
		iMod++;
	%>
		<div class="panel panel-primary">
			<div class="panel-body no-padding">
				<div class="row no-margin">

					<div class="col-md-3 col-xs-12 post-info post-info-left text-center">

					<a name="post<%=field.getForumId()%>"></a>

					<%if (!active || !field.getActive()) {%>
						<img src="/components/forum/images/folder_locked_big.gif" style="border:0px;" align="absbottom"/>
					<%}%>

					<iwcm:notEmpty name="field" property="authorEmail">
						<span class="name"><a href="mailto:<iwcm:beanWrite name="field" property="authorEmail"/>"><iwcm:beanWrite name="field" property="authorName"/></a></span>
					</iwcm:notEmpty>
					<iwcm:empty name="field" property="authorEmail">
						<span class="name"><iwcm:beanWrite name="field" property="authorName"/></span>
					</iwcm:empty>
					<br/>
					<span class="postdetails">
					<%
					uDet = null;
					if (field.getUserId() > 0)
					{
						uDet = UsersDB.getUserCached(field.getUserId());

						//FORUM RANK
						String photo = null;
						if(uDet != null)
						{
							if (forumGroupBean.isAdmin(uDet)) out.println(prop.getText("components.forum.bb.rank.admin"));
							else if(uDet.getForumRank() < 100) out.println(prop.getText("components.forum.bb.rank.lt_100"));
							else if(uDet.getForumRank() >= 100 && uDet.getForumRank() < 500) out.println(prop.getText("components.forum.bb.rank.100-500"));
							else if(uDet.getForumRank() >= 500) out.println(prop.getText("components.forum.bb.rank.gt-500"));
							out.println("<br/>");

							photo = uDet.getPhoto();
						}
						if (Tools.isEmpty(photo)) photo = Constants.getString("ntlmDefaultUserPhoto");
						if(photo != null && photo.length()>2){ %>
						<img style="margin:5px 0px 5px 0px;" src="/thumb<%=photo%>?w=150&amp;h=150&amp;ip=1" style="border:0px;"><br/>
						<%
						}
						if (uDet != null) {
							out.println(prop.getText("components.forum.bb.posts")+": "+uDet.getForumRank());
						}
					}
					%>
					<%
					if (profileDocId > 0 && field.getUserId() > 0 && uDet != null) {
					%>
						<br/><!-- stranka s profilom usera -->
						<br/><span class="profile" ><a href="<%=Tools.addParameterToUrl(docDB.getDocLink(profileDocId),"uId",String.valueOf(field.getUserId()))%>"><iwcm:text key="components.forum.display_profile"/></a></span><br/>
						<% }else if(field.getUserId() > 0 && uDet != null) {%>
						<br/><span class="profile" ><a href="<%=Tools.addParameterToUrl(docDB.getDocLink(docId),"uId",String.valueOf(field.getUserId()))%>&amp;type=view_profile"><iwcm:text key="components.forum.display_profile"/></a></span><br/>
					<%}%>
					</span>
				</div>

				<div class="col-md-9 col-xs-12 post-content post-content-right">

						<div class="row post-head hidden-xs no-margin-bottom">
							<div class="col-md-8 col-xs-8 author">
								<span class="postdetails"><iwcm:text key="components.forum.bb.posted"/>: <iwcm:beanWrite name="field" property="questionDateDisplayDate"/> <iwcm:beanWrite name="field" property="questionDateDisplayTime"/>
								<span class="panel-title"><iwcm:text key="components.forum.bb.subject"/>: <iwcm:beanWrite name="field" property="subject"/></span>
							</div>
							<div class="col-md-4 col-xs-4 no-padding">
									<div class="btn-toolbar topic-buttons" role="toolbar">
										<%	if (field.canUpload(user, uploadLimits, forumGroupBean)){%>
										<div class="btn-group">
											<a class="btn btn-default" href="javascript:popupNewUpload(<iwcm:beanWrite name="field" property="forumId"/>, <%=docId%>, <%=parentId%>);">
												<iwcm:text key="components.forum.upload"/>
											</a>
										</div>
										<% } %>

										<%if (field.canPost(forumGroupBean, user)) {%>
										<div class="btn-group">
											<span>
												<a class="btn btn-info" href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=<iwcm:beanWrite name="field" property="forumId"/>&parent2=<%=parentId%>&type=mb_open&rootForumId=<%=rootForumId%>&docid=<%=docId%>&isCite=true&pageNum=<%=pageNum%>');">
													<iwcm:text key="components.forum.quote"/>
												</a>
											</span>
										</div>
										<% } %>

										<%
										 	if (field.canDelete(user, pageParams.getIntValue("delMinutes", 30), forumGroupBean)){%>
										 	<div class="btn-group">
										 		<span class="deleteMessage">
										 			<a class="btn btn-danger" href="javascript:delMessage('<%=field.getForumId()%>');">
										 				<iwcm:text key="components.forum.delete_message"/>
										 			</a>
										 		</span>
										 	</div>
										<% } %>

									</div>
							</div>
						</div>
						<div class="content">
							<%
								outStr = field.getQuestion();
								for (Map.Entry<String, String> entry : emoticons.entrySet())
								{
									outStr = DB.replace(outStr, entry.getKey(), entry.getValue());
								}
								out.println(outStr);
							%>

							<%
							//nacitanie priloh (ak nejake su)
							List prilohy = MediaDB.getMedia(session, "document_forum", field.getForumId(), null);
							if (prilohy!=null && prilohy.size()>0)
							{
								Iterator iter = prilohy.iterator();
								Media m;
								String iconUrl, fileSize;
								out.println("<ul>");
								while (iter.hasNext())
								{
								   m = (Media)iter.next();
								   iconUrl = FileTools.getFileIcon(m.getMediaLink());
								   fileSize = FileTools.getFileLength(m.getMediaLink());

								   if (Tools.isEmpty(fileSize) || "0 B".equals(fileSize))
								   {
								   	//asi to nie je subor / subor neexistuje
								   	//out.println("<li><a href='"+m.getMediaLink()+"' title='"+m.getMediaTitleSk()+"'>"+m.getMediaTitleSk()+"</a></li>");
								   }
								   else
								   {
								   	//je to subor, zobrazime aj ikonu aj velkost suboru
								   	out.println("<li><img src='"+iconUrl+"' alt='media' /> <a href='"+m.getMediaLink()+"'>"+m.getMediaTitleSk()+"</a> ["+fileSize+"]</li>");
								   }
								}
								out.println("</ul>");
							}
							%>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- post -->


	</iwcm:iterate>
	<!--/table-->

	<div class="panel-footer post-footer">
		<div class="to-top pull-right"> </div>
	</div>

</iwcm:present>

	<div class="row mobile-fix">
		<div class="col-md-2 col-xs-12">
			<% if (canPostNewTopic) { %>
			<a class="btn btn-primary btn-sm" href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=<%=parentId%>&parent2=<%=parentId%>&type=mb_open&rootForumId=<%=rootForumId%>&docid=<%=docId%>&isCite=false&pageNum=<%=pageNum%>');">
		 		<iwcm:text key="components.forum.post.reply"/>
			</a>
			<% } %>
		</div>

		<div class="col-md-3 col-xs-12">
			<nav aria-label="breadcrumb">
				<ol class="breadcrumb">
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(rootForumId)%>"><iwcm:text key="components.forum.show_topics"/></a></li>
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(docId)%>"><iwcm:beanWrite name="doc_title"/></a></li>
				</ol>
			</nav>
		</div>

		<% if(forum != null && forum.size() > 0)
			{
				request.setAttribute("pagingList", forum);
		%>
				<jsp:include page="paging_component.jsp" />
		<%	}%>
	</div>
<% request.removeAttribute("delTimeLimitExpired");
if(!isAjaxCall)
{%>
</div>
<%}%>