<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.components.basket.InvoiceDB"%>
<%@page import="sk.iway.iwcm.components.basket.BasketItemBean"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.ElectronicPayments"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoicePaymentDB"%>
<%@page import="java.math.BigDecimal"%>
<%
	//Zoznam objednavok
	
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	
	PageParams pageParams = new PageParams(request);
	
	session.setAttribute("returnEmail", pageParams.getValue("notifyEmail", "info@"+Tools.getServerName(request)) );
	session.setAttribute("notifyEmail", pageParams.getValue("notifyEmail", "info@"+Tools.getServerName(request)) );
	
	Prop prop = Prop.getInstance(lng);
	
	//zistim, ci su povolene ciastkove platby
	boolean allowPartialPayments = pageParams.getBooleanValue("allowPartialPayments", false);
	//ak niesu, odstranim zo session poslednu ciastkovu platbu
	if(allowPartialPayments == false && session.getAttribute("partialPaymentPrice") != null && request.getParameter("partialPaymentPrice") == null)
		session.removeAttribute("partialPaymentPrice");
	
	Identity user = UsersDB.getCurrentUser(session);
	if (user == null)
	{
		%><iwcm:text key="error.userNotLogged"/><%
		return;
	}
	
	if(request.getParameter("action") != null)
	{
		if("repay".equals(request.getParameter("action")))
		{
			pageContext.include("invoice_repay.jsp");
			return;
		}
		else if("repayaction".equals(request.getParameter("action")))
		{
			//ziskaj invoice
			int invoiceId = Tools.getIntValue(request.getParameter("invoiceId"), -1);
			if (invoiceId < 1) return;
	
			BasketInvoiceBean invoice = InvoiceDB.getInvoiceById(invoiceId);
			request.setAttribute("invoice", invoice);
			%><%@include file="/components/basket/_invoice_security_guard.jsp" %><%
			
			if (invoice == null)
			{
				out.println(prop.getText("components.basket.nastala_chyba_ziskania_objednavky"));
				return;
			}
			
			List<BasketItemBean> basketItems = invoice.getBasketItems();
			if (basketItems.size() > 0)
				request.setAttribute("basketItems", basketItems);
			List<String> displayedPaymentMethods = new ArrayList<String>( Arrays.asList(pageParams.getValue("displayedPayments","").split(",")) );
			
			while (displayedPaymentMethods.contains(""))
				displayedPaymentMethods.remove("");
			
			Set<String> paymentMethodsWithSubpages = new HashSet<String>();
			paymentMethodsWithSubpages.add("money_transfer");
			paymentMethodsWithSubpages.addAll( ElectronicPayments.getSupportedPaymentMethodsToBasketString() );
	
			String thanksUrl = pageParams.getValue("thanksUrl", null);
			//zaregistrujeme objednavku do requestu, prijimacie stranky sa to nemaju odkial dozvediet
			session.setAttribute("invoice", invoice);
			
			//ak ma sposob platby nejaku svoju vlastnu stranku, tak ho tam redirectni
			//--------------------------REDIRECT NA PLATBY--------------------------------
			String paymentMethod = request.getParameter("paymentMethod");
			
			//dame do session ciastkovu platbu
			if(request.getParameter("partialPaymentPrice") != null)
			{
				session.setAttribute("partialPaymentPrice", request.getParameter("partialPaymentPrice"));
			}
			
			if ( paymentMethodsWithSubpages.contains(paymentMethod) )
			{
				request.setAttribute("moneyTransferAccount",pageParams.getValue("moneyTransferAccount",""));
				request.setAttribute("moneyTransferNote",pageParams.getValue("moneyTransferNote",""));
				
				if (ElectronicPayments.getSupportedPaymentMethodsToBasketString().contains(paymentMethod))
				{
					request.setAttribute("paymentMethod",paymentMethod);
					paymentMethod = "bank";
				}
				
				pageContext.include("order_payment_"+paymentMethod+".jsp");
			}
			
			if (Tools.isNotEmpty(thanksUrl))
			{
				%>
				
				<script type="text/javascript">
				<!--
					window.location.href="<%=thanksUrl%>";
				//-->
				</script>
			<%
			}
			return;
		}
	}
	else if(request.getParameter("invoiceId") != null)
	{
		pageContext.include("invoice_detail.jsp");
		return;
	}
	
	List<BasketInvoiceBean> invoices = InvoiceDB.getInvoices(user.getUserId());
	if (invoices.size()>0) 
		request.setAttribute("invoices", invoices);
%>
<logic:notPresent name="invoices">
   <iwcm:text key="components.basket.invoices_list.este_ste_nevytvorili"/>
</logic:notPresent>

<logic:present name="invoices">   
	
	<table class="tabulkaStandard invoicesTable" cellspacing=0 cellpadding=0 width="100%">
   	<tr class="rezervacieHlavicka">
   	   <td><iwcm:text key="components.basket.admin_invoices_list.datum"/></td>
   	   <td align="center"><iwcm:text key="components.basket.invoices_list.cislo_objednavky"/></td>
   	   <td align="center"><iwcm:text key="components.basket.invoices_list.pocet_poloziek"/></td>
   	   <td align="center"><iwcm:text key="components.basket.admin_invoices_list.cena"/></td>
   	    <td align="center"><iwcm:text key="components.basket.invoices_list.uhradene"/></td>
   	   <td><iwcm:text key="components.basket.invoice_detail.stav"/></td>
   	   <td>&nbsp;</td>
   	   <td>&nbsp;</td>
   	</tr>
      <logic:iterate id="inv" name="invoices" type="sk.iway.iwcm.components.basket.BasketInvoiceBean" indexId="index">      
	      <tr<%if (index.intValue()==0) out.print(" class='destinacia'");%>>         
	         <td><%=Tools.formatDate(inv.getCreateDate())%></td>
	   	   <td align="center"><bean:write name="inv" property="basketInvoiceId"/></td>
	   	   <td align="center"><bean:write name="inv" property="totalItems"/></td>
	   	   <td align="right" nowrap="nowrap"><iway:curr><%=inv.getTotalPriceVat() %></iway:curr> <%=inv.getCurrency() %></td>
	   	   <td align="right" nowrap="nowrap">
	   	   <%BigDecimal uhradene = BasketInvoicePaymentDB.getPaymentsSum(inv.getBasketInvoiceId());  %>
	   	   <iway:curr><%=uhradene%></iway:curr> <%=inv.getCurrency()%>
	   	   </td>
	   	   <td><iwcm:text key='<%=("components.basket.invoice.status."+inv.getStatusId().intValue())%>'/></td>
	   	   <td>
	   	      <% if ((inv.getStatusId()==null || inv.getStatusId().intValue()==1 || inv.getStatusId().intValue()==4) && "cash_on_delivery".equals(inv.getPaymentMethod()) == false) { %>
	   	      <a class="hhButton" href="?action=repay&invoiceId=${inv.basketInvoiceId}"><iwcm:text key="components.basket.invoices_list.platba"/></a>
	   	      <% } else { out.print("&nbsp;"); } %>
	   	   </td>
	   	   <td><a class="hhButton" href="?invoiceId=${inv.basketInvoiceId}"><iwcm:text key="components.basket.invoices_list.zobrazit"/></a></td>
	      </tr>
      </logic:iterate>
   </table>
   
</logic:present>