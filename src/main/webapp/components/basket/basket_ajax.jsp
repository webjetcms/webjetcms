<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	String displayCurrency = EshopService.getDisplayCurrency(request);

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
			"displayCurrency": 		    "<%=displayCurrency%>",
			"totalLocalPrice": 	        "<iway:curr currency="<%=displayCurrency%>"><%= EshopService.getTotalLocalPrice(items,request) %></iway:curr>",
			"totalLocalPriceVat": 	    "<iway:curr currency="<%=displayCurrency%>"><%= EshopService.getTotalLocalPriceVat(items,request) %></iway:curr>",
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
						"price": "<iway:curr currency="<%=displayCurrency%>"><%= item.getLocalPriceVat(request) %></iway:curr>",
						"priceQty": "<iway:curr currency="<%=displayCurrency%>"><%= item.getItemLocalPriceQty(request) %></iway:curr>",
						"qty": "<%= item.getItemQty() %>",
						"vat": "<%= Math.round(item.getItemVat()) %>",
						"itemNote": "<%= item.getItemNote() %>",
						"priceVatQty": "<iway:curr currency="<%=displayCurrency%>"><%= item.getItemLocalPriceVatQty(request) %></iway:curr>"
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