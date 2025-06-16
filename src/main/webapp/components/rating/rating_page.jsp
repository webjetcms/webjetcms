<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

%><%

PageParams pageParams = new PageParams(request);
int range = pageParams.getIntValue("range", 10);
int ratingDocId = pageParams.getIntValue("ratingDocId", -1);
if (ratingDocId < 1) ratingDocId = Tools.getDocId(request);

RatingEntity rBean = new RatingEntity();

rBean = RatingService.getDocIdRating(ratingDocId);
request.setAttribute("docIdRating", "true");
%>

<%@page import="sk.iway.iwcm.components.rating.jpa.RatingEntity"%>
<%@page import="sk.iway.iwcm.components.rating.RatingService"%>
<div class="rating">
	<!--  RATING STRANKY -->
	<iwcm:present name="docIdRating">
	     <iwcm:text key="components.rating.hodnotenie" param1='<%=""+rBean.getRatingValueDouble()%>' param2='<%=""+range%>'/><br/>
		 <iwcm:text key="components.rating.hlasovalo" param1='<%=""+rBean.getTotalUsers()%>'/><br/>
	</iwcm:present>
</div>

	<%
		request.removeAttribute("docIdRating");
		rBean = null;
	%>
