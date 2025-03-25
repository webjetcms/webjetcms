<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ page import="sk.iway.iwcm.components.basket.BasketDB" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
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
	if (doc.getPrice().abs().compareTo(java.math.BigDecimal.valueOf(0)) > 0)
	{
%>

<%
String stockClass = "";

if(Integer.parseInt(doc.getFieldM()) < 1) stockClass = "disabled";
%>

<div class="buy">
	<div class="row">
		<div class="col-sm-5 col-md-12 col-xl-5">
			<p class="price">
				<iway:curr currency="<%= doc.getCurrency() %>"><%=doc.getLocalPriceVat(request, doc.getCurrency() ) %></iway:curr> <small>s DPH</small>
				<br>
				<small><iway:curr currency="<%= doc.getCurrency() %>"><%=doc.getLocalPrice(request, doc.getCurrency() ) %></iway:curr> bez DPH</small>
			</p>
		</div>
		<div class="col-sm-7 col-md-12 col-xl-7">
			<div class="input-group">
				<input type="number" class="form-control qty-input" <%=stockClass%> name="product_count_3" min="1" value="1" id="qty_${doc.docId}">
				<div class="input-group-append">
					<div class="controls">
						<button class="add"><i class="fas fa-plus"></i></button>
						<button class="remove"><i class="fas fa-minus"></i></button>
					</div>
					<button class="btn btn-primary btn-icon addToBasket itemId_${doc.docId} <%=stockClass%>"><i class="fas fa-shopping-cart"></i> <iwcm:text key="components.basket.buy"/></button>
				</div>
			</div>
		</div>
	</div>
</div>
<%
	}
%>