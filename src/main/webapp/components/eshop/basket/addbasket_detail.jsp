<%@page import="java.math.BigDecimal"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<script>
	$(document).ready(function(){
				$('.price').popover();
		$('.price').popover({ trigger: "hover" });
	});
</script>
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
		<script type="text/javascript">
		 	<!--
			function addToBasket(docId)
			{
				//alert('0');
				var qty = 1;
				var userNote = "";

				if (document.getElementById)
				{
					var qtyEl = document.getElementById("qty_"+docId);
					if (qtyEl != null) {
						qty = qtyEl.value;
						qtyEl.value = 1;
					}
					var poznEl = document.getElementById("pozn_"+docId);
					if (poznEl != null) userNote = poznEl.value;
				}

				wjPopup("/components/basket/addbasket_popup.jsp?act=add&basketQty="+qty+"&basketItemId="+docId+"&basketUserNote="+userNote, 300, 150);
			}
		   //-->
		</script>

<p>
	<iwcm:empty name="doc" property="fieldD">
		<iway:curr currency="eur"><%=doc.getLocalPriceVat(request, "eur" ) %></iway:curr>
	</iwcm:empty> <small>vr�tane HPH</small>
	<br>
	<small>986� bez DPH</small>
</p>


<div class="buy">
	<div class="input-group">
		<input type="number" class="form-control" name="product_count_3" value="1" id="qty_${doc.docId}">
		<div class="input-group-append">
			<button class="btn btn-success add_basket cart addToBasket itemId_${doc.docId}"><i class="fas fa-shopping-cart"></i> <iwcm:text key="components.basket.buy"/></button>
		</div>
	</div>
</div>

<%
	}
%>