<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

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

  List<BasketInvoiceItemEntity> basketItems = null;
  String act = request.getParameter("act");
  if ("set".equals(act))
  {
    boolean ok = EshopService.getInstance().setItemFromDoc(request);
    if (ok)
    {
      basketItems = EshopService.getInstance().getBasketItems(request);
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
    basketItems = EshopService.getInstance().getBasketItems(request);
  if (basketItems.size() > 0)
    request.setAttribute("basketItems", basketItems);
%>


  <script type="text/javascript">
  <!--
    ;(function($) {

      $('head').append('<style type="text/css" media="all">@import url("/components/basket/css/basket.css");</style>');

      $.basket = function(options) {

        var defaults = {
        itemQtyMin: 1,
        itemQtyMax: <%=Constants.getInt("basketMaxQty")%>,
           url: '/components/basket/basket_ajax.jsp'
        }

        var plugin = this;

        plugin.settings = {}

        var init = function() {
            plugin.settings = $.extend({}, defaults, options);

            // min and max quantity on keydown
            $("div.basketBox .basketQty").on("keyup", function(){
          var val = isNaN($(this).val()) ? plugin.settings.itemQtyMin : Number($(this).val());
          if (val > plugin.settings.itemQtyMax) {
            $(this).val(plugin.settings.itemQtyMax);
          }
          else if (val < plugin.settings.itemQtyMin) {
            $(this).val(plugin.settings.itemQtyMin);
          }
        });

           // number value of quantity
        $("div.basketBox .basketQty").on("blur", function(){
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
        $("div.basketBox .addItem, div.basketBox .removeItem").on("click", function(){
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
        $("div.basketBox").on("click", ".deleteItem", function(){
          var options = {
            act: "set",
            basketItemId: getItemId($(this)),
            basketId: getBasketId($(this)),
            basketQty: 0
          }

          sendData(options);
        });

           // add item to basket
           $(".priceSpan a.addToBasket, .priceDiv a.addToBasket, .col-addToBasket a.addToBasket").on("click", function(){
              $("div.basketSmallBox").show();
              var options = {
                act: "add",
                basketItemId: getClassValue($(this).attr("class"), "itemId")
              }

              sendData(options);
           });

           $("a.newWindow").on("click", function(){
               window.open(this.href, '_blank');
               return false;
           })

           $("a.showBasket, .showBasketBlock").on("click", function(){
              $("div.basketBox").stop().slideDown();
           });

           $("a.closeBasket").on("click", function(){
               $("div.basketBox").stop().slideUp();
           });
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
                    scrollTop: $("div.basketSmallBox").offset().top
                 }, 500);
              }

              $("div.basketSmallBox .basketSmallItems span").html(data.totalItems).hide().fadeIn();
              $("div.basketSmallBox .basketSmallPrice span").html(data.totalLocalPriceVat).hide().fadeIn();
            }

            if ($("div.basketBox").length > 0) {
              //var itemTrCount = $("div.basketBox .itemTr").length;
              $("div.basketBox .itemTr").remove();

              var html = "";
              $.each(data.items, function(index, item){
                html += getTrTemplate(item);
              });
              $("div.basketBox .emptyTr").after(html);

              $("tr.basketListTableTotal .basketPrice").html(data.totalLocalPrice);
              $("tr.basketListTableTotalVat .basketPrice").html(data.totalLocalPriceVat);
            }

            if(data.totalItems == 0) {
              $("div.basketBox, div.basketSmallBox").hide();
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

          result += '<tr class="itemTr itemTr itemId_'+ options.id +'">';

          result += '<td class="first"><a class="newWindow" href="'+ options.link +'">'+ options.title +'</a></td>';

          result += '<td class="fL">';
          result += '<a href="javascript:void(0)" class="removeItem">Remove item</a>';
          result += '<input type="text" class="basketQty" name="basketQty" maxlength="3" value="'+ options.qty +'">';
          result += '<a href="javascript:void(0)" class="addItem">Add item</a>';
          result += '</td>';

          result += '<td style="text-align: right;" class="basketPrice fL" nowrap="nowrap">'+ options.price +'</td>';

          result += '<td class="fL" style="text-align: right; padding-right: 150px;"><span class="mob">/</span>'+ options.priceVatQty +'</td>';

          result += '<td class="delete"><a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>">Delete</a></td>';

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

  //-->
  </script>

  <div class="basketBox" style="display:none;">
     <table class="basketListTable">
       <tr class="basketListTableHeader">
          <th style="text-align: left;"><strong><iwcm:text key="components.basket.cart"/></strong></th>
          <th><iwcm:text key="components.basket.count"/></th>
          <th style="text-align: right;"><iwcm:text key="components.basket.price"/></th>
          <th style="text-align: right; padding-right: 150px;"><iwcm:text key="components.basket.price_with_dph"/></th>
          <th style="width: 30px;"><a href="javascript:void();" class="closeBasket"><iwcm:text key="components.basket.close"/></a></th>
       </tr>

       <tr class="emptyTr" style="display: none;">
         <td colspan="6">
           <iwcm:text key="components.basket.basket_is_empty"/>
         </td>
       </tr>


      <iwcm:present name="basketItems">
          <iwcm:iterate id="good" name="basketItems" type="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity">

            <tr class="itemTr itemId_<bean:write name="good" property="itemId"/> basketId_<bean:write name="good" property="basketItemId"/>">
               <td>
                  <a target="_blank" href="<%=docDB.getDocLink(good.getItemIdInt()) %>"><bean:write name="good" property="title"/></a>
               </td>
               <td class="fL">
                 <a href="javascript:void(0)" class="removeItem">Remove item</a>
                 <input type="text" class="basketQty" name="basketQty" maxlength="3" value="<bean:write name="good" property="itemQty"/>">
                 <a href="javascript:void(0)" class="addItem">Add item</a>
               </td>
               <td style="text-align: right;" class="basketPrice fL" nowrap="nowrap"><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%=good.getLocalPriceVat(request) %></iway:curr></td>

               <td class="fL" style="text-align: right; padding-right: 150px;"><span class="mob">/</span><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></td>
               <td class="delete">
                  <a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>">Delete</a>
                 </td>
            </tr>
          </iwcm:iterate>
       </iwcm:present>


        <tr class='basketListTableTotalVat'>
          <td>
            <span class="basketContinueBtn" id="orderContinurButton">
              <a href="javascript:void(0)" class="closeBasket">
                <iwcm:text key="components.basket.continue"/>
              </a>
            </span>
           </td>
           <td colspan="4">
             <span class="basketOrderBtn" id="orderButton">
                <a href="<%= orderFormUrl %>">
                  <iwcm:text key="components.basket.order"/>
                </a>
              </span>
             <span class="basketPrice">
               <iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>">
                 <%=EshopService.getTotalLocalPriceVat(basketItems,request)%>
               </iway:curr>
             </span>
          </td>
        </tr>
     </table>
  </div>
