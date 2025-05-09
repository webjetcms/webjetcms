<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.util.*,sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.GroupDetails" %>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
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
   if(actualGroup != null) parentGroup = GroupsDB.getInstance().getGroup(actualGroup.getParentGroupId());
   if (parentGroup != null) rootForumId = parentGroup.getDefaultDocId();
}

PageParams pageParams = new PageParams(request);
boolean sortAscending = pageParams.getBooleanValue("sortAscending", true);

int pageSize = 10;
if(request.getAttribute("pageSize") != null)
	pageSize = sk.iway.iwcm.Tools.getIntValue((String)request.getAttribute("pageSize"), 10);

boolean showSearchBox = pageParams.getBooleanValue("showSearchBox", true);

boolean active = false;
int docId = Tools.getIntValue(request.getParameter("docid"), -1);
int forumDocId = Tools.getIntValue(request.getParameter("forumDocId"), -1);

List searchResults = null;
String offset = "0";
String end = ""+(pageSize);
String searchStr = request.getParameter("words");

try
{
	if (request.getParameter("uId") != null)
	{
		int userId = Tools.getIntValue(request.getParameter("uId"), -1);
		searchResults = ForumDB.searchForum(0, null, userId);
		if (searchResults.size() > 0)
			request.setAttribute("searchResults", searchResults);
		else
			request.setAttribute("noResults", "");
	}
	else
	{
		request.setAttribute("noUserId", "");
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

<iwcm:present name="noUserId">
  <p align="center"><b><iwcm:text key="components.forum.wrong_user_id"/>. </b></p>
</iwcm:present>

<iwcm:present name="noResults">
  <p align="center"><b><iwcm:text key="components.forum.no_posts_for_user"/>.</b></p>
</iwcm:present>

<iwcm:present name="searchResults">

	<h1><iwcm:text key="components.search.search_results"/></h1>

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
			   	<th class="forum-name"><iwcm:text key="forum.title"/></th>
				<th class="forum-name"><iwcm:text key="components.forum.message_name"/></th>
				<th class="autor"><iwcm:text key="forum.author"/></th>
				<th class="lastpost"><iwcm:text key="components.forum.forum_post_date"/></th>
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
			<td class="">
				<%
			      DocForumEntity fBean = null;
					if(sr.getParentId() >= 0)
					{
						fBean = ForumDB.getForumBean(request, ForumDB.getForumMessageParent(sr.getParentId(), sr.getDocId()), sortAscending);
					}
					else
					{
						fBean = ForumDB.getForumBean(request, sr.getForumId(), sortAscending);
					}
					if (fBean==null) fBean = new DocForumEntity();

					String flag = fBean.getFlag();
					int forumId = sr.getForumId();
					if(sr.getParentId()>0) forumId = sr.getParentId();
				%>
				<strong><a href="<%=docDB.getDocLink(sr.getDocId(), request)%>"><iwcm:strutsWrite name="sr" property="forumName"/></a></strong></td>
			<td class=""><span class="topictitle"><%
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
			<td class="">
				<% if (sr.getUserId()>0) { %>
				<a href="<%=Tools.addParameterToUrl(docDB.getDocLink(sr.getDocId()),"uId",String.valueOf(sr.getUserId()))%>&amp;type=view_profile"><iwcm:strutsWrite name="sr" property="autorFullName"/></a>
				<% } else { %>
				<iwcm:strutsWrite name="sr" property="autorFullName"/>
				<% } %>
			</td>
			<td class=""><iwcm:strutsWrite name="sr" property="questionDate"/></td>
		</tr>
		</iwcm:iterate>
	</table>
</div>
					<!--  ******** KOMPONENTA STRANKOVANIA ********  -->
   	<div class="row mobile-fix">
		<div class="col-md-3 col-xs-12">
			<nav aria-label="breadcrumb">
				<ol class="breadcrumb">
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(rootForumId)%>"><iwcm:text key="components.forum.show_topics"/></a></li>
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(docId)%>"><iwcm:strutsWrite name="doc_title"/></a></li>
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
	<!--  ***** KONIEC KOMPONENTY STRANKOVANIA ***** -->
</iwcm:present>

<%if(showSearchBox==true){%>
<div class="forumSearchBox row mobile-fix">
	<div class="col-md-3 col-xs-12 pull-right">
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
<%}%>