<%@page import="sk.iway.iwcm.components.basket.StavyObjednavok"%>

<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.components.basket.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true" perms="cmp_basket"/>

<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.users.*"%>
<%@page import="sk.iway.tags.CurrencyTag"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@page import="java.math.BigDecimal"%>
<%
	//presmeruje na /components/INSTALL_NAME/basket/admin_invoices_list.jsp, ak existuje
	String pageURL = "/components/basket/admin_invoice_detail.jsp";
	String nahrada = WriteTag.getCustomPage(pageURL, request);
	if (pageURL.equals(nahrada)==false)
	{
		pageContext.include(nahrada);
		return;
	}
%>

<%
	Identity user = UsersDB.getCurrentUser(request);
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);

	boolean somCestovka = "C".equals(Constants.getString("wjVersion"));

	//Zobrazenie objednavky do emailu
	int invoiceId = Tools.getIntValue(request.getParameter("invoiceId"), -1);
	if (invoiceId < 1)
		return;
	BasketInvoiceBean invoice = InvoiceDB.getInvoiceById(invoiceId);

	if("insertPay".equals(request.getParameter("act")))
	{
		String sposobPlatby = request.getParameter("sposobPlatby");
		String suma = request.getParameter("suma");
		String datum = request.getParameter("datum");
		if(invoice != null && invoice.getBasketInvoiceId() > 0 && sposobPlatby != null && datum != null && suma != null && Tools.getDoubleValue(suma, 0) > 0)
		{
			BasketInvoicePayment invoicePayment = new BasketInvoicePayment();
			invoicePayment.setCreateDate(new Date(DB.getTimestamp(datum)));
			invoicePayment.setInvoiceId(invoice.getBasketInvoiceId());
			invoicePayment.setPayedPrice(Tools.getBigDecimalValue(suma, "0"));
			invoicePayment.setPaymentMethod(sposobPlatby);
			invoicePayment.setClosedDate(new Date(Tools.getNow()));
			invoicePayment.setConfirmed(Boolean.TRUE);
			invoicePayment = BasketInvoicePaymentDB.insertUpdateBasketInvoicePayment(invoicePayment);

			if(invoicePayment == null)
			{
				//pokial sucet so zadanou sumou prekroci cenu objednavky
				%>
				<div class='error' style='color: red; font-weight: bold;'><p><iwcm:text key="components.basket.payment.error.platba_prevysuje_celkovu_sumu"/></p></div>
				<%
			}
			else
			{
				BigDecimal invoicePaymentPrice = BasketInvoicePaymentDB.getPaymentsSum(invoiceId);
				//ak je po uspesnej platbe zaplatena cela suma objednavky, nastavim status na zaplatena
				if(invoicePaymentPrice != null && CurrencyTag.formatNumber(invoice.getTotalPriceVat()).equals(CurrencyTag.formatNumber(invoicePaymentPrice)))
					invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PAID);
				else if(invoicePaymentPrice != null && invoicePaymentPrice != BigDecimal.ZERO)
					invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PARTIALLY_PAID);

				InvoiceDB.saveInvoice(invoice);
			}
		}
	}

	if(request.getParameter("deletePayId") != null)
	{
		int deletePayId = Tools.getIntValue(request.getParameter("deletePayId"), -1);
		BasketInvoicePaymentDB.deleteBasketInvoicePayment(deletePayId);

		BigDecimal invoicePaymentPrice = BasketInvoicePaymentDB.getPaymentsSum(invoiceId);

		if(invoicePaymentPrice != null && CurrencyTag.formatNumber(invoice.getTotalPriceVat()).equals(CurrencyTag.formatNumber(invoicePaymentPrice)))
			invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PAID);
		else if(invoicePaymentPrice != null && invoicePaymentPrice != BigDecimal.ZERO)
			invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_PARTIALLY_PAID);
		else
			invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_NEW);

		InvoiceDB.saveInvoice(invoice);
	}

	if ("save".equals(request.getParameter("act")))
	{
	   int newStatusId = Tools.getIntValue(request.getParameter("statusId"), invoice.getStatusId().intValue());
	   String newUserNote = request.getParameter("userNote");

	   invoice.setStatusId(new Integer(newStatusId));
	   invoice.setUserNote(newUserNote);

	   //pre cestovku sa tu uklada datum do kedy je rezervacia
	   if (Tools.isNotEmpty(request.getParameter("fieldD"))) invoice.setFieldD(request.getParameter("fieldD"));

	   boolean saveOK = InvoiceDB.saveInvoice(invoice);

	   boolean emailSendOK = false;
	   if (saveOK)
	   {
		 	//ak treba odosli notifikaciu klientovi
			if ("true".equals(request.getParameter("sendNotify")))
			{
				String fromEmail = request.getParameter("fromEmail");
				String fromName = fromEmail;
				String toEmail = request.getParameter("toEmail");
				String subject = request.getParameter("subject");
				String body = request.getParameter("body");

				emailSendOK = SendMail.send(fromName, fromEmail, toEmail, subject, body, request);
			}
	   }

	   if (saveOK)
	   {
%>
			<script type="text/javascript">
			<!--
				window.opener.location.reload();
				window.close();
			//-->
			</script>
<%
			return;
		}
		else if ("true".equals(request.getParameter("sendNotify")) && emailSendOK == false)
		{
	   	%>
	   		<span style='color: red;'><iwcm:text key="components.basket.errorSendingEmail"/></span>
	   	<%
		}
	   	else
	   	{
	   	%>
	   		<span style='color: red;'><iwcm:text key="components.tips.saving_error"/></span>
	   	<%
		}
	}

	//ziskaj ciastkove platby
	List basketInvoicePayments = BasketInvoicePaymentDB.getBasketInvoicePaymentByInvoiceId(invoiceId, Boolean.TRUE);
	if(basketInvoicePayments.size() > 0) request.setAttribute("basketInvoicePayments",basketInvoicePayments);

	pageContext.setAttribute("lng", invoice.getUserLng());
	request.setAttribute("invoice", invoice);

	List<BasketItemBean> basketItems = invoice.getBasketItems();
	if (basketItems.size()>0)
		request.setAttribute("basketItems", basketItems);

	request.setAttribute("dialogTitle", prop.getText("components.basket.invoice.number_long")+": " + invoice.getBasketInvoiceId());

	DocDB docDB = DocDB.getInstance();
%>

<%@ include file="/admin/layout_top_dialog.jsp" %>

<div class="padding10">

<script type="text/javascript">
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
		{
			if(partialPaymentPriceArray[1].length == 1)
				partialPaymentPriceArray[1] += '0';
			partialPaymentPrice2 = parseInt(partialPaymentPriceArray[1]);
		}
		totalPriceArray = totalPrice.replace(',','.').split('.');
		var totalPrice1 = 0;
		var totalPrice2 = 0;
		if(totalPriceArray.length > 0)
			totalPrice1 = parseInt(totalPriceArray[0]);
		if(totalPriceArray.length > 1)
		{
			if(totalPriceArray[1].length == 1)
				totalPriceArray[1] += '0';
			totalPrice2 = parseInt(totalPriceArray[1]);
		}
		if(isNaN(partialPaymentPrice1) || isNaN(partialPaymentPrice2))
		{
			alert("<iwcm:text key="components.baslet.invoices_list.error.zly_format"/>");
			inputPrice.value = totalPrice;
			inputPrice.focus();
			return;
		}
		else
		{
			if(partialPaymentPrice1 == 0 && partialPaymentPrice2 == 0)
			{
				alert("<iwcm:text key="components.baslet.invoices_list.error.nulova_uhrada"/>");
				inputPrice.value = totalPrice;
				inputPrice.focus();
				return;
			}
			else
			{
				if(partialPaymentPrice1 > totalPrice1)
				{
					alert("<iwcm:text key="components.baslet.invoices_list.error.velka_uhrada"/> " + totalPrice);
					inputPrice.value = totalPrice;
					inputPrice.focus();
					return;
				}
				else if(partialPaymentPrice1 == totalPrice1)
				{
					if(partialPaymentPrice2 > totalPrice2)
					{
						alert("<iwcm:text key="components.baslet.invoices_list.error.velka_uhrada"/> " + totalPrice);
						inputPrice.value = totalPrice;
						inputPrice.focus();
						return;
					}

				}
			}
		}
	}
}

	function statusCheck(status)
	{
		if(status.value == <%=BasketInvoiceBean.INVOICE_STATUS_PAID%>)
		{
		<%
			BigDecimal invoicePaymentPrice = BasketInvoicePaymentDB.getPaymentsSum(invoiceId);
			if(CurrencyTag.formatNumber(invoice.getTotalPriceVat()).equals(CurrencyTag.formatNumber(invoicePaymentPrice)) == false)
			{
		%>
				alert("<iwcm:text key="components.basket.admin_invoices_detail.error.nemoze_byt_stav_zaplatena"/>");
				document.getElementById('statusId').value = <%=invoice.getStatusId()%>;
		<%
			}
		%>
		}
		if(status.value == <%=BasketInvoiceBean.INVOICE_STATUS_ISSUED%>)
		{
			<%
				if(invoice.getStatusId() != BasketInvoiceBean.INVOICE_STATUS_PAID){
					%>
					alert("<iwcm:text key="components.basket.admin_invoices_detail.error.nemoze_byt_stav_vydana"/>");
					document.getElementById('statusId').value = <%=invoice.getStatusId()%>;
				<%
				}
			%>
		}
	}

	function Ok()
	{
		<%
			if (somCestovka)
			{
		%>
				if (!skontrolujRezervaciu())
					return false;
		<%
			}
		%>
		document.detailForm.bSubmit.click();
	}

	function skontrolujRezervaciu()
	{
		if(document.getElementById('statusId').value != <%=StavyObjednavok.REZERVOVANA_DO%>)
			return true;
		try
		{
			date = document.getElementById('fieldD').value
			if (date == '')
				throw "<iwcm:text key="components.basket.errorReservationState"/>"
			//TODO skontrolovat, ci to nie je v minulosti
			return true;
		}
		catch(ex)
		{
			alert(ex);
			return false;
		}
	}
//-->
</script>

<logic:notPresent name="basketItems">
   <iwcm:text key="components.basket.invoice_email.no_products_in_order"/>.
</logic:notPresent>

<logic:present name="basketItems">
	<style type="text/css" media="all">
		/* nakupny kosik */

		table.basketListTable
		{
			border: 1px solid #ccc;
			border-collapse: collapse;
		}

		table.basketListTable th
		{
			border: 1px solid #ccc;
			font-weight: bold;
		}

		table.basketListTable td
		{
			border: 1px solid #ccc;
			padding-left: 4px;
		}

		table.basketListTable td input,
		table.basketListTable td select
		{
			font-size: 10px;
		}

		table.basketListTable tr.basketListTableTotal td,
		table.basketListTable tr.basketListTableTotalVat td
		{
			text-align: right;
			font-weight: bold;
		}

		table.basketListTable tr.basketListTableFooter td
		{
			text-align: center;
			font-weight: bold;
		}
		/* nakupny kosik koniec */
	</style>

	<%@include file="/components/_common/cleditor/jquery.cleditor.js.jsp" %>

	<script type="text/javascript">
	<!--
		var dataDivDataSet = false;
		function sendNotifyClick(cb)
		{
			showHideRow("notifyRow", cb.checked);
			if (dataDivDataSet==false)
			{
				var iframeHtml = $("#dataDiv").contents().find("html").html();
				cb.form.body.value = cb.form.body.value + iframeHtml;
				dataDivDataSet = true;
			}
			cb.form.body.value=cb.form.body.value.replace("{STATUS}", cb.form.statusId.options[cb.form.statusId.selectedIndex].text);

			loadClEditorIfReady();
		}

		var textareaId = 'wysiwyg1';

		function loadClEditorIfReady()
		{
			if ($("#" + textareaId + ":visible").size() == 0)
				window.setTimeout(loadClEditorIfReady, 1);
			else
				$("#" + textareaId).cleditor({
					width      : 850,
					height     : 260,
					controls   : "bold italic underline bullets numbering outdent indent image link icon size color highlight pastetext",
					bodyStyle  : "font: 11px  Arial, Helvetica, sans-serif;"
				});
		}
		//-->
	</script>

	<div style="_width: 885px; overflow: auto;" id="adminInvoiceDetail">

	<form action="admin_invoice_detail.jsp" class="detailForm" name="detailForm" method="post">
		<input type="hidden" name="invoiceId" value="<%=invoiceId%>" />
		<input type="hidden" name="act" value="save" />

		<table border="0">
			<tr>
				<td style="width: 110px;"><iwcm:text key="components.basket.invoice.date_created"/></td>
				<td><%=Tools.formatDate(invoice.getCreateDate())%></td>
			</tr>
			<tr>
				<td style="width: 110px;"><label for="statusId"><iwcm:text key="components.basket.invoice.state"/>:</label></td>
				<td>
				<select name="statusId" style="width: auto;" id="statusId" onchange="statusCheck(this);">
					<%
						if (somCestovka)
						{
					%>
							<option value="<%=invoice.getStatusId().intValue()%>"><%=StavyObjednavok.nazovPre(invoice.getStatusId()) %></option>
							<option value="<%=invoice.getStatusId().intValue()%>">---</option>
					<%
							for(int idStavu : StavyObjednavok.mozneStavy())
							{
					%>
								<option value="<%=idStavu%>"><%=StavyObjednavok.nazovPre(idStavu) %></option>
					<%
							}
						}
						else
						{
					%>
							<option value="<%=invoice.getStatusId().intValue()%>"><iwcm:text key='<%=("components.basket.invoice.status."+invoice.getStatusId().intValue())%>'/></option>
							<option value="<%=invoice.getStatusId().intValue()%>">---</option>
							<option value="1"><iwcm:text key="components.basket.invoice.status.1"/></option>
							<option value="2"><iwcm:text key="components.basket.invoice.status.2"/></option>
							<option value="4"><iwcm:text key="components.basket.invoice.status.4"/></option>
							<option value="3"><iwcm:text key="components.basket.invoice.status.3"/></option>
							<option value="<%=BasketInvoiceBean.INVOICE_STATUS_ISSUED%>"><iwcm:text key="components.basket.invoice.status.5"/></option>
					<%
						}
					%>
				</select>
				</td>
			</tr>

			<%
			if (somCestovka)
			{
			%>
				<tr>
					<td valign="top">
						<label for="fieldD"><iwcm:text key="components.basket.invoiceDetail.reservedUntil"/>:</label>
					</td>
					<td style="vertical-align: middle;">
						<input name="fieldD" class="input datepicker" style="width: auto;" value="${invoice.fieldD}" id="fieldD" />
					</td>
				</tr>
			<%
			}
			%>

			<tr>
				<td valign="top"><label for="userNoteId"><iwcm:text key="components.basket.invoice.note"/>:</label></td>
				<td>
					<textarea name="userNote" id="userNoteId" class="input" rows="5" cols="60"><bean:write name="invoice" property="userNote"/></textarea>
				</td>
			</tr>

			<tr>
				<td></td>
				<td>
					<label>
						<input type="checkbox" style="width: auto;" name="sendNotify" value="true" onclick="sendNotifyClick(this)"/> <iwcm:text key="components.basket.sendNotificationToClient"/>
					</label>
				</td>
			</tr>

			<tr id="notifyRow" style="display: none; border: 1px solid #bcbcbc; border-right: 0px; background-color: #f9f9f9">
				<td colspan="2">
					<table border="0">
						<tr>
							<td style="width: 110px;"><label for="fromEmailId"><iwcm:text key="components.qa.add_action.sender"/>:</label></td>
							<td><input type="text" name="fromEmail" id="fromEmailId" value="<%=ResponseUtils.filter(user.getEmail())%>" size="60" maxlength="255"/></td>
						</tr>

						<tr>
							<td><label for="toEmailId"><iwcm:text key="components.qa.add_action.recipient"/>:</label></td>
							<td><input type="text" name="toEmail" id="toEmailId" value="<%=ResponseUtils.filter(invoice.getContactEmail())%>" size="60" maxlength="255"/></td>
						</tr>

						<tr>
							<td><label for="subjectId"><iwcm:text key="components.qa.add_action.subject"/>:</label></td>
							<td><input type="text" id="subjectId" name="subject" value="<iwcm:text key="components.basket.invoiceDetail.subject" param1='<%=""+invoice.getBasketInvoiceId()%>'/>" size="60" maxlength="255"/></td>
						</tr>

						<tr>
							<td colspan="2">
								<textarea name="body" class="wysiwyg" id="wysiwyg1" rows="12" cols="35">
									<%=prop.getText("components.basket.invoiceDetail.body", "" + invoice.getBasketInvoiceId(), Tools.getServerName(request)) %>
								</textarea>
							</td>
						</tr>
					</table>
				</td>
			</tr>

			<tr>
				<td></td>
				<td align="right">
					<input type="submit" name="bSubmit" value="<iwcm:text key="components.basket.invoice_edit.set"/>" class="button100" />
				</td>
			</tr>
		</table>
	</form>
	<h2><iwcm:text key="components.basket.admin_invoice_detail.prehlad_platieb"/></h2>
	<form action="admin_invoice_detail.jsp" class="detailForm" name="platbyForm" method="post">

		<table border="0" cellspacing="0" cellpadding="1">
			<tr>
				<td><label for="date11"><iwcm:text key="components.basket.invoice.date"/>: </label></td>
				<td>
					<input type="hidden" name="act" value="insertPay" />
					<input type="hidden" name="invoiceId" value="<%=invoiceId%>" />
					<input type="text" name="datum" id="date11" style="width: 80px;" class="datepicker" value="<%=Tools.formatDate(Tools.getNow())%>" />
				</td>
				<td>
					<label for="sposobPlatbyId"><iwcm:text key="components.basket.editor.payment_methods"/>:</label>
				</td>
				<td>
					<select name="sposobPlatby" id="sposobPlatbyId">
						<option value="cash_on_delivery"><iwcm:text key="components.basket.order_form.cash_on_delivery"/></option>
						<option value="money_transfer"><iwcm:text key="components.basket.order_form.money_transfer"/></option>
					</select>
				</td>
				<td><label for="sumaId"><iwcm:text key="components.basket.admin_invoices_detail.suma"/>:</label></td>
				<td>
					<input type="text" name="suma" id="sumaId" value="" onchange="checkPrice(this);" style="width: 100px;"/>
					&nbsp;<%=invoice.getCurrency()%>
				</td>
				<%
					BigDecimal uhradene = BasketInvoicePaymentDB.getPaymentsSum(invoice.getBasketInvoiceId());
					BigDecimal totalPriceVat = new BigDecimal(BasketDB.getTotalLocalPriceVat(basketItems,request));
					totalPriceVat = totalPriceVat.setScale(2,BigDecimal.ROUND_HALF_UP);
					BigDecimal doplatit = totalPriceVat.subtract(uhradene);
				%>
				<input type="hidden" name="totalPrice" id="totalPrice" value="<%=CurrencyTag.formatNumber(doplatit)%>" />
				<td>
	         		<input type="submit" name="bPlatbySubmit" value="<iwcm:text key="components.basket.invoice_edit.set"/>" class="button100" />
	        	</td>
			</tr>
		</table>
	</form>

	<iframe frameborder="0" id="dataDiv" width="850" height="950" src="/components/basket/invoice_email.jsp?invoiceId=<%=invoiceId%>&amp;auth=<%=BasketInvoiceBean.getAuthorizationToken(invoiceId)%>"></iframe>

   </div>
</logic:present>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>