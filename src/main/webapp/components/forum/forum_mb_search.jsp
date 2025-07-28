<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.util.*,sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
DocDB docDB = DocDB.getInstance();

int rootForumId = Tools.getIntValue(request.getParameter("rootForumId"),-1);
if (request.getParameter("rootForumId")==null)
{
   //vydedukuj
   GroupDetails actualGroup = (GroupDetails)request.getAttribute("pageGroupDetails");
   GroupDetails parentGroup = null;
   if(actualGroup != null) GroupsDB.getInstance().getGroup(actualGroup.getParentGroupId());
   if (parentGroup != null) rootForumId = parentGroup.getDefaultDocId();
}

PageParams pageParams = new PageParams(request);
boolean sortAscending = pageParams.getBooleanValue("sortAscending", true);

int pageSize = 10;
if(request.getAttribute("pageSize") != null)
	pageSize = sk.iway.iwcm.Tools.getIntValue((String)request.getAttribute("pageSize"), 10);

boolean active = false;
int docId = Tools.getIntValue(request.getParameter("docid"), -1);
int forumDocId = Tools.getIntValue(request.getParameter("forumDocId"), -1);
if (forumDocId < 1) forumDocId = docId;

List searchResults = new ArrayList();

String offset = "0";
String end = ""+(pageSize);

String searchStr = request.getParameter("words");
try
{
	if (Tools.isNotEmpty("searchStr"))
	{
		if("true".equals(request.getAttribute("rootGroup"))) {
				String forumIds="";
				GroupsDB groupsDB = GroupsDB.getInstance();
				List groups = groupsDB.getGroups(docDB.getDoc(docId).getGroupId());
				for(int i=0;i<groups.size();i++) {
					GroupDetails gd = (GroupDetails)groups.get(i);

					List docs = docDB.getDocByGroup(gd.getGroupId());
					for(int j=0;j<docs.size();j++) {
						DocDetails dd = (DocDetails)docs.get(j);
						searchResults.addAll(ForumDB.searchForum(dd.getDocId(), searchStr, -1));
						forumIds += dd.getGroupId()+",";
					}
				}
		}else
			searchResults = ForumDB.searchForum(forumDocId, searchStr, -1);
		if (searchResults.size() > 0)
			request.setAttribute("searchResults", searchResults);
		else
			request.setAttribute("noResults", "");
	}
	else
	{
		request.setAttribute("shortWords", "");
	}
}
catch(Exception e)
{
	sk.iway.iwcm.Logger.error(e);
}

int allowedTextLength;
int length;
int pos;
int startIndex;
int endIndex;
String outStr;
int sOd;
int sDo;
int jumpIndex;

int iMod = 1;

%>
<script type="text/javascript">
<!--
document.write('<style type="text/css" media="screen">	@import "/components/forum/forum_mb.css"; </style>');


-->
</script>

<iwcm:present name="noResults">
  <p align="center"><b><iwcm:text key="components.forum.search.no_matches_found"/></b></p>
</iwcm:present>

<iwcm:present name="searchResults">
	<h2><iwcm:text key="components.search.search_results"/></h2>
	<div class="forumSearchBox row mobile-fix">
		<div class="col-md-4 offset-md-8 col-xs-12 pull-right pr-0">
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
	</div>

<!--  ******** KOMPONENTA STRANKOVANIA ********  -->
<%
	if(request.getAttribute("offset") != null)
		offset = request.getAttribute("offset").toString();
	if(request.getAttribute("end") != null)
		end = request.getAttribute("end").toString();
 %>
<!--  ***** KONIEC KOMPONENTY STRANKOVANIA ***** -->

	<div class='searchResult'>
		<div class="table-responsive">
			<table class="forumline table table-striped table-bordered">
				<thead class="topiclist">
					<tr class="forumHead">
						<th class="thCornerL forum-name"><i class="fa fa-sitemap"> </i><iwcm:text key="forum.title"/></th>
						<th class="thTop forum-name"><i class="fa fa-pencil-square-o"> </i><iwcm:text key="components.forum.post"/></th>
						<th class="thTop autor"><i class="fa fa-comments-o"> <iwcm:text key="forum.author"/></th>
						<th class="thCornerR lastpost"><i class="fa fa-history"></i> <iwcm:text key="editor.date"/></th>
					</tr>
				</thead>
				<iwcm:iterate offset="<%= offset%>" length="<%= end%>" name="searchResults" id="sr" type="sk.iway.iwcm.forum.ForumSearchBean" indexId="index">
					<% if ((iMod % 2) == 0) {
						  out.println("<tr class='even'>");
					   }
					   else
					   {
						  out.println("<tr class='odd'>");
						}
						iMod++;
					%>
					   <td class="row1 forum-name"><%
					      DocForumEntity fBean = null;
							if(sr.getParentId() >= 0)
							{
								fBean = ForumDB.getForumBean(request, ForumDB.getForumMessageParent(sr.getParentId(), sr.getDocId()), sortAscending);
							}
							else
							{
								fBean = ForumDB.getForumBean(request, sr.getForumId(), sortAscending);
							}
							String flag = "";
							if (fBean != null) flag = fBean.getFlag();
							String topicIcon = "folder.gif";
							if("O".equals(flag)) topicIcon = "folder_announce.gif";
							else if("D".equals(flag)) topicIcon = "folder_sticky.gif";

							int forumId = sr.getForumId();
							if(sr.getParentId()>0) forumId = sr.getParentId();
							%><img src="/components/forum/images/subSilver/<%=topicIcon%>" alt=""/>
						<strong><a href="<%=docDB.getDocLink(sr.getDocId(), request)%>"><iwcm:beanWrite name="sr" property="forumName"/></a></strong></td>
						<td class="row2 forum-name"><span class="topictitle"><%
					        if("O".equals(flag)) {%><b><iwcm:text key="components.forum.announcement"/>: </b><%}
							  else if("D".equals(flag)) {	%><b><iwcm:text key="components.forum.sticky"/>: </b><%}
							%><a href='<%=Tools.addParametersToUrl(docDB.getDocLink(sr.getDocId()), "pId="+forumId)%>' >
							<%
								try
								{
									if(sr!=null)
									{
										if(fBean != null)
											out.println(fBean.getSubject()+ "</a></span>");

										/*if (Tools.isNotEmpty(sr.getQuestion()) && Tools.isNotEmpty(searchStr))
										{
											allowedTextLength = 30;
											length = sr.getQuestion().length();
											pos = sr.getQuestion().indexOf(searchStr);
											startIndex = 0;
											endIndex = length;
											outStr = "";
											if(pos != -1)
											{
												if((pos-allowedTextLength)>=0)
													startIndex = pos - allowedTextLength;
												if((pos+allowedTextLength)<=length)
													endIndex = pos + allowedTextLength;
												if(startIndex > 0)
													outStr = " ...";
												outStr += sr.getQuestion().substring(startIndex, endIndex);
												if(endIndex < length)
													outStr += "... ";
												out.println(outStr);
											}
										}*/
									}
								}
								catch(Exception e)
								{
									sk.iway.iwcm.Logger.error(e);
								}
							%>
						</td>
						<td class="row1 autor">
						   <% if (sr.getUserId()>0) { %>
						   <a href="<%=Tools.addParameterToUrl(docDB.getDocLink(sr.getDocId()),"uId",String.valueOf(sr.getUserId()))%>&amp;type=view_profile"><iwcm:beanWrite name="sr" property="autorFullName"/></a>
						   <% } else { %>
						   <iwcm:beanWrite name="sr" property="autorFullName"/>
						   <% } %>
						</td>
						<td class="row2 lastpost"><iwcm:beanWrite name="sr" property="questionDate"/></td>
					</tr>
				</iwcm:iterate>
			</table>
		</div>
	</div>

   	<div class="row mobile-fix">
		<div class="col-md-3 col-xs-12">
			<nav aria-label="breadcrumb">
				<ol class="breadcrumb">
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(rootForumId)%>"><iwcm:text key="components.forum.show_topics"/></a></li>
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(docId)%>"><iwcm:beanWrite name="doc_title"/></a></li>
				</ol>
			</nav>
		</div>
		<% if(searchResults != null && searchResults.size() > 0)
			{
				request.setAttribute("pagingList", searchResults);
				request.setAttribute("pagingListKey", "components.forum.search_results");
				pageContext.include("/components/forum/paging_component_search.jsp");
			}
		%>
	</div>
</iwcm:present>
