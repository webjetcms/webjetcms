<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_basket"/>

<%@ include file="/admin/layout_top.jsp" %>

<%@page import="java.util.List,sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB,java.util.Arrays,sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.Map,java.util.LinkedHashMap,java.util.Collections"%>

<%
	//priznak, ci ukazat iba produkty ponukane na predaj, alebo vsetky
	boolean showOnlyAvailable = request.getParameter("onlyAvailable") != null;
	boolean roundPrices = request.getParameter("roundPrices") != null;
	String currency = (request.getParameter("currency") == null) ? "none" : request.getParameter("currency");
	//budeme zobrazovat podla parovacieho cisla alebo podla docId?
	boolean displayByPartNo = !"".equals( Constants.getString("basketPartNoField") );

	//---------------------ZISKANIE ZOZNAMU PRODUKTOV-------------------------
	List<DocDetails> products = DocDB.getInstance().getItemsWithPrice(showOnlyAvailable);

	DocDB docDb = DocDB.getInstance();


	Collections.sort(products,DocDetails.getTitleComparator());
	request.setAttribute("products",products);

	//---------------------POVOLENE MENY--------------------------------
	List<String> supportedCurrencies = Arrays.asList( Constants.getString("supportedCurrencies").split(",") );
	Collections.sort(supportedCurrencies);
	Map<String,String> currencies = new LinkedHashMap<String,String>();

	for (String curr: supportedCurrencies)
		currencies.put(curr,curr);

	currencies.put("none", Prop.getInstance(request).getText("components.basket.price_list.database_state"));
%>
<script type="text/javascript">
<!--
	helpLink = "components/basket.jsp&book=components";
//-->
</script>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-basket-loaded"></i><iwcm:text key="components.basket.invoices"/><i class="ti ti-chevron-right"></i><iwcm:text key="components.basket.pricelist.dialog_title"/></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-bs-toggle="tab">
				<iwcm:text key="components.basket.pricelist.dialog_title"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">

			<form class="globalFilterForm" method="get" action="admin_pricelist.jsp">

				<div class="col-md-2 col-sm-6">
					<div class="form-group">
						<label for="lngSelectId" class="form-label"><iwcm:text key="components.basket.price_list.currency"/>:</label>
						<select id="lngSelectId" name="currency" class="select form-control" style="position: static;">
							<%
								for(String curr: currencies.keySet())
								{
							%>
									<option value="<%=curr %>" <%=(currency.equals(curr))?"selected":"" %>>
										<%=currencies.get(curr) %>
									</option>
							<%
								}
							%>
						</select>
					</div>
				</div>
				<div class="col-md-2 col-sm-6">
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<input type="checkbox" id="onlyAvailable" name="onlyAvailable" <%=showOnlyAvailable?"checked":"" %>/>
						<label for="onlyAvailable"><iwcm:text key="components.basket.price_list.only_available"/></label>
					</div>
				</div>
				<div class="col-md-2 col-sm-6">
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<input type="checkbox" id="roundPrices" name="roundPrices" <%=roundPrices?"checked":"" %>/>
						<label for="roundPrices"><iwcm:text key="components.basket.price_list.round_prices"/></label>
					</div>
				</div>

				<div class="col-md-2 col-sm-6">
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<input type="submit" value="<iwcm:text key="user.prop_search.search"/>" class="btn green" />
					</div>
				</div>



			</form>

		</div>
	</div>
</div>



<%
	if (products.size() >0)
	{
%>
		<display:table uid="product" name="products" class="sort_table" cellspacing="0" cellpadding="0" pagesize="30" export="true" style="width: 90%;">
			<display:setProperty name="export.excel.filename" value="pricelist.xls" />
			<display:setProperty name="export.csv.filename" value="pricelist.csv" />
			<display:setProperty name="export.xml.filename" value="pricelist.xml" />
			<display:setProperty name="export.pdf.filename" value="pricelist.pdf" />

			<%
				DocDetails doc = ((DocDetails)product);
				String curr = "none".equals(currency) ? doc.getCurrency() : currency;

				if (displayByPartNo)
				{
			%>
					<display:column titleKey="components.basket.price_list.part_number" sortable="true" property='<%=Constants.getString("basketPartNoField")%>'/>
			<%
				}
				else
				{
			%>
					<display:column titleKey="components.basket.price_list.map_number" property="docId" sortable="true"/>
			<%
				}
			%>

			<display:column titleKey="components.basket.price_list.product_title" sortable="true" property="title" />

			<display:column titleKey="components.basket.price_list.product_quantity" sortable="true" property="quantity" />

			<display:column titleKey="components.basket.price_list.price_without_vat" sortable="true" comparator="sk.iway.displaytag.NumberComparator" style="text-align: right;">
				<% if (roundPrices) { %><iway:curr round="<%=roundPrices %>"><%=doc.getLocalPrice(request,curr)%></iway:curr><% }
				else {
					out.print(Tools.replace(String.valueOf(doc.getLocalPrice(request,curr)), ".", ",") );
				}
				%>
			</display:column>

			<display:column titleKey="components.basket.price_list.currency" sortable="true" style="text-align: right;"><%=curr%></display:column>

			<display:column titleKey="components.basket.price_list.price_with_vat" sortable="true" comparator="sk.iway.displaytag.NumberComparator" style="text-align: right;">
				<% if (roundPrices) { %><iway:curr round="<%=roundPrices %>"><%=doc.getLocalPriceVat(request,curr)%></iway:curr><% }
				else {
					out.print(Tools.replace(String.valueOf(doc.getLocalPriceVat(request,curr)), ".", ",") );
				}
				%>
		    </display:column>

		    <display:column titleKey="components.basket.price_list.vat" style="text-align: right;">
		    	<iway:curr format="0"><%=doc.getVat()%></iway:curr>
		    </display:column>

		</display:table>
<%
	}
	else
	{
%>
		<iwcm:text key="components.basket.price_list.no_products"/>
<%
	}
%>

<%@ include file="/admin/layout_bottom.jsp" %>