<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Tools, sk.iway.iwcm.doc.AtrBean, sk.iway.iwcm.doc.AtrDB" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%@page import="java.util.List"%><%
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docId"), -1);
//String group = Tools.getStringValue(Tools.getRequestParameter(request, "group"), "");
List<AtrBean> attrs = AtrDB.getAtributes(docId, null, request);
String name = "";
%>
$(document).ready(function(){
	<% for(AtrBean atr: attrs){
		name = "atr_"+atr.getAtrId();%>
		$('[name="<%=name%>"]').val('<%=atr.getValueString()%>');
	<%}%>
});
