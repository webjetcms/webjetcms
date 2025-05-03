<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*,sk.iway.iwcm.components.basket.jpa.*,sk.iway.iwcm.components.basket.rest.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%
	//Zobrazenie objednavky do emailu
	int invoiceId = Tools.getIntValue(request.getParameter("invoiceId"), -1);
	if (invoiceId < 1)
		return;

	BasketInvoiceEntity invoice = EshopService.getInstance().getInvoiceById(invoiceId);
	if (invoice != null)
		pageContext.setAttribute("lng", invoice.getUserLng());

	//------------SECURITY TEST---------------
	String _authKey = request.getParameter("auth");
	if (Tools.isEmpty(_authKey) || invoice == null || !_authKey.equals(invoice.getAuthorizationToken()))
	{
		out.println(Prop.getInstance(request).getText("components.basket.invoice_detail.zadana_objednavka_neexistuje"));
		return;
	}
	//----------------

	request.setAttribute("invoice", invoice);

	List<BasketInvoiceItemEntity> basketItems = invoice.getBasketItems();
	if (basketItems.size() > 0)
		request.setAttribute("basketItems", basketItems);
%>
<html><head><style type='text/css'>
<%
	boolean cssFromFile = false;
	try
	{
		//vloz do stranky priamo css styl ()
		String cssUrl = "/css/basketinvoice.css";
		IwcmFile f = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(cssUrl));
		if (f!=null && f.exists())
		{
			out.println(FileTools.readFileContent(cssUrl));
			cssFromFile = true;
		}
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}
	if (cssFromFile == false)
	{
		%>
		body
		{
			padding: 0px; margin: 0px;
			background-color: #ffffff;
			padding-top: 30px;
			padding-bottom: 30px;
		}
		body, p, td, a { font-family: Arial; font-size: 12px; color: #444444;}
		strong { color: #000000; }
		.alignRight { text-align: right; }
		a { color: #0085bd; text-decoration: none; }
		a:hover { text-decoration: underline; }

		table.invoiceDetailWrappingTable { width: 800px; }
		table.invoiceDetailWrappingTable td { background-color: #f2f3f4; padding: 50px; }
		table.invoiceDetailWrappingTable td.footer { padding-top: 0px; text-align: center; font-size: 11px; }

		table.invoiceDetailTable { width: 700px; border-collapse: collapse; }
		table.invoiceDetailTable td { border: 1px solid #c9c9ca; padding: 20px; font-size: 12px; background-color: white; vertical-align: top; }
		table.invoiceDetailTable td.nopadding { padding: 0px; }
		table.invoiceDetailTable td.invoiceHeader { font-weight: bold; font-size: 16px; padding: 0px; border: 0px; border-bottom: 4px solid #0085bd; background-color: transparent; width: 50%; padding-bottom: 6px; }
		table.invoiceDetailTable td.noRightBorder { border-right: 1px solid white; }

		table.invoiceInnerTable { width: auto; border-collapse: collapse; margin: 10px 0px 0px 0px; }
		table.invoiceInnerTable td { padding: 4px 2px 0px 0px; margin: 0px; border: 0px; }
		table.invoiceInnerTable th { font-size: 12px; text-align: left; }

		table.basketListTable { width: 100%; border-collapse: collapse; border: 0px; }
		table.basketListTable th { background-color: #0085bd; border: 0px; color: white; font-size: 12px; padding: 2px; white-space: nowrap; text-align: center; padding-top: 4px; padding-bottom: 4px; font-weight: normal; }
		table.basketListTable td { border: 0px; border-bottom: 1px dashed #c9c9ca; padding: 2px; padding-top: 4px; padding-bottom: 4px; }
		table.basketListTable td.noBorder { border: 0px; }
		table.basketListTable tr.basketListTableTotal td { padding-top: 20px; }
		table.basketListTable td.firstCell, table.basketListTable th.firstCell { padding-left: 20px; }
		table.basketListTable td.lastCell, table.basketListTable th.lastCell { padding-right: 20px; }
		table.basketListTable tr.basketListTableTotalVat td { padding-bottom: 20px; }
		<%
	}
%>
</style></head><body id='WebJETEditorBody' style="text-align: left;">

<jsp:include page="invoice_detail.jsp"></jsp:include>

</body></html>