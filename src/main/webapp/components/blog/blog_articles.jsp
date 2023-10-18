<%@page import="org.apache.struts.util.ResponseUtils"%>
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

<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Enumeration"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.stat.StatDB"%>
<%@page import="sk.iway.iwcm.stat.StatNewDB"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<iwcm:checkLogon admin="true" perms="cmp_blog"/>
<%@ include file="/admin/layout_top.jsp" %>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	//-----------FILTRACIA------------------------------------------------
	String filterBlogName = Tools.getStringValue(request.getParameter("filterBlogName"), "");
	int filterTopicId = Tools.getIntValue(request.getParameter("topicId"), -1);

	String startDateString = "";
	String endDateString = "";

	Date filterDateFrom = null;
	Date filterDateTo = null;

	if (Tools.isNotEmpty(request.getParameter("filterDateFrom")))
		filterDateFrom = new Date(DB.getTimestamp(request.getParameter("filterDateFrom"), "0:00", sk.iway.iwcm.DBPool.getDBName(request)));
	if (Tools.isNotEmpty(request.getParameter("filterDateTo")))
		filterDateTo = new Date(DB.getTimestamp(request.getParameter("filterDateTo"), "23:59", sk.iway.iwcm.DBPool.getDBName(request)));

	SimpleDateFormat formatter = new SimpleDateFormat(Constants.getString("dateFormat"));

	if (filterDateFrom != null)
		startDateString = formatter.format(filterDateFrom);
	if (filterDateTo != null)
		endDateString = formatter.format(filterDateTo);

	int authorId;
	if ( Tools.isInteger( request.getParameter("authorId") ) )
		authorId = Integer.valueOf( request.getParameter("authorId") );
	else
		authorId =  ((Identity)session.getAttribute(Constants.USER_KEY)).getUserId();

	boolean NO_RECURSIVE = false;
	String[] editableGroups = UsersDB.getUser(authorId).getEditableGroups().split(",");

	List<GroupDetails> distinctTopics = new ArrayList<GroupDetails>();
	for (String groupId : editableGroups)
		if (groupId.matches("^[0-9]+$") )
			distinctTopics.addAll( GroupsDB.getInstance().findChilds(Integer.valueOf(groupId), NO_RECURSIVE ) );

	//--------------AK SME SI VYBRALI VYMAZANIE PRISPEVKU----------------
	int deleteDocId = Tools.getIntValue(request.getParameter("deleteDocId"), -1);
	if (deleteDocId > 0)
		DocDB.deleteDoc(deleteDocId, request);

	//-------------VYBERIEME SI VSETKY CLANKY, KTORE MAJU TOHOTO AUTORA-------------------------------
	List<DocDetails> docs = DocDB.getBlogs(authorId, -1, filterBlogName, filterDateFrom, filterDateTo);

	request.setAttribute("docs",docs);

	//v tomto bloku si skonstruujeme url, aby sme napriklad nestratili filter rubriky, ak sme zmazali prispevok
	// - ak zmazeme, tak sa znovu odkazeme na tuto stranku, preto potrebujeme si zachovat URL
	StringBuilder url = new StringBuilder();
	url.append(request.getRequestURL() + "?");

	Enumeration en = request.getParameterNames();

	while (en.hasMoreElements())
	{
		String paramName = (String)en.nextElement();
		if ("deleteDocId".equalsIgnoreCase( paramName ))
			continue;
		url.append("&" + paramName + "=" + request.getParameter(paramName));
	}

	String target = "";
	if (request.getParameter("targetFrame") != null)
		target=" target=\""+ResponseUtils.filter(request.getParameter("targetFrame"))+"\" ";

	DocDB docDB = DocDB.getInstance();
	GroupsDB groupsDB = GroupsDB.getInstance();

	request.setAttribute("topics", distinctTopics);
%>

<script type="text/javascript">
<!--
	helpLink = "components/blog.jsp&book=components";

	function deleteOK(text,obj,url)
	{
		if(confirm(text))
			obj.href = url;
	}
//-->
</script>
<div class="row title">
    <h1 class="page-title"><i class="fa fa-cube"></i><iwcm:text key="components.blog"/><i class="fa fa-angle-right"></i><iwcm:text key="components.blog.articles.title"/></h1>
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

				<form name="blogFilterForm" action="/components/blog/blog_articles.jsp" class="zobrazenie">
					<fieldset>
						<p>
							<label>
								<iwcm:text key="components.blog.filter.name"/>:
								<input type="text" class="poleKratke" value="<%=filterBlogName %>" name="filterBlogName" />
							</label>

							<label for="filterDateFrom">
								<iwcm:text key="components.reservation.reservation_list.date_from"/>
			        			<input type="text" value="<%=startDateString %>" class="poleMini datepicker" id="filterDateFrom" maxlength="10" size="15" name="filterDateFrom" />
		        			</label>

		        			<label for="filterDateTo">
								<iwcm:text key="components.reservation.reservation_list.date_to"/>
			        			<input type="text" value="<%=endDateString %>" class="poleMini datepicker" id="filterDateTo" maxlength="10" size="15" name="filterDateTo" />
			        			<input type="hidden" name="authorId" value="<%=authorId %>"/>
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

<logic:empty name="docs">
	<iwcm:text key="components.blog.articles.no_articles"/>
</logic:empty>

<%-------------------A ZORAZIME JEHO CLANKY V TABULKE---------------------------%>

<logic:notEmpty name="docs">
	<display:table name="docs" pagesize="20" export="true" class="sort_table" cellspacing="0" cellpadding="0" uid="doc">
		<% DocDetails document = (DocDetails)doc; %>

		<display:column sortable="true" title="#">
			<%=pageContext.getAttribute("doc_rowNum") + "."%>
		</display:column>

		<display:column sortable="true" titleKey="components.blog.articles.article_title">
				<a <% if (document.isAvailable()==false) out.print("style='color: red !important;'"); %> href="<%="/admin/editor.do?docid="+document.getDocId() %>" <%=target %>>
					<%=document.getTitle() %>
				</a>
		</display:column>

		<display:column titleKey="components.table.column.tools">

			<a href="<%=docDB.getDocLink(((DocDetails)doc).getDocId(), request)%>" target="_blank" title="<iwcm:text key="groupslist.page_preview"/>" class="iconPreview">&nbsp;</a>

			<a href="<%="/admin/editor.do?docid="+document.getDocId() %>" <%=target %> title='<iwcm:text key="components.redirect.admin_list.edituj"/>' class="iconEdit">&nbsp;</a>

			<a href="javascript:" onclick="deleteOK('<iwcm:text key="components.blog.articles.do_you_really_want_to_delete"/>',this,'<%=url.toString() + "&deleteDocId=" + document.getDocId() %>')" title='<iwcm:text key="button.delete"/>' class="iconDelete">&nbsp;</a>

			<a href="<%="/apps/stat/admin/top-details/?docId="+document.getDocId()+"&title="+Tools.URLEncode(document.getTitle()) %>" title="<iwcm:text key="groupslist.stat_of_web_page"/>" class="iconStat">&nbsp;</a>
 		</display:column>

		<display:column sortable="true" property="dateCreated" titleKey="components.blog.table.created.date" decorator="sk.iway.displaytag.DateTimeDecorator"/>

		<display:column titleKey="components.blog.articles.topic">
			<%=document.getPerexGroupString() == null ? "" : document.getPerexGroupString() %>
		</display:column>

		<%--<display:column titleKey="components.blog.articles.views">
			<%= StatNewDB.getMonthViewsForDoc(24,document.getDocId()).get(0).getColumn1() %>
		</display:column> --%>

	</display:table>
</logic:notEmpty>

<br />
<br />

<%@ include file="/admin/layout_bottom.jsp" %>