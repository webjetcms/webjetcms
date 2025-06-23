<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" 
import="sk.iway.iwcm.*,
javax.xml.parsers.*,
org.w3c.dom.*,
java.io.*,
java.util.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String rssUrl = pageParams.getValue("rssUrl", null);
if (Tools.isEmpty(rssUrl)) return;
int maxItems = pageParams.getIntValue("maxItems", 3);

if (rssUrl.startsWith("http")==false) rssUrl = "http://"+Tools.getServerName(request)+rssUrl;
System.out.println("rss_reader.jsp, getting data from: "+rssUrl);

DocumentBuilderFactory b = DocumentBuilderFactory.newInstance();
DocumentBuilder dc = b.newDocumentBuilder();
Document doc = dc.parse(rssUrl);
if(doc != null) 
{
	//ziskaj zoznam listov /rateList/rate
	Vector list = XmlUtils.getChildNodesByPath(doc.getDocumentElement(), "/channel/item");
	if( list != null ) 
	{	
		//iteuj po jednotlivych uzloch a vypisuj do tabulky
		Iterator iter = list.iterator();
		int counter = 1;
		out.println("<ul>");
		while (iter.hasNext()) 
		{
			Node item = (Node)iter.next();
			
			String nodeText = XmlUtils.getFirstChildValue(item, "title");
			if (nodeText == null)
			{
				//CDATA fix
				Node nodeTitle = XmlUtils.getFirstChild(item, "title");
				if (nodeTitle!=null && nodeTitle.getChildNodes().getLength()>0)
				{
					nodeText = nodeTitle.getChildNodes().item(0).getNodeValue();
				}
			}
			if (nodeText == null) continue;
			
			String descriptionText = XmlUtils.getFirstChildValue(item, "description");
			if (descriptionText==null)
			{
				//CDATA fix
				Node nodeDescription = XmlUtils.getFirstChild(item, "description");
				if (nodeDescription!=null && nodeDescription.getChildNodes().getLength()>0)
				{
					descriptionText = nodeDescription.getChildNodes().item(0).getNodeValue();
				}
			}
			
			out.println("<li>");
			out.println("<a href='"+XmlUtils.getFirstChildValue(item, "link")+"' target='_blank'>"+nodeText+"</a>");
			out.println("<p>"+descriptionText+"</p>");
			out.println("</li>");

			if (counter++ >= maxItems) break;			
		}
		out.println("</ul>");
	}
}
%>

