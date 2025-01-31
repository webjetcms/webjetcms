<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
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
		ok = BasketDB.setItemFromDoc(request);
	}

	if ("set".equals(request.getParameter("act")))
	{
		ok = BasketDB.setItemFromDoc(request);
	}

	if (ok)
	{
		List<BasketItemBean> items = BasketDB.getBasketItems(request);
		DocDB docDB = DocDB.getInstance();

		%>{
			"itemId": 					"<%= itemId %>",
			"totalItems": 				"<%= BasketDB.getTotalItems(items) %>",
			"displayCurrency": 		"<%= BasketDB.getDisplayCurrency(request) %>",
			"totalLocalPrice": 	"<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%= BasketDB.getTotalLocalPrice(items,request) %></iway:curr>",
			"totalLocalPriceVat": 	"<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%= BasketDB.getTotalLocalPriceVat(items,request) %></iway:curr>",
			"items": [
				<%
				boolean isFirst = true;
				for (BasketItemBean item : items)
				{
				%><%= (!isFirst) ? "," : "" %>{
				 		"id": "<%= item.getItemId() %>",
				 		"image": "<%= item.getDoc().getPerexImage() %>",
				 		"basketId": "<%= item.getBasketItemId() %>",
						"link": "<%= docDB.getDocLink(item.getItemId()) %>",
						"title": "<%= item.getItemTitle() %>",
						"price": "<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%= item.getItemPriceVat() %></iway:curr>",
						"priceQty": "<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%= item.getItemPriceQty() %></iway:curr>",
						"qty": "<%= item.getItemQty() %>",
						"vat": "<%= Math.round(item.getItemVat()) %>",
						"itemNote": "<%= item.getItemNote() %>",
						"priceVatQty": "<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%= item.getItemPriceVatQty() %></iway:curr>"
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