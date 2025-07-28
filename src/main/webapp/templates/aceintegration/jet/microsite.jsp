<%@ page pageEncoding="utf-8" trimDirectiveWhitespaces="true" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="ninja" scope="request" type="sk.iway.iwcm.doc.ninja.Ninja"/>
<!doctype html>
<html class="no-js pg-subpage" <%@ include file="includes/html-attributes.jsp" %>>
    <head>
        <%@ include file="includes/head.jsp" %>
    </head>
    <body class='md-microsite <iwcm:write name="after_body"/>'>

		<iwcm:insertScript position="after-body"/>

		<div class="header container">
			<h1><iwcm:write name="perex_data"/></h1>
		</div>
		<iwcm:write name="doc_data"/>

		<footer class="ly-footer" id="blindBlock-footer">
			<%@ include file="includes/footer.jsp" %>
		</footer>

    </body>
</html>