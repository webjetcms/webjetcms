<%@page import="sk.iway.cloud.payments.paypal.PayPalExpressCheckoutMerchantAccountActionBean"%>
<%@page import="sk.iway.cloud.payments.paypal.PayPalExpressCheckoutMerchantAccountBean"%>
<%@page import="sk.iway.cloud.payments.pay24.Pay24MerchantAccountActionBean"%>
<%@page import="sk.iway.cloud.payments.pay24.Pay24MerchantAccountBean"%>
<%@page import="sk.iway.iwcm.system.ConfDB"%><%
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
<iwcm:checkLogon admin="true" perms="cmp_basket"/><%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
request.setAttribute("cmpName", "basket.editor.payments_setup");%><%@
include file="/admin/layout_top_dialog.jsp" %><%
if (request.getParameter("clientId") != null && request.getParameter("secret") != null)
{
	if(InitServlet.isTypeCloud())
	{
		//inicializuje sa podla hodnot z DB
		PayPalExpressCheckoutMerchantAccountBean byDomain = new PayPalExpressCheckoutMerchantAccountActionBean().getAccount();
		//a nasetuje poslane hodnoty
		byDomain.setClientId(Tools.getParameter(request, "clientId"));
		byDomain.setSecret(Tools.getParameter(request, "secret"));
		byDomain.setDomainId(sk.iway.iwcm.common.CloudToolsForCore.getDomainId());

		byDomain.save();
	}
	else
	{
		ConfDB.setName("PayPalExChClientId",request.getParameter("clientId"));
		ConfDB.setName("PayPalExChSecret",request.getParameter("secret"));
	}
	%>
		<h1 style="color:white;padding:15px;"><iwcm:text key="components.basket.editor.saved"/></h1>
	<%@ include file="/admin/layout_bottom_dialog.jsp" %>
	<script type="text/javascript">
	function Ok(){
		window.opener.location.reload();
		setTimeout('window.close();', 1000);
	}
	</script>
<%
	return;
}
%>
<script src="/components/form/check_form.js" type="text/javascript"></script>
<%=Tools.insertJQuery(request) %>

<script type="text/javascript">
	window.resizeTo(400,430);

	function Ok()
	{
		if( checkForm.recheckAjax(document.getElementById('paypalExChForm')) != false)
			document.getElementById('paypalExChForm').submit();
	}
</script>
<%
PayPalExpressCheckoutMerchantAccountBean paypalExChPayment = new PayPalExpressCheckoutMerchantAccountActionBean().getAccount();
%>
<div class="tab-pane toggle_content tab-pane-fullheight tab-pane-single" style="max-height: 500px;<!-- height: 500px; -->">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<body>
		<form method="post" id="paypalExChForm" action="" name="24pay">
			<table>
				<tr>
					<td>Client ID:</td>
					<td>
						<input type="text" size="30"  name="clientId" value="<%=Tools.isEmpty(paypalExChPayment.getClientId()) ? "" : paypalExChPayment.getClientId() %>"/>
					</td>
				</tr>
				<tr>
					<td>Secret :</td>
					<td>
						<input type="text" size="30"  name="secret" value="<%=Tools.isEmpty(paypalExChPayment.getSecret()) ? "" : paypalExChPayment.getSecret()%>"/><br/>
					</td>
				</tr>
			</table>
		</form>
		</body>
	</div>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>