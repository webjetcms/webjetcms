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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

%><%
if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
PageParams pageParams = new PageParams(request);
int range = pageParams.getIntValue("range", 10);
int ratingDocId = pageParams.getIntValue("ratingDocId", -1);
if (ratingDocId < 1) ratingDocId = Tools.getDocId(request);

RatingBean rBean = new RatingBean();

rBean = RatingDB.getDocIdRating(ratingDocId);
request.setAttribute("docIdRating", "true");	
%>

<%@page import="sk.iway.iwcm.components.rating.RatingBean"%>
<%@page import="sk.iway.iwcm.components.rating.RatingDB"%>
<div class="rating">
	<!--  RATING STRANKY -->
	<logic:present name="docIdRating">
	     <iwcm:text key="components.rating.hodnotenie" param1='<%=""+rBean.getRatingValue()%>' param2='<%=""+range%>'/><br/>
		 <iwcm:text key="components.rating.hlasovalo" param1='<%=""+rBean.getTotalUsers()%>'/><br/>
	</logic:present>
</div>		
	
	<%		
		request.removeAttribute("docIdRating");		
		rBean = null;
	%>
