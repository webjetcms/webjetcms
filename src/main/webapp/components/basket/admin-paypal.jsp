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
<style type="text/css">
	body { background-image: none !important; background-color: #eceef0 !important; }
</style>
<h3><iwcm:text key="components.basket.paypal.title"/></h3>

<%
//formular na nastavenie (editaciu) parametrov pre paypal
if(request.getParameter("save")!= null)
{
	pageContext.include("/sk/iway/payments/paypal/PayPalMerchantAccount.action");
}
%>
<c:if test="${paypalSaveok}">
	<p><iwcm:text key="components.basket.paypal.saveok"/></p>
<!-- 	TODO: add button to hide message -->
</c:if>
<script type="text/javascript" src="/components/form/check_form.js"></script>
<stripes:useActionBean beanclass="sk.iway.cloud.payments.paypal.PayPalMerchantAccountActionBean" var="ab" />

<p>&nbsp;</p>
<p id="help">
	<iwcm:text key="components.basket.paypal.help"/>
</p>

<iwcm:menu notName="anyPremiumService">
		<jsp:include page="/components/_common/admin/premium_required.jsp"></jsp:include>
</iwcm:menu>

<iwcm:stripForm action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.cloud.payments.paypal.PayPalMerchantAccountActionBean" method="post" >
	<stripes:errors />
	<stripes:hidden name="account.id"/>
	
	<table>
		<tr>
		 <td class="tdLabel"><iwcm:text key="components.basket.paypal.user"/>:</td>
	     <td><stripes:text name="account.user" size="40" class="required text"/> </td>
		</tr>
		<tr>
		 <td class="tdLabel"><iwcm:text key="components.basket.paypal.pwd"/>:</td>
	     <td><stripes:text name="account.pwd" size="40" class="required text"/> </td>
		</tr>
		<tr>
		 <td class="tdLabel"><iwcm:text key="components.basket.paypal.signature"/>:</td>
	     <td><stripes:text name="account.signature" size="40" class="required text"/> </td>
		</tr>
	</table>
	
	<iwcm:menu name="anyPremiumService">
		<input type="submit" name="save" value="<iwcm:text key="button.save"/>" class="button50" />
	</iwcm:menu>	
			
</iwcm:stripForm>


<%@ include file="/admin/layout_bottom.jsp" %>