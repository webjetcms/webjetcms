<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.PageParams,sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.doc.AtrBean" %>
<%@ page import="sk.iway.iwcm.doc.AtrDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway"%><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><%!

private Map<String, String> getGroupAtrsHtml(List atrs, String htmlName, String htmlId, String htmlClass)
{
   Iterator iter = atrs.iterator();
   AtrBean attr;
   Map<String, String> groupAtrsHtml = new HashMap<String, String>();

   while (iter.hasNext()) {
   	attr = (AtrBean)iter.next();
      if(Tools.isNotEmpty(attr.getValueString())) {
         groupAtrsHtml.put(attr.getAtrName(), attr.getHtml(false, htmlName, htmlId, htmlClass) );
      }
   }

   return groupAtrsHtml;
}

%><%
PageParams pageParams = new PageParams(request);
String htmlName = pageParams.getValue("htmlName", null);
String htmlId = pageParams.getValue("htmlId", null);
String htmlClass = pageParams.getValue("htmlClass", null);

DocDetails docDetails = (DocDetails)request.getAttribute("docDetails");
if (docDetails == null) return;
int docId = docDetails.getDocId();

List<List<AtrBean>> allGroupsAttrs = AtrDB.getAtributes(docId, request);
if(allGroupsAttrs == null || allGroupsAttrs.size() < 1) return;

Map<String, String> allGroupsAttrsHtml = new HashMap<String, String>();
for(List atrs : allGroupsAttrs) {
   allGroupsAttrsHtml.putAll( getGroupAtrsHtml(atrs, htmlName, htmlId, htmlClass) );
}

//Build final div with select's
if(allGroupsAttrsHtml == null || allGroupsAttrsHtml.size() < 1) return;
String htmlElement = "<div class=\"row\">";
for (Map.Entry<String, String> set : allGroupsAttrsHtml.entrySet()) { 
   htmlElement += "<div class=\"col-12 col-md-6\"><div class=\"form-group\"><label for=\"size\">" + set.getKey() + "</label>" + set.getValue() + "</div></div>";
}
htmlElement += "</div>";

//Remove empty options (something must be selected)
htmlElement = htmlElement.replaceAll("<option value=''></option>", "");
htmlElement = htmlElement.replaceAll("<option value=\"\"></option>", "");

if (Tools.isEmpty(htmlElement)) return;
request.setAttribute("atrValue", htmlElement);
%><iwcm:write name="atrValue"/>