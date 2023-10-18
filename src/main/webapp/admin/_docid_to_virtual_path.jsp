<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="org.json.JSONObject"%>

<%@page import="org.json.JSONStringer"%>
<%@page import="org.json.JSONWriter"%><iwcm:checkLogon admin="true" />
<%
	int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
	String virtualPath = DocDB.getInstance().getDocLink(docId);
	out.println(new JSONObject().put("virtualPath", virtualPath));
%>
