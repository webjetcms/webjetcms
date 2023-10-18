<%@page import="sk.iway.iwcm.components.basket.InvoiceDB"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentType"%>
<%@page import="sk.iway.cloud.payments.paypal.PayPalNvpInterface"%>
<%@page import="java.util.Date"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoicePaymentDB"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoicePayment"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%>
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

PageParams pageParams = new PageParams(request);

if(request.getAttribute("initTransaction")!= null)
{
	BasketInvoiceBean invoice = (BasketInvoiceBean)request.getAttribute("invoice");
	String originalPath = (String)request.getAttribute("returnUrl");
	String returnUrl = "http://"+ sk.iway.iwcm.common.CloudToolsForCore.getDomainName()+ originalPath + "?basketAct="+PaymentType.PAYPAL.toBasketString();
	String cancelUrl = returnUrl;

	%>
	<iwcm:text key="components.basket.payment.init"/>
	<%

	String token = PayPalNvpInterface.setExpressCheckout(invoice, returnUrl, cancelUrl);
	if(Tools.isEmpty(token))
	{
		%>
		<iwcm:text key="components.basket.paypal.token.fail"/>
		<%
		return;
	}

	session.setAttribute("paypal-token", token);
	session.setAttribute("invoice-"+token, invoice);

	//then redir here:
	%>
	<script type="text/javascript">
	window.location.href = "https://www.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=<%=token%>";
	</script>
	<%
	return;
}

if(request.getParameter("token")!= null)//na zaklade coho poznam ze to ide naozaj z paypalu? asi nijako
{
	String token = request.getParameter("token");
	if(Tools.isEmpty(token))
	{
		%>
		<iwcm:text key="components.basket.paypal.token.empty"/>
		<%
	}
	//schvalil transakciu
	if(request.getParameter("PayerID")!= null)
	{
		String payerId = request.getParameter("PayerID");
		BasketInvoiceBean invoice = (BasketInvoiceBean)session.getAttribute("invoice-"+token);
		boolean transok = PayPalNvpInterface.doExpressCheckoutPayment(invoice, token, payerId);

		if(transok)
		{
			//objednavku dame ako zaplatenu
			//ulozim do DB info o platbe
			BasketInvoicePayment invoicePayment = new BasketInvoicePayment();
			invoicePayment.setCreateDate(new Date(Tools.getNow()));
			invoicePayment.setInvoiceId(invoice.getBasketInvoiceId());
			invoicePayment.setPayedPrice(new BigDecimal(String.valueOf(invoice.getTotalPriceVat())));
			invoicePayment.setPaymentMethod("paypal");
			invoicePayment.setConfirmed(Boolean.TRUE);
			invoicePayment.setClosedDate(new Date(Tools.getNow()));
			invoicePayment = BasketInvoicePaymentDB.insertUpdateBasketInvoicePayment(invoicePayment);

			BasketInvoiceBean invoiceReloaded = InvoiceDB.getInvoiceById(invoice.getBasketInvoiceId());
			BigDecimal uhradene = BasketInvoicePaymentDB.getPaymentsSum(invoiceReloaded.getBasketInvoiceId());
			BigDecimal totalPriceVat = BigDecimal.valueOf(invoiceReloaded.getTotalPriceVat());
			BigDecimal doplatit = totalPriceVat.subtract(uhradene);

			if (doplatit.doubleValue() <= 0)
			{
				//stav zaplatena
				invoiceReloaded.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PAID);
				InvoiceDB.saveInvoice(invoiceReloaded);
			}

			%>
			<h1><iwcm:text key="components.basket.paypal.transaction.ok"/></h1>
			<%

		}
		else
		{
			%>
			<h1><iwcm:text key="components.basket.paypal.transaction.fail"/></h1>
			<%
		}
	}
	else
	{
		//user odmietol transakciu, what now? zatial asi nic..ked nechce platit tak asi nic nedostane :P
		%>
		<iwcm:text key="components.basket.paypal.transaction.refused"/>
		<%
	}

	//zrusime celu transakciu
	session.removeAttribute("paypal-token");
	session.removeAttribute("invoice-"+token);
}
%>