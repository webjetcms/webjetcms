<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.system.WJResponseWrapper" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.apache.struts.util.ResponseUtils" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/><%!

private void checkDir(String url, JspWriter out, HttpServletRequest request, HttpServletResponse response) throws IOException
{
	IwcmFile[] files = FileTools.sortFilesByName(new IwcmFile(Tools.getRealPath(url)).listFiles());
	for (IwcmFile f : files)
	{
		//stare projekty
		if ("city-university".equals(f.getName())) continue;
		if ("generali".equals(f.getName())) continue;
		if ("eekonomia".equals(f.getName())) continue;
		if ("ingbank".equals(f.getName())) continue;
		if ("lesy".equals(f.getName())) continue;
		if ("ppa".equals(f.getName())) continue;
		if ("centrum_byvania".equals(f.getName())) continue;
		if ("uniqa".equals(f.getName())) continue;
		if ("upsvar".equals(f.getName())) continue;
		if ("vucpresov".equals(f.getName())) continue;
		if ("cloud".equals(f.getName())) continue;
		if ("webjet_eu".equals(f.getName())) continue;
		if ("intranet".equals(f.getName())) continue;
		if ("mail".equals(f.getName())) continue;
		if ("mcalendar".equals(f.getName())) continue;
		if ("pss".equals(f.getName())) continue;
		if ("siaf_ps".equals(f.getName())) continue;

		//dlho trvajuce/zacyklenie
		if ("test_jspcompilation.jsp".equals(f.getName())) continue;
		if (f.getVirtualPath().startsWith("/admin/update/")) continue;
		if ("keep_alive_test.jsp".equals(f.getName())) continue;

		//toto su includes
		if ("/components/gdpr/admin_list_search_detail.jsp".equals(f.getVirtualPath())) continue;
		if (f.getName().startsWith("inc_")) continue;

		if (f.isDirectory())
		{
			checkDir(url+f.getName()+"/", out, request, response);
		}
		else if (f.getName().endsWith(".jsp"))
		{
			String fullUrl = url+f.getName();

			System.out.println(fullUrl);
			out.println(Tools.escapeHtml(fullUrl)+"<br/>");
			out.flush();

			try
			{
				WJResponseWrapper respWrapper = new WJResponseWrapper(response, request);
				request.getRequestDispatcher(fullUrl).include(request, respWrapper);
			}
			catch (Exception ex)
			{
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));

				String stack = sw.toString();

				if (stack.contains("Unable to compile class for JSP"))
				{
					int i = stack.indexOf("Stacktrace:");
					if (i > 20) stack = stack.substring(0, i);

					out.println("CHYBA:<br/>");
					out.println(ResponseUtils.filter(stack));
				}
			}

		}
	}
}


%>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Komponenta pre overenie, ci idu skompilovat JSP subory v danom adresari</h1>

<%
	String subdir = Constants.getInstallName();
	if (Tools.getRequestParameter(request, "subdir")!=null) subdir = Tools.getRequestParameter(request, "subdir");
%>

<form action="test_jspcompilation.jsp" method="post">
	Meno adresára: /components/<input type="text" name="subdir" value="<%=ResponseUtils.filter(subdir)%>"/> <input type="submit" value="Overiť kompiláciu"/>
</form>

<% if (Tools.getRequestParameter(request, "subdir")!=null) { %>
	<div style="white-space: pre">
		<%
		if (Tools.isEmpty(subdir) || "*".equals(subdir)) {
			checkDir("/components/", out, request, response);
		}
		else if ("admin".equals(subdir)) {
			checkDir("/admin/", out, request, response);
		}
		else {
			checkDir("/components/" + subdir + "/", out, request, response);
		}
		%>
	</div>
<% } %>

<%@ include file="/admin/layout_bottom.jsp" %>
