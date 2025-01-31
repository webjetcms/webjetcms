<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%
  //Maly vypis poloziek nakupneho kosika v tvare:
  //Pocet poloziek:
  //Celkova cena:

  String lng = PageLng.getUserLng(request);
  pageContext.setAttribute("lng", lng);

  List<BasketItemBean> items = BasketDB.getBasketItems(request);
%>

<div class="basketSmallBox" <%= items.size() == 0 ? "style=\"display: none;\"" : "" %>>
    <span class='basketSmallItems'>
      <span><%=BasketDB.getTotalItems(items)%></span>
    </span>

    <span class='basketSmallPrice'>
      <span>
        <iway:curr currency="<%= BasketDB.getDisplayCurrency(request) %>">
          <%= BasketDB.getTotalLocalPriceVat(items, request) %>
        </iway:curr>
      </span>
    </span>

    <a href="javascript:void(0)" class="showBasket">Show basket</a>
</div>