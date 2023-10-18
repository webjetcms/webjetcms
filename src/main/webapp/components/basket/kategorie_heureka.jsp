<%@page import="java.io.StringReader"%>
<%@page import="org.xml.sax.InputSource"%>
<%@page import="sk.iway.iwcm.Cache"%><%@page import="sk.iway.iwcm.DB"%><%@page import="org.json.JSONArray"%><%@
page import="java.util.ArrayList"%><%@
page import="java.util.HashSet"%><%@
page import="java.util.Set"%><%@
page import="sk.iway.iwcm.SelectionFilter"%><%@
page import="sk.iway.iwcm.Tools"%><%@
page import="java.util.List"%><%@
page import="org.w3c.dom.Element"%><%@
page import="org.w3c.dom.Node"%><%@
page import="org.w3c.dom.NodeList"%><%@
page import="java.io.File"%><%@
page import="org.w3c.dom.Document"%><%@
page import="javax.xml.parsers.DocumentBuilder"%><%@
page import="javax.xml.parsers.DocumentBuilderFactory"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%
String prefix = request.getParameter("prefix");
if (prefix == null)
	prefix = request.getParameter("text");
if(prefix==null)
	return;
final String prefixFinal = DB.internationalToEnglish(prefix.toLowerCase());

Cache c = Cache.getInstance();
String CACHE_KEY = "components.basket.heureka_kategorie_list";

List<String> allKeys = (List<String>)c.getObject(CACHE_KEY);

if (allKeys == null)
{
	Set<String> allKeysSet = new HashSet<String>();
	allKeys = new ArrayList<String>();

	try
	{
		String xmlData = c.downloadUrl("http://www.heureka.sk/direct/xml-export/shops/heureka-sekce.xml", 30*30);

		if(Tools.isNotEmpty(xmlData))
		{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xmlData));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("CATEGORY");

			for(int i=0; i<nList.getLength(); i++)
			{
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element eElement = (Element) nNode;

					if(eElement.getElementsByTagName("CATEGORY_FULLNAME").getLength()>0)
					{
						//out.print(eElement.getElementsByTagName("CATEGORY_FULLNAME").item(0).getTextContent()+"<br/>");
						allKeysSet.add(eElement.getElementsByTagName("CATEGORY_FULLNAME").item(0).getTextContent());
					}
				}
			}
		}
	}
	catch(Exception e){sk.iway.iwcm.Logger.error(e);}


	allKeys.addAll(allKeysSet);

	c.setObjectSeconds(CACHE_KEY, allKeys, 60*30);
}
List<String> keysToSort = Tools.filter(allKeys, new SelectionFilter<String>(){

	public boolean fullfilsConditions(String key)
	{
		return key != null && DB.internationalToEnglish(key.toLowerCase()).contains(prefixFinal);
	}
});

JSONArray keys = new JSONArray(keysToSort);
%><%=keys.toString() %>