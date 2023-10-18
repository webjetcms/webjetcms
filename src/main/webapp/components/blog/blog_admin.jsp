<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.sql.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="sk.iway.iwcm.users.UserDetails"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Collections"%>
<%@ page import="java.util.Comparator"%>
<%@ page import="sk.iway.iwcm.users.UsersDB"%>
<%@ page import="sk.iway.iwcm.doc.DocDB"%>
<%@ page import="java.util.Iterator"%>
<iwcm:checkLogon admin="true" perms="cmp_blog"/>
<%@ include file="/admin/layout_top.jsp" %>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	String filterBloggerName = Tools.getStringValue(request.getParameter("filterBloggerName"), "");

	int blogGroupId = DocDB.getBlogGroupId();//zistime si group id, do ktorej su zaradeni blogeri

	if (blogGroupId == -1)
	{
%>

		<br />
		<iwcm:text key="components.blog.admin.no_blog_group"/> <br /> <br />
		<input type="button" class="button100" value="<iwcm:text key="components.blog.admin.found_blog_group" />" onclick="javascript:openPopupDialogFromLeftMenu('/admin/editusergroup.do?id=-1&userGroupName=Blog');"/>
<%
		return;
	}

	List<UserDetails> users = UsersDB.getUsersByGroup(blogGroupId);
	if (Tools.isNotEmpty(filterBloggerName))
	{
		Iterator<UserDetails> iterator = users.iterator();
	 	while (iterator.hasNext())
	 	{
	 		UserDetails user = iterator.next();
	 		if(DB.internationalToEnglish(user.getFullName().toLowerCase()).indexOf(DB.internationalToEnglish(filterBloggerName.toLowerCase())) == -1)
				iterator.remove();
	 	}
	}

	Comparator<UserDetails> comparator = new Comparator<UserDetails>()
	{
		public int compare(UserDetails user1, UserDetails user2 )
		{
			if (user1 == null)
				return -1;
			if (user2 == null)
				return 1;
			if (user1.getLastName().compareTo(user2.getLastName()) == 0 )
				return user1.getFirstName().compareTo(user2.getFirstName());

			return user1.getLastName().compareTo(user2.getLastName());
		}
	};

	Collections.sort(users,comparator);

	request.setAttribute("users",users);
%>
<script type="text/javascript">
	helpLink = "components/blog.jsp&book=components";
</script>

<div class="row title">
	<h1 class="page-title"><i class="icon-book-open"></i><iwcm:text key="components.blog"/><i class="fa fa-angle-right"></i><iwcm:text key="components.blog.admin.blogers" /></h1>
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

				<form name="blogFilterForm" action="/components/blog/blog_admin.jsp" class="zobrazenie">
					<fieldset>
						<p>
							<label>
								<iwcm:text key="components.blog.admin.first_name"/>/<iwcm:text key="components.blog.admin.last_name"/>:
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
<logic:empty name="users">
	<iwcm:text key="components.blog.admin.no_blogers" />
</logic:empty>

<logic:notEmpty name="users">

	<display:table name="users" pagesize="30" class="sort_table" cellspacing="0" cellpadding="0" uid="usr">
		<%  UserDetails user = (UserDetails)usr; %>

		<display:column  titleKey="components.blog.admin.last_name" sortable="true" > <a href="blog_articles.jsp?authorId=<%=user.getUserId()%>"><%=user.getLastName() %></a></display:column>

		<display:column titleKey="components.blog.admin.first_name" property="firstName" />

		<display:column titleKey="components.table.column.tools">
			<a href="/apps/stat/admin/logon-user-details/?userId=<%=user.getUserId()%>" title="<iwcm:text key="groupslist.stat_of_web_page"/>" class="iconStat">&nbsp;</a>
		</display:column>

	</display:table>

</logic:notEmpty>

<br />
<br />

<input type="button" onclick="javascript:openPopupDialogFromLeftMenu('/components/blog/blog_new_user.jsp');" class="button100" value="<iwcm:text key="components.blog.add_user"/>" />

<%@ include file="/admin/layout_bottom.jsp" %>