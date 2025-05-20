<%@page import="java.util.List"%><%@page import="java.math.BigDecimal"%><%@page import="sk.iway.iwcm.helpers.RequestHelper"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.jpa.*,sk.iway.iwcm.components.basket.rest.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
    //Maly vypis poloziek nakupneho kosika v tvare:
    //Pocet poloziek:
    //Celkova cena:

    String lng = PageLng.getUserLng(request);
    pageContext.setAttribute("lng", lng);

    boolean showSmallBasket = true;
    DocDetails doc = new RequestHelper(request).getDocument();
    if(doc!=null){
        // ak je vlozena komponenta, ktora v sebe uz obsahuje basket
        if(doc.getData().indexOf("bootstrap_products.jsp, style=03")>0){
            showSmallBasket = false;
        }
    }
    request.setAttribute("basketIncluded", "true");

    if(showSmallBasket)
    {

        List<BasketInvoiceItemEntity> items = EshopService.getInstance().getBasketItems(request);
%>

<div class="basket">
    <a class="btn btn-default btn-icon btn-icon-no-text-small basketSmallPrice" href="#" role="button" data-bs-toggle="basket" aria-haspopup="true" aria-expanded="false">
        <i class="fas fa-shopping-cart"></i>
        <span class="price">
            <iway:curr currency="<%= EshopService.getDisplayCurrency(request) %>">
                <%= EshopService.getTotalLocalPriceVat(items, request) %>
            </iway:curr>
        </span>
    </a>
</div>

<%}%>