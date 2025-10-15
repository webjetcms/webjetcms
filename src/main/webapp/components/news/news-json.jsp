<%@page import="org.json.JSONException"%><%@page import="java.io.IOException"%><%@page import="org.json.JSONObject"%><%@page import="sk.iway.iwcm.doc.DocDetails"%><%@page import="java.util.List"%><%@page import="org.json.JSONArray"%><%@page import="sk.iway.iwcm.doc.DocDB"%><%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");response.setHeader("Access-Control-Allow-Origin","*");%><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!
JspWriter out;
String callback = null;
%><%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
PageParams pageParams = new PageParams(request);

String groupIds = pageParams.getValue("groupIds", "");
groupIds = groupIds.replace('+', ',');
boolean expandGroupIds = pageParams.getBooleanValue("expandGroupIds", false);
String perexGroup = pageParams.getValue("perexGroup", "");
perexGroup = perexGroup.replace('+', ',');
int orderType = DocDB.ORDER_PRIORITY;
String p_order = pageParams.getValue("orderType", "priority");
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
boolean ascending = pageParams.getBooleanValue("asc", true);
int publishType = DocDB.PUBLISH_NEW;
String p_publish = pageParams.getValue("publishType", "new");
if (p_publish!=null){
	if (p_publish.compareToIgnoreCase("new")==0)
	{
		publishType = DocDB.PUBLISH_NEW;
	}
	else if (p_publish.compareToIgnoreCase("old")==0)
	{
		publishType = DocDB.PUBLISH_OLD;
	}
	else if (p_publish.compareToIgnoreCase("all")==0)
	{
		publishType = DocDB.PUBLISH_ALL;
	}
	else if (p_publish.compareToIgnoreCase("next")==0)
	{
		publishType = DocDB.PUBLISH_NEXT;
	}
}
int truncate = pageParams.getIntValue("truncate", -1);
boolean noPerexCheck = pageParams.getBooleanValue("noPerexCheck", false);
if (noPerexCheck && publishType < 100)
{
	publishType = publishType + 100;
}
boolean paging = pageParams.getBooleanValue("paging", false);
int pageSize = pageParams.getIntValue("pageSize", 10);
int maxCols = pageParams.getIntValue("cols", 1);
// image moznosti - none, top, bottom
String image = pageParams.getValue("image", "none");
String pagingStyle = pageParams.getValue("pagingStyle", "top");
String newsName = pageParams.getValue("name", "");
String newsStyle = pageParams.getValue("style", "");
boolean perex = pageParams.getBooleanValue("perex", false);
boolean date = pageParams.getBooleanValue("date", false);
boolean place = pageParams.getBooleanValue("place", false);
int imgThumbWidth = pageParams.getIntValue("imgThumbWidth", 60);
int imgThumbHeight = pageParams.getIntValue("imgThumbHeight", 100);
String debugPort = "";
//String debugPort = ":8080";
//ziskaj DocDB
DocDB docDB = DocDB.getInstance();
int actualDocId = -1;
try
{
	actualDocId = Integer.parseInt(Tools.getRequestParameter(request, "docid"));
}
catch (Exception ex)
{
	//sk.iway.iwcm.Logger.error(ex);
}
//vyradime zo zobrazenia aktualnu stranku
String whereSql = " AND doc_id NOT IN ("+actualDocId+") ";
if (perexGroup != null && perexGroup.length()>0)
{
	request.setAttribute("perexGroup", perexGroup);
}
else
{
	request.removeAttribute("perexGroup");
}
request.setAttribute("whereSql", whereSql);

docDB.getDocPerex(groupIds, orderType, ascending, publishType, pageSize, "novinky", "pages", request);
List<DocDetails> novinky = null;
if(request.getAttribute("novinky") != null){
	novinky = (List<DocDetails>)request.getAttribute("novinky");
}
request.removeAttribute("novinky");
JSONArray jsonNovinky = new JSONArray();
if(novinky != null && novinky.size() > 0)
{
	//Gson gson = new Gson();
	JSONObject jsonNovinka=null;

	for(DocDetails novinka : novinky)
	{
		novinka.setData(JsonTools.prepare4Json(novinka.getData(), request));
		//jsonNovinka = new JSONObject(gson.toJson(novinka));
		jsonNovinka = new JSONObject();
		jsonNovinka.put("title", novinka.getTitle());
		jsonNovinka.put("perexImage", Tools.isNotEmpty(novinka.getPerexImageSmall())?"http://"+Tools.getServerName(request)+debugPort+novinka.getPerexImageSmall():"");
		jsonNovinka.put("htmlData", novinka.getHtmlData());
		jsonNovinka.put("data", novinka.getData());
		jsonNovinka.put("docid",novinka.getDocId());
		jsonNovinka.put("dateCreated", novinka.getDateCreated());
		jsonNovinky.put(jsonNovinka);
	}
}

this.out = out;
this.callback = Tools.getRequestParameter(request, "callback");

Tools.printJSON(out, jsonNovinky, callback);
%>
