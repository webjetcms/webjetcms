<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.components.basket.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentInformation"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.InvalidBankResponceException"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.InvalidSignatureException"%>
<%@page import="java.lang.reflect.Method"%>
<%@page import="java.math.BigDecimal"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="sk.iway.tags.CurrencyTag"%>
<%@page import="java.util.Date"%>

<html>
<head>
	<title><iwcm:write name="doc_title"/></title>

	<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />
	<meta http-equiv="Content-language" content="<%=PageLng.getUserLng(request)%>" />

	<meta name="description" content="WebJET Content Management web site" />
	<meta name="author" content="Interway, s.r.o." />

	<link rel="stylesheet" href="/css/page.css" type="text/css" />
	<logic:notEmpty name="css_link"><link rel="stylesheet" href="<iwcm:write name="css_link"/>" type="text/css" /></logic:notEmpty>

	<script type="text/javascript" src="/jscripts/common.js"></script>

	<iwcm:write name="group_htmlhead_recursive"/>
	<iwcm:write name="html_head"/>
</head>
<body bgcolor="#ffffff" leftmargin="5" topmargin="5" marginwidth="0" marginheight="0">

<div style="text-align: center;margin-top: 30px;">
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PaymentInformation paymentInfo = (PaymentInformation)(session.getAttribute("paymentMethodInfo"));

	String vs = paymentInfo.getResponceVS(request);
	String ss = paymentInfo.getResponceSS(request);
	BasketInvoicePayment invoicePayment = BasketInvoicePaymentDB.getBasketInvoicePaymentById(Tools.getIntValue(vs, -1));
%>
<%-- -------------------------REAKCIA NA ODPOVED--------------------------%>
<%
try
{
	boolean paymentSuccess = paymentInfo.validateBankResponce(request);
	if(paymentSuccess)
	{
		//ok, zmenime stav objednavky
		/*
		String vs = paymentInfo.getResponceVS(request);
		//overime ci je VS cislo
		vs = new BigInteger(vs).toString();
		*/
		int invoiceId = Tools.getIntValue(ss, -1);
		BasketInvoiceBean invoice = InvoiceDB.getInvoiceById(invoiceId);
		boolean saveOK = false;
		//nastavime stav na zaplatena a ulozime
		if (invoice != null && invoicePayment != null)
		{
			//sparujem platnu (ak uz nieje sparovana)
			if(invoicePayment.getClosedDate() == null && invoicePayment.getConfirmed() == null)
			{
				invoicePayment.setClosedDate(new Date(Tools.getNow()));
				invoicePayment.setConfirmed(true);
				invoicePayment = BasketInvoicePaymentDB.insertUpdateBasketInvoicePayment(invoicePayment);
			}
			else
			{
				throw new Exception("jointFailed");
			}

			int depositPercentage = Constants.getInt("basketDepositPercentage");
			if(depositPercentage>0 && depositPercentage<100 && !invoice.getStatusId().equals(BasketInvoiceBean.INVOICE_STATUS_DEPOSIT_PAID))
			{
				invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_DEPOSIT_PAID);
			}
			else
			{
				BigDecimal invoicePaymentPrice = BasketInvoicePaymentDB.getPaymentsSum(invoiceId);
				//ak je po uspesnej platbe zaplatena cela suma objednavky, nastavim status na zaplatena
				if(invoicePaymentPrice != null && CurrencyTag.formatNumber(invoice.getTotalPriceVat()).equals(CurrencyTag.formatNumber(invoicePaymentPrice)))
					invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PAID);
				else if(invoicePaymentPrice != null && invoicePaymentPrice != BigDecimal.ZERO)
					invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PARTIALLY_PAID);
			}

			// reflection
			String handleReplyMethod = Constants.getString("basketHandleReplyMethod");
			if (Tools.isNotEmpty(handleReplyMethod))
			{
				int i = handleReplyMethod.lastIndexOf(".");
				String beforePostClass = handleReplyMethod.substring(0, i);
				handleReplyMethod = handleReplyMethod.substring(i+1);
				//String
				try
				{
					Class c = Class.forName(beforePostClass);
					Object o = c.newInstance();
					Method m;
					Class[] parameterTypes = new Class[] {BasketInvoiceBean.class, HttpServletRequest.class};
					Object[] arguments = new Object[] {invoice, request};
					m = c.getMethod(handleReplyMethod, parameterTypes);
					m.invoke(o, arguments);
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}

			saveOK = InvoiceDB.saveInvoice(invoice);
		}

		if (saveOK)
		{
			String returnEmail = paymentInfo.getReturnEmail();
			if (Tools.isEmail(returnEmail) == false) returnEmail = (String)session.getAttribute("returnEmail");
			if (Tools.isEmail(returnEmail))
			{
				boolean sendOK = InvoiceDB.sendInvoiceEmail(request, invoice.getBasketInvoiceId(), invoice.getContactEmail(), returnEmail, "Nová platba v objednávke číslo: " + invoice.getBasketInvoiceId());
			}
		%>
		  <br /><br />
		  <h1><iwcm:text key="components.basket.payment.replyAcknowledged"/></h1>
		  <br /><br /><br />
		  <input type="button" class="hhButton" value="Zatvoriť okno" onclick="window.close();" />
		<%
		}
		else
		{%>
		   <br /><br />
		   <h1>Nepodarilo sa ulozit objednavku s id: <%=invoiceId%>, ale bola uspesne zaplatena. Prosim kontaktujte nas.</h1>
		<%
		}
	}
	else
	{
		//nastavim sparovanie za neuspesne (ak uz nieje sparovana)
		if(invoicePayment != null && invoicePayment.getClosedDate() == null && invoicePayment.getConfirmed() == null)
		{
			invoicePayment.setClosedDate(new Date(Tools.getNow()));
			invoicePayment.setConfirmed(false);
			invoicePayment = BasketInvoicePaymentDB.insertUpdateBasketInvoicePayment(invoicePayment);
		}
		else
		{
			throw new Exception("jointFailed");
		}
		%>
		<br /><br />
		<br /><br />
		<h1><iwcm:text key="components.basket.payment.replyNotAcknowledged" /></h1>
		<br /><br />
		<%
	}
}
catch(InvalidBankResponceException e)
{
%>
	<br /><br />
	<br /><br />
	<h1><iwcm:text key="components.basket.payment.replyInvalidResponce" /></h1>
	<br /><br />
<%
}
catch(InvalidSignatureException e)
{
%>
	<br /><br />
	<br /><br />
	<h1><iwcm:text key="components.basket.payment.replyInvalidSignature" /></h1>
	<br /><br />
<%
}
catch (Exception e)
{
	if("jointFailed".equals(e.getMessage()))
	{
		System.out.println("ERROR: Neuspesne sparovanie platby > ks= "+vs+"; ss= "+ss);

		//pokus o opatovne sparovanie platby
		%>
		<br /><br />
		<br /><br />
		<h1><iwcm:text key="components.basket.payment.jointFailed" /></h1>
		<br /><br />
		<%
	}
	else
	{
		sk.iway.iwcm.Logger.error(e);
	}
}

%>
</div>
</body>
</html>