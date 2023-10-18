<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.sql.*" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@page import="sk.iway.iwcm.components.blog.*"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.UniquenessViolationException"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_blog"/>
<%
	request.setAttribute("cmpName", "blog.add_user");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(request);
	
	String state = "";
	//--------------------SPRACOVANIE UZ ODOSLANEHO FORMULARU---------------------------
	if (request.getParameter("submit")!=null)	
	{
		try{
			BloggerCreationData data = new BloggerCreationData(request);
			UserDetails blogger = new BloggerMaker().makeNewBlogger(data);
			new BloggerDefaultPagesMaker(blogger, request).createDirectoryStructure();
			state = prop.getText("components.blog.admin.user_successfully_added");
		}catch(BlogGroupDoesntExistException exc){
			state = prop.getText("components.blog.admin.no_blog_group");
		}catch(UniquenessViolationException exc){
			state = prop.getText("components.blog.admin.blogger_alredy_exists", exc.getField(), exc.getValue());
		}
	}
%>

<script src="/components/form/check_form.js" type="text/javascript"></script>
<%=Tools.insertJQuery(request) %>
<script type="text/javascript">
	<!--
		resizeDialog(550, 600);
		document.body.style.cursor = "default";
	
		window.opener.location.reload();
		
		function Ok()
		{
			//musi to ist takto, inak by sa nezavolal check form
			document.getElementById('submit').click();
		}
	
		//funkcia je volana po vybere adresaru z popup okna, musime zadefinovat, co sa s docId stane
		function setStandardDoc(doc_id)
		{
			$("#parentDirectoryId").get(0).value = doc_id;			
		}
	
		function setParentGroupId(returnValue)
		{
			if (returnValue.length > 15)
			{
				var groupid = returnValue.substr(0,15);
				var groupname = returnValue.substr(15);
				groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
				document.addUserForm.parentDirectoryId.value = groupid;			
			}
		}

		function readDataByEmail()
		{
			window.location.href="blog_new_user.jsp?email="+document.addUserForm.email.value+"&readEmails=true";
		}
	//-->
</script>

<%
String login = request.getParameter("login") != null ?org.apache.struts.util.ResponseUtils.filter(request.getParameter("login")) : "";
String email = request.getParameter("email") != null ?org.apache.struts.util.ResponseUtils.filter(request.getParameter("email")) : "";
String parentDirectory = request.getParameter("parentDirectoryId") != null ?org.apache.struts.util.ResponseUtils.filter(request.getParameter("parentDirectoryId")) : "";
String firstName = request.getParameter("name") != null ?org.apache.struts.util.ResponseUtils.filter(request.getParameter("name")) : "";
String lastName = request.getParameter("surname") != null ?org.apache.struts.util.ResponseUtils.filter(request.getParameter("surname")) : "";
String password = request.getParameter("password") != null ?org.apache.struts.util.ResponseUtils.filter(request.getParameter("password")) : "";

if (request.getParameter("readEmails")!=null && Tools.isEmail(email))
{
	UserDetails usr = UsersDB.getUserByEmail(email);
	if (usr != null)
	{
		login = usr.getLogin();
		email = usr.getEmail();
		firstName = usr.getFirstName();
		lastName = usr.getLastName();
		password = "unchanged";
		
		int editableGroups[] = Tools.getTokensInt(usr.getEditableGroups(), ",");
		if (editableGroups != null && editableGroups.length>0)
		{
			GroupDetails rootGroup = GroupsDB.getInstance().getGroup(editableGroups[0]);
			if (rootGroup != null) parentDirectory = String.valueOf(rootGroup.getParentGroupId());
		}
		
	}
}
%>

<form action="blog_new_user.jsp" method="post" name="addUserForm">
	<table>
		<tr>
			<td><label for="emailId"><iwcm:text key="components.blog.admin.email"/>:</label></td>
			<td><input type="text" class="required email" name="email" id="emailId" size="30" value="<%=email  %>" />
				<input type="button" name="readEmails" value="<iwcm:text key="components.blog.newuser.readdata"/>" class="button" onclick="readDataByEmail(); return false"/>
			</td>
		</tr>
		<tr>
			<td><label for="parentDirectoryId"><iwcm:text key="components.blog.admin.parent_directory"/>:</label></td>
			<td><input type="text" size="5" id="parentDirectoryId" name="parentDirectoryId" class="required numbers" value="<%=parentDirectory  %>"/>&nbsp;&nbsp;
				<input type="button" class="button100" onclick="popupFromDialog('<iwcm:cp/>/admin/grouptree.jsp', 500, 500);" value="<iwcm:text key="components.blog.admin.directory_choose"/>"/> </td>
		</tr>		
		<tr>
			<td><label for="loginId"><iwcm:text key="components.blog.admin.login"/>:</label></td>
			<td><input type="text" class="required loginChars" size="30" name="login" id="loginId" value="<%=login  %>"/></td>
		</tr>		
		<tr>
			<td><label for="nameId"><iwcm:text key="components.blog.admin.first_name"/>:</label></td>
			<td><input type="text" class="required" size="30" name="name" id="nameId" value="<%=firstName %>"/></td>
		</tr>
		<tr>
			<td><label for="surnameId"><iwcm:text key="components.blog.admin.last_name"/>:</label></td>
			<td><input type="text" class="required" size="30" name="surname" id="surnameId" value="<%=lastName  %>"/></td>
		</tr>
		<tr>
			<td><label for="passwordId"><iwcm:text key="components.blog.admin.password"/>:</label></td>
			<td><input type="password" class="required" size="30" name="password" id="passwordId" value="<%=password  %>"/></td>
		</tr>		
		<input type="submit" id="submit" name="submit" style="display:none;" />
	</table>
</form>

<span style="color: red;font-weight: bold;"><%=state %></span>

</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>