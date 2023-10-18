<%@page import="java.util.TreeMap"%>
<%@page import="java.util.Map"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="org.json.JSONObject"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %><%@ 
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ 
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ 
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@ 
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@ 
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><iwcm:checkLogon admin="true" perms="cmp_news"/><%
pageContext.include("/sk/iway/iwcm/components/news/News.action");
%>