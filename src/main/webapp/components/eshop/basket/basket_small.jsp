<%@page import="sk.iway.iwcm.helpers.RequestHelper"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="windows-1250"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

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

        List<BasketItemBean> items = BasketDB.getBasketItems(request);

%>

<div class="basket">
    <a class="btn btn-default btn-icon btn-icon-no-text-small basketSmallPrice" href="#" role="button" data-toggle="basket" aria-haspopup="true" aria-expanded="false">
        <i class="fas fa-shopping-cart"></i>
        <%--<span class="badge badge-dark qty"><%=BasketDB.getTotalItems(items)%></span>--%>
        <span class="price">
            <iway:curr currency="<%= BasketDB.getDisplayCurrency(request) %>">
                <%= BasketDB.getTotalLocalPriceVat(items, request) %>
            </iway:curr>
        </span>
    </a>
</div>

<%}%>