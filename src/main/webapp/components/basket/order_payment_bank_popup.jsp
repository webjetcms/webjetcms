<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.ebanking.Payment"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentInformation"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.lang.reflect.Method"%>
<%@page import="sk.iway.iwcm.components.basket.*" %>
<%@page import="java.util.Date" %>

<%
	//ziskaj objednavku
	try
	{
	BasketInvoiceBean invoice = (BasketInvoiceBean)session.getAttribute("invoice");
	if (invoice == null)
	{
		out.println("Chyba pri ziskavani objednavky");
		return;
	}
	//sformatuj jej cenu, za variabilny symbol povazuj id objednavky
	BigDecimal suma = BigDecimal.valueOf(invoice.getTotalPriceVatIn( BasketDB.getDisplayCurrency(request)));

	int depositPercentage = Constants.getInt("basketDepositPercentage");

	if(depositPercentage > 0 && depositPercentage < 100)
	{
		BigDecimal deposit = suma.divide(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(depositPercentage));
		if(!invoice.getStatusId().equals(BasketInvoiceBean.INVOICE_STATUS_DEPOSIT_PAID))
		{
			suma = deposit;
		}
		else
		{
			suma = suma.subtract(deposit);
		}
	}
	else
	{
		//skusi zobrat ciastkovu platbu
		if(session.getAttribute("partialPaymentPrice") != null)
			suma = BigDecimal.valueOf(Tools.getDoubleValue((String)session.getAttribute("partialPaymentPrice"), 0));
	}

	//ulozim do DB info o platbe
	BasketInvoicePayment invoicePayment = new BasketInvoicePayment();
	invoicePayment.setCreateDate(new Date(Tools.getNow()));
	invoicePayment.setInvoiceId(invoice.getBasketInvoiceId());
	invoicePayment.setPayedPrice(suma);
	invoicePayment.setPaymentMethod((String)session.getAttribute("paymentMethod"));
	invoicePayment = BasketInvoicePaymentDB.insertUpdateBasketInvoicePayment(invoicePayment);

	if(invoicePayment == null)
	{
		//pokial sumar so zadanou sumou prekroci cenu objednavky
		%>
		<div class='error' style='color: red; font-weight: bold;'><p><iwcm:text key="components.basket.payment.error.platba_prevysuje_celkovu_sumu"/></p></div>
		<%
		return;
	}

	String vs = ""+invoicePayment.getPaymentId();
	String ss = ""+invoicePayment.getInvoiceId();

	//String rurl = request.getScheme() + "://" + request.getServerName() + "/components/basket/order_payment_bank_reply.jsp?invoiceId="+invoice.getInvoiceId();
	String rurl = request.getScheme() + "://" + Tools.getServerName(request) + "/components/basket/order_payment_bank_reply.jsp";
	request.setAttribute("RURL",rurl);

	//ak uz bola zaplatena, tak nepokracuj dalej
	if (invoice.getStatusId().intValue() == BasketInvoiceBean.INVOICE_STATUS_PAID )
	{
%>
		<iwcm:text key="components.basket.payments.already_payed"/>
<%
		return;
	}


	PaymentInformation paymentInfo = (PaymentInformation)(session.getAttribute("paymentMethodInfo"));
	//ziskaj si udaje, potrebne k vyskladaniu url
	String mid = paymentInfo.getMerchantId();
	String key = paymentInfo.getKey();
	String returnEmail = paymentInfo.getReturnEmail();

	Payment payment = new Payment();
	payment.setAmount(suma);

	payment.setVariableSymbol(vs);
	if(paymentInfo.getConstantSymbol()>0)
		payment.setConstantSymbol(""+paymentInfo.getConstantSymbol());
	else
		payment.setConstantSymbol("0308");
	payment.setSpecificSymbol(ss);
	payment.setDescription("");
	payment.setBuyerName(invoice.getContactFirstName() + " " + invoice.getContactLastName());

	String paymentMethod = Constants.getString("basketSetPaymentMethod");
	if (Tools.isNotEmpty(paymentMethod))
	{
		int i = paymentMethod.lastIndexOf(".");
		String beforePostClass = paymentMethod.substring(0, i);
		paymentMethod = paymentMethod.substring(i+1);
		//String
		try
		{
			Class c = Class.forName(beforePostClass);
			Object o = c.newInstance();
			Method m;
			Class[] parameterTypes = new Class[] {BasketInvoiceBean.class, Payment.class, HttpServletRequest.class};
			Object[] arguments = new Object[] {invoice, payment, request};
			m = c.getMethod(paymentMethod, parameterTypes);
			m.invoke(o, arguments);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}


	if (paymentInfo.hasOwnForm())
		out.print(paymentInfo.generateForm(payment,request));
	else
	{
%>
		<%@page import="sk.iway.iwcm.i18n.Prop"%>

		<form action="<%=paymentInfo.getUrlString() %>" name="cardPayForm" method="get">
			<iway:cardPay mid="<%=mid %>" key="<%=key %>" amount="<%=payment.getAmountString()%>" vs="<%=vs%>" cs="<%=paymentInfo.getConstantSymbol()+"" %>"
				rurl="<%=rurl%>" name="<%=invoice.getContactFirstName() + " " + invoice.getContactLastName()%>" rem="<%=returnEmail %>"/>
		</form>

		<logic:present name="cardPayTagShowForm" scope="page">
			<br /><br /><br />
			<b><iwcm:text key="components.basket.payment.wait"/></b>
			<script type="text/javascript">
			<!--
				document.cardPayForm.submit();
			//-->
			</script>
		</logic:present>
<%
	}
%>

<logic:notPresent name="cardPayTagShowForm" scope="page">
	<br /><br /><br />
	<b><iwcm:text key="components.basket.payment.wait"/></b>
	<script type="text/javascript">
	<!--
		document.payForm.submit();
	//-->
	</script>
</logic:notPresent>

<%
	}//koniec try
	catch (NullPointerException e)
	{
		out.println(Prop.getInstance(request).getText("components.basket.payment.invoiceTimedOut"));
		sk.iway.iwcm.Logger.error(e);
	}
	catch (Exception e)
	{
		out.println(e.getMessage());
		sk.iway.iwcm.Logger.error(e);
	}
%>