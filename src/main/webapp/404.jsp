<%

//404 stranku uz generoval ShowDocAction
if (request.getAttribute("404.allready.generated")!=null) return;

sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="sk.iway.iwcm.*,sk.iway.iwcm.common.DocTools"
%><%@ page import="sk.iway.iwcm.components.export.ExportDatBean"%><%@page import="sk.iway.iwcm.components.export.ExportDatDB"
%><%@ page import="sk.iway.iwcm.doc.DocDB,sk.iway.iwcm.doc.DocDetails,sk.iway.iwcm.i18n.Prop" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@page import="sk.iway.iwcm.io.IwcmFile"%><%

//otestuj ci existuje nahrada za tuto stranku
String forward = "/404-"+Constants.getInstallName()+".jsp";
if(Tools.isNotEmpty(Constants.getString("logInstallName")))
{
	forward = "/404-"+Constants.getString("logInstallName")+".jsp";
	java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
	if (fForward.exists())
	{
		pageContext.forward(forward);
		return;
	}
}
java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
if (fForward.exists())
{
	pageContext.forward(forward);
	return;
}

String text = Prop.getInstance().getText("stat.error.404");
if (text.contains("<")==false) {
	text = 	"<p style=\"text-align: center\">\n"+
			"   <br/><br/>"+
			text+
			"\n</p>";
}

//nastav security hlavicky
sk.iway.iwcm.SetCharacterEncodingFilter.setCommonHeaders(response, request);

if (request.getAttribute("is404")!=null)
{
	response.setStatus(404);
	out.println("<html><body>"+text+"</body></html>");
	return;
}

request.setAttribute("is404", "true");

String ua = request.getHeader("User-Agent");

String path = (String)request.getAttribute("path_filter_orig_path");
if (path == null)
{
	path = Tools.getRequestURI(request);
}
if (path == null) path = "??? unknown path ???";


if (path.endsWith("/undefined") || path.endsWith(".map"))
{
	response.setStatus(404);
	out.println("<html><body>"+text+"</body></html>");
	return;
}

/****** ak by nemali modul messages haluzila by admin cast ***/
if ("/components/messages/refresher-ac.jsp".equals(path) || "/none".equals(path) || "/favicon.ico".equals(path))
{
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    out.println("                                     ");
    return;
}

//ochrana pred DOS utokom na neexistujuce stranky (OWASP DirBuster)
String userIP = Tools.getRemoteIP(request);
Cache c = Cache.getInstance();
String KEY = "404-viewCount-"+userIP;
Integer count404 = (Integer)c.getObject(KEY);
if (count404 == null) count404 = Integer.valueOf(0);
count404 = Integer.valueOf(count404.intValue()+1);
c.setObjectSeconds(KEY, count404, 5*60, false);
//System.out.println("||||||||||||||||||||||||||||||||||||||||"+count404.intValue());

//ocharana pre Google aby sa po redizajne / zmene URL nehadzali 404
if (ua != null && ua.toLowerCase().indexOf("googlebot")!=-1) count404 = 0;

int count404Limit = SpamProtection.getHourlyPostLimit("404");
if (count404Limit < 150) count404Limit = 150;

if (count404.intValue()>count404Limit && "iwcm.interway.sk".equals(request.getServerName())==false)
{
	System.out.println("404 attack, KEY="+KEY+" count="+count404);

	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	out.println("<html><body>"+text+"</body></html>");

	try
	{
		if (ua != null && ua.indexOf("DirBuster")!=-1) session.invalidate();
		else if (request.getServerName().indexOf("cms")==-1) session.invalidate();
		else session.setMaxInactiveInterval(300);
	} catch (Exception e) {}

	return;
}

//niekedy nam moze prist request vratane ;jsessionid a teda dochadza len k zlemu dekodovaniu URL
int jsessionid = path.indexOf(";jsessionid=");
if (jsessionid > 0)
{
	path = path.substring(0, jsessionid);
	response.setStatus(302);
	response.setHeader("Location", path);
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");
	out.println("Redirecting");
	return;
}

//skus ziskat redirect

//jscripts/ adresy rovno vratim ako 404 pretoze by sa mi to aj tak detekovalo ako XSS a zbytocne auditovalo
if ("/404.jsp".equals(path) || path.startsWith("/jscripts/"))
{
	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	out.println("<html><body>"+text+"</body></html>");
	return;
}

Logger.warn("404.jsp", "SOM STRANKA 404, path="+path+" serverName="+request.getServerName()+" domain="+DocDB.getDomain(request));

if (DocTools.testXss(path))
{
	//je to pokus o XSS: /404.html/'onmouseover=prompt(915761)
	Adminlog.add(Adminlog.TYPE_XSS, "XSS path="+path, -1, -1);
	response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	out.println("<html><body>"+text+"</body></html>");
	return;
}

String queryString = (String)request.getAttribute("path_filter_query_string");

/******* DEFAULT ROBOTS.TXT ***********/
if ("/robots.txt".equals(path))
{
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/plain");
	response.setStatus(HttpServletResponse.SC_OK);

	IwcmFile file = new IwcmFile(Tools.getRealPath("/files/robots.txt"));
	if (file.exists()) {
	    text = FileTools.readFileContent(file.getVirtualPath());
	    out.print(text);
	    return;
	}

	String robotsNoindexDomains = Constants.getString("robotsNoindexDomains");
	if(Tools.isNotEmpty(robotsNoindexDomains))
	{
		for(String robotsDomain : Tools.getTokens(robotsNoindexDomains, "|,", true))
		{
			if(Tools.getServerName(request).indexOf(robotsDomain) > -1)
			{
				out.println("User-agent: *");
				out.println("Disallow: /");
				return;
			}
		}
	}
	%>User-Agent: *
<% if (Tools.isEmpty(Constants.getString("adminEnableIPs")) && Constants.getBoolean("adminLogonShowSimpleErrorMessage")==false && "full".equals(Constants.getString("clusterMyNodeType"))) {  %>Disallow: /admin
<% }
%>Disallow: /components
Disallow: /thumb
Sitemap: <%=Tools.getBaseHref(request)%>/sitemap.xml
<%
	return;
}

/******* DEFAULT SITEMAP.XML ***********/
if ("/sitemap.xml".equals(path) || "/google-sitemap.jsp".equals(path))
{
	//tu mame natvrdo UTF-8 pretoze generujeme UTF-8 XML subor
	response.setContentType("text/xml; charset=utf-8");
	response.setStatus(HttpServletResponse.SC_OK);
	String customPage = sk.iway.iwcm.tags.WriteTag.getCustomPage("/components/sitemap/google-sitemap.jsp", request);
	pageContext.forward(customPage);
	return;
}

ExportDatDB expDB = new ExportDatDB();
ExportDatBean exportDatBean = expDB.findFirstExportByUrlAddress(path);
if(null != exportDatBean){
	int exportDatId = exportDatBean.getExportDatId();
	String format = exportDatBean.getFormat();
	request.setAttribute("exportDatBean",exportDatBean);
	String urlExportDat = "";
	if(format.contains("rss") || format.contains("xml") || path.contains(".xml") || path.contains(".rss")){
		response.setContentType("text/xml; charset="+SetCharacterEncodingFilter.getEncoding());
	} else {
		response.setContentType("application/json; charset="+SetCharacterEncodingFilter.getEncoding());
	}
	response.setStatus(HttpServletResponse.SC_OK);
	urlExportDat = "/components/export/"+format+".jsp";
	String customPage = sk.iway.iwcm.tags.WriteTag.getCustomPage(urlExportDat, request);
	pageContext.forward(customPage);
	return;
}



String redirectURL = null;

//znak ^ sa interne spracovava ako ? kvoli handlingu parametrov (aka exact match)
boolean redirectIncludingQuery = false;
if (Tools.isNotEmpty(queryString))
{
	redirectURL = UrlRedirectDB.getRedirect(path+"^"+queryString, DocDB.getDomain(request));
	if (Tools.isNotEmpty(redirectURL)) redirectIncludingQuery = true;
}
if (Tools.isEmpty(redirectURL)) redirectURL = UrlRedirectDB.getRedirect(path, DocDB.getDomain(request));

if (Constants.getBoolean("multiDomainEnabled")==true && Tools.isEmpty(redirectURL))
{
	redirectURL = UrlRedirectDB.getRedirect(MultiDomainFilter.fixDomainPaths(path, request), DocDB.getDomain(request));
}

if (Tools.isNotEmpty(redirectURL))
{
	int statusCode = 301;
	try {
		statusCode = (new sk.iway.iwcm.database.SimpleQuery()).forInt("SELECT redirect_code FROM url_redirect WHERE old_url=? ORDER BY insert_date DESC", path);
		if (statusCode<1) statusCode = (new sk.iway.iwcm.database.SimpleQuery()).forInt("SELECT redirect_code FROM url_redirect WHERE old_url=? ORDER BY insert_date DESC", path+"/");
	} catch (Exception ex) {}
	if (statusCode<1) statusCode=301;
	response.setStatus(statusCode);

	if (redirectIncludingQuery==false && Tools.isNotEmpty(queryString)) redirectURL = Tools.addParametersToUrlNoAmp(redirectURL, queryString);

	if (redirectURL.toLowerCase().startsWith("http")==false) redirectURL = Tools.getBaseHref(request)+redirectURL;

	response.setHeader("Location", redirectURL);
	%>
	<html><script>window.location.href='<%=redirectURL%>';</script></html>
	<%
	System.out.println("SOM STRANKA 404, redirecting to:"+redirectURL);
	return;
}

//toto je standard hacking pokus
if (path.startsWith("/_vti") || path.startsWith("/MSOffice") || path.endsWith("/undefined") || (path.startsWith("/admin") && "public".equals(Constants.getString("clusterMyNodeType"))==false))
{
	//musime vygenerovat nejaky vystup, inak tomcat pouzije defaultne hlasenie
	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	out.println("<html><body>"+text+"</body></html>");
	return;
}

//GoogleBot parsuje aj obfuscovane formulare a potom to hlasi ako chybne linky
if (path.length() <= 3 && ua!=null && ua.toLowerCase().indexOf("bot")!=-1)
{
	response.setStatus(301);
	response.setHeader("Location", Tools.getBaseHref(request)+"/");
	out.println("<html><body>redirecting</body></html>");
	return;
}

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

String referer = request.getHeader("referer");

String statPath = path;
if (queryString != null)
{
   statPath = path + "?" + queryString;
}
if (referer != null)
{
   referer = "referer: " + referer;
}

//zapis do logu
StatDB.addError(statPath, referer);

if ("/components/gdpr/jscripts/jquery.cookie.js".equals(path)) {
	//Bezpecnost - odstranena stara verzia jquery.cookie.js v aplikacii GDPR, nahradena verziou v _common adresari.
	response.sendRedirect("/components/_common/javascript/jquery.cookie.js");
	return;
}

path = path.toLowerCase();
if (path.endsWith(".gif") || path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".js") || path.endsWith(".swf") || path.endsWith(".ico") || path.endsWith(".css") || path.endsWith(".ttf") || path.endsWith(".eot") || path.endsWith(".woff2") || path.endsWith(".woff"))
{
	Logger.warn("404.jsp", "404 ("+Constants.getInstallName()+"): " + path+"?"+request.getQueryString());
	//posli rovno chybu 404 do prehliadaca
   return;
}
else if (PathFilter.checkWebAccess(request, path)==true)
{
	//skusme najst custom 404 stranku vo WebJETe
	try
	{
		String testPath = path;
		if (testPath.endsWith("/")) testPath = testPath.substring(0, testPath.length()-1);
		int failsafe = 0;
		while (failsafe++ < 30)
		{
			String url404 = testPath + "/404.html";

			int docId = DocDB.getDocIdFromURL(url404, DocDB.getDomain(request));

			//System.out.println("Testing: "+url404+" docid="+docId);
			if (docId > 0 && request.getParameter("historyid")==null)
			{

				DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
				if (doc != null && doc.isAvailable()) {

					//System.out.println("404.jsp: forwarding "+url404+" = "+docId+" ip="+Tools.getRemoteIP(request));
					if (ContextFilter.isRunning(request)) {
						//response.sendRedirect(request.getContextPath()+url404);
						//pageContext.forward("/showdoc.do?docid="+docId);
						ContextRequestWrapper contextRequest = new ContextRequestWrapper(request);
						ContextResponseWrapper wrapper = new ContextResponseWrapper(response, request);

						request.getRequestDispatcher("/showdoc.do?docid=" + docId).forward(contextRequest, wrapper);

						ContextFilter.doFilterAddContextPathImpl(contextRequest, response, path, wrapper, true);
					} else {
						pageContext.forward("/showdoc.do?docid=" + docId);
					}
					return;
				}
			}

			int start = testPath.lastIndexOf("/");
			if (start != -1) testPath = testPath.substring(0, start);
			else break;
		}
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}


	//ak chcete spravit presmerovanie na nejaku chybovu stranku - odkomentujte
	//response.sendRedirect("/showdoc.do?docid=4");
	if (text.indexOf("<body")!=-1)
	{
		out.println(text);
		return;
	}
%>


<%@page import="sk.iway.iwcm.stat.StatDB"%>
<%@page import="sk.iway.iwcm.system.UrlRedirectDB"%>
<%@ page import="sk.iway.iwcm.system.context.ContextFilter" %>
<%@ page import="sk.iway.iwcm.system.context.ContextRequestWrapper, sk.iway.iwcm.system.context.ContextResponseWrapper, sk.iway.iwcm.system.multidomain.MultiDomainFilter" %>
<html>
	<head>
		<title>404</title>
		<meta http-equiv="Content-type" content="text/html">
	</head>
	<body>
	<!--
	-- Unnnfortunately, Microsoft has added a clever new
	-- "feature" to Internet Explorer. If the text in
	-- an error's message is "too small", specifically
	-- less than 512 bytes, Internet Explorer returns
	-- its own error message. Yes, you can turn that
	-- off, but *surprise* it's pretty tricky to find
	-- buried as a switch called "smart error
	-- messages" That means, of course, that many of
	-- error messages are censored by default.
	-- And, of course, you'll be shocked to learn that
	-- IIS always returns error messages that are long
	-- enough to make Internet Explorer happy. The
	-- workaround is pretty simple: pad the error
	-- message with a big comment to push it over the
	-- five hundred and twelve byte minimum. Of course,
	-- that's exactly what you're reading right now....
	-->
	<p style="text-align: center">
	   <br/><br/>
		<iwcm:text key="stat.error.404"/>
	</p>
	</body>
	</html>

<%
}
%>
