<%@page import="java.util.List"%><%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
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
<%--
	Stranka sluziaca na zobrazenie produktov formatovane, strankovane, s linkami na blizsie informacie
	a ich cenou. V skutocnosti sa zobrazuju iba dokumenty, pretoze produkty su v systeme vedene ako specialne
	typy dokumentov (stranok).

	Pouziva

	DocDB, DocDetails
--%>

<%
	//komponenta nepodporuje packager ControlJS
	request.setAttribute("packagerEnableControljs", Boolean.FALSE);
	request.setAttribute("packagerMode", "none");


	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	String displayCurrency = EshopService.getDisplayCurrency(request);

	PageParams pageParams = new PageParams(request);

	String basketOrder = Tools.getStringValue(request.getParameter("basketAct"), "");
	boolean isbasketOrder = basketOrder.equals("orderform") || "saveorder".equals(request.getParameter("act"));

	String basketSmall = "!INCLUDE(/components/basket/basket_small_inline.jsp)!";
	String basket = "!INCLUDE(/components/basket/basket_inline.jsp)!";
	request.setAttribute("basketSmall", basketSmall);
	request.setAttribute("basket", basket);

	if (isbasketOrder) {
		  pageContext.include("/components/basket/order_form.jsp");
		  return;
		}

	String groupIds = pageParams.getValue("groupIds", "");
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

	<iwcm:write name="basket"/>
	<iwcm:write name="basketSmall"/>

<%= Tools.insertJQuery(request) %>
<script type="text/javascript">
 	<!--

 	$(document).ready(function(){
 		$('head').append('<style type="text/css" media="all">@import url("/components/basket/css/basket.css");</style>');
 	});

	function addToBasket(docId)
	{
		var qty = 1;
		var userNote = "";

		if (document.getElementById)
		{

			var qtyEl = $("#qty_"+docId);
			if ($("#qty_"+docId).length > 0)
				qty = qtyEl.val();

			if ($("#pozn_"+docId).length > 0)
				userNote = poznEl.val();
		}

		wjPopup("/components/basket/addbasket_popup.jsp?act=add&basketQty="+qty+"&basketItemId="+docId+"&basketUserNote="+userNote, 300, 150);
	}

	$(document).ready(function()
	{
		<%-- presmerje po vybere poradia zobrazovania--%>
		$(".filterKategorii").change(function(){
			window.location.replace('<iwcm:cp/><%=PathFilter.getOrigPath(request)+"?orderType="%>'+$(this).val());
		});
	});
   //-->
</script>

<script type="text/javascript">
$(document).ready(function(){
	$('head').append( $('<link rel="stylesheet" type="text/css" />').attr('href', '/components/_common/css/basket.css') );
});
</script>

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

	GroupsDB groupsDB = GroupsDB.getInstance();

	if (pageParams.getBooleanValue("showSort", true))
	{
	%>
	<div class="productsOrder">
		<%--Zoradit podla: --%>
		<iwcm:notEmpty name="novinky">
			<iwcm:text key="components.cloud.basket.orderBy"/>:
			<select name="orderType" class="filterKategorii">
				<option <%=("asc_price".equals(param_order_type))? "selected":""%> value="asc_price"><iwcm:text key="components.cloud.basket.orderBy.priceAsc"/></option>
				<option <%=("desc_price".equals(param_order_type))? "selected":""%> value="desc_price"><iwcm:text key="components.cloud.basket.orderBy.priceDesc"/></option>

				<option <%=("asc_date".equals(param_order_type))? "selected":""%> value="asc_date"><iwcm:text key="components.cloud.basket.orderBy.dateAsc"/></option>
				<option <%=("desc_date".equals(param_order_type))? "selected":""%> value="desc_date"><iwcm:text key="components.cloud.basket.orderBy.dateDesc"/></option>

				<option <%=("asc_title".equals(param_order_type))? "selected":""%> value="asc_title"><iwcm:text key="components.cloud.basket.orderBy.titleAsc"/></option>
				<option <%=("desc_title".equals(param_order_type))? "selected":""%> value="desc_title"><iwcm:text key="components.cloud.basket.orderBy.titleDesc"/></option>

				<option <%=("asc_priority".equals(param_order_type))? "selected":""%> value="asc_priority"><iwcm:text key="components.cloud.basket.orderBy.priorityAsc"/></option>
				<option <%=("desc_priority".equals(param_order_type))? "selected":""%> value="desc_priority"><iwcm:text key="components.cloud.basket.orderBy.priorityDesc"/></option>
			</select>
		</iwcm:notEmpty>
	</div>
	<%
	}

	int groupId = Tools.getIntValue(groupIds,-1);
	String helpGroupName="";
	List<GroupDetails> ibaPodPriecinky = new ArrayList<GroupDetails>();
	ibaPodPriecinky = groupsDB.getGroups(groupId);

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

	if (paging)
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

<div class="products">
	<iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
		<iwcm:notEmpty name="doc" property="perexImage">
			<div class="itemDiv" style="height:<%=pageParams.getIntValue("thumbHeight", 190)%>px;">
			<h2><a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>"><iwcm:beanWrite name="doc" property="title"/></a></h2>
				<a href="<%=docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)%>" class="info">
					<div class="itemImageDiv" style="height:<%=pageParams.getIntValue("thumbHeight", 190)%>px;"><img src="/thumb<jsp:getProperty name="doc" property="perexImageSmall"/>?w=<%=pageParams.getIntValue("thumbWidth", 190)%>&amp;h=<%=pageParams.getIntValue("thumbHeight", 190)%>&amp;ip=5" alt="" /></div>
					<%--<p>
						<jsp:getProperty name="doc" property="perexPre"/>
					</p>--%>
				</a>
				<%-- NASLEDUJUCE ZOBRAZENIE CENY A KOSIKA MA VYZNAM IBA AK MA PRODUKT NENULOVU CENU --%>
				<%
				if (doc.getLocalPrice(request).abs().compareTo(java.math.BigDecimal.valueOf(0)) > 0)
				{%>
					<div class="priceDiv">
						<a class="cart addToBasket itemId_<jsp:getProperty name="doc" property="docId"/>" href="javascript:void(0)"><img alt="" src="/components/basket/img/kosik.png" class="cart" /></a>
						<span class="cena"><iway:curr currency="<%=displayCurrency%>"><%=doc.getLocalPriceVat(request) %></iway:curr></span>
					</div><%
				}%>
			</div>
		</iwcm:notEmpty>
	</iwcm:iterate>
	<span class="cleaner"></span>
</div>
	<iwcm:empty name="novinky">
		<iwcm:text key="components.cloud.basket.category.empty"/>
	</iwcm:empty>
