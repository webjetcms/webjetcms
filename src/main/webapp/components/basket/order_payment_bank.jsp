<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.components.basket.*, java.util.*,java.text.*,sk.iway.iwcm.ebanking.epayments.ElectronicPayments" %>

<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentInformation"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentType"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
	try
	{
		PageParams pageParams = new PageParams(request);
		String paymentMethod = (String) (request.getAttribute("paymentMethod"));
		session.setAttribute("paymentMethod",paymentMethod);
		PaymentInformation paymentMethodInfo = ElectronicPayments.getPaymentInformation(PaymentType.getPaymentTypeFromBasketString(paymentMethod));
		session.setAttribute("paymentMethodInfo",paymentMethodInfo);

		//ziskaj invoice
		BasketInvoiceBean inv = (BasketInvoiceBean)session.getAttribute("invoice");
		String lng = PageLng.getUserLng(request);

		if (inv == null)
		{
			out.println("Nepodarilo sa ziskat invoice");
			return;
		}

		pageContext.setAttribute("lng", lng);

		String url = "/components/basket/order_payment_bank_popup.jsp?invoiceId="+inv.getBasketInvoiceId();
%>

<script type="text/javascript">
<!--
	//otvorenie popup okna
	window.open('<%=url%>',null,"scrollbars=1,resizable=1,location=0,menubar=0,status=1,toolbar=0",true);
//-->
</script>

<br /><br />

<iwcm:text key="components.basket.payment.popupShouldAppear" />

<a href='<%=url%>' target='_blank'>
	<iwcm:text key="components.basket.payment.popupNotAppearedLink"/>
</a>.

<%
	}
	catch(Exception e)
	{
		out.print("<span class=\"error\">"+e.getMessage()+" </span>");
		sk.iway.iwcm.Logger.error(e);
	}
%>