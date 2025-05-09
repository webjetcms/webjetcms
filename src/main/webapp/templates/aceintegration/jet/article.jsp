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
<html class="no-js pg-subpage" <%@ include file="includes/html-attributes.jsp" %>>
    <head>
        <%@ include file="includes/head.jsp" %>
    </head>
    <body>

		<iwcm:insertScript position="after-body"/>

		<header class="ly-header" data-aos="fade-down" data-aos-duration="600">
			<%@ include file="includes/header.jsp" %>
		</header>

		<article class="ly-content">
			<div class="container">
				<h1><iwcm:write name="doc_title"/></h1>
				<iwcm:notEmpty name="perex_image">

					<iwcm:empty name="field_q">
						<div class="row banner">
							<div class="col-md-8"><img src="/thumb<iwcm:write name="perex_image"/>?w=730&h=400&ip=5" alt="<iwcm:write name="doc_title"/>" /></div>
							<div class="col-md-4">
								<iwcm:write>!INCLUDE(/components/banner/banner.jsp, group=&quot;terminovany-vklad&quot;, status=enabled, displayMode=2, refreshRate=0, bannerIndex=&quot;&quot;, showInIframe=false, iframeWidth=, iframeHeight=)!</iwcm:write>
							</div>
						</div>
					</iwcm:empty>
					<iwcm:notEmpty name="field_q">
						<div class="row banner">
							<div class="col-md-12"><img src="/thumb<iwcm:write name="perex_image"/>?w=1110&h=400&ip=5" alt="<iwcm:write name="doc_title"/>" /></div>
						</div>
					</iwcm:notEmpty>

				</iwcm:notEmpty>
				<iwcm:write name="doc_data"/>
				<iwcm:write name="template_object_a"/>
			</div>
		</article>

		<footer class="ly-footer" id="blindBlock-footer">
			<%@ include file="includes/footer.jsp" %>
		</footer>

    </body>
</html>