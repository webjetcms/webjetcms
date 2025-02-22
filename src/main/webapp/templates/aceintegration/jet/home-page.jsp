<%@ page pageEncoding="windows-1250" trimDirectiveWhitespaces="true" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="ninja" scope="request" type="sk.iway.iwcm.doc.ninja.Ninja"/>
<!doctype html>
<html class="no-js pg-homepage" <%@ include file="includes/html-attributes.jsp" %>>
    <head>
        <%@ include file="includes/head.jsp" %>
    </head>
    <body class="<%= fieldA %>">

		<iwcm:insertScript position="after-body"/>

		<header class="ly-header" data-aos="fade-down" data-aos-duration="600">
			<%@ include file="includes/header.jsp" %>
		</header>

		<%@ include file="includes/browser-support.jsp" %>

		<iwcm:write name="doc_data"/>

		<footer class="ly-footer" id="blindBlock-footer">
			<%@ include file="includes/footer.jsp" %>
		</footer>

    </body>
</html>