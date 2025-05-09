<%@page import="java.util.List"%><%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="java.math.BigDecimal"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService"%>
<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>

<%
	//Zobrazenie objednavky do emailu
	int invoiceId = Tools.getIntValue(request.getParameter("invoiceId"), -1);
	if (invoiceId < 1) return;

	BasketInvoiceEntity invoice = EshopService.getInstance().getInvoiceById(invoiceId);

	pageContext.setAttribute("lng", invoice.getUserLng());
	request.setAttribute("invoice", invoice);
%><%@include file="/components/basket/_invoice_security_guard.jsp" %><%

	String lng = PageLng.getUserLng(request);
	Prop prop = Prop.getInstance(lng);

	//ziskaj ciastkove platby
	List basketInvoicePayments = EshopService.getInstance().getBasketInvoicePaymentByInvoiceId(invoiceId, Boolean.TRUE);
	if(basketInvoicePayments.size() > 0) request.setAttribute("basketInvoicePayments",basketInvoicePayments);

	List<BasketInvoiceItemEntity> basketItems = invoice.getBasketItems();
	if (basketItems.size() > 0)
		request.setAttribute("basketItems", basketItems);
	DocDB docDB = DocDB.getInstance();
	int counter = 1;

	String basketInvoiceServerName = Constants.getString("basketInvoiceServerName", Tools.getServerName(request));

	String contactCountryKey = Tools.isEmpty(invoice.getContactCountry()) == true ? "&nbsp;" : prop.getText( "stat.countries.tld" + invoice.getContactCountry() );
	String deliveryCountryKey = Tools.isEmpty(invoice.getDeliveryCountry()) == true ? "&nbsp;" : prop.getText( "stat.countries.tld" + invoice.getDeliveryCountry() );
%>

<iwcm:notPresent name="basketItems">
   <iwcm:text key="components.basket.invoice_email.no_products_in_order"/>.
</iwcm:notPresent>

<iwcm:present name="basketItems">

	<table class="invoiceDetailWrappingTable" border="0" cellspacing="0" cellpadding="20" align="center">
		<tr>
			<td>
				<table class="invoiceDetailTable" border="0" cellspacing="0" cellpadding="0" align="center">
					<tr>
						<td class="invoiceHeader"><%=basketInvoiceServerName%></td>
						<td class="invoiceHeader alignRight"><iwcm:text key="components.basket.orderConfirmation"/></td>
					</tr>
					<tr>
						<td class="noRightBorder"><strong><iwcm:text key="components.basket.invoice_detail.objednavka_cislo"/>:</strong> <bean:write name="invoice" property="basketInvoiceId"/></td>
						<td class="alignRight"><iwcm:text key="components.basket.invoice_detail.datum_vytvorenia"/>: <%=Tools.formatDateTime(invoice.getCreateDate())%></td>
					</tr>
					<tr>
						<td colspan="2">
							<iwcm:text key="components.basket.orderConfirmationIntroText" param1="<%=Tools.isNotEmpty(invoice.getContactLastName()) ? invoice.getContactLastName() : invoice.getContactFirstName()%>" param2="<%=String.valueOf(invoice.getBasketInvoiceId()) %>" param3="<%=Tools.formatDateTime(invoice.getCreateDate()) %>"/>
							<br/><br/>
							<strong><iwcm:text key="components.basket.invoice_detail.stav"/>:</strong> <iwcm:text key='<%=("components.basket.invoice.status."+invoice.getStatusId().intValue())%>'/>
						</td>
					</tr>
					<tr>
						<td>
							<strong><iwcm:text key="components.basket.invoice_email.contact"/></strong>
							<br/>
							<table class="invoiceInnerTable">
						      	<tr>
						         	<td><iwcm:text key="components.basket.invoice_email.name"/>:</td>
						         	<td><bean:write name="invoice" property="contactFirstName"/></td>
						      	</tr>
								<tr>
									<td><iwcm:text key="components.basket.invoice_email.surname"/>:</td>
									<td><bean:write name="invoice" property="contactLastName"/></td>
						      	</tr>
								<tr>
						         	<td><iwcm:text key="components.basket.price_list.product_title"/>:</td>
						         	<td><bean:write name="invoice" property="contactTitle"/></td>
						      	</tr>
								<tr>
						         	<td><iwcm:text key="components.basket.invoice.email"/>:</td>
						         	<td><bean:write name="invoice" property="contactEmail"/></td>
						      	</tr>
								<tr>
						         	<td><iwcm:text key="components.basket.invoice_email.phone_number"/>:</td>
						         	<td><bean:write name="invoice" property="contactPhone"/></td>
						      	</tr>

						    </table>

						   	<hr>

						   	<strong><iwcm:text key="components.basket.delivery_address_title"/></strong>
							<br/>
							<table class="invoiceInnerTable">
								<tr>
						         	<td><iwcm:text key="components.basket.invoice_email.name"/>:</td>
						         	<td><bean:write name="invoice" property="deliveryName"/>&nbsp;</td>
						      	</tr>
								<tr>
									<td><iwcm:text key="components.basket.invoice_email.surname"/>:</td>
									<td><bean:write name="invoice" property="deliverySurName"/>&nbsp;</td>
						      	</tr>
								<tr>
									<td><iwcm:text key="components.basket.invoice_email.street"/>:</td>
									<td><bean:write name="invoice" property="deliveryStreet"/>&nbsp;</td>
						      	</tr>
								<tr>
									<td><iwcm:text key="components.basket.invoice_email.city"/>:</td>
									<td><bean:write name="invoice" property="deliveryCity"/>&nbsp;</td>
						      	</tr>
								<tr>
									<td><iwcm:text key="components.basket.invoice_email.ZIP"/>:</td>
									<td><bean:write name="invoice" property="deliveryZip"/>&nbsp;</td>
						      	</tr>
								<tr>
									<td><iwcm:text key="components.basket.invoice_email.country"/>:</td>
									<td><%=deliveryCountryKey%></td>
						      	</tr>
								<tr>
									<td><iwcm:text key="components.basket.invoice_email.company"/>:</td>
									<td><bean:write name="invoice" property="deliveryCompany"/>&nbsp;</td>
						      	</tr>
							</table>
						</td>

						<td>
							<strong><iwcm:text key="components.invoice.invoice_adress"/></strong>
							<br/>
							<table class="invoiceInnerTable">
							  <tr>
						         <td><iwcm:text key="components.basket.invoice_email.company"/>:</td>
						         <td><bean:write name="invoice" property="contactCompany"/></td>
						      </tr>
							  <tr>
						         <td><iwcm:text key="components.contact.property.ico"/>:</td>
						         <td><bean:write name="invoice" property="contactIco"/></td>
						      </tr>
							  <tr>
						         <td><iwcm:text key="components.contact.property.vatid"/>:</td>
						         <td><bean:write name="invoice" property="contactDic"/></td>
						      </tr>
						      <tr>
						         <td><iwcm:text key="components.basket.invoice_email.street"/>:</td>
						         <td><bean:write name="invoice" property="contactStreet"/></td>
						      </tr>
						      <tr>
						         <td><iwcm:text key="components.basket.invoice_email.city"/>:</td>
						         <td><bean:write name="invoice" property="contactCity"/></td>
						      </tr>
						      <tr>
						         <td><iwcm:text key="components.basket.invoice_email.ZIP"/>:</td>
						         <td><bean:write name="invoice" property="contactZip"/></td>
						      </tr>
							  <tr>
						         <td><iwcm:text key="components.basket.invoice_email.country"/>:</td>
						         <td><%=contactCountryKey%></td>
						      </tr>
						   </table>

						   <hr>

							<strong><iwcm:text key="components.basket.invoice_email.delivery_method"/>:</strong>
							<br/>
							<br/>
							<bean:write name="invoice" property="deliveryMethod"/>

						   <%

							String paymentMethod = PaymentMethodsService.getPaymentMethosLabel(invoice.getPaymentMethod(), request);
							if (Tools.isNotEmpty(paymentMethod))
							{
								%>
								<hr>
							   <strong><iwcm:text key="components.basket.invoice_email.payment_method"/>:</strong>
							   <br/>
							   <br/>
								<%
								out.println(paymentMethod);
								%>
								<br/>
								<%
							}
							%>
							<%
								BigDecimal uhradene = EshopService.getInstance().getPaymentsSum(invoice.getBasketInvoiceId());
								BigDecimal totalPriceVat = invoice.getPriceToPayVat(); //TODO - to local price
								totalPriceVat = totalPriceVat.setScale(2,BigDecimal.ROUND_HALF_UP);
								BigDecimal doplatit = totalPriceVat.subtract(uhradene);
							%>
							<hr>
							<strong><iwcm:text key="components.basket.invoices_list.platba"/></strong>
							<br/>
							<br/>
							<iwcm:text key="components.basket.allreadyPayed"/>: <iway:curr currency="<%=EshopService.getDisplayCurrency(request)%>"><%=uhradene %></iway:curr>, <strong><iwcm:text key="components.basket.toPay"/> <iway:curr currency="<%=EshopService.getDisplayCurrency(request)%>"><%=doplatit %></iway:curr></strong>

							<iwcm:present name="basketInvoicePayments">
								<br/>
								<br/>
								<strong><iwcm:text key="components.basket.admin_invoice_detail.prehlad_platieb"/></strong>
								<br/>
								<table class="invoiceInnerTable">
								   <tr>
							   	   <th><iwcm:text key="components.basket.invoice.date"/></th>
							   	   <th><iwcm:text key="components.basket.invoice_email.payment_method"/></th>
							   	   <th><iwcm:text key="components.basket.admin_invoices_detail.suma"/></th>
							   	</tr>
							   	<logic:iterate id="invoicePayment" name="basketInvoicePayments" type="sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity">
									<tr>
										<td><%=Tools.formatDateTime(invoicePayment.getCreateDate().getTime())%></td>
										<td><%
										out.print(paymentMethod);
										%>
										</td>
										<td><iway:curr currency="<%=EshopService.getDisplayCurrency(request)%>"><%=invoicePayment.getPayedPrice()%></iway:curr></td>
									</tr>
									</logic:iterate>
								</table>
							</iwcm:present>

							<%
							if ("cloud".equals(Constants.getInstallName()))
							{
								String basketBankAccount = sk.iway.iwcm.common.CloudToolsForCore.getValue("bankAccount");
								if (Tools.isNotEmpty(basketBankAccount))
								{
									basketBankAccount = ResponseUtils.filter(basketBankAccount);
									basketBankAccount = Tools.replace(basketBankAccount, "\n", "\n<br/>");
									%>
									<br/>
									<br/>
									<strong><iwcm:text key="cloud.site.customize.bankAccount"/></strong>
									<br/>
									<br/>
									<%
									out.println(basketBankAccount);
								}
							}
							%>
						</td>
					</tr>
					<iwcm:notEmpty name="invoice" property="userNote">
			      <tr>
			         <td colspan="2">
			         	<strong><iwcm:text key="components.basket.note"/></strong>
			         	<br/>
			         	<br/>
			         	<bean:write name="invoice" property="userNote"/>
			         </td>
			      </tr>
			      </iwcm:notEmpty>
			      <tr>
			      	<td colspan="2" class="nopadding">
			      		<table class="basketListTable" border="0" cellspacing="0" cellpadding="0">
						   	<tr class="basketListTableHeader">
						   	   <th class="firstCell" style="text-align: center;"><iwcm:text key="components.basket.item_name"/></th>
						   	   <th style="width: 70px;"><iwcm:text key="components.basket.price_without_DPH"/></th>
						   	   <th style="width: 70px;"><iwcm:text key="components.basket.count"/></th>
						   	   <th style="width: 70px;"><iwcm:text key="components.basket.all"/></th>
						   	   <th style="width: 70px;"><iwcm:text key="components.basket.DPH"/></th>
						   	   <th class="lastCell" style="width: 70px;"><iwcm:text key="components.basket.price_with_dph"/></th>
						   	</tr>
						      <logic:iterate id="good" name="basketItems" type="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity">
							      <tr>
							         <td align="center" class="firstCell">
							         	<%
							         	String docLink = null;
							         	if (good.getItemIdInt() > 0) docLink = docDB.getDocLink(good.getItemIdInt());
							         	if (docLink != null && docLink.indexOf("javascript:void(")==-1 && docLink.indexOf("/system")==-1) {
							         	%>
							            	<a href="<%=docLink %>" target="_blank"><bean:write name="good" property="title"/></a>
							            <% } else { %>
							            	<bean:write name="good" property="title"/>
							            <% } %>
											<iwcm:notEmpty name="good" property="itemNote">
								            <br/>
								            <iwcm:text key="components.basket.note"/>:&nbsp;<bean:write name="good" property="itemNote"/>
											</iwcm:notEmpty>
							         </td>
							         <td align="center" class="basketPrice" nowrap="nowrap"><iway:curr currency="<%=invoice.getCurrency() %>"><%=good.getLocalPrice(request) %></iway:curr></td>
							         <td align="center">
											<bean:write name="good" property="itemQty"/><%
											String qtyTypeFieldName = Constants.getString("basketQuantityTypeField");
											if (Tools.isNotEmpty(qtyTypeFieldName)) {
												%>&nbsp;<bean:write name="good" property="<%=\"doc.\"+qtyTypeFieldName%>"/>
											<% } %>
							         </td>
							         <td align="center" class="basketPrice" nowrap="nowrap"><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>" ><%=good.getItemPriceQty() %></iway:curr></td>
							         <td align="center"><%=(""+Math.round(good.getItemVat()))%>%</td>
							         <td align="center"><iway:curr currency="<%=invoice.getCurrency() %>" ><%=good.getItemPriceVatQty() %></iway:curr></td>
							      </tr>
						      </logic:iterate>
						      <tr class='basketListTableTotal'>
						         <td colspan="6" class="noBorder alignRight lastCell">
						         	<iwcm:text key="components.basket.price_without_DPH_complete"/>:
						         	<span class="basketPrice"><iway:curr currency="<%=invoice.getCurrency() %>"><%=invoice.getPriceToPayNoVat()%></iway:curr></span>
						         </td>
						      </tr>
						      <tr class='basketListTableTotalVat'>
						         <td colspan="6" class="noBorder alignRight lastCell">
						         	<strong><iwcm:text key="components.basket.price_with_DPH_complete"/>:</strong>
						         	<span class="basketPrice"><iway:curr currency="<%=invoice.getCurrency() %>"><%=invoice.getPriceToPayVat()%></iway:curr></span>
									</td>
						      </tr>
					   	</table>
			      	</td>
			      </tr>
				</table>
			</td>
		</tr>
		<tr>
			<td class="footer">
				<iwcm:text key="components.basket.orderConfirmationFooter" param1='<%="http://"+basketInvoiceServerName%>'/>
			</td>
		</tr>
	</table>

</iwcm:present>
