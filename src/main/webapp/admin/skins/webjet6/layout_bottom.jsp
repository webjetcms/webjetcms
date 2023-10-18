<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%
if ("true".equals(Tools.getRequestParameter(request, SetCharacterEncodingFilter.PDF_PRINT_PARAM)))
{
	pageContext.include("/admin/layout_bottom_pdf.jsp");
	return;
}

try
{
	if (request.getAttribute("generationStartDate")!=null)
	{
		java.util.Date generationStartDate = (java.util.Date)request.getAttribute("generationStartDate");
		long startTime = generationStartDate.getTime();
		long endTime = (new java.util.Date()).getTime();
		long generationTimeDiff = endTime - startTime;

		if ("i222wcm.interway.sk".equals(request.getServerName()) && Tools.getRequestURI(request).endsWith("editor.jsp")==false && Tools.getRequestURI(request).endsWith("admin_gallery_popup.jsp")==false)
		{
		%><div style='clear: both;'><iwcm:text key="layout.page_generated_at"/>: <%=generationTimeDiff%> ms</div><%
		//session.removeAttribute("generationStartDate");
		}
	}
}
catch (Exception ex)
{
	sk.iway.iwcm.Logger.error(ex);
}
%>

</body>
<script type="text/javascript">
if (document.getElementById("waitDiv")!=null) document.getElementById("waitDiv").style.display="none";
addTableTDRollover();
<%
String actualURL = sk.iway.iwcm.PathFilter.getOrigPath(request);
if (sk.iway.iwcm.Tools.isNotEmpty(Tools.getRequestQueryString(request))) actualURL = actualURL+"?"+Tools.getRequestQueryString(request);
%>
</script>

<%@page import="sk.iway.iwcm.SetCharacterEncodingFilter"%>
<%@ page import="sk.iway.iwcm.Tools" %>
</html>
