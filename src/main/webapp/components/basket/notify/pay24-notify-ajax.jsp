<%@page import="sk.iway.cloud.payments.pay24.Pay24MerchantAccountActionBean"%>
<%@page import="sk.iway.cloud.payments.pay24.Pay24MerchantAccountBean"%>
<%@page import="java.util.Date"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoicePayment"%>
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
<%@page import="org.apache.commons.codec.binary.Hex"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!
public String generateSign(String message, String key, String iv) {
	try {
		Security.addProvider(new BouncyCastleProvider());
		byte[] keyBytes = Hex.decodeHex(key.toCharArray());
		byte[] ivBytes = iv.getBytes();
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
		encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
		byte[] sha1Hash = DigestUtils.sha(message);
		byte[] encryptedData = encryptCipher.doFinal(sha1Hash);
		return Hex.encodeHexString(encryptedData).substring(0, 32);
	} catch (Exception e) {
		System.out.print("ERROR! " + e.getMessage());
		sk.iway.iwcm.Logger.error(e);
		return null;
	}
}

public static String getPspVategory(String params)
{
	int paymentCategory = Tools.getIntValue(StringUtils.substringBetween(params, "<PSPCategory>","</PSPCategory>"), -1);
	if(paymentCategory == 1)
		return "platby kartou";
	else if(paymentCategory == 2)
		return "okamžité platby";
	else if(paymentCategory == 3)
		return "bankové prevody";
	else if(paymentCategory == 4)
		return "ostatné";

	return "0";
}
%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

//ak su nastavene systemove premenne, resp cloudove premenne z eshop modulu tak nastavi tie. Ak ziadne nenajde, nastavi testovacie premenne
Pay24MerchantAccountBean pay24Gateway = new Pay24MerchantAccountActionBean().getAccount();

String mid = pay24Gateway.getMid(); //"demoOMED";
String key = pay24Gateway.getKey(); //"1234567812345678123456781234567812345678123456781234567812345678";
String initVector = mid + new StringBuilder(mid).reverse().toString();

PageParams pageParams = new PageParams(request);
String params = request.getParameter("params");
String emailBody = ResponseUtils.filter(params);
emailBody += "<br><hr>"+"generovane: /components/basket/notify/pay24-notify-ajax.jsp";
int signIndex = params.indexOf("sign=\"");
String msTxnId = "";
String result = "";
String amount = "";
String pspTxnId = "";
boolean valid = false;
try
{
	String sign = params.substring(signIndex+6,params.indexOf("\"",signIndex+8));
	emailBody += "<br>"+sign;

	//
	amount = StringUtils.substringBetween(params, "<Amount>","</Amount>");
	String currency = StringUtils.substringBetween(params, "<Currency>","</Currency>");
	pspTxnId = StringUtils.substringBetween(params, "<PspTxnId>","</PspTxnId>");
	msTxnId = StringUtils.substringBetween(params, "<MsTxnId>","</MsTxnId>");
	String timestamp = StringUtils.substringBetween(params, "<Timestamp>","</Timestamp>");
	result = StringUtils.substringBetween(params, "<Result>","</Result>");

	emailBody += "<br>message = "+mid+amount+currency+pspTxnId+msTxnId+timestamp+result;
	emailBody += "<br>sign counted = "+ generateSign(mid+amount+currency+pspTxnId+msTxnId+timestamp+result, key, initVector).toUpperCase();
	valid = sign.equals(generateSign(mid+amount+currency+pspTxnId+msTxnId+timestamp+result, key, initVector).toUpperCase());
	emailBody += "<br>equals ? "+ valid;
}
catch (Exception exc)
{
	System.out.println("Exception in /components/basket/notify/pay24-notify-ajax.jsp "+exc.getMessage());
	sk.iway.iwcm.Logger.error(exc);
	emailBody += "<br> Exception "+signIndex;
}

BasketInvoiceBean basketInvoiceBean = InvoiceDB.getInvoiceById(Tools.getIntValue(msTxnId, -1));
if(valid && basketInvoiceBean != null)
{
	if("OK".equals(result))
	{
		basketInvoiceBean.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PAID);
		BasketInvoicePayment basketInvoicePayment = new BasketInvoicePayment();
		//basketInvoicePayment.setCreateDate(new Date());
		basketInvoicePayment.setInvoiceId(basketInvoiceBean.getBasketInvoiceId());
		basketInvoicePayment.setPayedPrice(Tools.getBigDecimalValue(amount));
		basketInvoicePayment.setPaymentMethod("PAY24-"+getPspVategory(params)+"-"+pspTxnId);
		basketInvoicePayment.setClosedDate(new Date());
	}
	else if("FAIL".equals(result))
		basketInvoiceBean.setStatusId(BasketInvoiceBean.INVOICE_STATUS_CANCELLED);
	else
		Adminlog.add(Adminlog.TYPE_CLIENT_SPECIFIC, -1, "Pay 24 stav platby pre invoice id = "+result, -1, -1);

	InvoiceDB.saveInvoice(basketInvoiceBean);
}

emailBody += "<br>/components/basket/notify/pay24-notify-ajax.jsp";

//Len pre otestovanie funkcnosti po nasadeni
SendMail.send("24 PAY automat", "noreplay@webjet.sk", "pavol.rau@interway.sk", "24 PAY platba notifikacia ("+Tools.getBaseHref(request)+PathFilter.getOrigPath(request)+")", emailBody+Tools.getIntValue(msTxnId, -1));
%>