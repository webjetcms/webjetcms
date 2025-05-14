<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%
  String lng = PageLng.getUserLng(request);
  pageContext.setAttribute("lng", lng);

  List<BasketInvoiceItemEntity> items = EshopService.getInstance().getBasketItems(request);
%>

<div class="basketSmallBox" <%= items.size() == 0 ? "style=\"display: none;\"" : "" %>>
    <span class='basketSmallItems'>
      <span><%=EshopService.getTotalItems(items)%></span>
    </span>

    <span class='basketSmallPrice'>
      <span>
        <iway:curr currency="<%= EshopService.getDisplayCurrency(request) %>">
          <%= EshopService.getTotalLocalPriceVat(items, request) %>
        </iway:curr>
      </span>
    </span>

    <a href="javascript:void(0)" class="showBasket">Show basket</a>
</div>