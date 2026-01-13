<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.helpers.RequestHelper"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%
  String lng = PageLng.getUserLng(request);
  pageContext.setAttribute("lng", lng);

  String act = request.getParameter("basketAct");
  if (act == null) act = request.getParameter("act");
  if("orderform".equalsIgnoreCase(act) || "saveorder".equalsIgnoreCase(act))
  {
    pageContext.include("/components/basket/order_form.jsp");
    return;
  } 


  PageParams pageParams = new PageParams(request);
	String displayCurrency = EshopService.getInstance().getDisplayCurrency(request);
  int orderFormDocId = pageParams.getIntValue("orderFormDocId", -1);
  int orderFormMainPageDocId = pageParams.getIntValue("orderFormMainPageDocId", -1);

  boolean showSmallBasket = true;
  DocDetails doc = new RequestHelper(request).getDocument();
  if(doc!=null){
	  String data = doc.getData();
	  // ak je vlozena komponenta, ktora v sebe uz obsahuje basket
	  if(doc.getData().indexOf("bootstrap_products.jsp, style=03")>0){
		  showSmallBasket = false;
	  }
  }
  request.setAttribute("basketIncluded", "true");

  if(showSmallBasket)
  {

  List<BasketInvoiceItemEntity> items = EshopService.getInstance().getBasketItems(request);

  String basket = "!INCLUDE(/components/basket/basket.jsp, orderFormDocId="+orderFormDocId+", orderFormMainPageDocId="+orderFormMainPageDocId+")!";
  request.setAttribute("basket", basket);

%>

<link rel="stylesheet" type="text/css" href="/components/basket/css/basket.css"/>

<iwcm:write name="basket"/>

<div class="basketSmallBox" <%= items.size() == 0 ? "style=\"display: none;\"" : "" %>>
    <span class='basketSmallItems showBasketBlock'>
      <span><%=EshopService.getTotalItems(items)%></span>
    </span>

    <span class='basketSmallPrice showBasketBlock'>
      <span>
        <iway:curr currency="<%=displayCurrency%>">
          <%= EshopService.getTotalLocalPriceVat(items, request) %>
        </iway:curr>
      </span>
    </span>

    <a href="javascript:void(0)" class="showBasket">Show basket</a>
</div>

<%}%>