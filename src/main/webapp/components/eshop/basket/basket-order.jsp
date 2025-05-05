<%@page import="java.util.List"%><%@page import="java.math.BigDecimal"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

if (sk.iway.iwcm.common.CloudToolsForCore.hasShop(request)==false) return;

%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.jpa.*,sk.iway.iwcm.components.basket.rest.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<script type="text/javascript" src="/components/basket/jscript.jsp"></script>

<%!
void addTransportFee(List<BasketInvoiceItemEntity> basketItems, HttpServletRequest request)
{
	if(basketItems!=null && basketItems.size()>0)
	{
		//int transportTreshold = Constants.getInt("");
		//int transportFee = Constants.getInt("");
		int transportTreshold = 50;
		int transportFee = 5;
		BigDecimal totalPrice = EshopService.getTotalLocalPriceVat(basketItems, request);

		if(totalPrice.compareTo(new BigDecimal(transportTreshold)) == -1  )
		{
			BasketInvoiceItemEntity fee = new BasketInvoiceItemEntity();
			fee.setItemPrice(BigDecimal.valueOf(transportFee));
			fee.setItemVat(0);
			fee.setItemTitle("Cena za dopravu.");
			//basketItems.add(fee);
		}
	}
}
%>

<%
  //Vypis obsahu nakupneho kosika
  String lng = PageLng.getUserLng(request);
  pageContext.setAttribute("lng", lng);

  DocDB docDB = DocDB.getInstance();

  PageParams pageParams = new PageParams(request);
  int orderFormDocId = pageParams.getIntValue("orderFormDocId", -1);
  String orderFormUrl;
  if (orderFormDocId > 0)
    orderFormUrl = docDB.getDocLink(orderFormDocId, request);
  else
    orderFormUrl = Tools.addParametersToUrl(docDB.getDocLink(Tools.getDocId(request), request), "basketAct=orderform");

  List<BasketInvoiceItemEntity> basketItems = null;
  String act = request.getParameter("act");
  if ("set".equals(act))
  {
    boolean ok = EshopService.getInstance().setItemFromDoc(request);
    if (ok)
    {
      basketItems = EshopService.getInstance().getBasketItems(request);
      addTransportFee(basketItems, request);
    %>
         <div style='display:none'>
        <span id='basketSmallItemsResult'><iwcm:text key="components.basket.total_items"/>: <span><%=EshopService.getTotalItems(basketItems)%></span></span>
        <span id='basketSmallPriceResult'><iwcm:text key="components.basket.total_price"/>: <span><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%=EshopService.getTotalLocalPriceVat(basketItems,request)%></iway:curr></span></span>
        </div>
        <script type="text/javascript">
        <!--
          //prepocitaj hodnoty v parent okne
          writeHtml("basketSmallItems", getHtml("basketSmallItemsResult"));
          writeHtml("basketSmallPrice", getHtml("basketSmallPriceResult"));
        //-->
        </script>
      <%
    }
    else
    {
  %>
      <iwcm:text key="components.basket.canot_save_product"/>.
  <%
    }
  }
  else if ("deleteall".equals(act))
  {
    EshopService.getInstance().deleteAll(request);
  }

  if (basketItems == null)
  {
    basketItems = EshopService.getInstance().getBasketItems(request);
    addTransportFee(basketItems, request);
  }
  if (basketItems.size() > 0)
    request.setAttribute("basketItems", basketItems);
%>

  <form>
  <div class="thumbnail basket_objednavka">
  <div class="basketBoxInPage" style="display:block;">
  <h1><iwcm:text key="components.basket.admin_invoice_detail.polozky_objednavky"/></h1>
      <div class="table-responsive">
          <table class="table">
              <thead>
              <tr>
                  <th>Produkt</th>
                  <th><iwcm:text key="components.basket.count"/></th>
                  <th><iwcm:text key="components.basket.price"/> / kus</th>
                  <th><iwcm:text key="components.basket.price_with_dph"/></th>
              </tr>
              </thead>

              <tr class="emptyTr" style="display: none;">
                  <td colspan="6">
                      <iwcm:text key="components.basket.basket_is_empty"/>
                  </td>
              </tr>

              <iwcm:present name="basketItems">
                  <logic:iterate id="good" name="basketItems" type="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity">
                      <tr class="itemTr itemId_<bean:write name="good" property="itemId"/> basketId_<bean:write name="good" property="basketItemId"/>">
                          <td class="w-5">
                              <a target="_blank" href="<%=docDB.getDocLink(good.getItemIdInt()) %>"><bean:write name="good" property="title"/></a>
                          </td>
                          <td class="fL w-2">
                              <bean:write name="good" property="itemQty"/>
                          </td>
                          <td nowrap="nowrap"><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%=good.getLocalPriceVat(request) %></iway:curr></td>

                          <td><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></td>

                      </tr>
                  </logic:iterate>
              </iwcm:present>
              <tr class="sucet">
                  <td></td>
                  <td></td>

                  <td > Celkov� cena:</td>

                  <td > <iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>">
                      <%=EshopService.getTotalLocalPriceVat(basketItems,request)%>
                  </iway:curr>
                  </td>
              </tr>
          </table>
      </div>

  </div>
</form></div>