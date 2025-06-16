<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="java.util.*,sk.iway.iwcm.*,sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,sk.iway.iwcm.tags.support.ResponseUtils" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%
if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(sk.iway.iwcm.tags.support.CustomTagUtils.XHTML_KEY, "true", PageContext.PAGE_SCOPE);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

int docId = Tools.getIntValue(request.getParameter("docid"), 0);
if (docId < 1) return;





//kontrola prav
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user == null)
{
	%>
	<iwcm:text key="error.userNotLogged"/>
	<%
	return;
}

LabelValueDetails uploadLimits = ForumDB.getUploadLimits(docId, request);request.setAttribute("forumForm", new DocForumEntity());
if (uploadLimits == null)
{
	%>
	<iwcm:text key="components.forum.new.upload_not_allowed"/>
	<%
	return;
}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
	<head>
	<title><iwcm:text key="forum.new.title"/></title>
		<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />
		<meta http-equiv="Content-language" content="<%=PageLng.getUserLng(request)%>" />

		<meta name="description" content="WebJET Content Management web site" />
		<meta name="author" content="Interway, s.r.o." />

		<link type="text/css" rel="stylesheet" href="/css/page.css" />
		<link type="text/css" rel="stylesheet" href="/components/forum/forum.css" />
		<script type="text/javascript" src="/components/form/check_form.js"></script>

		<script type="text/javascript">
			<!--
				window.resizeTo(510, 500);
			-->
		</script>
	</head>
	<body class="forumNewMainTable">
	<div class="forum">
	<h3><iwcm:text key="components.forum.new.insert_new_file"/></h3>
	<form:form method="post" modelAttribute="forumForm" action="/apps/forum/saveForumFile" name="forumForm" enctype="multipart/form-data">
		<fieldset>
			<legend><iwcm:text key="components.forum.new.insert_new_file"/></legend>
			<p>
				<iwcm:text key="components.forum.new.limitsFileTypes"/>:
				<%=uploadLimits.getValue()%>
			</p>
			<p>
				<iwcm:text key="components.forum.new.limitsFileSize"/>:
				<%=uploadLimits.getInt1()%> kB
			</p>
			<p>
				<label>
					<iwcm:text key="forum.new.file"/>
				</label>
				<input type="file" name="uploadedFile" size="30"/>
			</p>

			<input type="hidden" name="forumId" value="<%=ResponseUtils.filter(request.getParameter("forumId"))%>" />


			<input type="hidden" name="type" value="upload" />

			<p>
				<input type="submit" name="submit" value="<iwcm:text key="forum.new.send"/>"/>
				<input type="button" onclick="javascript:window.close();" name="cancel" value="<iwcm:text key="forum.new.cancel"/>" />
			</p>
		</fieldset>
	</form:form>
	</div>
	</body>
</html>