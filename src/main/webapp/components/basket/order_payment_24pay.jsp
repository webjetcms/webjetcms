<%@page import="sk.iway.cloud.payments.pay24.Pay24MerchantAccountActionBean"%>
<%@page import="sk.iway.cloud.payments.pay24.Pay24MerchantAccountBean"%>
<%@page import="sk.iway.iwcm.components.basket.InvoiceDB"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Calendar"%>
<%@page import="org.apache.commons.codec.digest.DigestUtils"%>
<%@page import="javax.crypto.Cipher"%>
<%@page import="javax.crypto.spec.IvParameterSpec"%>
<%@page import="javax.crypto.spec.SecretKeySpec"%>
<%@page import="java.security.Security"%>
<%@page import="org.bouncycastle.jce.provider.BouncyCastleProvider"%>
<%@page import="org.apache.commons.codec.binary.Hex"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!

//public form vyplna a odosiela user
%><%
if(false) 
{	%>
	<form method="post" name="pay24Form" action="/components/basket/order_payment_24pay.jsp" ><br>
		<input type="text" name="FirstName" value="<%=Tools.getParameter(request, "deliveryName") %>" placeholder="<iwcm:text key="user.Name"/>"><br>
		<input type="text" name="FamilyName"  placeholder="<iwcm:text key="reguser.lastname"/>"><br>
		<input type="text" name="Email"  value="<%=Tools.getParameter(request, "contactEmail") %>" placeholder="<iwcm:text key="gallery.card.from_email"/>" ><br>
		<input type="text" name="Street" value="<%=Tools.getParameter(request, "deliveryStreet") %>" placeholder="<iwcm:text key="components.basket.invoice_email.street"/>"><br>
		<input type="text" name="Zip" value="<%=Tools.getParameter(request, "deliveryZip") %>" placeholder="<iwcm:text key="components.contact.property.zip"/>"><br>
		<input type="text" name="City" placeholder="<iwcm:text key="components.contact.property.city"/>"><br>
		<select name="Country">
			<option value="SVK"><iwcm:text key="stat.countries.tld.sk"/></option>
			<option value="CZE" ><iwcm:text key="stat.countries.tld.cz"/></option>
			<option value="POL"><iwcm:text key="stat.countries.tld.pl"/></option>
		</select><br>
		<input type="submit" name="sended" value="<iwcm:text key="components.sms.send"/>">
	</form>
	<%
}
//ak nebol odoslany public form nepokracujeme
else
{
	//samo detekuje testovaciu/ live verziu
	Pay24MerchantAccountBean pay24Merchant = new Pay24MerchantAccountActionBean().getAccount(); 
	
	String key = pay24Merchant.getKey();
	String mid = pay24Merchant.getMid();
	String eshopId = pay24Merchant.getEshopId();
	String payUrl = null;
	
	if(new Pay24MerchantAccountActionBean().getAccount(true).getEshopId().equals(eshopId))
	{
		//testovacie data
		payUrl = Pay24MerchantAccountBean.getTestUrl();//"https://doxxsl-staging.24-pay.eu/pay_gate/paygt";
	}
	else
	{
		//produkcne
		payUrl = Pay24MerchantAccountBean.getLiveUrl();//"https://admin.24-pay.eu/pay_gate/paygt";
	}
	
	String initVector = mid + new StringBuilder(mid).reverse().toString();
	BasketInvoiceBean invoice = (BasketInvoiceBean)session.getAttribute("invoice");
	
	String amount = Tools.replace(String.format("%.2f", invoice.getTotalPriceVat()), ",", ".") ; // "10.50";
	String firstName = Tools.getParameter(request, "deliveryName");//"Test";
	String familyName = Tools.getParameter(request, "deliverySurName");//   "Payment";
	String currAlphaCode = "EUR";
	int msTxnId = invoice.getBasketInvoiceId();
	Calendar cal = Calendar.getInstance();
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	dateFormat.setTimeZone(cal.getTimeZone());
	String timeStamp = dateFormat.format(cal.getTime());
	String clientId = "100";
	
	String message = mid + amount + currAlphaCode + msTxnId + firstName + familyName + timeStamp;
	
	//private form vyplna a odosiela server
	%>
	<form method="post" name="pay24FormPrivate" action="<%=payUrl%>" >
		<input type="hidden" name="Mid" value="<%=mid%>">
		<input type="hidden" name="EshopId" value="<%=eshopId%>">
		<input type="hidden" name="PreAuthProvided" value="false">
		<input type="hidden" name="MsTxnId" value="<%=msTxnId%>">
		<input type="hidden" name="Amount" value="<%=amount%>">
		<input type="hidden" name="CurrAlphaCode" value="<%=currAlphaCode%>">
		<input type="hidden" name="ClientId" value="<%=clientId%>">
		<input type="hidden" name="FirstName" value="<%=firstName%>">
		<input type="hidden" name="FamilyName" value="<%=familyName%>">
		<input type="hidden" name="Email" value="<%=Tools.getParameter(request, "contactEmail") %>">
		<input type="hidden" name="Street" value="<%=Tools.getParameter(request, "deliveryStreet") %>">
		<input type="hidden" name="Zip" value="<%=Tools.getParameter(request, "deliveryZip") %>">
		<input type="hidden" name="City" value="<%=Tools.getParameter(request, "deliveryCity") %>">
		<input type="hidden" name="Country" value="<%=Tools.getParameter(request, "deliveryCountry") %>">
		<input type="hidden" name="Timestamp" value="<%=timeStamp%>"><!-- 2016-11-16 12:37:13 -->
		<input type="hidden" name="Sign" value="<%=Pay24MerchantAccountBean.generateSign(message, key, initVector)%>">
		<input type="hidden" name="NURL" value="<%=Tools.getBaseHref(request)%>/components/basket/notify/pay24-notify-ajax.jsp">
		<input type="hidden" name="RURL" value="<%=Tools.getBaseHref(request)+PathFilter.getOrigPath(request) %>">
		
		
		<!-- PARAMETER DEBUG POUZIVAJTE LEN PRI TESTOVANI -->
		<%if(new Pay24MerchantAccountActionBean().getAccount(true).getEshopId().equals(eshopId)) {%>
			<input type="hidden" name="Debug" value="true">
		<%} %>
		<input style="display:none;" type="submit" name="Odoslat" value="true">
	</form>
	<script type="text/javascript">
		<!--
		document.pay24FormPrivate.submit();
		-->
	</script>
	<%
	if(invoice != null)
	{
		invoice.setFieldA(Pay24MerchantAccountBean.generateSign(message, key, initVector));
		session.setAttribute("invoice", invoice);
	}
}
%>