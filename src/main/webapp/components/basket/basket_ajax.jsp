<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	int itemId = Tools.getIntValue(request.getParameter("basketItemId"), -1);
	if (itemId == -1) {
		out.print("{}");
		return;
	}

	boolean ok = false;

	if ("add".equals(request.getParameter("act")))
	{
		ok = EshopService.getInstance().setItemFromDoc(request);
	}

	if ("set".equals(request.getParameter("act")))
	{
		ok = EshopService.getInstance().setItemFromDoc(request);
	}

	if (ok)
	{
		List<BasketInvoiceItemEntity> items = EshopService.getInstance().getBasketItems(request);
		DocDB docDB = DocDB.getInstance();

		%>{
			"itemId": 					"<%= itemId %>",
			"totalItems": 				"<%= EshopService.getTotalItems(items) %>",
			"displayCurrency": 		"<%= EshopService.getDisplayCurrency(request) %>",
			"totalLocalPrice": 	"<iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%= EshopService.getTotalLocalPrice(items,request) %></iway:curr>",
			"totalLocalPriceVat": 	"<iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%= EshopService.getTotalLocalPriceVat(items,request) %></iway:curr>",
			"items": [
				<%
				boolean isFirst = true;
				for (BasketInvoiceItemEntity item : items)
				{
				%><%= (!isFirst) ? "," : "" %>{
				 		"id": "<%= item.getItemIdInt() %>",
				 		"image": "<%= item.getDoc().getPerexImage() %>",
				 		"basketId": "<%= item.getBasketItemId() %>",
						"link": "<%= docDB.getDocLink(item.getItemIdInt()) %>",
						"title": "<%= item.getItemTitle() %>",
						"price": "<iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%= item.getItemPriceVat() %></iway:curr>",
						"priceQty": "<iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%= item.getItemPriceQty() %></iway:curr>",
						"qty": "<%= item.getItemQty() %>",
						"vat": "<%= Math.round(item.getItemVat()) %>",
						"itemNote": "<%= item.getItemNote() %>",
						"priceVatQty": "<iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%= item.getItemPriceVatQty() %></iway:curr>"
					}<%
					isFirst=false;
				}
				%>
			]
		}<%
	}
	else
	{
		%>{"error": "<iwcm:text key="components.basket.addbasket_popup.canot_add_to_basket"/>"}<%
	}
%>