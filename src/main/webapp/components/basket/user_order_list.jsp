<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.components.basket.*" %>

<%@page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.components.basket.InvoiceDB"%>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%!
	String statusToString(int statusAsNumber)
	{
		if (statusAsNumber == BasketInvoiceBean.INVOICE_STATUS_CANCELLED)
			return "zrusena";
		if (statusAsNumber == BasketInvoiceBean.INVOICE_STATUS_NEW)
			return "nova";
		if (statusAsNumber == BasketInvoiceBean.INVOICE_STATUS_PAID)
			return "zaplatena";
		
		return "unknown";
	}
%>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	
	PageParams pageParams = new PageParams(request);
	
	List<BasketInvoiceBean> invoices = InvoiceDB.getInvoices(new Identity(new UserDetails(request)).getUserId());
	
	//invoices.get(0).getItemsRel().size()
	request.setAttribute("invoices",invoices);
%>

<display:table uid="invoice" pagesize="50" name="invoices">
	<%BasketInvoiceBean currentInvoice = (BasketInvoiceBean)invoice; %>
	
	<display:column title="Poloziek"> <%=currentInvoice.getBasketItems().size() %></display:column>
	<display:column title="Sposob platby"> <%=currentInvoice.getPaymentMethod() %></display:column>
	<display:column sortable="true" title="Cena s DPH" style="text-align:right;">
		<iway:curr currency="<%=currentInvoice.getCurrency() %>"> <%=currentInvoice.getTotalPriceVat() %></iway:curr>
	</display:column>
	<display:column sortable="true" title="Stav">
		<%=statusToString(currentInvoice.getStatusId()) %>
		<% if(currentInvoice.getStatusId().intValue() == BasketInvoiceBean.INVOICE_STATUS_NEW){ %>
			<a href="/components/basket/order_form.jsp?act=saveorder&paymentMethod=<%=currentInvoice.getPaymentMethod() %>">zaplatit</a> 
		<% }%>
 	</display:column>
</display:table>