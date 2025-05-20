<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="sk.iway.iwcm.doc.DocDetails"%>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	
	PageParams pageParams = new PageParams(request);
	DocDetails product = (DocDetails)(request.getAttribute("docDetails"));
%>
 
<script type="text/javascript">
<!--
	function addToBasket(docId)
	{
		var qty = 1;
		var userNote = "";
		if (document.getElementById)
		{
			var qtyEl = document.getElementById("qty_" + docId);
			if (qtyEl != null)
				qty = qtyEl.value;
			var poznEl = document.getElementById("pozn_" + docId);
			if (poznEl != null) 
				userNote = poznEl.value;
		}
		wjPopup("/components/basket/addbasket_popup.jsp?act=add&basketQty=" + qty +"&basketItemId=" + docId + "&basketUserNote=" + userNote, 300, 150);
	}
//-->
</script>
      
<div class="priceDiv">
	<p>&nbsp;</p>
	<p align="right">
		<a href="javascript:addToBasket(<%=product.getDocId()%>)">
			<img title="Pridať do košíka" alt="Pridať do košíka" src="/components/basket/kosik.gif" align="Middle" border="0"/>
		</a> 
		<br />
		<br />
		
		<span class="cena"><iway:curr currency="skk"><%= product.getLocalPriceVat(request,"skk")%></iway:curr></span>
		|
		<span class="cena"><iway:curr currency="eur"><%= product.getLocalPriceVat(request,"eur")%></iway:curr></span>
	</p>
	<input type="hidden" value='1' id="qty_<%=product.getDocId()%>" />
	<input type="hidden" value='' id="pozn_<%=product.getDocId()%>" />
</div>