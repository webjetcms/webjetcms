<%@page import="sk.iway.iwcm.FileTools"%>
<%@page import="sk.iway.iwcm.Identity"%><%@page import="sk.iway.iwcm.SetCharacterEncodingFilter"%><%

String type = "js";
if ("css".equals(Tools.getRequestParameter(request, "t"))) type = "css";

if ("css".equals(type)) sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/css");
else {type="js"; sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript"); }

sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/forceCache/admin-combine."+type, null, request, response);
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Tools" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@page import="sk.iway.iwcm.common.WriteTagToolsForCore"%><%@page import="sk.iway.iwcm.filebrowser.BrowseAction"%><%@page import="sk.iway.iwcm.tags.CombineTag"%>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.StringTokenizer" %>
<%

String lng = CombineTag.getLng(pageContext, request);
if (Tools.getRequestParameter(request, "lng")!=null && Tools.getRequestParameter(request, "lng").length()==2)
{
	lng = Tools.getRequestParameter(request, "lng");
}
String files = Tools.replace( CombineTag.getFiles(Tools.getRequestParameter(request, "set")), "USERLANG", Tools.replace(lng, "cz", "cs"));


if (Tools.isEmpty(files)) return;

StringTokenizer st = new StringTokenizer(files, ",");

Identity user = UsersDB.getCurrentUser(request);

while (st.hasMoreTokens())
{
	String fileUrl = st.nextToken();

    if (fileUrl.indexOf("WEB-INF")!=-1 || fileUrl.indexOf("combine.jsp")!=-1) continue;
	if (BrowseAction.hasForbiddenSymbol(fileUrl)) continue;

	if (fileUrl.endsWith(".js") && (new File(sk.iway.iwcm.Tools.getRealPath(fileUrl+".jsp"))).exists())
	{
		pageContext.include(fileUrl+".jsp");
		out.println();
	}
	else
	{
		String noCustomFileUrl = fileUrl;
        fileUrl = WriteTagToolsForCore.getCustomPage(fileUrl, request);
		if (FileTools.isFile(fileUrl))
		{
			out.println("/* =========================================================================== */");
			out.println("/* "+noCustomFileUrl+" */");
			out.println("/* =========================================================================== */");

			if (fileUrl.toLowerCase().endsWith(".jsp"))
			{
				pageContext.include(fileUrl);
				out.println();
			}
			else if (fileUrl.toLowerCase().endsWith(".js") || fileUrl.toLowerCase().endsWith(".css"))
			{
				//Logger.println(this,"sending custom page:
				// "+f.getAbsolutePath());
				//je to nejaky obrazok, alebo subor, posli na vystup
				String fileContent = FileTools.readFileContent(fileUrl, SetCharacterEncodingFilter.getEncoding());
				fileContent = Tools.replace(fileContent, "url(../", "url(/");
				fileContent = Tools.replace(fileContent, "url('../", "url('/");
				fileContent = Tools.replace(fileContent, "url(\"../", "url(\"/");
				out.println(fileContent);
				out.println();
			}
		}
	}
}
%>
