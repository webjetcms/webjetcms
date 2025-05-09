<%@page import="java.util.List"%><%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>

<%@page import="sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%!
public class CustomComparatorAsc implements Comparator<DocDetails> {
   public int compare(DocDetails d1, DocDetails d2) {
       return Double.compare(Tools.getDoubleValue(d1.getPriceVat(), -1),Tools.getDoubleValue( d2.getPriceVat(),-1))    ;
   }
}
public class CustomComparatorDesc implements Comparator<DocDetails> {
   public int compare(DocDetails d1, DocDetails d2) {
       return Double.compare(Tools.getDoubleValue(d2.getPriceVat(), -1),Tools.getDoubleValue( d1.getPriceVat(),-1))    ;
   }
}
%>

<%
	String act = request.getParameter("basketAct");
	if (act == null) act = request.getParameter("act");
	if("orderform".equalsIgnoreCase(act) || "saveorder".equalsIgnoreCase(act))
	{
		pageContext.include("/components/basket/order_form.jsp");
        return;
	} else if("afterpay".equalsIgnoreCase(act)) {
		pageContext.include("/components/basket/order_payment_reply.jsp");
		return;
	}

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);

	String groupIds = pageParams.getValue("groupIds", (String)request.getAttribute("group_id"));
	String style = pageParams.getValue("style", "01");

	//mame to v takomto formate, takze to convertneme
	groupIds = groupIds.replace('+', ',');

	String perexGroup = pageParams.getValue("perexGroup", "");
	//mame to v takomto formate, takze to convertneme
	perexGroup = perexGroup.replace('+', ',');

	String param_order = (request.getParameter("orderType") != null)? request.getParameter("orderType") :null;
	String param_ascending = (param_order != null && param_order.startsWith("asc_"))? "true" :null;
	param_ascending = (param_order != null && param_order.startsWith("desc_"))? "false" :null;
	String param_order_type = "";


	if(param_order !=  null)
	{
		param_order_type = param_order;
		param_order = Tools.replace(param_order, "asc_", "");
		param_order = Tools.replace(param_order, "desc_", "");
	}

	int orderType = DocDB.ORDER_PRIORITY;
	String p_order = pageParams.getValue("orderType", "priority");
	p_order = (param_order != null )? param_order:p_order;
	if (p_order != null)
	{
		if (p_order.compareTo("date") == 0)
		{
			orderType = DocDB.ORDER_SAVE_DATE;
		}
		else if (p_order.compareTo("id") == 0)
		{
			orderType = DocDB.ORDER_ID;
		}
		else if (p_order.compareTo("priority") == 0)
		{
			orderType = DocDB.ORDER_PRIORITY;
		}
		else if (p_order.compareTo("title") == 0)
		{
			orderType = DocDB.ORDER_TITLE;
		}
		else if (p_order.compareTo("place") == 0)
		{
			orderType = DocDB.ORDER_PLACE;
		}
		else if (p_order.compareTo("eventDate") == 0)
		{
			orderType = DocDB.ORDER_EVENT_DATE;
		}
		else if (p_order.compareTo("saveDate") == 0)
		{
			orderType = DocDB.ORDER_SAVE_DATE;
		}
		else if (p_order.compareTo("price") == 0)
		{
			orderType = DocDB.ORDER_PRICE;
		}
	}

	boolean ascending = pageParams.getBooleanValue("asc", true);
	if(param_ascending != null && param_ascending.startsWith("false"))
	{
		ascending = false;
	}

	if (Tools.isEmpty(param_order_type))
	{
		if (ascending) param_order_type="asc_";
		else  param_order_type="desc_";

		param_order_type += p_order;
	}

	int publishType = DocDB.PUBLISH_NEW;
	String p_publish = pageParams.getValue("publishType", "new");
	if (p_publish!=null)
	{
		if (p_publish.compareToIgnoreCase("new")==0)
		{
			publishType = DocDB.PUBLISH_NEW;
		}
		else if (p_publish.compareToIgnoreCase("old")==0)
		{
			publishType = DocDB.PUBLISH_OLD;
		}
		else if (p_publish.compareToIgnoreCase("all")==0)
		{
			publishType = DocDB.PUBLISH_ALL;
		}
		else if (p_publish.compareToIgnoreCase("next")==0)
		{
			publishType = DocDB.PUBLISH_NEXT;
		}
	}

	boolean noPerexCheck = pageParams.getBooleanValue("noPerexCheck", true);
	if (noPerexCheck && publishType < 100)
	{
		publishType = publishType + 100;
	}

	boolean paging = pageParams.getBooleanValue("paging", false);
	int pageSize = pageParams.getIntValue("pageSize", 10);

	//ziskaj DocDB
	DocDB docDB = DocDB.getInstance();

	int actualDocId = Tools.getDocId(request);

	//vyradime zo zobrazenia aktualnu stranku + vyradime stranky s vlozenou komponentou
	String whereSql = " AND doc_id NOT IN ("+actualDocId+") AND (LENGTH(field_k) > 0 OR LENGTH(perex_image) > 0)";
	if (perexGroup != null && perexGroup.length()>0)
	{
		request.setAttribute("perexGroup", perexGroup);
	}
	else
	{
		request.removeAttribute("perexGroup");
	}
	request.setAttribute("whereSql", whereSql);

	//System.out.println("groupIds="+groupIds+" perexGroup="+perexGroup);

	docDB.getDocPerex(groupIds, orderType, ascending, publishType, pageSize, "novinky", "pages", request);

	int counter=0;
%>
<%= Tools.insertJQuery(request) %>
<iwcm:script type="text/javascript">
 	function addToBasket(docId)
 	{
 	   var qty = 1;
 	   var userNote = "";
 	   if (document.getElementById)
 	   {
 	    var qtyEl = document.getElementById("qty_"+docId);
 	    if (qtyEl != null) qty = qtyEl.value;
 	    var poznEl = document.getElementById("pozn_"+docId);
 	    if (poznEl != null) userNote = poznEl.value;
 	   }

		$.get( "/components/basket/addbasket_popup.jsp?act=add&basketQty="+qty+"&basketItemId="+docId+"&basketUserNote="+userNote, function( data ) {
			$(".basketPrice").html($(data).find('.cena').html());
			$(".basketSmallPrice").html("<span>"+$(data).find('.cena').html()+"</span>");

			$(".basketSmallItems").html($(data).find('.pocet').html());

			$(".basketSmallBox").show();
		});
 	}
	$(document).ready(function()
	{
		<%-- presmerje po vybere poradia zobrazovania--%>
		$(".filterKategorii").change(function(){
			window.location.replace('<iwcm:cp/><%=PathFilter.getOrigPath(request)+"?orderType="%>'+$(this).val());
		});

	    $("a.addToBasket").click(function()
	    {
			//if there is small basket, it will handle click event and add item to basket
			if ($(".basketBox").length > 0) return;

	    	var itemId = $(this).attr("data-itemid");
	    	addToBasket(itemId);
	    });
	});
</iwcm:script>


<%
	List<DocDetails> products = (List<DocDetails>)request.getAttribute("novinky");
	if("price".equals(p_order) && ascending)
	{
		// usporiadanie podla ceny Asc
		Collections.sort(products, new CustomComparatorAsc());
	}
	else if("price".equals(p_order) && !ascending)
	{
		// usporiadanie podla ceny Desc
		Collections.sort(products, new CustomComparatorDesc());
	}

	%>
		<%@ include file="/components/basket/navbar.jsp" %>

	<%
	if (pageParams.getBooleanValue("showSort", true) && pageParams.getValue("pagingPosition", "both").equals("both") || pageParams.getValue("pagingPosition", "both").equals("top"))
	{
session.setAttribute("testRun", pageParams.getBooleanValue("testRun", false));
session.setAttribute("katalogProduktov", pageParams.getBooleanValue("katalogProduktov", false));
session.setAttribute("overeneZakaznikmi", pageParams.getValue("overeneZakaznikmi", ""));

		String basketSmall ="";
		boolean showBasketSmall = false;
		String basketIncluded = (String)request.getAttribute("basketIncluded");

		if(basketIncluded==null || "false".equals(basketIncluded)){
			showBasketSmall = true;
		}else{showBasketSmall = false;}

		if(showBasketSmall){
			basketSmall = "!INCLUDE(/components/basket/basket_small.jsp)!";
		}

		request.setAttribute("basketSmall", basketSmall);
%>

<iwcm:link rel="stylesheet" type="text/css" href="/components/basket/css/basket.css"/>
<div class="row">
	<div class="col-sm-12 form-group"><iwcm:write name="basketSmall"/></div>
</div>

<div class="row">
	<div class="productsOrder bootstrap col-sm-12">
		<%--Zoradit podla: --%>
		<iwcm:notEmpty name="novinky">
			<iwcm:text key="components.basket.orderBy"/>:
			<select name="orderType" class="filterKategorii">
				<option <%=("asc_price".equals(param_order_type))? "selected":""%> value="asc_price"><iwcm:text key="components.basket.orderBy.priceAsc"/></option>
				<option <%=("desc_price".equals(param_order_type))? "selected":""%> value="desc_price"><iwcm:text key="components.basket.orderBy.priceDesc"/></option>

				<option <%=("asc_date".equals(param_order_type))? "selected":""%> value="asc_date"><iwcm:text key="components.basket.orderBy.dateAsc"/></option>
				<option <%=("desc_date".equals(param_order_type))? "selected":""%> value="desc_date"><iwcm:text key="components.basket.orderBy.dateDesc"/></option>

				<option <%=("asc_title".equals(param_order_type))? "selected":""%> value="asc_title"><iwcm:text key="components.basket.orderBy.titleAsc"/></option>
				<option <%=("desc_title".equals(param_order_type))? "selected":""%> value="desc_title"><iwcm:text key="components.basket.orderBy.titleDesc"/></option>

				<option <%=("asc_priority".equals(param_order_type))? "selected":""%> value="asc_priority"><iwcm:text key="components.basket.orderBy.priorityAsc"/></option>
				<option <%=("desc_priority".equals(param_order_type))? "selected":""%> value="desc_priority"><iwcm:text key="components.basket.orderBy.priorityDesc"/></option>
			</select>
		</iwcm:notEmpty>
	</div>
	<%
	}
	%>

	<%

	int groupId = Tools.getIntValue(groupIds,-1);
	String helpGroupName="";
	List<GroupDetails> ibaPodPriecinky = new ArrayList<GroupDetails>();
	ibaPodPriecinky = GroupsDB.getInstance().getGroups(groupId);
	int productCounter = 1;

	//Sub kategorie & rychle tipy

	if(ibaPodPriecinky.size() > 0 && pageParams.getBooleanValue("showCategory", true))
	{%>
	<div class="productsTags">
		<iwcm:text key="components.cloud.basket.category.sub"/>:
			<%for(GroupDetails podPriecinok:ibaPodPriecinky)
			{%>
				<a href="<%=DocDB.getURLFromDocId(podPriecinok.getDefaultDocId(), request)  %>"><%=podPriecinok.getGroupName()%></a>
			<%} %>
	</div><%
	}
	//nastavujeme bud z poslananeho parametra alebo defaultne nastavenie kopomponenty
	param_order = (request.getParameter("orderType") != null)?request.getParameter("orderType") : (ascending)?"asc_"+p_order:"desc_"+p_order;
	%>

	<%
	if ((paging && pageParams.getValue("pagingPosition", "both").equals("both") || pageParams.getValue("pagingPosition", "both").equals("top")) && GroupsDB.getInstance().getGroup(groupId)!=null)
	{
%>
	<!-- strankovanie (naraz sa zobrazi iba urceny pocet web stranok) -->
	<iwcm:present name="pages">
		<div class="paging" align="right"><iwcm:text key="calendar.page"/>:
			<iwcm:iterate id="page2" name="pages" type="sk.iway.iwcm.LabelValueDetails">
				<!-- vytvorenie odkazu <a href=... -->
				<jsp:getProperty name="page2" property="value"/>
				<!-- vlozenie cisla stranky a ukoncenie odkazu a -->
				[<jsp:getProperty name="page2" property="label"/>]<%if(page2.getValue().indexOf("<a")!=-1) out.print("</a>");%>&nbsp;
			</iwcm:iterate>
		</div>
		<hr style="color: #f0f0f0; border: 1px solid #f0f0f0;" />
	</iwcm:present>
	<!-- koniec strankovania -->
<%
	}

%>

</div>
<!-- NEW -->

<% if("01".equals(style)){ %>

<div>
    	<!-- BEGIN PRODUCTS -->
    	<div class="row basket style01">
    <iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">

  		<div class="col-md-3 col-sm-6">
    		<span class="thumbnail clearfix">
      			<%if(Tools.isNotEmpty(doc.getFieldQ())){
      			String styleAttr="";

      			if(Tools.isNotEmpty(doc.getFieldR())){
      				styleAttr+="color:"+doc.getFieldR()+"; ";
      			}
      			if(Tools.isNotEmpty(doc.getFieldS())){
      				styleAttr+="background-color:"+doc.getFieldS()+"; ";
      			}

      			%>
      			<div class="basket-label" style="<%=styleAttr%>"><%=doc.getFieldQ() %></div>
      			<%} %>
      			<a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>"><img src="/thumb<jsp:getProperty name="doc" property="perexImageSmall"/>?w=500&h=400&ip=5" alt="" /></a>
      			<h4><a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>"><bean:write name="doc" property="title"/></a></h4>
      			<div class="ratings">
			<%
				request.setAttribute("ratingForm", "!INCLUDE(/components/rating/rating_form.jsp, ratingDocId="+doc.getDocId()+", range=5)!");
			%>
				<iwcm:write name="ratingForm"/>
                </div>
                <p class="basketPerexText">
                <% if(Tools.isNotEmpty(doc.getPerex())) out.print(doc.getPerex()); %>
                </p>
                	<%
				if ( (doc.getPrice().abs().compareTo(java.math.BigDecimal.valueOf(0)) > 0) && (session.getAttribute("katalogProduktov")==null || !Boolean.valueOf(session.getAttribute("katalogProduktov").toString())))
				{%>
      			<hr class="line">

      			<div class="bottom">
      				<div class="col-price col-md-6 col-sm-6">
      					<p class="price"><span class="cenaOld"><iway:curr currency="<%= doc.getCurrency() %>"><%= doc.getFieldM() %></iway:curr></span><iway:curr currency="<%= doc.getCurrency() %>"><%=doc.getLocalPriceVat(request, doc.getCurrency()) %></iway:curr> </p>
      				</div>
      				<div class="col-addToBasket col-md-6 col-sm-6">
      					<a  class="btn btn-success right addToBasket itemId_<jsp:getProperty name="doc" property="docId"/>" data-itemid="<jsp:getProperty name="doc" property="docId"/>" ><iwcm:text key="components.basket.add_to_basket"/></a>
      				</div>

      			</div>
      			<% } %>
    		</span>
  			</div>

  		<% if((productCounter)%4 == 0){ %></div> <div class="row basket style01"><%}%>

  		 <%   productCounter++;%>

	</iwcm:iterate>

	</div>

	<iwcm:empty name="novinky">
		<iwcm:text key="components.basket.category.empty"/>
	</iwcm:empty>
  		<!-- END PRODUCTS -->
</div>
<%} else if("02".equals(style)){ %>

  <div class="row basket style02">
  <iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">

        <div class="col-md-4">
            <div class="product-item">
            	<%if(Tools.isNotEmpty(doc.getFieldQ())){
      			String styleAttr="";

      			if(Tools.isNotEmpty(doc.getFieldR())){
      				styleAttr+="color:"+doc.getFieldR()+"; ";
      			}
      			if(Tools.isNotEmpty(doc.getFieldS())){
      				styleAttr+="background-color:"+doc.getFieldS()+"; ";
      			}

      			%>
      			<div class="basket-label" style="<%=styleAttr%>"><%=doc.getFieldQ() %></div>
      			<%} %>
              <div class="pi-img-wrapper">

               <img style="width: 100%;" src="/thumb<jsp:getProperty name="doc" property="perexImageSmall"/>?w=<%=pageParams.getIntValue("thumbWidth", 500)%>&amp;h=<%=pageParams.getIntValue("thumbHeight", 400)%>&amp;ip=5" alt="" />
                <div>
                  <a href="shop-item.html"><a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>" class="btn">Zobraziť</a>
                </div>
              </div>
              <h3><a href="shop-item.html"><a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>"><bean:write name="doc" property="title"/></a></a></h3>
             <%  if ( (doc.getPrice().abs().compareTo(java.math.BigDecimal.valueOf(0)) > 0) && (session.getAttribute("katalogProduktov")==null || !Boolean.valueOf(session.getAttribute("katalogProduktov").toString())))
				{%>
              <div class="pi-price"><span class="cenaOld"><iway:curr currency="<%= doc.getCurrency() %>"><%= doc.getFieldM() %></iway:curr></span> <iway:curr currency="<%= doc.getCurrency() %>"><%=doc.getLocalPriceVat(request, doc.getCurrency()) %></iway:curr></div>
              <a class="btn add2cart addToBasket itemId_<jsp:getProperty name="doc" property="docId"/>" data-itemid="<jsp:getProperty name="doc" property="docId"/>"><iwcm:text key="components.basket.add_to_basket"/></a>

              <% } %>
            </div>
        </div>

  		<% if((productCounter)%3 == 0){ %></div> <div class="row basket style02"><%}%>

  		 <%   productCounter++;%>

	</iwcm:iterate>




</div>
<%} else if("03".equals(style)){

request.setAttribute("sideBasket", "!INCLUDE(/components/basket/basket.jsp, style=02)!");
%>
<div class="basket style03">
	<div class="col-sm-7">
    	<!-- BEGIN PRODUCTS -->
    	<div class="row">
    <iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">

  		<div class=" col-md-6 col-sm-6  col-xs-6 col-lg-4 ">
    		<span class="thumbnail">
      			<%if(Tools.isNotEmpty(doc.getFieldQ())){
      			String styleAttr="";

      			if(Tools.isNotEmpty(doc.getFieldR())){
      				styleAttr+="color:"+doc.getFieldR()+"; ";
      			}
      			if(Tools.isNotEmpty(doc.getFieldS())){
      				styleAttr+="background-color:"+doc.getFieldS()+"; ";
      			}

      			%>
      			<div class="basket-label" style="<%=styleAttr%>"><%=doc.getFieldQ() %></div>
      			<%} %>
      			<a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>"><img src="/thumb<jsp:getProperty name="doc" property="perexImageSmall"/>?w=500&h=400&ip=5" alt="" /></a>
      			<h4><a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>"><bean:write name="doc" property="title"/></a></h4>
      			<div class="ratings">
			<%
				request.setAttribute("ratingForm", "!INCLUDE(/components/rating/rating_form.jsp, ratingDocId="+doc.getDocId()+", range=5)!");
			%>
				<iwcm:write name="ratingForm"/>
                </div>
                <p class="basketPerexText">
                <% if(Tools.isNotEmpty(doc.getPerex())) out.print(doc.getPerex()); %>
                </p>
                	<%
				if ( (doc.getPrice().abs().compareTo(java.math.BigDecimal.valueOf(0)) > 0) && (session.getAttribute("katalogProduktov")==null || !Boolean.valueOf(session.getAttribute("katalogProduktov").toString())))
				{%>
      			<hr class="line">

      			<div class="row">
      				<div class="col-md-6 col-sm-6">
      					<p class="price"><span class="cenaOld"><iway:curr currency="<%= doc.getCurrency() %>"><%= doc.getFieldM() %></iway:curr></span><iway:curr currency="<%= doc.getCurrency() %>"><%=doc.getLocalPriceVat(request, doc.getCurrency()) %></iway:curr> </p>
      				</div>
      				<div class="col-md-6 col-sm-6">
      					<a  class="btn btn-success right addToBasket itemId_<jsp:getProperty name="doc" property="docId"/>" data-itemid="<jsp:getProperty name="doc" property="docId"/>"><span class="fa fa-shopping-cart" aria-hidden="true"></span></i>
</a>
      				</div>

      			</div>
      			<% } %>
    		</span>
  			</div>

  		<% if((productCounter)%3 == 0){ %></div> <div class="row"><%}%>

  		 <%   productCounter++;%>

	</iwcm:iterate>

	</div>

	<iwcm:empty name="novinky">
		<iwcm:text key="components.basket.category.empty"/>
	</iwcm:empty>
  		<!-- END PRODUCTS -->
</div>
<div class="col-sm-5 sideBasket">
				<iwcm:write name="sideBasket"/>
</div>
</div>


</div>
<%} %>

	<iwcm:empty name="novinky">
		<iwcm:text key="components.basket.category.empty"/>
	</iwcm:empty>

<%
	if ((pageParams.getBooleanValue("showSort", true) && pageParams.getValue("pagingPosition", "both").equals("both") || pageParams.getValue("pagingPosition", "both").equals("bottom")) && GroupsDB.getInstance().getGroup(groupId)!=null)
	{
session.setAttribute("testRun", pageParams.getBooleanValue("testRun", false));
session.setAttribute("katalogProduktov", pageParams.getBooleanValue("katalogProduktov", false));
session.setAttribute("overeneZakaznikmi", pageParams.getValue("overeneZakaznikmi", ""));
%>

	<div class="productsOrder">
		<%--Zoradit podla: --%>
		<iwcm:notEmpty name="novinky">
			<iwcm:text key="components.basket.orderBy"/>:
			<select name="orderType" class="filterKategorii">
				<option <%=("asc_price".equals(param_order_type))? "selected":""%> value="asc_price"><iwcm:text key="components.basket.orderBy.priceAsc"/></option>
				<option <%=("desc_price".equals(param_order_type))? "selected":""%> value="desc_price"><iwcm:text key="components.basket.orderBy.priceDesc"/></option>

				<option <%=("asc_date".equals(param_order_type))? "selected":""%> value="asc_date"><iwcm:text key="components.basket.orderBy.dateAsc"/></option>
				<option <%=("desc_date".equals(param_order_type))? "selected":""%> value="desc_date"><iwcm:text key="components.basket.orderBy.dateDesc"/></option>

				<option <%=("asc_title".equals(param_order_type))? "selected":""%> value="asc_title"><iwcm:text key="components.basket.orderBy.titleAsc"/></option>
				<option <%=("desc_title".equals(param_order_type))? "selected":""%> value="desc_title"><iwcm:text key="components.basket.orderBy.titleDesc"/></option>

				<option <%=("asc_priority".equals(param_order_type))? "selected":""%> value="asc_priority"><iwcm:text key="components.basket.orderBy.priorityAsc"/></option>
				<option <%=("desc_priority".equals(param_order_type))? "selected":""%> value="desc_priority"><iwcm:text key="components.basket.orderBy.priorityDesc"/></option>
			</select>
		</iwcm:notEmpty>
	</div>
	<%
	}

	if ((paging && pageParams.getValue("pagingPosition", "both").equals("both") || pageParams.getValue("pagingPosition", "both").equals("bottom")) && GroupsDB.getInstance().getGroup(groupId)!=null)
	{
%>
	<!-- strankovanie (naraz sa zobrazi iba urceny pocet web stranok) -->
	<iwcm:present name="pages">
		<div class="paging" align="right"><iwcm:text key="calendar.page"/>:
			<iwcm:iterate id="page2" name="pages" type="sk.iway.iwcm.LabelValueDetails">
				<!-- vytvorenie odkazu <a href=... -->
				<jsp:getProperty name="page2" property="value"/>
				<!-- vlozenie cisla stranky a ukoncenie odkazu a -->
				[<jsp:getProperty name="page2" property="label"/>]<%if(page2.getValue().indexOf("<a")!=-1) out.print("</a>");%>&nbsp;
			</iwcm:iterate>
		</div>
		<hr style="color: #f0f0f0; border: 1px solid #f0f0f0;" />
	</iwcm:present>
	<!-- koniec strankovania -->
<%
	}
%>

<%
WriteTag.setInlineComponentEditTextKey("components.basket.editSettingsAndItems", request);

WriteTag.hideInlineComponentEditButton(request);
WriteTag.addInlineButtonTop("components.basket.title", "/components/_common/admin/inline/toptoolbar/basket.png", "{inlineComponentEdit}", null, null, request);
%>