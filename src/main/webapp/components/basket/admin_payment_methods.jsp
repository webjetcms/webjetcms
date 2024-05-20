<%@page import="java.util.Collections"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.ElectronicPayments"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.ebanking.epayments.PaymentType"%>
<%@page import="java.util.List"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_basket"/>
<%@ include file="/admin/layout_top.jsp" %>
<script type="text/javascript">
<!--
	helpLink = "components/basket.jsp&book=components";
//-->
</script>
<%

%>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-basket-loaded"></i><iwcm:text key="components.basket.invoices"/><i class="ti ti-chevron-right"></i><iwcm:text key="components.basket.admin_invoices_list.sposob_platby"/></h1>
</div>

<style>
	table{
		margin: 1rem;
		width: 90%;
		text-align: left;
	}
	table tr td:first-child{
		width: 350px;
	}
	table tr td{
		padding: 1rem;
	}
	table tr{
		border-bottom: 1px solid #dadada;
	}
	table tr:last-child{
		border-bottom: none;
	}
</style>

<table>
<%  
	List<PaymentType> paymentMethods = new ArrayList(ElectronicPayments.getKnownPaymentMethods());
	Collections.sort(paymentMethods);
	for( PaymentType paymentType: paymentMethods ) {
	if ( ElectronicPayments.isPaymentMethodConfigured( paymentType ) ){	%>
		<tr><td><%=paymentType.toString() %></td>
		<%
		}else{%>
			<tr><td>Služba <%=paymentType.toString() %> nie je nakonfigurovaná</td>
		<%} %>
			<td><input type="button" class="btn green" value="Nastav" onclick="window.open('/components/basket/electronic_payments_config/<%=paymentType.getEditorName() %>_config.jsp',null,'scrollbars=1,resizable=1,location=0,menubar=0,status=1,toolbar=0',true)" /></td> 
		</tr>
		
<%} %>
</table>
<%@ include file="/admin/layout_bottom.jsp" %>