<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);

	DocDetails doc = (DocDetails)request.getAttribute("docDetails");
	if (doc == null) return;

	pageContext.setAttribute("doc", doc);
%>

<%-- NASLEDUJUCE ZOBRAZENIE CENY A KOSIKA MA VYZNAM IBA AK MA PRODUKT NENULOVU CENU --%>
<%
	boolean katalog = Tools.getBooleanValue(String.valueOf(session.getAttribute("katalogProduktov")), false);
	if (doc.getPrice().abs().compareTo(java.math.BigDecimal.valueOf(0)) > 0 && !katalog)
	{
%>
		<style type="text/css" media="all">@import url("/components/basket/css/basket.css");</style>
		<span class="priceSpan">
  			<a class="btn btn-primary addToBasket itemId_${doc.docId}" href="javascript:void(0)"><span class="glyphicons glyphicons-shopping-cart"></span> <iwcm:text key="components.basket.buy"/> </a>
  	  		<span class="cena"><iway:curr currency="<%= doc.getCurrency() %>"><%=doc.getLocalPriceVat(request, doc.getCurrency() ) %></iway:curr></span>
          <span class="cenaOld"><iway:curr currency="<%= doc.getCurrency() %>"><%= doc.getFieldM() %></iway:curr></span>
		</span>
<%
	}
%>