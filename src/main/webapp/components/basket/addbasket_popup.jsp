<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	
	int itemId = Tools.getIntValue(request.getParameter("basketItemId"), -1);
	if (itemId == -1)
		return;
	DocDB docDB = DocDB.getInstance();
	DocDetails good = docDB.getDoc(itemId);
	request.setAttribute("good", good);
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<title><bean:write name="good" property="title"/></title>
	<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" >
	<meta http-equiv="Content-language" content="<%=PageLng.getUserLng(request)%>" >
	<meta name="description" content="WebJET Content Management web site" >
	<meta name="author" content="Interway, s.r.o." >
	<link rel="stylesheet" href="/css/page.css" type="text/css" >
	<link rel="stylesheet" href="/css/print.css" type="text/css" media="print">
	<%=Tools.insertJQuery(request) %>
	<script type="text/javascript" src="/jscripts/common.js"></script>
	<script type="text/javascript" src="/components/basket/jscript.jsp"></script>
</head>

<body id="addBasketPopup">

<%
	if ("add".equals(request.getParameter("act")))
	{
		boolean ok = BasketDB.setItemFromDoc(request);
		
		if (ok)
		{
		   List<BasketItemBean> items = BasketDB.getBasketItems(request);
			%>
				<iwcm:text key="components.basket.addbasket_popup.product_added"/>
	
				<br /><br />
				<span id='basketSmallItems'>
					<iwcm:text key="components.basket.total_items"/>: <span class="pocet"><%=BasketDB.getTotalItems(items)%></span>
				</span>
				<span id='basketSmallPrice'>
					<iwcm:text key="components.basket.total_price"/>: 
					<span class="cena">
						<iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>">
							<%=BasketDB.getTotalLocalPriceVat(items,request)%>
						</iway:curr>
					</span>
				</span>
				
				<script type="text/javascript">
				<!--
					setTimeout("window.close();", 1000);
					//prepocitaj hodnoty v parent okne
					writeHtmlParent("basketSmallItems", getHtml("basketSmallItems"));
					writeHtmlParent("basketSmallPrice", getHtml("basketSmallPrice"));
				//-->
				</script>
			<%
		}
		else
		{
		%>
			<iwcm:text key="components.basket.addbasket_popup.canot_add_to_basket"/>
		<%
		}
		out.println("</body></html>");
		return;
	}
%>

<script type="text/javascript">
<!--
var lastSelectedIndex;
lastSelectedIndex = 0;

function recalculateTotal()
{
	qty = document.addBasketForm.basketQty.value;

	if (qty<0)
	{
		myValue = window.prompt("<iwcm:text key='components.basket.addbasket_popup.count_of_pieces'/>", "30")
		if (myValue!="" && myValue!="null" && myValue!=null && !isNaN(myValue))
		{
			var optionName = new Option(myValue, myValue, true, true)
			document.addBasketForm.basketQty.options[document.addBasketForm.basketQty.length] = optionName;
			qty = myValue;
		}
		else
		{
			document.addBasketForm.basketQty.selectedIndex=lastSelectedIndex;
			qty = document.addBasketForm.basketQty.value;
			window.alert("<iwcm:text key='components.basket.addbasket_popup.you_must_enter_number'/>");
		}
	}

	lastSelectedIndex = document.addBasketForm.basketQty.selectedIndex;
	price = document.addBasketForm.price.value;
	total = qty * price;
	writeHtml("tdTotal", "<b>"+formatCurrency(total)+"</b>");

}
//-->
</script>

<table border=0 cellspacing=0 cellpadding=0 height="100%" width="100%">
<tr>
	<td valign="top">
		<table border=0 cellspacing=0 cellpadding=2>
			<tr>
				<td valign="top" class="addbasketName"><bean:write name="good" property="title"/></td>
			</tr>
		</table>
	</td>
</tr>
<tr>
	<td valign="bottom">

        <form action="addbasket_popup.jsp" name="addBasketForm" style="margin: 0px;">
		<table border="0" cellspacing="0" cellpadding="2" width="100%">		
			<tr>
				<td><iwcm:text key="components.basket.addbasket_popup.price"/>:</td>
				<td width="70%" nowrap><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=good.getLocalPriceVat(request) %></iway:curr></td>
				<td valign="top" rowspan=2 align="right">
					<input type="submit" id="bSubmit" name="bSubmit" value="<iwcm:text key="components.basket.addbasket_popup.add"/>">
					<input type="hidden" name="basketItemId" value="<%=itemId%>" />
					<input type="hidden" name="price" value="<%=good.getLocalPriceVat(request) %>" />
					<input type="hidden" name="act" value="add" />
				</td>
			</tr>
			<tr>
				<td nowrap><iwcm:text key="components.basket.addbasket_popup.total_pieces"/>: </td>
				<td><select name="basketQty" onchange="recalculateTotal();">
						 <option value="1">1</option>
						 <option value="2">2</option>
						 <option value="3">3</option>
						 <option value="4">4</option>
						 <option value="5">5</option>
						 <option value="6">6</option>
						 <option value="7">7</option>
						 <option value="8">8</option>
						 <option value="9">9</option>
						 <option value="10">10</option>
						 <option value="15">15</option>
						 <option value="20">20</option>
						 <option value="-1"><iwcm:text key="components.basket.different"/></option>
					 </select>
				</td>
			</tr>			
			<tr>
				<td><iwcm:text key="components.basket.note"/>:</td>
				<td colspan=2><input type="text" name="basketUserNote" size="20" maxlength="255"/></td>
			</tr>
			<tr>
				<td><iwcm:text key="components.basket.addbasket_popup.all"/>:</td>
				<td id="tdTotal" colspan=2><b><iway:curr currency="<%=BasketDB.getDisplayCurrency(request) %>"><%=good.getLocalPriceVat(request) %></iway:curr></b></td>
			</tr>		
		</table>
		</form>
		<script type="text/javascript">
		<!--
			lastSelectedIndex = document.addBasketForm.basketQty.selectedIndex;
		//-->
		</script>
	</td>
</tr>
</table>

</body>
</html>