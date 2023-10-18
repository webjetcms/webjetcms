<%@page import="org.apache.commons.codec.binary.Base64"%><%@page import="org.apache.struts.util.ResponseUtils"%><%@page import="sk.iway.iwcm.*"%><%@page import="sk.iway.iwcm.common.DocTools"%><%@page import="sk.iway.iwcm.doc.DocDB"%><%@page import="sk.iway.iwcm.system.stripes.CSRF"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.Random" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

//modul pre deaktivovanie SPAM ochrany formularov

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String backurl = Tools.getRequestParameterUnsafe(request, "backurl");
if (Tools.isEmpty(backurl) || DocTools.testXss(backurl) || backurl.startsWith("/") == false) backurl = "/";

//pridany test na existenciu stranky kvoli podvrhom backurl
try
{
	String url = backurl;
	if (url.indexOf("?")!=-1) url = url.substring(0, url.indexOf("?"));

	//ak tam nebol otaznik ale je tam znak & je to isto podvrh
	if (url.indexOf("&")!=-1) backurl = "/";

	int docid = DocDB.getInstance().getDocIdFromURLImpl(url, DocDB.getDomain(request));

	if (docid < 1)
	{
		if (url.endsWith(".jsp")==false && url.endsWith(".action")==false && url.endsWith(".do")==false)
		{
			backurl = "/";
		}
	}
}
catch (Exception ex)
{

}

Base64 b64 = new Base64();

boolean vysledokSpravny = false;
if (Tools.isNotEmpty(Tools.getRequestParameter(request, "result")) && "post".equalsIgnoreCase(request.getMethod()))
{
	//verifikuj CSRF token
	boolean isCsrfOk = CSRF.verifyTokenAndDeleteIt(request);
	if (isCsrfOk==false)
	{
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		pageContext.forward("/403.jsp");
		return;
	}

	int vysledok = Tools.getIntValue(Tools.getRequestParameter(request, "result"), 0);

   int vysledokDecoded = vysledok - 1;
   if (Tools.getRequestParameter(request, "hash")!=null)
   {
   	String decoded = new String(b64.decode(Tools.replace(Tools.getRequestParameter(request, "hash"), "|", "=").getBytes()));
   	vysledokDecoded = Tools.getIntValue( decoded, 0 );
   }

   //System.out.println("Spamprotectiondisable, vysledok="+vysledok+" vysledokDecoded="+vysledokDecoded);

   if (vysledok != vysledokDecoded) request.setAttribute("vysledokNespravny", "true");
   else
   {
   	vysledokSpravny = true;
   	session.setAttribute("WriteTag.disableSpamProtectionJavascript", "true");
   }
}

Random rand = new Random();
int cislo1 = 10 + rand.nextInt(15);
int cislo2 = 10 + rand.nextInt(15);

boolean scitaj = rand.nextInt(100) < 50;

int vysledok;
if (scitaj) vysledok = cislo1 + cislo2;
else vysledok = cislo1 - cislo2;

//System.out.println("Spamprotectiondisable, cislo1="+cislo1+" cislo2="+cislo2+" spravne="+vysledok+" sid="+session.getId());

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="<%=PageLng.getUserLng(request)%>" lang="<%=PageLng.getUserLng(request)%>">
   <head>
		<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
		<title><iwcm:text key="spamprotectiondisable.title"/></title>

		<meta http-equiv="Content-Type" content="text/html; charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />
		<%
		if (FileTools.isFile("/css/page.css")) {
		%>
			<link rel="stylesheet" href="/css/page.css" type="text/css" media="screen" />
	   <% } %>
	</head>
<body class="spamProtectionBody">
<h1><iwcm:text key="spamprotectiondisable.title"/></h1>

<% if (vysledokSpravny) { %>
	<iwcm:text key="spamprotectiondisable.resultok" param1="<%=backurl %>"/>
<% } else { %>

<iwcm:text key="spamprotectiondisable.introtext"/>

<logic:present name="vysledokNespravny"><iwcm:text key="spamprotectiondisable.resultinvalid" /></logic:present>

<form action="<%=PathFilter.getOrigPath(request) %>" method="post">

	<label for="spamProtectionDisableResult"><iwcm:text key="<%=(\"spamprotectiondisable.calculate_\"+scitaj) %>" param1="<%=String.valueOf(cislo1) %>" param2="<%=String.valueOf(cislo2) %>" /></label>

	<input id="spamProtectionDisableResult" type="text" size="5" maxlength="10" name="result"/>
	<input type="submit" value="<iwcm:text key="button.submit"/>"/>

	<%CSRF.writeCsrfTokenInputFiled(request.getSession(), out);%>

	<input type="hidden" name="backurl" value="<%=ResponseUtils.filter(backurl) %>"/>
	<input type="hidden" name="hash" value="<%=Tools.replace(new String(b64.encode(String.valueOf(vysledok).getBytes())), "=", "|") %>"/>
</form>

<% } %>

</body>
</html>
