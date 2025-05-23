<%@page import="sk.iway.iwcm.database.JpaDB"%>
<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>

<%@page import="sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService"%>
<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity"%>
<%@page import="sk.iway.iwcm.system.datatable.json.LabelValue"%>

<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*,java.math.BigDecimal" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.tags.CurrencyTag"%>

<%@page import="java.net.URL"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="java.io.DataOutputStream"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>

<iwcm:script type="text/javascript" src="/components/basket/jscript.jsp"></iwcm:script>
<iwcm:script type="text/javascript">
</iwcm:script>
<%
	Prop prop = Prop.getInstance(request);

	//Vytvorenie objednavky
	PageParams pageParams = new PageParams(request);

	//zistim, ci su povolene ciastkove platby
	boolean allowPartialPayments = pageParams.getBooleanValue("allowPartialPayments", false);
	//ak niesu, odstranim zo session poslednu ciastkovu platbu
	if(allowPartialPayments == false && session.getAttribute("partialPaymentPrice") != null && request.getParameter("partialPaymentPrice") == null)
		session.removeAttribute("partialPaymentPrice");

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	List<BasketInvoiceItemEntity> basketItems = null;

	//New logic
	List<LabelValue> displayedPaymentMethods = PaymentMethodsService.getConfiguredPaymentMethodsLabels(prop);

	String thanksUrl = pageParams.getValue("thanksUrl", null);

	//-----------------FORMULAR SA ODOSLAL SAM NA SEBA, REAGUJ-------------------
	if ("saveorder".equals(request.getParameter("act")))
	{
		int deliveryMethod = Tools.getIntValue(request.getParameter("deliveryMethod"),-1);

		if(deliveryMethod > 0)
		{
			EshopService.getInstance().setItemFromDoc(request, deliveryMethod, 1, prop.getText("components.basket.invoice_email.delivery_method"));
		}

		BasketInvoiceEntity invoice = EshopService.getInstance().saveOrder(request);

		//nesmie obsahovat nulovu cenu
		if ( invoice.getTotalPriceVat().compareTo(BigDecimal.ZERO) < 1)
		{
			%>
				<div class="alert alert-danger" role="alert"><iwcm:text key="components.basket.payment.invoiceTimedOut"/></div>
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
			sendOK = EshopService.getInstance().sendInvoiceEmail(request, invoice.getBasketInvoiceId(), fromEmail, notifyEmail, prop.getText("components.basket.order_form.email_subject_admin", String.valueOf(invoice.getBasketInvoiceId())));
		}

		boolean notifyClient = pageParams.getBooleanValue("notifyClient", true);
		if (notifyClient && Tools.isEmail(invoice.getContactEmail()))
		{
			String fromEmail = notifyEmail;
			if (fromEmail.indexOf(",") > 0)
				fromEmail = fromEmail.substring(0, fromEmail.indexOf(","));

			EshopService.getInstance().sendInvoiceEmail(request, invoice.getBasketInvoiceId(), fromEmail, invoice.getContactEmail(), prop.getText("components.basket.order_form.email_subject", String.valueOf(invoice.getBasketInvoiceId())));
		}

		basketItems = EshopService.getInstance().getBasketItems(request);
		//odober pocet produktov zo skladovych zasob
		EshopService.getInstance().decreaseCountOfProductFromStock(invoice.getBasketInvoiceId());

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

				for(BasketInvoiceItemEntity bib : invoice.getBasketItems())
				{
					if(bib.getItemIdInt() != deliveryMethod)
						itemId = itemId + "itemId[]=" + bib.getItemIdInt() + "&";
				}

				String overeneZakaznikmiURL = "http://www.heureka.sk/direct/dotaznik/objednavka.php?"+idOZ+emailOZ+itemId+orderidOZ;

				URL obj = new URL(overeneZakaznikmiURL);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();

				con.setRequestMethod("GET");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");

				int responseCode = con.getResponseCode();
			}

%>
<iwcm:script type="text/javascript">
	$("document").ready(function(){
	$("div.basketSmallBox").hide();
	});
</iwcm:script>
	<div class="alert alert-success" role="alert"><iwcm:text key="components.basket.order_form.order_send"/></div>
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

	String paymentResponse  = "";
	if ( PaymentMethodsService.isPaymentMethodConfigured(paymentMethod, prop) )
	{
		request.setAttribute("invoiceId", invoice.getBasketInvoiceId());

		String returnUrl = PathFilter.getOrigPath(request);
		returnUrl = Tools.addParametersToUrl(returnUrl, "basketAct=afterpay");
		request.setAttribute("returnUrl", returnUrl);
		request.setAttribute("paymentMethod", paymentMethod);

		paymentResponse = PaymentMethodsService.getPaymentResponse(request);

%>
		<div>
			<%=paymentResponse%>
		</div>
<%
	}

	if (Tools.isNotEmpty(thanksUrl))
	{
%>
<%
	}

} else { %>
	<div class="alert alert-danger" role="alert"><iwcm:text key="components.basket.order_form.canot_save_order"/></div>
<% } %>

<div style='display:none'>
	<span id='basketSmallItemsResult'><iwcm:text key="components.basket.total_items"/>: <span><%=EshopService.getTotalItems(basketItems)%></span></span>
	<span id='basketSmallPriceResult'><iwcm:text key="components.basket.total_price"/>: <span><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%=EshopService.getTotalLocalPriceVat(basketItems,request)%></iway:curr></span></span>
</div>
<%
}
else
{%>
	<div class="alert alert-danger" role="alert"><iwcm:text key="components.basket.order_form.canot_save_order"/></div>
<%}
	return;
}

	if (basketItems == null)
		basketItems = EshopService.getInstance().getBasketItems(request);
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
	<%
		String act = request.getParameter("basketAct");
    	if (act == null) act = request.getParameter("act");

		if("afterpay".equalsIgnoreCase(act)) {
			pageContext.include("/components/basket/order_payment_reply.jsp");
		} else {
			%>
				<iwcm:text key="components.basket.basket_is_empty"/>.
			<%
		}
	%>
</logic:notPresent>
<logic:present name="basketItems">
	<iwcm:script type="text/javascript" src="/components/form/check_form.js"></iwcm:script>

	<form action="<%=defaultFormActionUrl %>" id="orderFormBasket" method="post">
		<input type="hidden" name="act" value="saveorder" />

		<div class="accordion" id="orderFormAccordion">
			<div class="accordion-item">
				<h2 class="accordion-header">
					<button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#orderFormContact" aria-expanded="true" aria-controls="orderFormContact">
						<iwcm:text key="components.basket.invoice_email.contact"/>
					</button>
				</h2>
				<div id="orderFormContact" class="accordion-collapse collapse show">
					<div class="accordion-body">
						<div class="row">
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactFirstNameId"><iwcm:text key="components.basket.invoice_email.name"/>:</label>
								<input type="text " name="contactFirstName" id="contactFirstNameId" class="form-control required" size="25" maxlength="255" value="<%=user.getFirstName()%>"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactLastNameId"><iwcm:text key="reguser.lastname"/>:</label>
								<input type="text " name="contactLastName" id="contactLastNameId" class="form-control required" size="25" maxlength="255" value="<%=user.getLastName()%>"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactEmailId"><iwcm:text key="components.basket.invoice_email.email"/>:</label>
								<input type="text " name="contactEmail" class="form-control required email form-control" id="contactEmailId" size="25" maxlength="255" value="<%=user.getEmail()%>"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactPhoneId"><iwcm:text key="components.basket.invoice_email.phone_number"/>:</label>
								<input type="text " name="contactPhone" class="form-control" size="25" id="contactPhoneId" maxlength="255" value="<%=user.getPhone()%>"/>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="accordion-item">
				<h2 class="accordion-header">
					<button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#orderFormAddress" aria-expanded="true" aria-controls="orderFormAddress">
						<iwcm:text key="components.invoice.invoice_adress"/>
					</button>
				</h2>
				<div id="orderFormAddress" class="accordion-collapse collapse show">
					<div class="accordion-body">
						<div class="row">
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactStreetId"><iwcm:text key="components.basket.invoice_email.street"/>:</label>
								<input type="text " name="contactStreet" id="contactStreetId" class="form-control required" size="25" maxlength="255" value="<%=user.getAdress()%>"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactCityId"><iwcm:text key="components.basket.invoice_email.city"/>:</label>
								<input type="text " name="contactCity" id="contactCityId" class="form-control required" size="25" maxlength="255" value="<%=user.getCity()%>"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactZipId"><iwcm:text key="components.basket.invoice_email.ZIP"/>:</label>
								<input type="text " name="contactZip" id="contactZipId" class="form-control required numbers" size="5" maxlength="5" value="<%=user.getPSC()%>"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="contactCountryId"><iwcm:text key="components.basket.invoice_email.country"/>:</label>
								<select name="contactCountry" id="contactCountryId" class="form-control">
									<%for (String countryTld : Constants.getArray("basketInvoiceSupportedCountries")) {%>
										<option value="<%=countryTld%>"><%=prop.getText("stat.countries.tld" + countryTld)%></option>
									<%}%>
								</select>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="accordion-item">
				<h2 class="accordion-header">
					<button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#orderFormCompany" aria-expanded="false" aria-controls="orderFormCompany">
						<iwcm:text key="components.invoice.company_info"/>
					</button>
				</h2>
				<div id="orderFormCompany" class="accordion-collapse collapse">
					<div class="accordion-body">
						<div class="row">
							<div class="form-group col-md-12 col-xl-4">
								<label class="form-label " for="contactCompanyId"><iwcm:text key="components.basket.invoice_email.company"/>:</label>
								<input type="text " name="contactCompany" class="form-control" id="contactCompanyId" size="25" maxlength="255" value="<%=user.getCompany()%>"/>
							</div>
							<div class="form-group col-md-6 col-xl-4">
								<label class="form-label " for="contactIcoId"><iwcm:text key="components.contact.property.ico"/>:</label>
								<input type="text " name="contactIco" class="form-control" id="contactIcoId" size="25" maxlength="255" value="<%=user.getCompany()%>"/>
							</div>
							<div class="form-group col-md-6 col-xl-4">
								<label class="form-label " for="contactDicId"><iwcm:text key="components.contact.property.vatid"/>:</label>
								<input type="text " name="contactDic" class="form-control" id="contactDicId" size="25" maxlength="255" value="<%=user.getCompany()%>"/>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="accordion-item">
				<h2 class="accordion-header">
					<button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#orderFormDeliveryInfo" aria-expanded="false" aria-controls="orderFormDeliveryInfo">
						<iwcm:text key="components.basket.delivery_address_title"/>
					</button>
				</h2>
				<div id="orderFormDeliveryInfo" class="accordion-collapse collapse">
					<div class="accordion-body">
						<div class="row">
							<div class="form-group col-md-12 col-xl-4">
								<label class="form-label " for="deliveryCompanyId"><iwcm:text key="components.basket.invoice_email.company"/>:</label>
								<input type="text " name="deliveryCompany" id="deliveryCompanyId" class="form-control" size="25" maxlength="255"/>
							</div>
							<div class="form-group col-md-6 col-xl-4">
								<label class="form-label " for="deliveryNameId"><iwcm:text key="components.basket.invoice_email.name"/>:</label>
								<input type="text " name="deliveryName" id="deliveryNameId" class="form-control" size="25" maxlength="255"/>
							</div>
							<div class="form-group col-md-6 col-xl-4">
								<label class="form-label " for="deliverySurNameId"><iwcm:text key="reguser.lastname"/>:</label>
								<input type="text " name="deliverySurName" id="deliverySurNameId" class="form-control" size="25" maxlength="255"/>
							</div>
						</div>
						<div class="row">
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="deliveryStreetId"><iwcm:text key="components.basket.invoice_email.street"/>:</label>
								<input type="text " name="deliveryStreet" id="deliveryStreetId" class="form-control" size="25" maxlength="255"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="deliveryCityId"><iwcm:text key="components.basket.invoice_email.city"/>:</label>
								<input type="text " name="deliveryCity" id="deliveryCityId" class="form-control" size="25" maxlength="255"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="deliveryZipId"><iwcm:text key="components.basket.invoice_email.ZIP"/>:</label>
								<input type="text " name="deliveryZip" id="deliveryZipId" class="form-control numbers" size="5" maxlength="5"/>
							</div>
							<div class="form-group col-sm-12 col-md-6 col-xl-3">
								<label class="form-label " for="deliveryCountryId"><iwcm:text key="components.basket.invoice_email.country"/>:</label>
								<select name="deliveryCountry" class="form-control">
									<option value="">-</option>
									<%for (String countryTld : Constants.getArray("basketInvoiceSupportedCountries")) {%>
										<option value="<%=countryTld%>"><%=prop.getText("stat.countries.tld" + countryTld)%></option>
									<%}%>
								</select>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="accordion-item">
				<h2 class="accordion-header">
					<button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#orderFormPaymentInfo" aria-expanded="true" aria-controls="orderFormPaymentInfo">
						<iwcm:text key="components.basket.delivery_details"/>
					</button>
				</h2>
				<div id="orderFormPaymentInfo" class="accordion-collapse collapse show">
					<div class="accordion-body">
						<div class="row">
							<div class="form-group col-sm-12">
								<label class="form-label " for="deliveryMethodId"><iwcm:text key="components.basket.invoice_email.delivery_method"/>:</label>
								<%List<DocDetails> modeOfTransports = EshopService.getInstance().getModeOfTransports(request);

								if(modeOfTransports != null && modeOfTransports.size() > 0)
									{%>
								<select name="deliveryMethod" id="deliveryMethodId" class="form-control"><%
									for(DocDetails transport:modeOfTransports)
									{
								%><option data-currency="<%= CurrencyTag.getLabelFromCurrencyCode(transport.getCurrency()) %>" data-value="<%= transport.getPriceVat() %>" value="<%=transport.getDocId()%>"><%=transport.getTitle()%>: <%= CurrencyTag.formatNumber(transport.getPriceVat()) + " " + CurrencyTag.getLabelFromCurrencyCode(transport.getCurrency()) %></option>
									<%}%>
								</select>

								<iwcm:script type="text/javascript">
									$("document").ready(function() {
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

							<div class="form-group col-sm-12">
								<label class="form-label " for="paymentMethodId"><iwcm:text key="components.basket.invoice.payment_method"/>:</label>
								<select name="paymentMethod" id="paymentMethodId" class="form-control">
									<%for (LabelValue paymentMethod : displayedPaymentMethods) {
										String label = paymentMethod.getLabel();
										String value = paymentMethod.getValue();
									%>
										<option value="<%=value%>"> <%=label%> </option>
									<%
										}
									%>
								</select>
							</div>

							<div class="form-group col-sm-12">
								<label class="form-label " for="userNoteId"><iwcm:text key="components.basket.note"/>:</label>
								<textarea name="userNote" class="form-control" id="userNoteId" rows="5" cols="30"></textarea>
							</div>

						</div>
					</div>
				</div>
			</div>
		</div>


		<div class="row">

			<div class="form-group col-sm-12">
				<h3>
					<iwcm:text key="components.basket.invoices_list.uhradit"/>
					<%
					if(allowPartialPayments)
					{%>
						<span id="partialPaymentPriceSpan">
						<input type="text " name="partialPaymentPrice" id="partialPaymentPrice" onchange="checkPrice(this);" value="<%=CurrencyTag.formatNumber(EshopService.getTotalLocalPriceVat(basketItems,request))%>" /> /
						</span> <%
					}
					%>
					<span class="totalOrderPrice" data-value="<%= EshopService.getTotalLocalPriceVat(basketItems,request) %>" style="font-weight: bold;">
						<iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%=EshopService.getTotalLocalPriceVat(basketItems,request)%></iway:curr>
					</span>
					<input type="hidden" name="totalPrice" id="totalPrice" value="<%=CurrencyTag.formatNumber(EshopService.getTotalLocalPriceVat(basketItems,request))%>" />
				</h3>
			</div>

			<div class="form-group col-sm-12">
				<input type="submit" class="btn btn-primary" name="bSubmit" value="<iwcm:text key="components.basket.order_form.create"/>" />
			</div>

		</div>

		<input type="hidden" name="rurl" value="<%=Tools.getBaseHref(request) + PathFilter.getOrigPath(request)%>">
	</form>
</logic:present>
<%}%>