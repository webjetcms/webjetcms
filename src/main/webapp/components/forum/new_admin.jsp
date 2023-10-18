<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="cmp_diskusia"/><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

request.setAttribute("cmpName", "forum");
if(request.getParameter("edit") == null)
{
	request.setAttribute("titleKey", "forum.new.title");
	request.setAttribute("descKey", "components.forum.new.insert_new_post");
}
else
{
	request.setAttribute("titleKey", "forum.edit.title");
	request.setAttribute("descKey", "components.forum.edit.edit_post");
}

session.setAttribute("forum-shown", "true");
request.setAttribute("isAdmin", "true");
%>
<jsp:include page="/components/top-public.jsp"/>

<%=Tools.insertJQuery(request) %>
<script type="text/javascript">
	function doOK()
	{
		document.getElementById("bSubmit").click();
	}
	function initHandler()
	{
		resizeDialog(530, 520);
	}
</script>

<jsp:include page="/components/forum/new.jsp"/>
<jsp:include page="/components/bottom-public.jsp"/>