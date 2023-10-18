<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%>
<%@page import="sk.iway.iwcm.components.basket.BasketItemBean"%>
<%@page import="sk.iway.iwcm.components.basket.InvoiceDB"%>
<%@page import="sk.iway.iwcm.components.basket.BasketDB"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.ElectronicPayments"%>
<%@page import="sk.iway.tags.CurrencyTag"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentType"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoicePaymentDB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.math.BigDecimal"%>

<script type="text/javascript">
<!--
function checkPrice(inputPrice)
{
	partialPaymentPrice = inputPrice.value;
	totalPrice = document.getElementById("totalPrice").value.split(' ').join('');
	if(partialPaymentPrice != null && partialPaymentPrice != '')
	{
		partialPaymentPriceArray = partialPaymentPrice.replace(',','.').split('.');
		var partialPaymentPrice1 = 0;
		var partialPaymentPrice2 = 0;
		if(partialPaymentPriceArray.length > 0)
			partialPaymentPrice1 = parseInt(partialPaymentPriceArray[0]);
		if(partialPaymentPriceArray.length > 1)
		{
			if(partialPaymentPriceArray[1].length == 1)
				partialPaymentPriceArray[1] += '0';
			partialPaymentPrice2 = parseInt(partialPaymentPriceArray[1]);
		}
		totalPriceArray = totalPrice.replace(',','.').split('.');
		var totalPrice1 = 0;
		var totalPrice2 = 0;
		if(totalPriceArray.length > 0)
			totalPrice1 = parseInt(totalPriceArray[0]);
		if(totalPriceArray.length > 1)
		{
			if(totalPriceArray[1].length == 1)
				totalPriceArray[1] += '0';
			totalPrice2 = parseInt(totalPriceArray[1]);
		}

		if(isNaN(partialPaymentPrice1) || isNaN(partialPaymentPrice2))
		{
			alert('<iwcm:text key="components.baslet.invoices_list.error.zly_format"/>');
			inputPrice.value = totalPrice;
			inputPrice.focus();
			return;
		}
		else
		{
			if(partialPaymentPrice1 == 0 && partialPaymentPrice2 == 0)
			{
				alert('<iwcm:text key="components.baslet.invoices_list.error.nulova_uhrada"/>');
				inputPrice.value = totalPrice;
				inputPrice.focus();
				return;
			}
			else
			{
				if(partialPaymentPrice1 > totalPrice1)
				{
					alert('<iwcm:text key="components.baslet.invoices_list.error.velka_uhrada"/> '+totalPrice);
					inputPrice.value = totalPrice;
					inputPrice.focus();
					return;
				}
				else if(partialPaymentPrice1 == totalPrice1)
				{
					if(partialPaymentPrice2 > totalPrice2)
					{
						alert('<iwcm:text key="components.baslet.invoices_list.error.velka_uhrada"/> '+totalPrice);
						inputPrice.value = totalPrice;
						inputPrice.focus();
						return;
					}
				}
			}
		}
	}
}
//-->
</script>
<%
	//ziskaj invoice
	int invoiceId = Tools.getIntValue(request.getParameter("invoiceId"), -1);
	if (invoiceId < 1)
		return;
	
	BasketInvoiceBean invoice = InvoiceDB.getInvoiceById(invoiceId);
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	request.setAttribute("invoice", invoice);
	
	Prop prop = Prop.getInstance(lng);
	PageParams pageParams = new PageParams(request);
	
	if (invoice == null)
	{
		out.println(prop.getText("components.basket.nastala_chyba_ziskania_objednavky"));
		return;
	}
	
	List<BasketItemBean> basketItems = invoice.getBasketItems();
	if (basketItems.size() > 0)
		request.setAttribute("basketItems", basketItems);
	
	List<String> displayedPaymentMethods = 
		new ArrayList<String>( Arrays.asList(pageParams.getValue("displayedPayments","").split(",")) );
	while (displayedPaymentMethods.contains(""))
		displayedPaymentMethods.remove("");
%>

<h1><%=prop.getText("components.basket.invoice.number_long")+": "+invoiceId%></h1>
<p><strong><iwcm:text key="components.basket.payment.vyberte_sposob_platby"/></strong></p>
<logic:present name="basketItems">  
	<form action="/showdoc.do" method="post">
	   <input type="hidden" name="docid" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("docid"))%>" />
	   <input type="hidden" name="action" value="repayaction" />
	   <input type="hidden" name="invoiceId" value="<%=invoiceId%>"/>
	    <table class="basketOrderTable" border="0">
	      <tr>
	      	<td><iwcm:text key="components.basket.invoices_list.uhradit"/>: </td>
	      	<td>
	      		<%
	      		BigDecimal uhradene = BasketInvoicePaymentDB.getPaymentsSum(invoice.getBasketInvoiceId());
	      		BigDecimal totalPriceVat = new BigDecimal(BasketDB.getTotalLocalPriceVat(basketItems,request));
				totalPriceVat = totalPriceVat.setScale(2,BigDecimal.ROUND_HALF_UP);
	      		BigDecimal doplatit = totalPriceVat.subtract(uhradene);
	      		boolean allowPartialPayments = pageParams.getBooleanValue("allowPartialPayments", false);
	      		if(allowPartialPayments){
		      		%>
		      		<input type="text" name="partialPaymentPrice" onchange="checkPrice(this);" value="<%=CurrencyTag.formatNumber(doplatit)%>"> / 
		      		<%
	      		}else{
	      			%>
		      		<input type="hidden" name="partialPaymentPrice" value="<%=CurrencyTag.formatNumber(doplatit)%>">
		      		<%
	      		}
	      		%>
	      		<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=doplatit%></iway:curr>
	      		<input type="hidden" name="totalPrice" id="totalPrice" value="<%=CurrencyTag.formatNumber(doplatit)%>"/>
	      		
	      	</td>
	      </tr>
			<% if (displayedPaymentMethods.size() > 0){ %>
		  <tr>
	         <td><iwcm:text key="components.basket.invoice_email.payment_method"/></td>
	         <td>
	            <select name="paymentMethod">
				 	<%for (String paymentMethod : displayedPaymentMethods){
				 		//zakazem dobierku
				 		if("cash_on_delivery".equals(paymentMethod))
				 			continue;
				 		if (!ElectronicPayments.getKnownPaymentMethodsToBasketString().contains(paymentMethod) ||
				 			ElectronicPayments.isPaymentMethodConfigured(PaymentType.getPaymentTypeFromBasketString(paymentMethod))){ %>
						<option value="<%=paymentMethod %>"> <iwcm:text key="<%="components.basket.order_form."+paymentMethod%>"/> </option>
					<%}} %>
	         	</select>
	         </td>
	      </tr>
		<%} %>
		 <tr>
			<td colspan="2" align="right"><input type="submit" name="bSubmit" value="<iwcm:text key="components.basket.order_form.pay"/>" /></td>
		</tr>
		</table>
	</form>
</logic:present>