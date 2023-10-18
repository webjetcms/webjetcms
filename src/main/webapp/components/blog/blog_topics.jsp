<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<iwcm:checkLogon admin="true" perms="cmp_blog"/>
<%@ include file="/admin/layout_top.jsp" %>

<%
	
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	
	String filterTopicName = Tools.getStringValue(request.getParameter("filterTopicName"), "");
	
	int authorId = ((Identity) session.getAttribute(Constants.USER_KEY)).getUserId();
	boolean NO_RECURSIVE = false;
	String[] editableGroups = UsersDB.getUser(authorId).getEditableGroups().split(",");
	
	List<GroupDetails> blogGroups = new ArrayList<GroupDetails>();
	for (String groupId : editableGroups)
		if (groupId.matches("^[0-9]+$") )
			blogGroups.addAll( GroupsDB.getInstance().findChilds(Integer.valueOf(groupId), NO_RECURSIVE ) );
	
	//Filtracia, vyhodi zo zoznamu tie polozky, ktore neobsahuju vstupny retazec
	if (Tools.isNotEmpty(filterTopicName))
	{
		Iterator<GroupDetails> iterator = blogGroups.iterator();
	 	while (iterator.hasNext()) 
	 	{
	 		GroupDetails blogGroup = iterator.next();
	 		
	 		if(DB.internationalToEnglish(blogGroup.getGroupName().toLowerCase()).indexOf(DB.internationalToEnglish(filterTopicName.toLowerCase())) == -1)
				iterator.remove();
	 	}
	}
	
	request.setAttribute("topics", blogGroups);
%>
<script type="text/javascript">
	helpLink = "components/blog.jsp&book=components";
</script>
<div class="row title">
    <h1 class="page-title"><i class="fa fa-cube"></i><iwcm:text key="components.blog"/><i class="fa fa-angle-right"></i><iwcm:text key="components.blog.topics.title"/></h1>
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
			
				<form name="blogFilterForm" action="/components/blog/blog_topics.jsp" class="zobrazenie">
					<fieldset>
						<p>
							<label>
								<iwcm:text key="components.blog.topics.topic_name"/>:
								<input type="text" class="poleKratke" value="<%=filterTopicName %>" name="filterTopicName" />
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

<logic:empty name="topics">
	<iwcm:text key="components.blog.topics.no_topics"/>
</logic:empty>

<logic:notEmpty name="topics">
	<display:table name="topics" export="true" pagesize="30" class="sort_table" cellspacing="0" cellpadding="0" uid="group">
		<%  GroupDetails topic = (GroupDetails)group; %>
		
		<display:column titleKey="components.blog.topics.topic_name" sortable="true">
			<a href="blog_articles.jsp?topicId=<%=topic.getGroupId()%>">
				<%=topic.getGroupName() %>
			</a>
		</display:column>
		
		<display:column titleKey="components.blog.topics.articles_count">
			<%=DocDB.getInstance().getDocCountInGroup(topic.getGroupId()) - 1 %>
		</display:column>
		
		<display:column titleKey="components.table.column.tools">

	 		<a href="blog_articles.jsp?topicId=<%=topic.getGroupId()%>" title='<iwcm:text key="components.blog.filter.topic"/>' class="iconEdit">&nbsp;</a>
	
 		</display:column>
		
	</display:table>
</logic:notEmpty>

<br />
<br />

<input type="button" class="button100" value="<iwcm:text key="components.blog.topics.new_topic"/>" onclick="javascript:openPopupDialogFromLeftMenu('/components/blog/blog_new_topic.jsp')"/>
<%@ include file="/admin/layout_bottom.jsp" %>