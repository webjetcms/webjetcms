<%@page import="sk.iway.cloud.payments.pay24.Pay24MerchantAccountBean"%>
<%@page import="sk.iway.cloud.payments.paypal.PayPalMerchantAccountBean"%>
<%@page import="sk.iway.iwcm.database.JpaDB"%>
<%@page import="sk.iway.cloud.payments.paypal.PayPalMerchantAccountActionBean"%>
<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.components.basket.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentType"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.ElectronicPayments"%>
<%@page import="sk.iway.tags.CurrencyTag"%>

<%@page import="java.net.URL"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="java.io.DataOutputStream"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>

<iwcm:script type="text/javascript" src="/components/basket/jscript.jsp"/>
<iwcm:script type="text/javascript">
	<!--
	function checkPrice(inputPrice)
	{
	partialPaymentPrice = inputPrice.value;
	totalPrice = document.getElementById("totalPrice").value.split(' ').join('');
	if(partialPaymentPrice != null && partialPaymentPrice != '')
	{
	partialPaymentPriceArray = partialPaymentPrice.replace(',','.').split('.');
	var partialPaymentPrice1 = 0;
	var partialPaymentPrice2 = 0;
	if(partialPaymentPriceArray.length > 0)
	partialPaymentPrice1 = parseInt(partialPaymentPriceArray[0]);
	if(partialPaymentPriceArray.length > 1)
	partialPaymentPrice2 = parseInt(partialPaymentPriceArray[1]);
	totalPriceArray = totalPrice.replace(',','.').split('.');
	var totalPrice1 = 0;
	var totalPrice2 = 0;
	if(totalPriceArray.length > 0)
	totalPrice1 = parseInt(totalPriceArray[0]);
	if(totalPriceArray.length > 1)
	totalPrice2 = parseInt(totalPriceArray[1]);

	if(isNaN(partialPaymentPrice1) || isNaN(partialPaymentPrice2))
	{
	alert('<iwcm:text key="components.baslet.invoices_list.error.zly_format"/>');
	inputPrice.value = totalPrice;
	inputPrice.focus();
	return;
	}
	else
	{
	if(partialPaymentPrice1 == 0 && partialPaymentPrice2 == 0)
	{
	alert('<iwcm:text key="components.baslet.invoices_list.error.nulova_uhrada"/>');
	inputPrice.value = totalPrice;
	inputPrice.focus();
	return;
	}
	else
	{
	if(partialPaymentPrice1 > totalPrice1)
	{
	alert('<iwcm:text key="components.baslet.invoices_list.error.velka_uhrada"/> '+totalPrice);
	inputPrice.value = totalPrice;
	inputPrice.focus();
	return;
	}
	else if(partialPaymentPrice1 == totalPrice1)
	{
	if(partialPaymentPrice2 > totalPrice2)
	{
	alert('<iwcm:text key="components.baslet.invoices_list.error.velka_uhrada"/> '+totalPrice);
	inputPrice.value = totalPrice;
	inputPrice.focus();
	return;
	}

	}
	}
	}
	}
	}

	function checkPayMethod()
	{
	var payMethod = document.getElementById('paymentMethodId');
	var partialPaymentPriceSpan = document.getElementById('partialPaymentPriceSpan');
	if(payMethod != null && payMethod != 'undefined' && partialPaymentPriceSpan != null && partialPaymentPriceSpan != 'undefined')
	{
	if(payMethod.value == 'cash_on_delivery')
	partialPaymentPriceSpan.style.display = "none";
	else
	partialPaymentPriceSpan.style.display = "inline";
	document.getElementById("partialPaymentPrice").value = document.getElementById("totalPrice").value;
	}
	}
	//-->
</iwcm:script>
<%
	Prop prop = Prop.getInstance(request);

	if(Pay24MerchantAccountBean.is24PayResponse(request))
	{
		if(Pay24MerchantAccountBean.isSuccess(request))
		{%>

<iwcm:script type="text/javascript">
	alert('<iwcm:text key="components.basket.payment.replyAcknowledged"/>');
</iwcm:script>
<%//out.print(prop.getText("components.basket.payment.replyAcknowledged"));
}
else
{%>
<iwcm:script type="text/javascript">
	alert('<iwcm:text key="basket.payment.24pay.notsuccess"/>');
</iwcm:script>

<%//out.print(prop.getText("basket.payment.24pay.notsuccess"));
}
}

	//Vytvorenie objednavky
	PageParams pageParams = new PageParams(request);

	//zistim, ci su povolene ciastkove platby
	boolean allowPartialPayments = pageParams.getBooleanValue("allowPartialPayments", false);
	//ak niesu, odstranim zo session poslednu ciastkovu platbu
	if(allowPartialPayments == false && session.getAttribute("partialPaymentPrice") != null && request.getParameter("partialPaymentPrice") == null)
		session.removeAttribute("partialPaymentPrice");

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	List<BasketItemBean> basketItems = null;

	Set<String> paymentMethodsWithSubpages = new HashSet<String>();
	paymentMethodsWithSubpages.add("money_transfer");
	paymentMethodsWithSubpages.addAll( ElectronicPayments.getSupportedPaymentMethodsToBasketString() );


	List<String> displayedPaymentMethods =
			new ArrayList<String>( Arrays.asList(pageParams.getValue("displayedPayments","").split(",")) );
	while (displayedPaymentMethods.contains(""))
		displayedPaymentMethods.remove("");
	//pridame paypal ak je nakonfigurovany
	if(ElectronicPayments.isPaymentMethodConfigured(PaymentType.PAYPAL))
	{
		displayedPaymentMethods.add("paypal");
	}

	//pridame PAYPAL_EXPRESS_CHECKOUT ak je nakonfigurovany
	if(ElectronicPayments.isPaymentMethodConfigured(PaymentType.PAYPAL_EXPRESS_CHECKOUT))
	{
		displayedPaymentMethods.add(PaymentType.PAYPAL_EXPRESS_CHECKOUT.toString());
	}

	//pridame 24pay ak je nakonfigurovany
	if(ElectronicPayments.isPaymentMethodConfigured(PaymentType.PAY24))
	{
		displayedPaymentMethods.add(PaymentType.PAY24.toBasketString());
	}

	String thanksUrl = pageParams.getValue("thanksUrl", null);

	//-----------------FORMULAR SA ODOSLAL SAM NA SEBA, REAGUJ-------------------
	if ("saveorder".equals(request.getParameter("act")))
	{
		int deliveryMethod = Tools.getIntValue(request.getParameter("deliveryMethod"),-1);

		if(deliveryMethod > 0)
		{
			BasketDB.setItemFromDoc(request, deliveryMethod, 1, prop.getText("components.basket.invoice_email.delivery_method"));
		}

		BasketInvoiceBean invoice = InvoiceDB.saveOrder(request);

		//nesmie obsahovat nulovu cenu
		double eps = 1e-7;
		if ( Math.abs(invoice.getTotalPriceVat() - 0.0) < eps)
		{
			%>
			<h2><iwcm:text key="components.basket.payment.invoiceTimedOut"/></h2>
			<p>&nbsp;</p>
			<%
		return;
		}

	if (invoice != null)
	{
		//posli mail
		String notifyEmail = pageParams.getValue("notifyEmail", null);
		if(Tools.isEmpty(notifyEmail))
		{
			UserDetails admin = sk.iway.iwcm.common.CloudToolsForCore.getAdmin();
			if (admin != null) notifyEmail = admin.getEmail();
			else notifyEmail = "order@"+Tools.getServerName(request);
		}

		boolean sendOK = true;
		if (Tools.isNotEmpty(notifyEmail))
		{
			String fromEmail = invoice.getContactEmail();
			if (Tools.isEmpty(fromEmail) || fromEmail.indexOf("@")==-1)
			{
				fromEmail = notifyEmail;
			}
			sendOK = InvoiceDB.sendInvoiceEmail(request, invoice.getBasketInvoiceId(), fromEmail, notifyEmail, prop.getText("components.basket.order_form.email_subject_admin", String.valueOf(invoice.getBasketInvoiceId())));
		}

		boolean notifyClient = pageParams.getBooleanValue("notifyClient", true);
		if (notifyClient && Tools.isEmail(invoice.getContactEmail()))
		{
			String fromEmail = notifyEmail;
			if (fromEmail.indexOf(",") > 0)
				fromEmail = fromEmail.substring(0, fromEmail.indexOf(","));

			InvoiceDB.sendInvoiceEmail(request, invoice.getBasketInvoiceId(), fromEmail, invoice.getContactEmail(), prop.getText("components.basket.order_form.email_subject", String.valueOf(invoice.getBasketInvoiceId())));
		}

		basketItems = BasketDB.getBasketItems(request);
		//odober pocet produktov zo skladovych zasob
		InvoiceDB.decreaseCountOfProductFromStock(invoice.getBasketInvoiceId());

		//mail bol uspesne poslany, redirectneme pouzivatela na zaplatenie
		if (sendOK)
		{
			//sluzba Heureka - overene zakaznikmi
			if(session.getAttribute("overeneZakaznikmi")!=null && Tools.isNotEmpty(session.getAttribute("overeneZakaznikmi").toString()))
			{
				String idOZ = "id=" + session.getAttribute("overeneZakaznikmi") + "&";
				String emailOZ = "email=" + invoice.getContactEmail() + "&";
				String itemId="";
				String orderidOZ = "orderid=" + String.valueOf(invoice.getBasketInvoiceId());

				for(BasketItemBean bib : invoice.getBasketItems())
				{
					if(bib.getItemId() != deliveryMethod)
						itemId = itemId + "itemId[]=" + bib.getItemId() + "&";
				}

				String overeneZakaznikmiURL = "http://www.heureka.sk/direct/dotaznik/objednavka.php?"+idOZ+emailOZ+itemId+orderidOZ;

				URL obj = new URL(overeneZakaznikmiURL);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();

				con.setRequestMethod("GET");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");

				int responseCode = con.getResponseCode();

					/*BufferedReader in = new BufferedReader(
					        new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuffer responseOZ = new StringBuffer();

					while ((inputLine = in.readLine()) != null) {
						responseOZ.append(inputLine);
					}
					in.close();

					String textResponse = responseOZ.toString();
					textResponse = textResponse + "";*/
			}

%>
<iwcm:script type="text/javascript">
	$("document").ready(function(){
	$("div.basketSmallBox").hide();
	});
</iwcm:script>
<h2><iwcm:text key="components.basket.order_form.order_send"/></h2>
<p>&nbsp;</p>
<%
	//zaregistrujeme objednavku do requestu, prijimacie stranky sa to nemaju odkial dozvediet
	session.setAttribute("invoice", invoice);

	//ak ma sposob platby nejaku svoju vlastnu stranku, tak ho tam redirectni
	//--------------------------REDIRECT NA PLATBY--------------------------------
	String paymentMethod = request.getParameter("paymentMethod");

	//dame do session ciastkovu platbu
	if(request.getParameter("partialPaymentPrice") != null)
	{
		session.setAttribute("partialPaymentPrice", request.getParameter("partialPaymentPrice"));
	}

	if ( paymentMethodsWithSubpages.contains(paymentMethod) )
	{
		if(paymentMethod.equalsIgnoreCase("paypal"))
		{
			PayPalMerchantAccountBean merchantAccount = new JpaDB<PayPalMerchantAccountBean>(PayPalMerchantAccountBean.class).findFirst("domainId", sk.iway.iwcm.common.CloudToolsForCore.getDomainId());
			if(merchantAccount == null)
			{
				Logger.debug(PayPalMerchantAccountBean.class, "Failed to locate PayPal merchant account, terminating");
				return;
			}
			else
			{
				request.setAttribute("invoice", invoice);
				request.setAttribute("initTransaction", true);
				request.setAttribute("returnUrl", PathFilter.getOrigPath(request));
			}
		}
		request.setAttribute("moneyTransferAccount",pageParams.getValue("moneyTransferAccount",""));
		request.setAttribute("moneyTransferNote",pageParams.getValue("moneyTransferNote",""));
		if (ElectronicPayments.getSupportedPaymentMethodsToBasketString().contains(paymentMethod))
		{
			request.setAttribute("paymentMethod",paymentMethod);
			//	paymentMethod = "bank";
		}

		//otestuj ci existuje nahrada za tuto stranku
		String forward = "/components/basket/order_payment_"+paymentMethod+".jsp";
		java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
					/*if (fForward.exists())
					{
					   pageContext.forward(forward);
					   //return;
					}
					else
					{*/
		pageContext.include("order_payment_"+paymentMethod+".jsp");
		//}
	}

	if (Tools.isNotEmpty(thanksUrl))
	{
%>
<iwcm:script type="text/javascript">
	<!--
	window.location.href="<%=thanksUrl%>";
	//-->
</iwcm:script>
<%
	}

} else { %>
<h1><iwcm:text key="components.basket.order_form.canot_save_order"/></h1>
<p>&nbsp;</p>
<% } %>

<div style='display:none'>
	<span id='basketSmallItemsResult'><iwcm:text key="components.basket.total_items"/>: <span><%=BasketDB.getTotalItems(basketItems)%></span></span>
	<span id='basketSmallPriceResult'><iwcm:text key="components.basket.total_price"/>: <span><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=BasketDB.getTotalLocalPriceVat(basketItems,request)%></iway:curr></span></span>
</div>
<iwcm:script type="text/javascript">
	<!--
	//prepocitaj hodnoty v parent okne
	writeHtml("basketSmallItems", getHtml("basketSmallItemsResult"));
	writeHtml("basketSmallPrice", getHtml("basketSmallPriceResult"));
	//-->
</iwcm:script>
<%
}
else
{%>
<h1><iwcm:text key="components.basket.order_form.canot_save_order"/></h1>
<p>&nbsp;</p>
<%}
	return;
}

	if (basketItems == null)
		basketItems = BasketDB.getBasketItems(request);
	if (basketItems.size()>0)
		request.setAttribute("basketItems", basketItems);

	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user == null)
		user = new Identity();

	String defaultFormActionUrl = PathFilter.getOrigPath(request);
%>
<% if (session.getAttribute("testRun")!=null && Boolean.valueOf(session.getAttribute("testRun").toString()) )
{%>
<iwcm:text key="cloud.basket.testRunMessage"/>
<%}
else {%>
<logic:notPresent name="basketItems">
	<iwcm:text key="components.basket.basket_is_empty"/>.
</logic:notPresent>
<logic:present name="basketItems">
	<iwcm:script type="text/javascript" src="/components/form/check_form.js"/>
	<form action="<%=defaultFormActionUrl %>" id="orderFormBasket" method="post">
		<input type="hidden" name="act" value="saveorder" />


		<div class="row">
			<div class="col-md-6">
				<h2><iwcm:text key="components.basket.invoice_email.delivery_address"/></h2>
				<div class="flex">
					<div class="form-group">
						<label for="deliveryNameId"><iwcm:text key="components.basket.invoice_email.name"/>:</label>
						<input type="text" name="deliveryName" id="deliveryNameId" class="required form-control" size="25" maxlength="255" value="<%=user.getFirstName()%>"/>
					</div>
					<div class="form-group">
						<label for="deliverySurNameId"><iwcm:text key="reguser.lastname"/>:</label>
						<input type="text" name="deliverySurName" id="deliverySurNameId" class="required form-control" size="25" maxlength="255" value="<%=user.getLastName()%>"/>
					</div>
				</div>
				<div class="form-group">
					<label for="deliveryStreetId"><iwcm:text key="components.basket.invoice_email.street"/>:</label>
					<input type="text" name="deliveryStreet" id="deliveryStreetId" class="required form-control" size="25" maxlength="255" value="<%=user.getAdress()%>"/>
				</div>
				<div class="flex">
					<div class="form-group">
						<label for="deliveryCityId"><iwcm:text key="components.basket.invoice_email.city"/>:</label>
						<input type="text" name="deliveryCity" id="deliveryCityId" class="required form-control" size="25" maxlength="255" value="<%=user.getCity()%>"/>
					</div>
					<div class="form-group">
						<label for="deliveryZipId"><iwcm:text key="components.basket.invoice_email.ZIP"/>:</label>
						<input type="text" name="deliveryZip" id="deliveryZipId" class="required form-control numbers" size="5" maxlength="5" value="<%=user.getPSC()%>"/>
					</div>
				</div>
				<div class="form-group">
					<label for="deliveryCountryId"><iwcm:text key="components.basket.invoice_email.country"/>:</label>
					<%/*Tento select box nemenit, je tu kvoli pay24 !!! */ %>
					<select name="deliveryCountry" class="form-control">
						<option value="SVK"><iwcm:text key="stat.countries.tld.sk"/></option>
						<option value="CZE" ><iwcm:text key="stat.countries.tld.cz"/></option>
						<option value="POL"><iwcm:text key="stat.countries.tld.pl"/></option>
					</select>
				</div>
			</div>
			<div class="col-md-6">
				<h2><iwcm:text key="components.basket.invoice_email.contact"/></h2>
				<div class="form-group">
					<label for="contactEmailId"><iwcm:text key="components.basket.invoice_email.email"/>:</label>
					<input type="text" name="contactEmail" class="required email form-control" id="contactEmailId" size="25" maxlength="255" value="<%=user.getEmail()%>"/>
				</div>
				<div class="form-group">
					<label for="contactPhoneId"><iwcm:text key="components.basket.invoice_email.phone_number"/>:</label>
					<input type="text" name="contactPhone" class="form-control" size="25" id="contactPhoneId" maxlength="255" value="<%=user.getPhone()%>"/>
				</div>
				<div class="form-group">
					<label for="contactCompanyId"><iwcm:text key="components.basket.invoice_email.company"/>:</label>
					<input type="text" name="contactCompany" class="form-control" id="contactCompanyId" size="25" maxlength="255" value="<%=user.getCompany()%>"/>
				</div>
			</div>
		</div>

		<div class="row">
			<div class="col-12">

				<h2><iwcm:text key="components.basket.invoice_email.delivery_method"/></h2>
				<div class="form-group">
					<%List<DocDetails> modeOfTransports = BasketDB.getModeOfTransports(request);

						if(modeOfTransports != null && modeOfTransports.size() > 0)
						{%>
					<select name="deliveryMethod" id="deliveryMethodId" class="form-control"><%
						for(DocDetails transport:modeOfTransports)
						{
					%><option data-currency="<%= CurrencyTag.getLabelFromCurrencyCode(transport.getCurrency()) %>" data-value="<%= transport.getPriceVat() %>" value="<%=transport.getDocId()%>"><%=transport.getTitle()%>: <%= CurrencyTag.formatNumber(transport.getPriceVat()) + " " + CurrencyTag.getLabelFromCurrencyCode(transport.getCurrency()) %></option>
						<%}%>
					</select>

					<iwcm:script type="text/javascript">
						$("document").ready(function(){
						$('select#deliveryMethodId').on('change', function () {
						countPrice();
						});
						countPrice();
						});

						function countPrice() {
						var optionSelected = $("select#deliveryMethodId option:selected"),
						deliveryPrice = +optionSelected.data("value"),
						currency = optionSelected.data("currency"),
						span = $("span.totalOrderPrice"),
						price = +span.data("value");

						var totalPrice = Number(price + deliveryPrice).toFixed(2) + " " + currency;
						totalPrice = totalPrice.replace(".", ",");
						span.text(totalPrice);
						}
					</iwcm:script><%
				}
				else
				{%>
					<select name="deliveryMethod" id="deliveryMethodId" class="form-control">
						<option value="<iwcm:text key="components.basket.order_form.delivery_personally"/>"><iwcm:text key="components.basket.order_form.delivery_personally"/></option>
						<option value="<iwcm:text key="components.basket.order_form.delivery_post"/>"><iwcm:text key="components.basket.order_form.delivery_post"/></option>
						<option value="<iwcm:text key="components.basket.order_form.delivery_courier"/>"><iwcm:text key="components.basket.order_form.delivery_courier"/></option>
						<option value="<iwcm:text key="components.basket.order_form.delivery"/>"><iwcm:text key="components.basket.order_form.delivery"/></option>
					</select>
					<%} %>
				</div>
				<div class="form-group">
					<label for="userNoteId"><iwcm:text key="components.basket.note"/>:</label>
					<textarea name="userNote" class="form-control" id="userNoteId" rows="3" cols="30"></textarea>
				</div>

				<div class="form-group">
					<h3>
						<iwcm:text key="components.basket.invoices_list.uhradit"/>
						<%
						if(allowPartialPayments)
						{%>
							<span id="partialPaymentPriceSpan">
							<input type="text" name="partialPaymentPrice" id="partialPaymentPrice" onchange="checkPrice(this);" value="<%=CurrencyTag.formatNumber(BasketDB.getTotalLocalPriceVat(basketItems,request))%>" /> /
							</span> <%
						}
						%>
						<span class="totalOrderPrice" data-value="<%= BasketDB.getTotalLocalPriceVat(basketItems,request) %>">
					      		<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=BasketDB.getTotalLocalPriceVat(basketItems,request)%></iway:curr>
			      		</span>
						<input type="hidden" name="totalPrice" id="totalPrice" value="<%=CurrencyTag.formatNumber(BasketDB.getTotalLocalPriceVat(basketItems,request))%>" />
					</h3>
				</div>
				<% if (displayedPaymentMethods.size() > 0){ %>
				<div class="form-group">
					<label for="paymentMethodId"><iwcm:text key="components.basket.order_form.payment_method"/></label>

					<select name="paymentMethod" id="paymentMethodId" onchange="checkPayMethod(this);">
						<%for (String paymentMethod : displayedPaymentMethods)
						{
							if (!ElectronicPayments.getKnownPaymentMethodsToBasketString().contains(paymentMethod) ||
									ElectronicPayments.isPaymentMethodConfigured(PaymentType.getPaymentTypeFromBasketString(paymentMethod)))
							{ %>
						<option value="<%=paymentMethod %>"> <iwcm:text key='<%="components.basket.order_form."+paymentMethod%>'/> </option><%
							}
						} %>
					</select>
					<iwcm:script type="text/javascript">
						<!--
						checkPayMethod();
						//-->
					</iwcm:script>

				</div>
				<%} %>

				<div class="form-group">
					<input type="submit" class="btn btn-primary pull-right" name="bSubmit" value="<iwcm:text key="components.basket.order_form.create"/>" />
				</div>

			</div>
		</div>


		<input type="hidden" name="rurl" value="<%=Tools.getBaseHref(request) + PathFilter.getOrigPath(request)%>">
	</form>
</logic:present>
<%}%>