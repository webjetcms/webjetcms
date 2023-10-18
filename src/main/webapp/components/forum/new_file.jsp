<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="java.util.*,sk.iway.iwcm.*,sk.iway.iwcm.forum.*,org.apache.struts.util.ResponseUtils" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

int docId = Tools.getIntValue(request.getParameter("docid"), 0);
if (docId < 1) return;

System.out.println(docId);



//kontrola prav
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user == null)
{
	%>
	<iwcm:text key="error.userNotLogged"/>
	<%
	return;
}

LabelValueDetails uploadLimits = ForumDB.getUploadLimits(docId, request);
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
		<script type="text/javascript" src="<iwcm:cp/>/components/calendar/popcalendar.jsp"></script>
		<script type="text/javascript">
			<!--
				window.resizeTo(510, 500);
			-->
		</script>
	</head>
	<body class="forumNewMainTable">
	<div class="forum">
	<h3><iwcm:text key="components.forum.new.insert_new_file"/></h3>
	<html:form scope="request" method="post" enctype="multipart/form-data" action="/saveforum.do" type="sk.iway.iwcm.forum.ForumForm">
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
				<html:file property="uploadedFile" size="30"/>
			</p>
			
			<input type="hidden" name="parentId" value="<%=ResponseUtils.filter(request.getParameter("parent"))%>" />
			<input type="hidden" name="docid" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
			<input type="hidden" name="docId" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
			<html:hidden property="forumId"/>
						
			<p>
				<input type="submit" name="submit" value="<iwcm:text key="forum.new.send"/>"/>
				<input type="button" onclick="javascript:window.close();" name="cancel" value="<iwcm:text key="forum.new.cancel"/>" />
			</p>
		</fieldset>
	</html:form>
	</div>
	</body>
</html>