<%@page import="java.util.List"%><%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>

<%@page import="sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService"%>
<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	int invoiceId = Tools.getIntValue(request.getParameter("invoiceId"), -1);
	if (invoiceId < 1) return;

	BasketInvoiceEntity invoice = EshopService.getInstance().getInvoiceById(invoiceId);

	pageContext.setAttribute("lng", invoice.getUserLng());
	request.setAttribute("invoice", invoice);
%>

<%@include file="/components/basket/_invoice_security_guard.jsp" %>

<%-- <p><iwcm:text key="components.basket.price_with_DPH_complete"/>: <%=balanceToPay%> </p> --%>

<%
String paymentMethod = invoice.getPaymentMethod();
Prop prop = Prop.getInstance(request);

if ( PaymentMethodsService.isPaymentMethodConfigured(paymentMethod, request, prop) ) {
	String destination = Tools.getStringValue(request.getParameter("destination"), null);

  	//Supported payment method
	String returnUrl;
	if(Tools.isEmpty(destination)) returnUrl = PathFilter.getOrigPath(request);
	else returnUrl = destination;

	returnUrl = Tools.addParametersToUrl(returnUrl, "act=afterpay");
	request.setAttribute("returnUrl", returnUrl);
	request.setAttribute("paymentMethod", paymentMethod);
	request.setAttribute("invoiceId", invoiceId);

  String paymentResponse = PaymentMethodsService.getPaymentResponse(request);

  %>
    <div>
		<%=paymentResponse%>
	</div>
  <%
} else {
  //Not supported payment method
  out.println("NOT SUPPORTED PAYMENT METHOD");
}
%>
