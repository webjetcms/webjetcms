
<%@page import="sk.iway.iwcm.editor.EditorDB"%>
<%@page import="sk.iway.iwcm.editor.EditorForm"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_blog"/>
<%
 request.setAttribute("dontCheckAdmin", true);
%>
<%@ include file="/admin/layout_top.jsp" %>2222
<%
	//--------TREBA VYTVORIT SKELET NOVEJ STRANKY, ABY TO NEMUSEL NASTAVOVAT POUZIVATEL--------------
	String topic = request.getParameter("topic");
	Identity user = ((Identity)session.getAttribute(Constants.USER_KEY));
	String editableGroups = UsersDB.getUser(user.getUserId()).getEditableGroups().split(",")[0];
	GroupDetails parent = GroupsDB.getInstance().findGroup( Integer.valueOf(editableGroups) );
	GroupDetails group = GroupsDB.getInstance().getGroup(topic, parent.getGroupId() );
	/* --------------NAKONIEC SA SKELET NOVEJ STRANKY NEVYTVORI--------------------
	EditorForm newArticle = EditorDB.getEditorForm(request,-1,-1,group.getGroupId());
		newArticle.setTitle("New page");
		newArticle.setNavbar("New page");
		newArticle.setData("Your article here");
		newArticle.setAuthorId( user.getUserId() );
		newArticle.setAvailable(true);
		newArticle.setSearchable(true);
		newArticle.setCacheable(false);

		newArticle.setHtmlData("<span></span>");
		newArticle.setPublish("1");
		int ret = EditorDB.saveEditorForm(newArticle,request);

	EditorForm inOrderToPublish = EditorDB.getEditorForm(request, newArticle.getDocId(),-1, group.getGroupId());
		inOrderToPublish.setTitle("New page");
		inOrderToPublish.setNavbar("New page");
		inOrderToPublish.setData("<p>Your article here</p>");
		inOrderToPublish.setAuthorId( user.getUserId() );
		inOrderToPublish.setAvailable(true);
		inOrderToPublish.setSearchable(true);
		inOrderToPublish.setCacheable(false);
		inOrderToPublish.setTempId( group.getTempId() );
		inOrderToPublish.setHtmlData("<span></span>");
		inOrderToPublish.setPublish("1");
	EditorDB.saveEditorForm(inOrderToPublish,request);

		System.out.println("NAVRATOVY STAV: "+ret);
	*/
	String redirectUrl = "/admin/v9/webpages/web-pages-list/?docid=-1&groupid="+group.getGroupId();

%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>

<%@page import="sk.iway.iwcm.doc.DocDB"%>

<script type="text/javascript">
	window.location.href="<%=redirectUrl%>";
</script>
<%@ include file="/admin/layout_bottom.jsp" %>