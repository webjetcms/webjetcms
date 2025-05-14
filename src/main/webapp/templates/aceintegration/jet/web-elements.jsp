<jsp:useBean id="ninja" scope="request" type="sk.iway.iwcm.doc.ninja.Ninja"/>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
page pageEncoding="windows-1250" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html class="no-js pg-homepage" <%@ include file="includes/html-attributes.jsp" %>>
    <head>
        <%@ include file="includes/head.jsp" %>
    </head>
    <body>
        <div class="ly-page-wrapper">

            <div class="container-fluid">

                <main class="ly-content-wrapper">

                    <div class="ly-content">

                        <article class="ly-article" id="blindBlock-article">
                            
                            <h1><iwcm:write name="doc_title"/></h1>
                            <iwcm:write name="doc_data"/>
                            <div class="bootstrap-example-wrapper">
                                <jsp:include page="includes/bootstrap-elements.jsp" />
                                <jsp:include page="includes/bootstrap-elements-2.jsp" />
                            </div>
                        
                        </article><!--/.ly-article-->
                    
                    </div><!--/.ly-content-->

                </main><!--/.ly-content-wrapper-->

            </div><!--/.container-->

        </div> <!--/.ly-page-wrapper-->

    </body>
</html>