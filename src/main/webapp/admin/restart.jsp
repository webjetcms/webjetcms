<%@ page import="java.io.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="modUpdate"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>

<%
out.println("restarting");
%>
<br>done...