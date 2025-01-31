<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.PageParams,sk.iway.iwcm.Tools,sk.iway.iwcm.doc.*,java.util.List" %>
<%@ page import="java.util.Map" %>
<%@page import="sk.iway.iwcm.PageLng"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%
PageParams pageParams = new PageParams(request);
String htmlName = pageParams.getValue("htmlName", null);
String htmlId = pageParams.getValue("htmlId", null);
String htmlClass = pageParams.getValue("htmlClass", null);
String htmlOnchange = pageParams.getValue("htmlOnchange", null);

String htmlElement = "<select";
if(Tools.isNotEmpty(htmlName)) { htmlElement += " name=\"" + htmlName + "\""; }
if(Tools.isNotEmpty(htmlId)) { htmlElement += " id=\"" + htmlId + "\""; }
if(Tools.isNotEmpty(htmlClass)) { htmlElement += " class=\"" + htmlClass + "\""; }
if(Tools.isNotEmpty(htmlOnchange)) { htmlElement += " onchange=" + htmlOnchange; }
htmlElement += " multiple>";

String language = PageLng.getUserLng(request);
Prop prop = Prop.getInstance(language);
String defaultOption = prop.getText("apps.eshop.shop.display.default");

int groupId = -1;
if (request.getAttribute("docDetails") != null) {
    groupId = ((DocDetails) request.getAttribute("docDetails")).getGroupId();
}

List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(groupId);
//default options
htmlElement += "<option value=\"-1\">" + defaultOption + "</option>";
for (PerexGroupBean perexGroup : perexGroups) {
    htmlElement += "<option value=\"" + perexGroup.getPerexGroupId() + "\">" + perexGroup.getPerexGroupName() + "</option>";
}

htmlElement += "</select>";

request.setAttribute("atrValue", htmlElement);
%><iwcm:write name="atrValue"/>