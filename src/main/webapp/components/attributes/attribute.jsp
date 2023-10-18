<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.PageParams,sk.iway.iwcm.Tools" %><%@ page import="sk.iway.iwcm.doc.AtrBean" %><%@ page import="sk.iway.iwcm.doc.AtrDB" %><%@ page import="sk.iway.iwcm.doc.DocDetails" %><%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway"%><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html"%><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><%!

private String getAtrValue(List atrs, String name)
{
   Iterator iter = atrs.iterator();
   AtrBean atr;
   while (iter.hasNext())
   {
   	atr = (AtrBean)iter.next();
   	if (atr.getAtrName().equals(name))
   	{
   		return(atr.getValueHtml());
   	}
   }
   return("");
}

%><%
PageParams pageParams = new PageParams(request);
String group = pageParams.getValue("group", "default");
String name = pageParams.getValue("name", null);

if (Tools.isEmpty(name)) return;

DocDetails docDetails = (DocDetails)request.getAttribute("docDetails");
if (docDetails == null) return;
int docId = docDetails.getDocId();

List atrs = AtrDB.getAtributes(docId, group, request);

String value = getAtrValue(atrs, name);
if (Tools.isEmpty(value)) return;
request.setAttribute("atrValue", value);
%><iwcm:write name="atrValue"/>