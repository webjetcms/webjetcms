<%@page import="java.util.Calendar"%><%@
page import="sk.iway.iwcm.users.UsersDB"%><%@
page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%><%@
page import="sk.iway.iwcm.components.basket.InvoiceDB"%><%@
page import="org.json.JSONArray"%><%@
page import="java.util.TimeZone"%><%@
page import="java.util.Date"%><%@
page import="java.util.Locale"%><%@
page import="java.text.SimpleDateFormat"%><%@
page import="org.json.JSONObject"%><%
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

String dataString = request.getParameter("data");//"{\"id\":\"PAY-1DM67566GW755603SLHPTYVI\",\"intent\":\"sale\",\"state\":\"approved\",\"cart\":\"4KV22900KA5502045\",\"payer\":{\"payment_method\":\"paypal\",\"status\":\"UNVERIFIED\",\"payer_info\":{\"email\":\"testeringinsurance@gmail.com\",\"first_name\":\"Pavol\",\"last_name\":\"Rau\",\"payer_id\":\"FLJR6AARJCCUQ\",\"shipping_address\":{\"recipient_name\":\"Pavol Rau\"},\"phone\":\"0904514679\",\"country_code\":\"SK\"}},\"transactions\":[{\"amount\":{\"total\":\"1.00\",\"currency\":\"EUR\",\"details\":{\"subtotal\":\"1.00\"}},\"payee\":{\"merchant_id\":\"UDSNPU9U69UBJ\"},\"soft_descriptor\":\"PAYPAL *TESTFACILIT\",\"item_list\":{\"items\":[],\"shipping_address\":{\"recipient_name\":\"Pavol Rau\"}},\"related_resources\":[{\"sale\":{\"id\":\"6Y974712447139208\",\"state\":\"completed\",\"amount\":{\"total\":\"1.00\",\"currency\":\"EUR\",\"details\":{\"subtotal\":\"1.00\"}},\"payment_mode\":\"INSTANT_TRANSFER\",\"protection_eligibility\":\"INELIGIBLE\",\"transaction_fee\":{\"value\":\"0.38\",\"currency\":\"EUR\"},\"receipt_id\":\"4847133062962202\",\"parent_payment\":\"PAY-1DM67566GW755603SLHPTYVI\",\"create_time\":\"2017-10-12T09:59:17Z\",\"update_time\":\"2017-10-12T09:59:17Z\",\"links\":[{\"href\":\"https://api.sandbox.paypal.com/v1/payments/sale/6Y974712447139208\",\"rel\":\"self\",\"method\":\"GET\"},{\"href\":\"https://api.sandbox.paypal.com/v1/payments/sale/6Y974712447139208/refund\",\"rel\":\"refund\",\"method\":\"POST\"},{\"href\":\"https://api.sandbox.paypal.com/v1/payments/payment/PAY-1DM67566GW755603SLHPTYVI\",\"rel\":\"parent_payment\",\"method\":\"GET\"}],\"soft_descriptor\":\"PAYPAL *TESTFACILIT\"}}]}],\"create_time\":\"2017-10-12T09:59:17Z\",\"links\":[{\"href\":\"https://api.sandbox.paypal.com/v1/payments/payment/PAY-1DM67566GW755603SLHPTYVI\",\"rel\":\"self\",\"method\":\"GET\"}]}}";
System.out.println("dataString: "+dataString);
JSONObject data = new JSONObject(dataString.toString());

//out.print("id: "+data.getString("id")+"<br>");//PAY-1DM67566GW755603SLHPTYVI
String payPalId = data.getString("id");
//out.print("intent: "+data.getString("intent")+"<br>");
//out.print("state: "+data.getString("state")+"<br>");

JSONArray transtactionsArray = data.getJSONArray("transactions");
JSONObject transactionsElm  = (JSONObject) transtactionsArray.get(0);
	JSONObject objectAmount = transactionsElm.getJSONObject("amount");
		//out.print("total: "+objectAmount.getDouble("total") +"<br>");
double totalPrice = objectAmount.getDouble("total");
		//out.print("currency: "+objectAmount.getString("currency") +"<br>");
String currency = objectAmount.getString("currency");
SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
format.setTimeZone(TimeZone.getTimeZone("UTC"));
String createTime = data.getString("create_time");
Date transactionTime = format.parse(createTime);
//out.print("createTime: "+transactionTime+"<br>");

int invoiceId = Tools.getIntValue(session.getAttribute("invoice_id")+"", -1) ;
BasketInvoiceBean invoice = InvoiceDB.getInvoiceById(invoiceId);
String error = null;
if(invoice == null)
{
	invoice = new BasketInvoiceBean();
	error = "process_papypal_response_json_ajax.jsp BasketInvoiceBean je null";
}

if(error == null && Tools.isNotEmpty(invoice.getInternalInvoiceId()))
{
	error = "process_papypal_response_json_ajax.jsp platba "+invoice.getInternalInvoiceId()+" uz bola raz uhradena!";
}

invoice.setInternalInvoiceId(payPalId);

if(error == null && invoice.getStatusId() != BasketInvoiceBean.INVOICE_STATUS_NEW)
{
	error = "process_papypal_response_json_ajax.jsp objednavka musi byt nova (1). Aktualne je: "+invoice.getStatusId();
}

if(error == null && InvoiceDB.getInvoiceByInvoiceId(payPalId) != null)
{
	error = "process_papypal_response_json_ajax.jsp id objednavky "+payPalId+" uz bolo raz pouzite, pravdepodobne niekto podvrhol data.";
}

if(error == null && !invoice.getCurrency().toUpperCase().equals(currency.toUpperCase()))
{
	error = "process_papypal_response_json_ajax.jsp currency objednavky "+currency+" sa nezhoduje.";
}

if(error == null && !Tools.doubleEquals(invoice.getTotalPriceVat(), totalPrice))
{
	error = "process_papypal_response_json_ajax.jsp total price  sa nezhoduje.";
}
int hoursDelay = 1;
Calendar calNow = Calendar.getInstance();
calNow.add(Calendar.HOUR_OF_DAY, - hoursDelay);
if(error == null && calNow.getTimeInMillis() < transactionTime.getTime())
{
	error = "process_papypal_response_json_ajax.jsp cas vytvorenia objednavky je starsi ako "+hoursDelay+" hodina;";
}

if(Tools.isNotEmpty(error))
{
	invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_CANCELLED);
	if(invoice.getBrowserId() != null && invoice.getBrowserId() > 0)
		InvoiceDB.saveInvoice(invoice);
	Logger.debug(null,error);
	Identity user = UsersDB.getCurrentUser(request);
	int userId = -1;
	if(user != null)
	{
		userId = user.getUserId();
	}
	Adminlog.add(Adminlog.TYPE_PAYMENT_GATEWAY, userId, error+" id: "+payPalId, -1, -1);
	out.print("{\"result\":\"not OK\"}");
	return;
}


// JSONObject dataObj = data.getJSONObject("data");
// out.print("id: "+dataObj.getString("id")+"<br>");
// out.print("intent: "+dataObj.getString("intent")+"<br>");
// out.print("state: "+dataObj.getString("state")+"<br>");
// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
// format.setTimeZone(TimeZone.getTimeZone("UTC"));
// String createTime = dataObj.getString("create_time");
// Date transactionTie = format.parse(createTime);
// out.print("createTime: "+transactionTie+"<br>");

out.print("{\"result\":\"ok\"}");
%>