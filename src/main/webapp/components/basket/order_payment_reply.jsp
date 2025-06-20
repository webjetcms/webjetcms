<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*" %>

<%@page import="sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService"%>
<%@page import="sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState"%>
<%@page import="sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState.PaymentStatus"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.util.Date"%>

<html>
<body>

<%
	PaymentState paymentState = PaymentMethodsService.handlePayment(request);
	PaymentStatus paymentStatus = paymentState.getStatus();

	Long invoiceId = paymentState.getInvoiceId();
	if(invoiceId == null) invoiceId = -1L;

	String paymentId = Tools.getStringValue(paymentState.getPaymentId(), "uknown");

	Date paymentDateTime = paymentState.getPaymentDateTime();
	if(paymentDateTime == null) paymentDateTime = new Date();
	String paymentDateTimeStr = Tools.formatDateTimeSeconds(paymentDateTime);

	if(paymentStatus == PaymentStatus.SUCCESS) {
		//All good
%>
		<div class="alert alert-success">
			<span>Platba prebehla úspešne.</span>
		</div>
		<p>Vašu objednávku ste úspešne zaplatili.</p>
<%
	}
	else if(paymentStatus == PaymentStatus.FAIL) {
		//Payment failed on bank side
%>
		<div class="alert alert-danger">
			<span>Platba sa nepodarila!</span>
		</div>
		<p>Váš pokus o zaplatenie objednávky sa nepodaril.</p>
<%
	}
	else if(paymentStatus == PaymentStatus.ERROR) {
		//Some error on our side
%>
		<div class="alert alert-danger">
			<span>Platbu sa nepodarilo spracovať !</span>
		</div>
		<p>Vašu platbu sme z technických dôvodov nedokázali spracovať.</p>
		<p>Kontaktujte prosím správcu.</p>
<%
	}
%>

	<span>
		<iwcm:text key="components.basket.invoices_list.cislo_objednavky"/>: <span><%=invoiceId%></span>
	</span><br>
	<span>
		<iwcm:text key="components.basket.real_payment_id"/>: <span><%=paymentId%></span>
	</span><br>
	<span>
		<iwcm:text key="components.basket.invoice.date_created"/>: <span><%=paymentDateTimeStr%></span>
	</span><br>

</body>
</html>