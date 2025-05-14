<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/xml");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*,java.text.*"%><%@page import="sk.iway.iwcm.io.IwcmFile"%><%@ page import="java.text.SimpleDateFormat" %><%@ page import="java.util.Locale" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%!

private static SimpleDateFormat sdf;

static
{
   sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
   sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
}

public synchronized static String formatDate(long date)
{
	return sdf.format(new Date(date));
}

%><%
//ak komponentu nemate v stranke ale volate ju priamo
//tak sem zadajte ID adresarov z ktorych sa maju brat clanky
String defaultGroupIds = "1";

PageParams pageParams = new PageParams(request);
String groupIds = pageParams.getValue("groupIds", defaultGroupIds);

//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

//ak je nastavene na true beru sa do uvahy aj podadresare
boolean expandGroupIds = pageParams.getBooleanValue("expandGroupIds", false);

String title = "RSS Feed";
String description = "";
String logo = "/images/logo.gif";
String url = "/";

DocDetails docDetails = (DocDetails)request.getAttribute("docDetails");
if (docDetails != null)
{
	title = docDetails.getTitle();
	description = docDetails.getPerex();
	if (Tools.isNotEmpty(docDetails.getPerexImage())) logo = docDetails.getPerexImage();
}

//usporiada dokumenty podla datumu vytvorenia
int orderType = DocDB.ORDER_SAVE_DATE;
String p_order = pageParams.getValue("orderType", "date");
if (p_order != null)
{
	if (p_order.compareTo("date") == 0)
	{
		orderType = DocDB.ORDER_DATE;
	}
	else if (p_order.compareTo("id") == 0)
	{
		orderType = DocDB.ORDER_ID;
	}
	else if (p_order.compareTo("priority") == 0)
	{
		orderType = DocDB.ORDER_PRIORITY;
	}
	else if (p_order.compareTo("title") == 0)
	{
		orderType = DocDB.ORDER_TITLE;
	}
	else if (p_order.compareTo("place") == 0)
	{
		orderType = DocDB.ORDER_PLACE;
	}
	else if (p_order.compareTo("eventDate") == 0)
	{
		orderType = DocDB.ORDER_EVENT_DATE;
	}
	else if (p_order.compareTo("saveDate") == 0)
	{
		orderType = DocDB.ORDER_SAVE_DATE;
	}
}

//vrati vsetky dokumenty, bez ohladu na datum publikovania neberie ohlad na to, ci je zadany text perexu
int publishType = DocDB.PUBLISH_NO_PEREX_CHECK_ALL;

//pocet dokumentov, ktore sa vygeneruju v XML
int pageSize = 10;

//ziskaj DocDB
DocDB docDB = DocDB.getInstance();
docDB.getDocPerex(groupIds, orderType, false, publishType, pageSize, "novinky", "pages", request);
request.setAttribute("NO WJTOOLBAR", "true");
%><?xml version="1.0" encoding="<%=sk.iway.iwcm.SetCharacterEncodingFilter.getEncoding() %>" ?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
	<channel>
		<title><%=title %></title>
		<% out.print("<link>"+Tools.getBaseHref(request)+"/</link>"); //takto je to kvoli validatoru %>
		<description><%=description %></description>
		<language><%=PageLng.getUserLng(request) %></language>
		<pubDate><%
		try
	   {
			out.print(formatDate(Tools.getNow()));
	   } catch (Exception ex) {}
		%></pubDate>
		<generator>WebJET Content Management</generator>
		<ttl>60</ttl>
		<image>
			<title><%=title %></title>
			<url><%=Tools.getBaseHref(request)%><%=logo %></url>
			<%
			IwcmFile imageFile = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(url));
			if (imageFile.exists())
			{
				out.print("<link>"+Tools.getBaseHref(request)+url+"</link>");
			} %>
		</image>
		<atom:link href="<%=Tools.getBaseHref(request)%><%=PathFilter.getOrigPathDocId(request) %>" rel="self" type="application/rss+xml" />
		<iwcm:present name="novinky">
			<iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
				<item>
					<guid><%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request) %></guid>
					<title><iwcm:notEmpty name="doc" property="title"><![CDATA[<jsp:getProperty name="doc" property="title"/>]]></iwcm:notEmpty></title>
					<% out.print("<link>"+docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), true, request)+"</link>"); %>
					<description>
						<![CDATA[
						<iwcm:notEmpty name="doc" property="perex"><jsp:getProperty name="doc" property="perex"/></iwcm:notEmpty>
						]]>
					</description>
					<author><iwcm:notEmpty name="doc" property="authorEmail"><iwcm:beanWrite name="doc" property="authorEmail"/> (<iwcm:beanWrite name="doc" property="authorName"/>)</iwcm:notEmpty></author>
					<pubDate><%
					   try
					   {
					   	  if (doc.getPublishStart()>0) out.print(formatDate(doc.getPublishStart()));
					   	  else if (doc.getDateCreated() > 0) out.print(formatDate(doc.getDateCreated()));
					   } catch (Exception ex) {}
					%></pubDate>
				</item>
			</iwcm:iterate>
		</iwcm:present>
	</channel>
</rss>