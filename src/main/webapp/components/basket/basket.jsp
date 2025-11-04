<%@page import="java.util.List"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

if (sk.iway.iwcm.common.CloudToolsForCore.hasShop(request)==false) return;

%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><iwcm:script type="text/javascript" src="/components/basket/jscript.jsp"></iwcm:script>

<%
  //Vypis obsahu nakupneho kosika
  String lng = PageLng.getUserLng(request);
  pageContext.setAttribute("lng", lng);

  String displayCurrency = EshopService.getInstance().getDisplayCurrency(request);

  DocDB docDB = DocDB.getInstance();

  PageParams pageParams = new PageParams(request);
  int orderFormDocId = pageParams.getIntValue("orderFormDocId", -1);
  String orderFormUrl;
  if (orderFormDocId > 0)
    orderFormUrl = docDB.getDocLink(orderFormDocId, request);
  else
  {
      //moznost zadat docID hlavnej stranky e-shopu ak mame e-shop s hlbsou urovnou podstranok ako 1, inak zle presmeruje na order form
      int orderFormMainPageDocId = pageParams.getIntValue("orderFormMainPageDocId", -1);
      if(orderFormMainPageDocId > 0)
      {
          orderFormUrl = docDB.getDocLink(orderFormMainPageDocId, request);
      }
      else
      {
          orderFormUrl = docDB.getDocLink(Tools.getDocId(request), request);
          if (orderFormUrl.endsWith(".html") && orderFormUrl.lastIndexOf("/") > 4)
          {
              //je to substranka, presmerujme na hlavnu
              orderFormUrl = orderFormUrl.substring(0, orderFormUrl.lastIndexOf("/") + 1);
          }
      }
      orderFormUrl = Tools.addParametersToUrl(orderFormUrl, "basketAct=orderform");
  }

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
        <span id='basketSmallPriceResult'><iwcm:text key="components.basket.total_price"/>: <span><iway:curr currency="<%=displayCurrency%>"><%=EshopService.getTotalLocalPriceVat(basketItems,request)%></iway:curr></span></span>
        </div>
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

	String style = pageParams.getValue("style", "01");

%>

  <script type="text/javascript">
  //<![CDATA[

    (function($) {

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
            $(".basketQty").on("keyup", function(){
          var val = isNaN($(this).val()) ? plugin.settings.itemQtyMin : Number($(this).val());
          if (val > plugin.settings.itemQtyMax) {
            $(this).val(plugin.settings.itemQtyMax);
          }
          else if (val < plugin.settings.itemQtyMin) {
            $(this).val(plugin.settings.itemQtyMin);
          }
        });


        // peidavanie a uperanie mnozstva produktu v kosiku
         $("div.basketBox").on("click", '.addItem, .removeItem', function(){
          var input = $(this).parents("tr").find("input.basketQty");
          var inputVal = isNaN(input.val()) ? plugin.settings.itemQtyMin : Number(input.val());
          var endVal = 1;

          if ($(this).hasClass("removeItem")) {
            //LOWER item qty
            if (inputVal != plugin.settings.itemQtyMin) {
              endVal = inputVal - 1;
              input.val(endVal);
            } else {
              //We want set itemQty to 0 aka remove item

              //Find parent TR
              let parentTr = $(this).parents("tr");
              //If parent is there
              if(parentTr != undefined && parentTr.length > 0){
                parentTr.find("a.deleteItem").click()
              }
              return;
            }
          } else {
            //INCREASE item qty
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



 // number value of quantity
        $(".basketQty").on("blur", function(){
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


$(".deleteItem").bind("click", function(){
	          var options = {
            act: "set",
            basketItemId: getItemId($(this)),
            basketId: getBasketId($(this)),
            basketQty: 0
          }

          sendData(options);
        });

           // add item to basket
$(".addToBasket").on("click", function(){
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

           $("a.showBasket, .showBasketBlock").on("click", function(){
              var basketHeight = $("div.basketBox").outerHeight(),
                  windowHeight = $(window).height();

              if( basketHeight + 100 > windowHeight ) {
               // window.location.href = '/kosik/';
               // return false;
              }

              $("div.basketBox").stop().fadeToggle();
              $(this).toggleClass("open");
              var navHeight = $("div#navbar").height();


           });

           $("a.closeBasket").on("click", function(){
               $("div.basketBox").stop().fadeOut();
               $("a.showBasket").removeClass("open");
           });
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

        var sendData = function(options) {
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

              if(data.totalItems == null || data.totalItems == 0) {
                //Hide small box, nothing to show
                $("div.basketSmallBox").hide();
              }

              $("div.basketSmallBox .basketSmallItems span").html(data.totalItems).hide().fadeIn();
              $("div.basketSmallBox .basketSmallPrice span").html(data.totalLocalPriceVat).hide().fadeIn();
            }

              //var itemTrCount = $("div.basketBox .itemTr").length;
              $("div.basketBox .itemTr").remove();

              var html = "";
              $.each(data.items, function(index, item){
                html += getTrTemplate(item);
              });

              <%if("02".equals(style)){ %>
              $(".sideBasket table tbody").html(html);
              <% } else if("01".equals(style)){ %>
               $("div.basketBox .emptyTr").after(html);
              <%} %>
              $("tr.basketListTableTotal .basketPrice").html(data.totalLocalPrice);
              $("tr.basketListTableTotalVat .basketPrice, div.basketListTableTotalVat .basketPrice").html(data.totalLocalPriceVat);

           if ($("div.basketBox").length > 0) {
              if(data.totalItems == 0) {
               $("div.basketBox").hide();
               $("a.showBasket").removeClass("open");
              }
            }

               // remove item form basket
                 $(".deleteItem").unbind("click");
                 $(".deleteItem").bind("click", function(){
                  var options = {
                    act: "set",
                    basketItemId: getItemId($(this)),
                    basketId: getBasketId($(this)),
                    basketQty: 0
                  }

                  sendData(options);
                });

          // min and max quantity on keydown
                $(".basketQty").on("keyup", function(){
              var val = isNaN($(this).val()) ? plugin.settings.itemQtyMin : Number($(this).val());
              if (val > plugin.settings.itemQtyMax) {
                $(this).val(plugin.settings.itemQtyMax);
              }
              else if (val < plugin.settings.itemQtyMin) {
                $(this).val(plugin.settings.itemQtyMin);
              }
            });

               // number value of quantity
            $(".basketQty").on("blur", function(){
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
            <% if("01".equals(style)){%>
          result += '<tr class="itemTr itemId_'+ options.id +' basketId_' + options.basketId + '">';

          result += '<td class="w-5"><a class="newWindow" href="'+ options.link +'">'+ options.title +'</a></td>';

          result += '<td class="fL w-2">';
          result += '<a href="javascript:void(0)" class="removeItem"><span>remove</span></a>';
          result += '<input type="text" class="basketQty" name="basketQty" maxlength="4" value="'+ options.qty +'">';
          result += '<a href="javascript:void(0)" class="addItem"><span>add</span></a>';
          result += '</td>';

          result += '<td class="basketPrice fL w-2" nowrap="nowrap">'+ options.price +'</td>';

          result += '<td class="fL w-2">'+ options.priceVatQty +'</td>';
          result += '<td class="fL">' + options.itemNote + '</td>';

          result += '<td class="delete w-1"><a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>"><span>Delete</span></a></td>';

          result += '</tr>';

          <% } else if("02".equals(style)){ %>
          result += ' <tr class="itemTr itemId_'+ options.id +' basketId_'+ options.basketId +'">';
          result += '<td>';
         result += '<a href="javascript:void(0)" class="deleteItem"><i class="ti ti-x" aria-hidden="true"></i></a>';
          result += ' </td>';
         result += '<td>';
         result += '<div class="media">';
         result += '                  <a class="pull-left">';
         result += ' <img src="/thumb'+ options.image +'?w=100&h=100&ip=5" class="media-photo">';
         result += '              </a>';
       result += '           <div class="media-body">';
            result += '           <span class="media-meta pull-right"><h4>'+ options.priceVatQty +'</h4></span>';
        result += '       <h4 class="title">';
        result += '        '+ options.title +'';
       result += '         </h4>';
        result += '     <p class="summary">Počet kusov: <input type="text" class="basketQty" name="basketQty" maxlength="4" value="'+options.qty+'"> ' + options.itemNote+'</p>';
       result += '          </div>';
       result += '    </div>';
       result += '    </td>';
       result += ' </tr>';
          <% }%>

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

  <iwcm:style type="text/css">
  .sideBasket .media img{
  width:60px;
  height:auto;
  }

    .sideBasket h4, .sideBasket h3 {
        margin: 0px;
    padding: 0px;
    }

    .sideBasket h3{
    margin-bottom:15px;
    }
  </iwcm:style>
  <% if("01".equals(style)){ %>

  <div class="basketBox" style="display:none;">

    <div class="basket_deco"> </div>
     <table class="basketListTable table table-striped">
       <tr class="basketListTableHeader">
          <th>Produkt <%--iwcm:text key="components.basket.cart"/--%></th>
          <th><iwcm:text key="components.basket.count"/></th>
          <th><iwcm:text key="components.basket.price"/> / kus</th>
          <th><iwcm:text key="components.basket.price_with_dph"/></th>
          <th><iwcm:text key="components.basket.variant_variant"/></th>
          <th><a href="javascript:;" class="closeBasket"><iwcm:text key="components.basket.close"/></a></th>
       </tr>

       <tr class="emptyTr" style="display: none;">
         <td colspan="6">
           <iwcm:text key="components.basket.basket_is_empty"/>
         </td>
       </tr>


      <iwcm:present name="basketItems">
          <iwcm:iterate id="good" name="basketItems" type="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity">

            <tr class="itemTr itemId_<iwcm:beanWrite name="good" property="itemId"/> basketId_<iwcm:beanWrite name="good" property="basketItemId"/>">
              <td class="w-5">
                <a target="_blank" href="<%=docDB.getDocLink(good.getItemIdInt()) %>"><iwcm:beanWrite name="good" property="title"/></a>
              </td>
              <td class="fL w-2">
                <a href="javascript:void(0)" class="removeItem"><span>remove</span></a>
                <input type="text" class="basketQty" name="basketQty" maxlength="4" value="<iwcm:beanWrite name="good" property="itemQty"/>">
                <a href="javascript:void(0)" class="addItem"><span>add</span></a>
              </td>
              <td class="basketPrice fL w-2" nowrap="nowrap"><iway:curr currency="<%=displayCurrency%>"><%=good.getLocalPriceVat(request) %></iway:curr></td>

              <td class="fL w-2"><iway:curr currency="<%=displayCurrency%>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></td>

              <td class="fL" style="padding-right: 20px;"><%= good.getItemNote() %></td>

              <td class="delete w-1">
                <a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>"><span>Delete</span></a>
              </td>
            </tr>
          </iwcm:iterate>
       </iwcm:present>
      </table>

      <div class="container_basket">
      <div class="row">
      <div style="text-align:right"class="col-sm-12 basketListTableTotalVat">
            <h3><span class="basketPriceText">
              Celková cena
            </span>
            <span class="basketPrice">
               <iway:curr currency="<%=displayCurrency%>">
                 <%=EshopService.getTotalLocalPriceVat(basketItems,request)%>
               </iway:curr>
             </span></h3>
      </div>
      </div>

        <div class="row">
          <div style="text-align:right" class="pull-xs-4  col-xs-4  col-sm-12 col-md-12 ">
             <span class="basketContinueBtn" id="orderContinurButton">
              <a class="btn btn-secondary closeBasket" href="javascript:void(0)">
                <iwcm:text key="components.basket.continue"/>
              </a>
            </span>
            <span id="orderButton">
              <a class="btn btn-primary" href="<%= orderFormUrl %>">Dokončiť nákup</a>
            </span>
          </div>
        </div>
      </div>
  </div>

  <% } else if("02".equals(style)){ %>
		<div class="sideBasket">

		<h3>Nákupný košík</h3>

						<div class="table-container">
							<table class="table table-filter">
								<tbody>
		      <iwcm:present name="basketItems">


					<iwcm:iterate id="good" name="basketItems" type="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity">

									<tr class="itemTr itemId_<iwcm:beanWrite name="good" property="itemId"/> basketId_<iwcm:beanWrite name="good" property="basketItemId"/>">

										<td>
										<a href="javascript:void(0)" class="deleteItem"><i class="ti ti-x" aria-hidden="true"></i></a>

										</td>
										<td>
											<div class="media">
												<a class="pull-left">
												<% if(Tools.isNotEmpty(good.getDoc().getPerexImage())){%>
										<img src="/thumb<%=good.getDoc().getPerexImage() %>?w=100&h=100&ip=5" class="media-photo">

												<%} %>
												</a>
												<div class="media-body">
													<span class="media-meta pull-right"><h4><iway:curr currency="<%=displayCurrency%>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></h4></span>
													<h4 class="title">
														<iwcm:beanWrite name="good" property="title"/>
													</h4>
													<p class="summary">Počet kusov: <input type="text" class="basketQty" name="basketQty" maxlength="4" value="<iwcm:beanWrite name="good" property="itemQty"/>"> <%= good.getItemNote() %></p>
												</div>
											</div>
										</td>
									</tr>

           </iwcm:iterate>


       </iwcm:present>
        	</tbody>
							</table>
						</div>
        <div class="container_basket">
      <div class="row">
      <div style="text-align:right"class="col-sm-12 basketListTableTotalVat">
            <h3><span class="basketPriceText">
              Celková cena
            </span>
            <span class="basketPrice">
               <iway:curr currency="<%=displayCurrency%>">
                 <%=EshopService.getTotalLocalPriceVat(basketItems,request)%>
               </iway:curr>
             </span></h3>
      </div>
      </div>

        <div class="row">
          <div style="text-align:right" class="pull-xs-4  col-xs-4  col-sm-12 col-md-12 ">
            <span id="orderButton">
              <a class="btn btn-success" href="<%= orderFormUrl %>">Dokončiť nákup</a>
            </span>
          </div>
        </div>
      </div>
		</div>
  <%} %>
