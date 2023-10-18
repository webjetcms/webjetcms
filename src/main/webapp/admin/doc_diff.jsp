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
page import="java.util.List"%><%!

public static TextNodeComparator getTextNodeComparator(String htmlCode, int docId, int historyId, Locale locale) throws IOException, SAXException
{
	//zapis to do TMP suboru, lebo to inak hadze nejake NPE na StringReaderi
	String fileURL = getFileName(docId, historyId);

	FileTools.saveFileContent(fileURL, htmlCode, SetCharacterEncodingFilter.getEncoding());

	HtmlCleaner cleaner = new HtmlCleaner();
	DomTreeBuilder oldHandler = new DomTreeBuilder();
	InputSource oldSource = new InputSource(new FileInputStream(new File(sk.iway.iwcm.Tools.getRealPath(fileURL))));
	cleaner.cleanAndParse(oldSource, oldHandler);

	TextNodeComparator comparator = new TextNodeComparator(oldHandler, locale);

	return comparator;
}

public static String getFileName(int docId, int historyId)
{
	String fileURL = "/WEB-INF/tmp/daisy_old_html-"+docId+"-"+historyId+".html";
	if (historyId < 1) fileURL = "/WEB-INF/tmp/daisy_old_html-"+docId+".html";

	return fileURL;
}

public static void cleanupFile(int docId, int historyId)
{
	File f = new File(sk.iway.iwcm.Tools.getRealPath(getFileName(docId, historyId)));
	if (f.exists()) f.delete();
}
%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

Cache cache = Cache.getInstance();
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

StringWriter outStreamWriter = new StringWriter();

SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();

TransformerHandler result = tf.newTransformerHandler();
result.setResult(new StreamResult(outStreamWriter));

XslFilter filter = new XslFilter();

ContentHandler postProcess = filter.xsl(result, "org/outerj/daisy/diff/htmlheader.xsl");

String prefix = "diff";

//todo: nejako rozumnejsie
Locale locale = Locale.getDefault();

TextNodeComparator leftComparator = getTextNodeComparator(htmlCodeOld, docId, historyId, locale);
TextNodeComparator rightComparator = getTextNodeComparator(htmlCodeNew, docId, -1, locale);

postProcess.startDocument();
postProcess.startElement("", "diffreport", "diffreport", new AttributesImpl());

postProcess.startElement("", "diff", "diff", new AttributesImpl());
HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess, prefix);

HTMLDiffer differ = new HTMLDiffer(output);
differ.diff(rightComparator, leftComparator);
postProcess.endElement("", "diff", "diff");
postProcess.endElement("", "diffreport", "diffreport");
postProcess.endDocument();

String headHtml = htmlCodeNew;
headHtml = headHtml.replaceAll("<head[\\s\\S]*?>([\\s\\S]*?)</head>[\\s]*?<body([\\s\\S]*?)>[\\s\\S]*?</body>","<head>$1<link href=\"/components/_common/css/daisy-diff.css\" type=\"text/css\" rel=\"stylesheet\"/><script type=\"text/javascript\" src=\"/components/_common/javascript/jquery.tools.tooltip.js\" ></script></head><body$2>");
headHtml = headHtml.replace("$","\\$");
if (headHtml.indexOf("<head") !=-1) headHtml = headHtml.substring(headHtml.indexOf("<head"));

String outputHtml = outStreamWriter.toString();
outputHtml = outputHtml.replaceAll("<head[\\s\\S]*?>[\\s\\S]*?<body[\\s\\S]*?>", headHtml);

outputHtml = Tools.replace(outputHtml, "\" changes=\"", "\" title=\"");

Tools.insertJQuery(request);
out.println(outputHtml);

cleanupFile(docId, historyId);
cleanupFile(docId, -1);
%>
<script type="text/javascript">
// Create the tooltips only on document load
$(document).ready(function()
{
   $('[title]').tooltip({opacity: 0.7, tipClass: "daisyDiffTooltip" });
});
</script>
