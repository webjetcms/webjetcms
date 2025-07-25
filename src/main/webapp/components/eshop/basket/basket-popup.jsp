<%@page import="java.util.List"%><%@page import="java.math.BigDecimal"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

if (sk.iway.iwcm.common.CloudToolsForCore.hasShop(request)==false) return;

%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.jpa.*,sk.iway.iwcm.components.basket.rest.*,java.util.*" %>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>

<iwcm:script type="text/javascript" src="/components/basket/jscript.jsp"></iwcm:script>

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
  {
      orderFormUrl = docDB.getDocLink(Tools.getDocId(request), request);
      if (orderFormUrl.endsWith(".html") && orderFormUrl.lastIndexOf("/")>4)
      {
         //je to substranka, presmerujme na hlavnu
          orderFormUrl = orderFormUrl.substring(0, orderFormUrl.lastIndexOf("/")+1);
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
         <div>
        <span id='basketSmallItemsResult'><iwcm:text key="components.basket.total_items"/>: <span><%=EshopService.getTotalItems(basketItems)%></span></span>
        <span id='basketSmallPriceResult'><iwcm:text key="components.basket.total_price"/>: <span><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%=EshopService.getTotalLocalPriceVat(basketItems,request)%></iway:curr></span></span>
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

        $("[data-bs-toggle='basket']").click(function(e){
            e.preventDefault();
            $(".md-basket-dropdown").toggleClass("open");
        });

      $.basket = function(options) {

        var defaults = {
        itemQtyMin: 1,
        itemQtyMax: 100,
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

           $("a.showBasket").on("click", function(){
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
               $(".md-basket-dropdown").removeClass("open");
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
        };

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

            if ($(".basketSmallPrice").length > 0) {
              $(".basketSmallPrice .price").html(data.totalLocalPriceVat);
              $(".basketSmallPrice .qty").html(data.totalItems);
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
                  };

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
              };

              sendData(options);
            });


          },
          error: function(jqXHR, textStatus, errorThrown){
            console.log(errorThrown);

          }
        });
        };

        var getParentTr = function(el){
        return $(el).parents("tr.itemTr");
        };

        var getItemId = function(el){
        return getClassValue(getParentTr(el).attr("class"), "itemId");
      };

        var getBasketId = function(el){
        return getClassValue(getParentTr(el).attr("class"), "basketId");
      };

        var getClassValue = function(classes, prefix){
        var classesArray = classes.split(" ");

        for (var i=0; i < classesArray.length; i++) {
          if (classesArray[i].indexOf(prefix) != -1) {

            return classesArray[i].substr(classesArray[i].indexOf(prefix) + prefix.length + 1);
          }
        }
      };

        var isBasketInWindow = function(elem){
        var docViewTop = $(window).scrollTop();
        var docViewBottom = docViewTop + $(window).height();

        var elemTop = $(elem).offset().top;
        var elemBottom = elemTop + $(elem).height();

        return ((elemBottom <= docViewBottom) && (elemTop >= docViewTop));
      };

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
          };

          options = $.extend({}, defaults, options);

          var result = "";
            <% if("01".equals(style)){%>
          result += '<tr class="itemTr itemTr itemId_'+ options.id +' basketId_' + options.basketId + '">';

          result += '<td class="w-5"><a class="newWindow" href="'+ options.link +'">'+ options.title +'</a></td>';

          result += '<td class="fL w-2">';
          result += '            <div class="input-group">';
          result += '         <div class="input-group-prepend">';
          result += '             <a href="javascript:void(0)" class="removeItem btn btn-primary btn-sm"><i class="fas fa-minus"></i></a>';
          result += '         </div>';
          result += '         <input type="text" class="basketQty form-control form-control-sm" name="basketQty" maxlength="3" value="'+ options.qty +'">';
          result += '         <div class="input-group-append">';
          result += '             <a href="javascript:void(0)" class="addItem btn btn-primary btn-sm"><i class="fas fa-plus"></i></a>';
          result += '         </div>';
          result += '     </div>';
          result += '</td>';

          result += '<td class="basketPrice fL w-2" nowrap="nowrap">'+ options.price +'</td>';

          result += '<td class="fL w-2">'+ options.priceVatQty +'</td>';
          result += '<td class="fL">' + options.itemNote + '</td>';

          result += '<td class="delete table-actions w-1"><a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>"><i class="fas fa-trash"></i></a></td>';

          result += '</tr>';

          <% } else if("02".equals(style)){ %>
          result += ' <tr class="itemTr itemId_'+ options.id +' basketId_'+ options.basketId +'">';
          result += '<td>';
         result += '<a href="javascript:void(0)" class="deleteItem"><i class="fa fa-times" aria-hidden="true"></i></a>';
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
        result += '     <p class="summary">Počet kusov: <input type="text" class="basketQty" name="basketQty" maxlength="3" value="'+options.qty+'"> ' + options.itemNote+'</p>';
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
  <% if("01".equals(style)){ %>

<div class="md-basket-dropdown">
    <div class="container">
        <div class="basketBox">
            <div class="basket_deco"> </div>
            <table class="basketListTable table table-striped">
                <tr class="basketListTableHeader">
                    <th>Produkt <%--iwcm:text key="components.basket.cart"/--%></th>
                    <th><iwcm:text key="components.basket.count"/></th>
                    <th><iwcm:text key="components.basket.price"/> / kus</th>
                    <th><iwcm:text key="components.basket.price_with_dph"/></th>
                    <%
                      boolean containsVariant = false;
                      for (BasketInvoiceItemEntity basketItem : basketItems) {
                        if (Tools.isNotEmpty(basketItem.getItemNote())){
                          containsVariant = true;
                          break;
                        }
                      }
                      if (containsVariant) { %>
                        <th><iwcm:text key="components.basket.variant_variant"/></th>
                      <% } %>
                    <th><%--<a href="javascript:;" class="closeBasket"><iwcm:text key="components.basket.close"/></a>--%></th>
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
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <a href="javascript:void(0)" class="removeItem btn btn-primary btn-sm"><i class="fas fa-minus"></i></a>
                                    </div>
                                    <input type="text" class="basketQty form-control form-control-sm" name="basketQty" maxlength="3" value="<iwcm:beanWrite name="good" property="itemQty"/>">
                                    <div class="input-group-append">
                                        <a href="javascript:void(0)" class="addItem btn btn-primary btn-sm"><i class="fas fa-plus"></i></a>
                                    </div>
                                </div>
                            </td>
                            <td class="basketPrice fL w-2" nowrap="nowrap"><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>"><%=good.getLocalPriceVat(request) %></iway:curr> / ks</td>

                            <td class="fL w-2"><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></td>
                            <% if (containsVariant) { %>
                              <td class="fL" style="padding-right: 20px;"><%= good.getItemNote() %></td>
                            <% } %>

                            <td class="delete table-actions w-1">
                                <a class="deleteItem" href="javascript:void(0);" title="<iwcm:text key="components.basket.delete"/>"><i class="fas fa-trash"></i></a>
                            </td>
                        </tr>
                    </iwcm:iterate>
                </iwcm:present>
            </table>
            <div class="container_basket">
                <div class="row">
                    <div class="col-12 col-md-4 align-self-center">
                        <span class="basketContinueBtn" id="orderContinurButton">
                  <a class="btn btn-danger btn-sm closeBasket" href="javascript:void(0)">
                    <iwcm:text key="components.basket.continue"/>
                  </a>
                </span>
                    </div>
                    <div class="col-12 col-md-8 align-self-center">
                        <div class="row">
                            <div class="col-12 col-md align-self-center">
                                <div class="basketListTableTotalVat">
                        <span class="price"><span class="basketPriceText">
                  Celková cena
                </span>
                            <span class="basketPrice">
                   <iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>">
                       <%=EshopService.getTotalLocalPriceVat(basketItems,request)%>
                   </iway:curr>
                 </span></span>
                                </div>
                            </div>
                            <div class="col-12 col-md-auto align-self-center">
                                <span id="orderButton">
                  <a class="btn btn-primary btn-sm" href="/kosik/?basketAct=orderform">Dokončiť nákup</a>
                </span>
                            </div>
                        </div>
                    </div>
                </div>
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
										<a href="javascript:void(0)" class="deleteItem"><i class="fa fa-times" aria-hidden="true"></i></a>

										</td>
										<td>
											<div class="media">
												<a class="pull-left">
												<% if(Tools.isNotEmpty(good.getDoc().getPerexImage())){%>
										<img src="/thumb<%=good.getDoc().getPerexImage() %>?w=100&h=100&ip=5" class="media-photo">

												<%} %>
												</a>
												<div class="media-body">
													<span class="media-meta pull-right"><h4><iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>" ><%=good.getItemLocalPriceVatQty(request) %></iway:curr></h4></span>
													<h4 class="title">
														<iwcm:beanWrite name="good" property="title"/>
													</h4>
													<p class="summary">Počet kusov: <input type="text" class="basketQty" name="basketQty" maxlength="3" value="<iwcm:beanWrite name="good" property="itemQty"/>"> <%= good.getItemNote() %></p>
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
               <iway:curr currency="<%=EshopService.getDisplayCurrency(request) %>">
                 <%=EshopService.getTotalLocalPriceVat(basketItems,request)%>
               </iway:curr>
             </span></h3>
      </div>
      </div>

        <div class="row">




          <div style="text-align:right" class="pull-xs-4  col-xs-4  col-sm-12 col-md-12 ">
            <span id="orderButton">
              <a class="btn btn-success" href="/kosik/?basketAct=orderform">Dokončiť nákup</a>
            </span>
          </div>
        </div>
      </div>
		</div>
  <%} %>
