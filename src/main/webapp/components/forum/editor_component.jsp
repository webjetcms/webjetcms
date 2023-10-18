<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.forum.ForumSortBy"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_diskusia"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>

<% request.setAttribute("cmpName", "forum"); %>
<jsp:include page="/components/top.jsp"/>

<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	Prop prop = Prop.getInstance();

	String jspFileName = request.getParameter("jspFileName");
	if(Tools.isNotEmpty(jspFileName)){
		int slash = jspFileName.lastIndexOf("/");
		int dot = jspFileName.lastIndexOf(".");
		if(slash > 0 && dot < jspFileName.length()) jspFileName = jspFileName.substring(slash+1, dot);	//ziskam len nazov komponenty bez koncovky
	}
	jspFileName = Tools.replace(jspFileName, "_bootstrap", "");

	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>

<script type='text/javascript'>

function Ok()
{
	var style = $('input[name=style]:checked').val();
	if(document.textForm.forumType.value == "forum_mb")
	{
		document.textForm.submit();
	}
	else if(document.textForm.forumType.value == "forum")
	{
		var type = document.textForm.type.value;
		var forumType = document.getElementById('forumType');
		var sortTopicsBy = document.getElementById('sortTopicsBy');
		var componentName = "forum.jsp";
		var params = "!INCLUDE(/components/forum/";
		if(document.textForm.forumType.value == "forum")
		{
			var forumDet = document.getElementById('forumDet').value;
			componentName = "forum.jsp";
			params += componentName+ ", type=" + forumDet;
		}

		if(document.textForm.usePaging.checked)
		{
			params += ", pageSize=" +  document.textForm.pageSizeForum.value;
		}
		else
			params += ", noPaging=true";
		var sortAscending = document.textForm.sortAscending.value;
		if(sortAscending == "undefined" || "true" == sortAscending)
			params += ", sortAscending=true";
		else
			params += ", sortAscending=false";
		htmlCode = ", style="+style+"";
		htmlCode = params + ")!";
		oEditor.FCK.InsertHtml(htmlCode);
		return true ;
	}
} // End function

if (isFck)
{

}
else
{
	resizeDialog(400, 270);
}



function showMenu()
{
	var mbMenu = document.getElementById('mbMenu');
	var forumMenu = document.getElementById('forumMenu');
	var forumType = document.getElementById('forumType');
	var sortOrderLabel = document.getElementById('sortOrderLabel');

	if(forumType.value == "forum_mb")
	{
		mbMenu.style.display = "block";
		forumMenu.style.display = "none";
		sortOrderLabel.innerHTML = "<%=prop.getText("components.forum.sort_order")%>";
	}
	else if(forumType.value == "forum")
	{
		forumMenu.style.display = "block";
		mbMenu.style.display = "none";
		sortOrderLabel.innerHTML = "<%=prop.getText("components.forum.sort_by_question_date")%>";
	}
}

function addPageSize(checkbox)
{
	if (checkbox.checked == true)
	   showHideRow("pageSizeForumRow", true);
   else
	   showHideRow("pageSizeForumRow", false);
}
function loadListIframe()
{
	var url = "/components/forum/admin_diskusia_zoznam.jsp";
	 $("#componentIframeWindowTabList").attr("src", url);
}
</script>

<iwcm:menu name="menuForum">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }
			.col-sm-6 {
				margin-bottom: 10px;
			}

			.leftCol {
				text-align: right;
				padding-top: 5px;
				margin-bottom: 5px;
			}

			.col-sm-10 {
				padding-top: 15px;
			}
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li class="last"><a href="#" onclick="loadListIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.forum.zoznam_diskusii"/></a></li>
		</ul>
	</div>
</iwcm:menu>

<div class="tab-pane toggle_content tab-pane-fullheight" style="width:790px;">
	<div class="tab-page" id="tabMenu1" style="display: block; width: 790px;">
		<form name="textForm" action="editor_component2.jsp">
			<div class="col-sm-10">
				<div class="col-sm-12">
					<div class="col-sm-6 leftCol">
						<label for="forumType"><iwcm:text key="components.forum.select_component"/>:</label>
					</div>
					<div class="col-sm-6">
						<select name="forumType" id="forumType"  onchange="showMenu();">
							<option value="forum"><iwcm:text key="components.forum.admin.forumType.simple"/></option>
							<option value="forum_mb"><iwcm:text key="components.forum.admin.forumType.mb"/></option>
						</select>
					</div>
				</div>
				<div class="col-sm-12">
					<div class="col-sm-6 leftCol">
						<span id="sortOrderLabel"><iwcm:text key="components.forum.sort_by_question_date"/></span>:
					</div>
					<div class="col-sm-6">
						<select name="sortAscending">
							<option value="true"><iwcm:text key="components.forum.sort_by_question_date.asc"/></option>
							<option value="false"><iwcm:text key="components.forum.sort_by_question_date.desc"/></option>
						</select>
					</div>
				</div>
				<div id="forumMenu" style="display: block;">
					<div class="col-sm-12" >
						<div class="col-sm-6 leftCol">
							<label for="forumDet"><iwcm:text key="components.forum.type"/>:</label>
						</div>
						<div class="col-sm-6">
							<select name="type" id="forumDet">
								<option value="iframe"><iwcm:text key="components.forum.type_iframe"/></option>
								<option value="perex"><iwcm:text key="components.forum.type_perex"/></option>
								<option value="none"><iwcm:text key="components.forum.type_none"/></option>
								<option value="normal"><iwcm:text key="components.forum.type_normal"/></option>
								<option value="open"><iwcm:text key="components.forum.start_open"/></option>
							</select>
						</div>
					</div>
					<div class="col-sm-12">
						<div class="col-sm-6 leftCol">
							<label for="usePagingId"><iwcm:text key="components.forum.paging"/>:&nbsp;</label>
						</div>
						<div class="col-sm-6">
							<input type="checkbox" name="usePaging" id="usePagingId" <%if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("pageSize", ""))))
							out.print("checked='checked'");%> onclick="addPageSize(this);" />
						</div>
					</div>
					<div class="col-sm-12" id="pageSizeForumRow">
						<div class="col-sm-6 leftCol">
							<label for="pageSizeForumId"><iwcm:text key="components.forum.page_size"/>:</label>
						</div>
						<div class="col-sm-6">
							<input type="text" name="pageSizeForum" id="pageSizeForumId" size="3" value="<%=pageParams.getIntValue("pageSize", 25)%>" />
						</div>
					</div>
				 </div>
				 <div id="mbMenu" style="display: none;">
				 	<% pageContext.setAttribute("sortValues", ForumSortBy.values()); %>
					<div class="col-sm-12" id="mbTopicsSort" style="display: block;">
						<div class="col-sm-6 leftCol">
							<iwcm:text key="components.forum.sort_topics_by"/> :
						</div>
						<div class="col-sm-6">
							<select name="sortTopicsBy">
								<logic:iterate id="sort" collection="${sortValues}">
									<option value="${sort}"><iwcm:text key="components.forum.sortBy.${sort}"/></option>
								</logic:iterate>
							</select>
						</div>
					</div>
					<div id="mbPaging" style="display: block;">
						<div class="col-sm-12" >
							<div class="col-sm-6 leftCol">
								<label for="pageSize"><iwcm:text key="components.forum.page_size"/>:&nbsp;</label>
							</div>
							<div class="col-sm-6">
								<input type="text" name="pageSize" id="pageSize" size="2" value="<%=pageParams.getIntValue("pageSize", 10)%>" />
							</div>
						</div>
						<div class="col-sm-12">
							<div class="col-sm-6 leftCol">
								<label for="pageLinksNum"><iwcm:text key="components.forum.page_links_num"/>:&nbsp;</label>
							</div>
							<div class="col-sm-6">
								<input type="text" name="pageLinksNum" id="pageLinksNum" size="2" value="<%=pageParams.getIntValue("pageLinksNum", 10)%>" />
							</div>
						</div>
					</div>
					<div id="mbTopics" style="display: block;">
						<div class="row">
							<div class="col-sm-6 leftCol">
								<label for="useDelTimeLimit"><iwcm:text key="components.forum.use_del_time_limit"/>:&nbsp;</label>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" name="useDelTimeLimit" id="useDelTimeLimit" <%if (pageParams.getBooleanValue("useDelTimeLimit", true))
								out.print("checked='checked'");%> />
							</div>
						</div>
						<div class="row">
							<div class="col-sm-6 leftCol">
								<label for="delMinutes"><iwcm:text key="components.forum.del_minutes"/> (min):&nbsp;</label>
							</div>
							<div class="col-sm-6">
								<input type="text" name="delMinutes" size="2" id="delMinutes" value="<%=pageParams.getIntValue("delMinutes", 30)%>" />
							</div>
						</div>
					</div>
					<div id="mbProfile" style="display: none;">
						<div class="col-sm-12">
							<div class="col-sm-6 leftCol">
								<label for="allowedUserGroup"><iwcm:text key="components.forum.allowed_user_group"/>:&nbsp;</label>
							</div>
							<div class="col-sm-6">
								<input type="text" name="allowedUserGroup" id="allowedUserGroup" size="2" value="<%=ResponseUtils.filter(pageParams.getValue("allowedUserGroup", "")) %>" />
							</div>
						</div>
						<div class="col-sm-12">
							<div class="col-sm-6 leftCol">
								<label for="searchPostsDocId"><iwcm:text key="components.forum.search_posts_docid"/>:&nbsp;</label>
							</div>
							<div class="col-sm-6">
								<input type="text" name="searchPostsDocId" id="searchPostsDocId" size="2" value="<%=ResponseUtils.filter(pageParams.getValue("searchPostsDocId", "")) %>"/>
							</div>
						</div>
					</div>
					<div id="mbEditProfile" style="display: none;">
						<div class="col-sm-12">
							<div class="col-sm-6 leftCol">
								<label id="searchPostsDocId2"><iwcm:text key="components.forum.search_posts_docid"/>:&nbsp;</label>							</div>
							<div class="col-sm-6">
								<input type="text" name="searchPostsDocId2" id="searchPostsDocId2" size="2" value="<%=ResponseUtils.filter(pageParams.getValue("searchPostsDocId2", "")) %>" />
							</div>
						</div>
					</div>
				</div>
			</div>
		</form>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTabList" frameborder="0" name="componentIframeWindowTabList" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

</div>

<script type="text/javascript">
	<% if (Tools.isNotEmpty(jspFileName)) {%>
	document.textForm.forumType.value = "<%=jspFileName%>";
	<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("type", "")))) {%>
		document.textForm.type.value = "<%=ResponseUtils.filter(pageParams.getValue("type", ""))%>";
		showMenu();
<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("sortAscending", "")))) {%>
	document.textForm.sortAscending.value = "<%=ResponseUtils.filter(pageParams.getValue("sortAscending", ""))%>";
<%}%>
	addPageSize(document.textForm.usePaging);
	//addPageSize(document.textForm.elements["pageSizeForum"]);
</script>

<jsp:include page="/components/bottom.jsp"/>
