<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.BufferedInputStream"%>
<%@page import="java.net.URL"%>
<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.common.FileBrowserTools" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!

public static int downloadUrl(String url, StringBuilder data, StringBuilder statusHeader, Identity logUser)
{
	int responseStatus = -1;

	if (url.startsWith("http://") || url.startsWith("https://"))
	{
		try
		{
			url = Tools.natUrl(url);

			if (url.startsWith("https://"))
			{
				Tools.doNotVerifyCertificates();
			}

			Logger.debug(Tools.class,"DownloadUrl: " + url);

			//body obsahuje URL adresu, ktoru je treba stiahnut
			HttpURLConnection conn = null;
			URL urlObj = new URL(url);
			conn = (HttpURLConnection)urlObj.openConnection();

			conn.setAllowUserInteraction(false);
			conn.setDoInput(true);
			conn.setDoOutput(false);
			conn.setInstanceFollowRedirects(false);

			if (logUser!=null)
			{
				Constants.getServletContext().setAttribute(Constants.USER_KEY, logUser);
				conn.setRequestProperty("userInServletContext", "true");
			}
			else
			{
				Constants.getServletContext().removeAttribute(Constants.USER_KEY);
			}

			conn.connect();
			String location = conn.getHeaderField("Location");
			String encoding = conn.getHeaderField("Content-Type");

			if (encoding==null || encoding.indexOf("charset=")==-1)
			{
				encoding = SetCharacterEncodingFilter.getEncoding();
			}
			else
			{
				encoding = encoding.substring(encoding.indexOf("charset=")+8).trim();
			}

			responseStatus = conn.getResponseCode();
			statusHeader.append(conn.getResponseMessage());

			Logger.debug(Tools.class,"---> ENCODING: " + encoding + " responseStatus="+responseStatus+" location="+location);

			BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
			InputStreamReader in = new InputStreamReader(is, encoding);
			char buffer[] = new char[8000];
			int n = 0;
			while (true)
			{
				 n = in.read(buffer);
				 if (n < 1) break;
				 data.append(buffer, 0, n);
			}
			in.close();

			if (Tools.isNotEmpty(location))
			{
				data.append("REDIRECT TO:").append(location);
			}

		}
		catch (Exception ex)
		{
			Logger.printlnError(Tools.class,"ERROR downloadUrl("+url+")");
			sk.iway.iwcm.Logger.error(ex);
		}
	}
	return(responseStatus);
}

private boolean isAdminFile(String url, String fileContent)
{

	if (url.contains("admin")) return true;

	if (fileContent.contains("admin")) return true;

	return false;
}

private void checkDir(String url, JspWriter out, String baseHref, Identity logUser) throws IOException
{
	IwcmFile[] files = new IwcmFile(Tools.getRealPath(url)).listFiles();
	for (IwcmFile f : files)
	{
//	    out.println(url+f.getName() + "<br>");

		if (f.isDirectory())
		{
			checkDir(url+f.getName()+"/", out, baseHref, logUser);
		}
		else if (f.getName().endsWith(".jsp"))
		{
			String fullUrl = url+f.getName();

			//aby nedoslo k zacykleniu
			if ("/admin/update/test_adminfilesaccess.jsp".equals(fullUrl)) continue;

			String fileContent = FileTools.readFileContent(fullUrl);

			if (isAdminFile(fullUrl, fileContent)==false) continue;


			if (fileContent.indexOf("iwcm:checkLogon")!=-1 && fileContent.indexOf("iwcm.tld")==-1)
			{
				out.println("<span style='color: red;'>"+Tools.escapeHtml(fullUrl)+" MISSING iwcm.tld ************************************************</span><br/>");
			}

			StringBuilder data = new StringBuilder();
			StringBuilder statusHeader = new StringBuilder();

			String httpAddr = baseHref+fullUrl;

			int responseStatus = downloadUrl(httpAddr, data, statusHeader, logUser);

			String dataStr = data.toString();

			if(f.getName().equalsIgnoreCase("conf_export.jsp")) {
			    out.println("<h1>" + Tools.escapeHtml(url+f.getName()) + "</h1>");
			    out.println("<p>responseStatus=" + responseStatus + "; dataStr=" + Tools.escapeHtml(dataStr) + "</p>");
			}

			if (responseStatus == 403) continue;
			if (responseStatus == 302 && dataStr.startsWith("REDIRECT TO:") && dataStr.contains("/admin/logon.jsp")) continue;
			if (responseStatus == 302 && dataStr.startsWith("REDIRECT TO:") && dataStr.contains("/admin/logon/")) continue;
			if (responseStatus == 302 && dataStr.startsWith("REDIRECT TO:") && dataStr.contains("/admin/m/logon.jsp")) continue;
			if (responseStatus == 200 && dataStr.contains("<!-- LOGIN CONTENT Start -->")) continue;
			if (responseStatus == 200 && dataStr.contains("mobile BETA logon")) continue;
			//if (fullUrl.contains("/admin/scripts/")) continue;
			if (fullUrl.contains("/admin/help/")) continue;

			String aStyle = "";
			if (responseStatus!=302)
			{
				aStyle="font-weight: bold; color: red;";
			}

			if (responseStatus!=302)
			{
				out.println("<a style='" + aStyle + "' href='" + Tools.escapeHtml(httpAddr) + "' target='_blank'>" + Tools.escapeHtml(httpAddr) + "</a> status:" + Tools.escapeHtml(responseStatus + " " + statusHeader.toString()) + "<br/>");
				//if (dataStr.startsWith("REDIRECT TO:"))
				{
					out.println(ResponseUtils.filter(DB.prepareString(dataStr, 1000)) + "<br/>");
				}
			}
		}
	}
}


%>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Komponenta pre overenie povolenia pristupu k JSP suborom z /admin/ adresara, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<p>
	S parametrom ?userid=true simuluje aktualne prihlaseneho pouzivatela.
	Odporucame vytvorit admin usera, ktory nema ziadne prava (len pristup na welcome obrazovku), s nim sa prihlasit
	a zavolat /admin/update/test_adminfilesaccess.jsp?userid=true pre overenie, kam vsade ma pristup. Parametrom search
	je mozne zadat meno komponenty, napr. /admin/update/test_adminfilesaccess.jsp?userid=true&search=banner
</p>
<%
if ("fix".equals(request.getParameter("act"))) {
	Identity logUser = null;
	if ("true".equals(Tools.getRequestParameter(request, "userid")))
	{
		logUser = UsersDB.getCurrentUser(request);
	}

	//checkDir("/admin/", out, Tools.getBaseHref(request), logUser);

	String componentName = Tools.getRequestParameter(request, "search");
	if (Tools.isEmpty(componentName) || FileBrowserTools.hasForbiddenSymbol(componentName)) componentName = "form";

	String baseUrl = "/components/"+componentName+"/";
	if (componentName.startsWith("admin"))
	{
		baseUrl = "/"+componentName+"/";
	}

	out.println("<h3>Testing: "+baseUrl+"</h3>");

	checkDir(baseUrl, out, Tools.getBaseHref(request), logUser);

	Constants.getServletContext().removeAttribute(Constants.USER_KEY);
}
%>

<%@ include file="/admin/layout_bottom.jsp" %>
