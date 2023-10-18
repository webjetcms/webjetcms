<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%>

<%
	//ziskaj invoice
	BasketInvoiceBean inv = (BasketInvoiceBean)session.getAttribute("invoice");
	
	String lng = PageLng.getUserLng(request);
	if (inv == null )
	{
		out.println("Invoice je null");
		return;
	}
	
	double suma = 0;
	if(session.getAttribute("partialPaymentPrice") != null)
		suma = Tools.getDoubleValue((String)session.getAttribute("partialPaymentPrice"), 0);
	else
		suma = inv.getTotalPriceVat();
%>

<br />
<b><iwcm:text key="components.basket.payment.moneyTransfer.realizeThisWay"/></b>
<br /><br />

<iwcm:text key="components.basket.payment.moneyTransfer.accountNumber"/> <%=request.getAttribute("moneyTransferAccount") %> <br />
<iwcm:text key="components.basket.payment.moneyTransfer.sum"/> <iway:curr><%=suma%></iway:curr> <%=inv.getCurrency()%><br />
<iwcm:text key="components.basket.payment.moneyTransfer.variableSymbol"/><bean:write name="invoice" property="basketInvoiceId"/><br />
<iwcm:text key="components.basket.payment.moneyTransfer.maturityDate"/> <%=Tools.formatDate(inv.getCreateDate())%><br />

<br /><br />
	<%=request.getAttribute("moneyTransferNote") %>
<br /><br />

<p style="text-align: right">
   <input type="button" class="button150 noprint" onclick="window.print();" value="<iwcm:text key="components.basket.payment.print"/>" />
</p>