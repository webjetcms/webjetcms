<%@page import="java.io.IOException"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.io.CharArrayReader"%>
<%@page import="java.io.Reader"%>
<%@page import="java.io.StringWriter"%>
<%@page import="java.util.Locale"%>
<%@page import="java.io.StringReader"%>
<%@page import="org.xml.sax.InputSource"%>
<%@page import="org.outerj.daisy.diff.html.HTMLDiffer"%>
<%@page import="org.outerj.daisy.diff.html.HtmlSaxDiffOutput"%>
<%@page import="org.xml.sax.helpers.AttributesImpl"%>
<%@page import="org.outerj.daisy.diff.html.TextNodeComparator"%>
<%@page import="org.outerj.daisy.diff.html.dom.DomTreeBuilder"%>
<%@page import="org.xml.sax.ContentHandler"%>
<%@page import="org.outerj.daisy.diff.*"%>
<%@page import="javax.xml.transform.stream.StreamResult"%>
<%@page import="javax.xml.transform.*"%>
<%@page import="javax.xml.transform.sax.*"%>
<%@page import="sk.iway.iwcm.tags.BuffTag"%>
<%@page import="sk.iway.iwcm.system.WJResponseWrapper"%>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true"/><%@
page import="org.xml.sax.SAXException"%><%@
page import="java.util.regex.Pattern"%><%@
page import="java.util.regex.Matcher"%><%@
page import="java.util.ArrayList"%><%@
page import="java.util.List"%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"),-1);
int historyId = Tools.getIntValue(Tools.getRequestParameter(request, "historyid"),-1);

WJResponseWrapper respWrapper = new WJResponseWrapper(response,request);
request.setAttribute(BuffTag.IS_BUFF_TAG, "true");
request.getRequestDispatcher("/showdoc.do?docid="+docId+"&historyid=-1&NO_WJTOOLBAR=true"+("true".equals(Tools.getRequestParameter(request, "onlyBody")) ? "&forwarddoccompare=true" : "")).include(request, respWrapper);
String htmlCodeNew = respWrapper.strWriter.getBuffer().toString();
if (respWrapper.redirectURL!=null)
{
	htmlCodeNew = "Redirected to: "+respWrapper.redirectURL;
}
htmlCodeNew = Tools.replace(htmlCodeNew, "&nbsp;", " "); //jeeff: pridane, lebo do WJ sme pridali editorSingleCharNbsp a potom to hlasilo nezmyselne rozdiely (tvrdu medzeru nevidno)

respWrapper = new WJResponseWrapper(response,request);
request.getRequestDispatcher("/showdoc.do?docid="+docId+"&historyid="+historyId+"&NO_WJTOOLBAR=true"+("true".equals(Tools.getRequestParameter(request, "onlyBody")) ? "&forwarddoccompare=true" : "")).include(request, respWrapper);
String htmlCodeOld = respWrapper.strWriter.getBuffer().toString();
if (respWrapper.redirectURL!=null)
{
	htmlCodeOld = "Redirected to: "+respWrapper.redirectURL;
}
htmlCodeOld = Tools.replace(htmlCodeOld, "&nbsp;", " ");

String outputHtml = sk.iway.iwcm.common.DocTools.getHtmlDiff(htmlCodeNew, htmlCodeOld);

Tools.insertJQuery(request);
out.println(outputHtml);
%>
<script type="text/javascript">
// Create the tooltips only on document load
$(document).ready(function()
{
   $('[title]').tooltip({opacity: 0.7, tipClass: "daisyDiffTooltip" });
});
</script>