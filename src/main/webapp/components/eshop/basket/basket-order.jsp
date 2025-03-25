<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

if (sk.iway.iwcm.common.CloudToolsForCore.hasShop(request)==false) return;

%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<script type="text/javascript" src="/components/basket/jscript.jsp"></script>

<%!
void addTransportFee(List<BasketItemBean> basketItems, HttpServletRequest request)
{
	if(basketItems!=null && basketItems.size()>0)
	{
		//int transportTreshold = Constants.getInt("");
		//int transportFee = Constants.getInt("");
		int transportTreshold = 50;
		int transportFee = 5;
		double totalPrice = BasketDB.getTotalLocalPriceVat(basketItems, request);

		if(totalPrice<transportTreshold)
		{
			BasketItemBean fee = new BasketItemBean();
			fee.setItemPrice(Double.valueOf(transportFee));
			fee.setItemVat(Double.valueOf("0"));
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

  List<BasketItemBean> basketItems = null;
  String act = request.getParameter("act");
  if ("set".equals(act))
  {
    boolean ok = BasketDB.setItemFromDoc(request);
    if (ok)
    {
      basketItems = BasketDB.getBasketItems(request);
      addTransportFee(basketItems, request);
    %>
         <div style='display:none'>
        <span id='basketSmallItemsResult'><iwcm:text key="components.basket.total_items"/>: <span><%=BasketDB.getTotalItems(basketItems)%></span></span>
        <span id='basketSmallPriceResult'><iwcm:text key="components.basket.total_price"/>: <span><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=BasketDB.getTotalLocalPriceVat(basketItems,request)%></iway:curr></span></span>
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
    BasketDB.deleteAll(request);
  }
  /*
  else if (("orderform".equals(act) || "saveorder".equals(act)) && orderFormDocId==-1)
  {
    pageContext.include("order_form.jsp");
    return;
  }
  */

  if (basketItems == null)
  {
    basketItems = BasketDB.getBasketItems(request);
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
                  <th>Produkt <%--iwcm:text key="components.basket.cart"/--%></th>
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


              <logic:present name="basketItems">
                  <logic:iterate id="good" name="basketItems" type="sk.iway.iwcm.components.basket.BasketItemBean">
                      <%--
                      <input type="hidden" name="docid" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("docid"))%>" />
                      <input type="hidden" name="basketItemId" class="input" value="<bean:write name="good" property="itemId"/>" />
                      <input type="hidden" name="act" value="set" />
                    --%>
                      <tr class="itemTr itemId_<bean:write name="good" property="itemId"/> basketId_<bean:write name="good" property="basketItemId"/>">
                          <td class="w-5">
                              <a target="_blank" href="<%=docDB.getDocLink(good.getItemId()) %>"><bean:write name="good" property="title"/></a>
                          </td>
                          <td class="fL w-2">
                              <bean:write name="good" property="itemQty"/>
                          </td>
                          <td nowrap="nowrap"><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=good.getLocalPriceVat(request) %></iway:curr></td>

                          <td><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></td>

                      </tr>
                  </logic:iterate>
              </logic:present>
              <tr class="sucet">
                  <td></td>
                  <td></td>

                  <td > Celkov� cena:</td>

                  <td > <iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>">
                      <%=BasketDB.getTotalLocalPriceVat(basketItems,request)%>
                  </iway:curr>
                  </td>
              </tr>
          </table>
      </div>


      <%--table>
        <tr class='basketListTableTotalVat'>
          <td>
            <span class="basketContinueBtn" id="orderContinurButton">
              <a href="javascript:void(0)" class="closeBasket">
                <iwcm:text key="components.basket.continue"/>
              </a>
            </span>
           </td>
           <td colspan="5">
             <span class="basketOrderBtn" id="orderButton">
                <a href="<%= orderFormUrl %>">
                  <iwcm:text key="components.basket.order"/>
                </a>
              </span>
             <span class="basketPrice">
               <iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>">
                 <%=BasketDB.getTotalLocalPriceVat(basketItems,request)%>
               </iway:curr>
             </span>
          </td>
        </tr>
     </table--%>
  </div>
</form></div>