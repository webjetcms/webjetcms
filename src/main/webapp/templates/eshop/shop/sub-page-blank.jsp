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
<html class="no-js pg-homepage" <%@ include file="/components/eshop/shop/includes/html-attributes.jsp" %>>
    <head>
        <%@ include file="/components/eshop/shop/includes/head.jsp" %>
    </head>
    <body>
    <iwcm:insertScript position="body_start"/>
        <div class="ly-page-wrapper">

            <%@ include file="/components/eshop/shop/includes/blind-friendly-panel.jsp" %>


                <header class="ly-header">
                    <%@ include file="/components/eshop/shop/includes/header.jsp" %>
                </header><!--/.ly-header-->

                <main class="ly-content-wrapper">

                    <div class="ly-content">

                        <div class="container">
                            <iwcm:write name="doc_data"/>
                        </div>

                    </div><!--/.ly-content-->

                </main><!--/.ly-content-wrapper-->

                <footer class="ly-footer" id="blindBlock-footer">
                    <%@ include file="/components/eshop/shop/includes/footer.jsp" %>
                </footer><!--/.ly-footer-->

        </div> <!--/.ly-page-wrapper-->
    <iwcm:insertScript position="body_end"/>
    </body>
</html>