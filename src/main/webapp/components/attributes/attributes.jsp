<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.PageParams,sk.iway.iwcm.doc.AtrDB" %><%@ page import="sk.iway.iwcm.doc.DocDetails" %><%@ page import="java.util.List" %>
<%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway"%><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html"%><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><%
PageParams pageParams = new PageParams(request);
String group = pageParams.getValue("group", "default");

DocDetails docDetails = (DocDetails)request.getAttribute("docDetails");
if (docDetails == null) return;
int docId = docDetails.getDocId();

List atrs = AtrDB.getAtributes(docId, group, request);
request.setAttribute("atrs", atrs);
%>
<table class="table table-sm tabulkaStandard atrTable">
<iwcm:iterate name="atrs" id="atr" type="sk.iway.iwcm.doc.AtrBean">
   <tr>
      <td><bean:write name="atr" property="atrName" filter="false"/></td>
      <td><bean:write name="atr" property="valueHtml" filter="false"/></td>
   </tr>
</iwcm:iterate>
</table>