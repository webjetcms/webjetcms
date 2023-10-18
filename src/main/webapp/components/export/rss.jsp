<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/xml");
%><%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
	import="sk.iway.iwcm.PageLng,sk.iway.iwcm.PathFilter,sk.iway.iwcm.Tools,sk.iway.iwcm.components.export.ExportDatBean, sk.iway.iwcm.doc.DocDB, sk.iway.iwcm.io.IwcmFile, java.text.SimpleDateFormat, java.util.Date, java.util.Locale, java.util.TimeZone"%><%@
taglib
	uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib
	uri="/WEB-INF/iway.tld" prefix="iway"%><%@
taglib
	uri="/WEB-INF/struts-bean.tld" prefix="bean"%><%@
taglib
	uri="/WEB-INF/struts-html.tld" prefix="html"%><%@
taglib
	uri="/WEB-INF/struts-logic.tld" prefix="logic"%><%!
	private static SimpleDateFormat sdf;
	static
	{
		sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public synchronized static String formatDate(long date)
	{
		return sdf.format(new Date(date));
	}%>
<%
	//ak komponentu nemate v stranke ale volate ju priamo
	//tak sem zadajte ID adresarov z ktorych sa maju brat clanky
	ExportDatBean bean = (ExportDatBean)request.getAttribute("exportDatBean");
	if (null == bean)
	{
		return;
	}
	String urlAddress = Tools.getStringValue(bean.getUrlAddress(), "");
	int numberItems = Tools.getIntValue(bean.getNumberItems() , 0);
	String groupIds = bean.getGroupIds();
	//mame to v takomto formate, takze to convertneme
	groupIds = groupIds.replace('+', ',');
	//ak je nastavene na true beru sa do uvahy aj podadresare
	boolean expandGroupIds = false;
	expandGroupIds = bean.getExpandGroupIds();
	String perexGroup = Tools.getStringValue(bean.getPerexGroup(), "");
	perexGroup = perexGroup.replace('+', ',');
	request.setAttribute("perexGroup", perexGroup);
	//usporiada dokumenty podla datumu vytvorenia
	int orderType = DocDB.ORDER_SAVE_DATE;
	String p_order = Tools.getStringValue(bean.getOrderType(), "priority");
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
	boolean ascending = true;
	ascending = bean.getAsc();
	int publishType = DocDB.PUBLISH_NEW;
	String p_publish = Tools.getStringValue(bean.getPublishType(), "new");
	if (p_publish != null)
	{
		if (p_publish.compareToIgnoreCase("new") == 0)
		{
			publishType = DocDB.PUBLISH_NEW;
		}
		else if (p_publish.compareToIgnoreCase("old") == 0)
		{
			publishType = DocDB.PUBLISH_OLD;
		}
		else if (p_publish.compareToIgnoreCase("all") == 0)
		{
			publishType = DocDB.PUBLISH_ALL;
		}
		else if (p_publish.compareToIgnoreCase("next") == 0)
		{
			publishType = DocDB.PUBLISH_NEXT;
		}
	}
	boolean noPerexCheck = false;
	noPerexCheck = bean.isNoPerexCheck();
	if (noPerexCheck && publishType < 100)
	{
		publishType = publishType + 100;
	}
	//pocet dokumentov, ktore sa vygeneruju v JSON
	String title = "RSS Feed";
	String description = "";
	String logo = "/images/logo.gif";
	String url = "/";
	//ziskaj DocDB
	DocDB docDB = DocDB.getInstance();
	docDB.getDocPerex(groupIds, orderType, ascending, publishType, numberItems, "novinky", "pages", request);
	request.setAttribute("NO WJTOOLBAR", "true");
%><?xml version="1.0" encoding="<%=sk.iway.iwcm.SetCharacterEncodingFilter.getEncoding() %>" ?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
<channel> <title><%=title%></title>
<%
	out.print("<link>" + Tools.getBaseHref(request) + "/</link>"); //takto je to kvoli validatoru
%> <description><%=description%></description>
<language><%=PageLng.getUserLng(request)%></language> <pubDate>
<%
	try
	{
		out.print(formatDate(Tools.getNow()));
	}
	catch (Exception ex)
	{
	}
%> </pubDate>
<generator>WebJET Content Management</generator>
<ttl>60</ttl>
<urlAddress><%=urlAddress%></urlAddress> <numberItems><%=numberItems%></numberItems>
<image>
<title><%=title%></title> <url><%=Tools.getBaseHref(request)%><%=logo%></url>
<%
	IwcmFile imageFile = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(url));
	if (imageFile.exists())
	{
		out.print("<link>" + Tools.getBaseHref(request) + url + "</link>");
	}
%> </image>
<atom:link
	href="<%=Tools.getBaseHref(request)%><%=PathFilter.getOrigPathDocId(request)%>"
	rel="self" type="application/rss+xml" /> <logic:present name="novinky">
	<logic:iterate id="doc" name="novinky"
		type="sk.iway.iwcm.doc.DocDetails">
		<item>
		<guid><%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%></guid>
		<title>
		<logic:notEmpty name="doc" property="title">
			<![CDATA[<jsp:getProperty name="doc" property="title"/>]]>
		</logic:notEmpty></title>
		<%
			out.print("<link>" + docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), true, request) + "</link>");
		%> <description><![CDATA[<logic:notEmpty name="doc" property="perex"><jsp:getProperty name="doc" property="perex"/></logic:notEmpty>]]></description>
		<author>
		<logic:notEmpty name="doc" property="authorEmail">
			<bean:write name="doc" property="authorEmail" />(<bean:write
				name="doc" property="authorName" />)</logic:notEmpty></author> <pubDate>
		<%
			try
					{
						if (doc.getPublishStart() > 0)
							out.print(formatDate(doc.getPublishStart()));
						else if (doc.getDateCreated() > 0)
							out.print(formatDate(doc.getDateCreated()));
					}
					catch (Exception ex)
					{
					}
		%> </pubDate></item>
	</logic:iterate>
</logic:present> </channel> </rss>
<%
request.removeAttribute("novinky");
request.removeAttribute("perexGroup");
%>