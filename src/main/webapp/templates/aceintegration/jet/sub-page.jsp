<%@ page pageEncoding="windows-1250" trimDirectiveWhitespaces="true" %>
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
    <body class="<%= fieldA %>">
	
		<iwcm:insertScript position="after-body"/>
	
		<header class="ly-header" data-aos="fade-down" data-aos-duration="600">
			<%@ include file="includes/header.jsp" %>
		</header>
	
		<article class="ly-content">
			<h1><iwcm:write name="doc_title"/></h1>
			<iwcm:notEmpty name="perex_image"><p><img src="<iwcm:write name="perex_image"/>" alt="<iwcm:write name="doc_title"/>" class="w-100" /></p></iwcm:notEmpty>	
			<iwcm:write name="doc_data"/>
		</article>
	
		<footer class="ly-footer" id="blindBlock-footer">
			<%@ include file="includes/footer.jsp" %>
		</footer>

    </body>
</html>