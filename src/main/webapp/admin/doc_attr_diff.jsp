<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%@page import="sk.iway.iwcm.doc.*,sk.iway.iwcm.i18n.Prop"%>

<%@ include file="/admin/skins/webjet8/layout_top_iframe.jsp" %>
<%
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
int historyId = Tools.getIntValue(Tools.getRequestParameter(request, "historyid"), -1);

DocDB docDB = DocDB.getInstance();
DocDetails docDetails = null;
DocDetails historyDetails = docDB.getDoc(docId, historyId);

if ("true".equals(Tools.getRequestParameter(request, "new"))==false) docDetails = docDB.getDoc(docId);

StringBuilder diff = sk.iway.iwcm.editor.service.ApproveService.getDiff(historyDetails, docDetails, prop);
%>
<div style="padding: 10px; font-family: Arial, Helvetica, sans-serif; font-size: 12px;">
    <%= diff.toString() %>
</div>

<%@ include file="/admin/skins/webjet8/layout_bottom_iframe.jsp" %>
