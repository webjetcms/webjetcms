<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><iwcm:checkLogon admin="true"/>


<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%><html>
<HEAD>
   <TITLE>WebJET</TITLE>
   <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
   <LINK rel='stylesheet' href='<%=request.getContextPath()%>/admin/css/style.css'>
   <LINK rel='stylesheet' href='<%=request.getContextPath()%>/admin/skins/webjet6/css/webjet6.css'>
	<!--[if lt IE 7]>
	    <LINK rel='stylesheet' href='<%=request.getContextPath()%>/admin/skins/webjet6/css/webjet6_ie6.css'>
	<![endif]-->
</HEAD>
<body style="padding-left: 5px; padding-top: 5px;">

<% String key = Tools.getRequestParameter(request, "textKey");
if (Tools.isNotEmpty(key)) {%><strong><iwcm:text key="<%=ResponseUtils.filter(key) %>"/></strong><%} %>
&nbsp;

<%
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
int historyId = Tools.getIntValue(Tools.getRequestParameter(request, "historyid"), -1);

DocDB docDB = DocDB.getInstance();
DocDetails doc = docDB.getDoc(docId, historyId);
if (doc != null)
{
	request.setAttribute("doc", doc);
	%>
	<br/>
	<iwcm:text key="history.changedBy"/>:
    <a href="mailto:<iwcm:strutsWrite name="doc" property="authorEmail"/>"><iwcm:strutsWrite name="doc" property="authorName"/></a>

    <iwcm:text key="editor.date"/>:
    <iwcm:strutsWrite name="doc" property="dateCreatedString"/> <iwcm:strutsWrite name="doc" property="timeCreatedString"/>&nbsp;
	<%
	if (Tools.isNotEmpty(doc.getPublishStartString()))
	{
		if (doc.isPublicable())
		{
			%><br/><img src="/admin/images/warning.gif" align="absmiddle"/> <strong><iwcm:text key="document.start.publication"/> <iwcm:strutsWrite name="doc" property="publishStartString"/> <iwcm:strutsWrite name="doc" property="publishStartTimeString"/></strong><%
		}
		else if (historyId > 0)
		{
			%><br/><iwcm:text key="components.reservation.addReservation.date_from"/> <iwcm:strutsWrite name="doc" property="publishStartString"/> <iwcm:strutsWrite name="doc" property="publishStartTimeString"/><%
		}
	}
	if (Tools.isNotEmpty(doc.getPublishEndString()))
	{
		if (doc.isDisableAfterEnd())
		{
			%><br/><img src="/admin/images/warning.gif" align="absmiddle"/> <strong><iwcm:text key="document.end.publication"/> <iwcm:strutsWrite name="doc" property="publishEndString"/> <iwcm:strutsWrite name="doc" property="publishEndTimeString"/></strong><%
		}
		else if (historyId > 0)
		{
			%><br/><iwcm:text key="components.reservation.addReservation.date_to"/> <iwcm:strutsWrite name="doc" property="publishEndString"/> <iwcm:strutsWrite name="doc" property="publishEndTimeString"/><%
		}
	}
}
%>

</body>
</html>
