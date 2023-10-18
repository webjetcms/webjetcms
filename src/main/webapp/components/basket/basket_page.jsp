<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

if (sk.iway.iwcm.common.CloudToolsForCore.hasShop(request)==false) return;

%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<script type="text/javascript" src="/components/basket/jscript.jsp"></script>
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

  else if (("orderform".equals(act) || "saveorder".equals(act)) && orderFormDocId==-1)
  {
    pageContext.include("order_form.jsp");
    return;
  }


  if (basketItems == null)
    basketItems = BasketDB.getBasketItems(request);
  if (basketItems.size() > 0)
    request.setAttribute("basketItems", basketItems);
%>


  <script type="text/javascript">
  //<![CDATA[
    //document.write('<style type="text/css" media="all">@import url("/components/basket/basket.css");</style>');

    (function($) {

      $.basket = function(options) {

        var defaults = {
        itemQtyMin: 1,
        itemQtyMax: 100,
           url: '/components/klaret/basket/basket_ajax.jsp'
        }

        var plugin = this;

        plugin.settings = {}

        var init = function() {
            plugin.settings = $.extend({}, defaults, options);

            // min and max quantity on keydown
            $("div.basketBoxInPage .basketQty").on("keyup", function(){
          var val = isNaN($(this).val()) ? plugin.settings.itemQtyMin : Number($(this).val());
          if (val > plugin.settings.itemQtyMax) {
            $(this).val(plugin.settings.itemQtyMax);
          }
          else if (val < plugin.settings.itemQtyMin) {
            $(this).val(plugin.settings.itemQtyMin);
          }
        });

           // number value of quantity
        $("div.basketBoxInPage .basketQty").on("blur", function(){
          var val = isNaN($(this).val()) ? plugin.settings.itemQtyMin : Number($(this).val());
          $(this).val(val);

          var options = {
            act: "set",
            basketItemId: getItemId($(this)),
            basketId: getBasketId($(this)),
            basketQty: val
          }

          sendData(options);
        });

           // remove and add item count
        $("div.basketBoxInPage").on("click", '.addItem, .removeItem', function(){
          var input = $(this).parents("tr").find("input.basketQty");
          var inputVal = isNaN(input.val()) ? plugin.settings.itemQtyMin : Number(input.val());
          var endVal = 1;

          if ($(this).hasClass("removeItem")) {
            if (inputVal != plugin.settings.itemQtyMin) {
              endVal = inputVal - 1;
              input.val(endVal);
            }
          }
          else {
            if (inputVal != plugin.settings.itemQtyMax) {
              endVal = inputVal + 1;
              input.val(endVal);
            }
          }

          var options = {
            act: "set",
            basketItemId: getItemId($(this)),
            basketId: getBasketId($(this)),
            basketQty: endVal
          }

          sendData(options);
        });

           // remove item form basket
        $("div.basketBoxInPage").on("click", '.deleteItem', function(){
        	//console.log(getBasketId($(this)));
          var options = {
            act: "set",
            basketItemId: getItemId($(this)),
            basketId: getBasketId($(this)),
            basketQty: 0
          }

          sendData(options);
        });

           // add item to basket
          $(".priceSpan a.addToBasket, .priceDiv a.addToBasket").on("click", function(){
               $("div.basketSmallBox").show();
               var id = getClassValue($(this).attr("class"), "itemId");
	          var options = {
	            act: "add",
	            basketItemId: id,
	            basketQty: $('#qty_'+id).val()
	          }

	          if ($(".variantsBox").length > 0) {
	        	  options = addVariants(options);
	          }

	          sendData(options);
           });

           $("a.newWindow").on("click", function(){
               window.open(this.href, '_blank');
               return false;
           })


        }

        var addVariants = function(data)
        {
        	var selectedVariants = [];
        	$('.variantsBox select').each(function(){
        		var value = $(this).val();
        		var name = $(this).prop("name");

        		selectedVariants.push(name + ": " + value);
        	});

        	data.basketUserNote = selectedVariants.join(", ");

        	return data;
        }

        var sendData = function(options){

          $.ajax({
          type: "POST",
          url: plugin.settings.url,
          data: options,
          dataType: "json",
          success: function(data) {

            if (typeof(data.error) != "undefined") {
              alert(data.error);
              return;
            }

            if ($("div.basketSmallBox").length > 0) {

              if (!isBasketInWindow($("div.basketSmallBox"))) {
                $('html, body').animate({
                    scrollTop: $("div.basketSmallBox").offset().top-300
                 }, 500);
              }

              $("div.basketSmallBox .basketSmallItems span").html(data.totalItems).hide().fadeIn();
              $("div.basketSmallBox .basketSmallPrice span").html(data.totalLocalPriceVat).hide().fadeIn();
            }

            if ($("div.basketBoxInPage").length > 0) {
              //var itemTrCount = $("div.basketBoxInPage .itemTr").length;
              $("div.basketBoxInPage .itemTr").remove();

              var html = "";
              $.each(data.items, function(index, item){
                html += getTrTemplate(item);
              });
              $("div.basketBoxInPage .emptyTr").after(html);

              $("tr.basketListTableTotal .basketPrice").html(data.totalLocalPrice);
              $("tr.basketListTableTotalVat .basketPrice, div.basketListTableTotalVat .basketPrice").html(data.totalLocalPriceVat);
            }

            if(data.totalItems == 0) {
              $("div.basketBoxInPage").hide();
              $("a.showBasket").removeClass("open");
            }
          },
          error: function(jqXHR, textStatus, errorThrown){
            console.log(errorThrown);
          }
        });
        }

        var getParentTr = function(el){
        return $(el).parents("tr.itemTr");
        }

        var getItemId = function(el){
        return getClassValue(getParentTr(el).attr("class"), "itemId");
      }

        var getBasketId = function(el){
        	console.log(getParentTr(el));
        return getClassValue(getParentTr(el).attr("class"), "basketId");
      }

        var getClassValue = function(classes, prefix){
        var classesArray = classes.split(" ");

        for (var i=0; i < classesArray.length; i++) {
          if (classesArray[i].indexOf(prefix) != -1) {

            return classesArray[i].substr(classesArray[i].indexOf(prefix) + prefix.length + 1);
          }
        }
      }

        var isBasketInWindow = function(elem){
        var docViewTop = $(window).scrollTop();
        var docViewBottom = docViewTop + $(window).height();

        var elemTop = $(elem).offset().top;
        var elemBottom = elemTop + $(elem).height();

        return ((elemBottom <= docViewBottom) && (elemTop >= docViewTop));
      }

        var getTrTemplate = function(options){

          var defaults = {
              id: "id",
              link: "link",
              title: "title",
              price: "price",
              priceQty: "priceQty",
              qty: "qty",
              vat: "vat",
              priceVatQty: "priceQtyVat"
          }

          options = $.extend({}, defaults, options);

          var result = "";

          result += '<tr class="itemTr itemTr itemId_'+ options.id +' basketId_' + options.basketId + '">';

          result += '<td class="first w-5"><a class="newWindow" href="'+ options.link +'">'+ options.title +'</a></td>';

          result += '<td class="fL w-2">';
          result += '<a href="javascript:void(0)" class="removeItem"><span>remove</span></a>';
          result += '<input type="text" class="basketQty" name="basketQty" maxlength="3" value="'+ options.qty +'">';
          result += '<a href="javascript:void(0)" class="addItem"><span>add</span></a>';
          result += '</td>';

          result += '<td class="basketPrice fL w-2" nowrap="nowrap">'+ options.price +'</td>';

          result += '<td class="fL w-2">'+ options.priceVatQty +'</td>';
          //result += '<td class="fL">' + options.itemNote + '</td>';

          result += '<td class="delete w-1"><a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>"><span>Delete</span></a></td>';

          result += '</tr>';

          return result;
        }

      init();
    }

    })(jQuery);


    $(document).ready(function(){
        try{
            $.basket();
        }
        catch(er) {
            //console.log(er);
        }
    });

  //]]>
  </script>

  <div class="basketBoxInPage" style="display:block;">
     <table class="basketListTable">
       <tr class="basketListTableHeader">
          <th>Produkt <%--iwcm:text key="components.basket.cart"/--%></th>
          <th><iwcm:text key="components.basket.count"/></th>
          <th><iwcm:text key="components.basket.price"/> / kus</th>
          <th><iwcm:text key="components.basket.price_with_dph"/></th>
          <%--th><iwcm:text key="components.cloud.basket.variant_variant"/></th--%>
          <th><a href="javascript:;" class="closeBasket"><iwcm:text key="components.basket.close"/></a></th>
       </tr>

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
                <a href="javascript:void(0)" class="removeItem"><span>remove</span></a>
                <input type="text" class="basketQty" name="basketQty" maxlength="3" value="<bean:write name="good" property="itemQty"/>">
                <a href="javascript:void(0)" class="addItem"><span>add</span></a>
              </td>
              <td class="basketPrice fL w-2" nowrap="nowrap"><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=good.getLocalPriceVat(request) %></iway:curr></td>

              <td class="fL w-2"><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></td>
              <%--td class="fL" style="padding-right: 20px;"><%= good.getItemNote() %></td--%>
              <td class="delete w-1">
                <a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>"><span>Delete</span></a>
              </td>
            </tr>
          </logic:iterate>
       </logic:present>
      </table>

      <div class="container_basket">
        <div class="row">
          <div class="col-md-4">
            <span class="basketContinueBtn" id="orderContinurButton">
              <a href="javascript:history.back()" class="closeBasket">
                <iwcm:text key="components.basket.continue"/>
              </a>
            </span>
          </div>
          <div class="col-md-6 basketListTableTotalVat">
            <span class="basketPriceText">
              Celková cena
            </span>
            <span class="basketPrice">
               <iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>">
                 <%=BasketDB.getTotalLocalPriceVat(basketItems,request)%>
               </iway:curr>
             </span>
          </div>
          <div class="col-md-2">
            <span class="basketOrderBtn" id="orderButton">
              <a href="<%= orderFormUrl %>">Dokončiť nákup</a>
            </span>
          </div>
        </div>
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
