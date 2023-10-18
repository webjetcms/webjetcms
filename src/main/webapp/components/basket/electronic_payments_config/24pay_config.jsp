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
if (request.getParameter("eshopId") != null && request.getParameter("key") != null && request.getParameter("mid") != null)
{
	ConfDB.setName("24payEshopId",request.getParameter("eshopId"));
	ConfDB.setName("24payKey",request.getParameter("key"));
	ConfDB.setName("24payMid",request.getParameter("mid"));
	if(request.getParameter("testPayment") != null)
		ConfDB.setName("24payTestPaymentActive","true");
	else
		ConfDB.setName("24payTestPaymentActive","false");
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
		if( checkForm.recheckAjax(document.getElementById('24payForm')) != false)
			document.getElementById('24payForm').submit();
	}
	$(document).ready(function(){
		$('#testPaymentChckbx').change(function () {
			<%Pay24MerchantAccountBean pay24TestPayment = new Pay24MerchantAccountActionBean().getAccount(true);%>
			if($('#testPaymentChckbx').prop('checked'))
			{
				$('#24payForm [name="eshopId"]').val('<%=pay24TestPayment.getEshopId()%>');
				$('#24payForm [name="key"]').val('<%=pay24TestPayment.getKey()%>');
				$('#24payForm [name="mid"]').val('<%=pay24TestPayment.getMid()%>');
			}
			else
			{
				$('#24payForm [name="eshopId"]').val('');
				$('#24payForm [name="key"]').val('');
				$('#24payForm [name="mid"]').val('');
			}
		});
	});
</script>
<%
Pay24MerchantAccountBean pay24Payment = new Pay24MerchantAccountActionBean().getAccount();
%>
<%-- <h2><iwcm:text key="components.basket.editor.payments_setup.title" /> <iwcm:text key="components.basket.order_form.vubEplatby" /> </h2> --%>
<div class="tab-pane toggle_content tab-pane-fullheight tab-pane-single" style="max-height: 500px;<!-- height: 500px; -->">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<body>
		<form method="post" id="24payForm" action="" name="24pay">
			<table>
				<tr>
					<td>EshopId id:</td>
					<td> 
						<input type="text" size="30"  name="eshopId" value="<%=Tools.isEmpty(pay24Payment.getEshopId()) ? "" : pay24Payment.getEshopId()  %>"/>
					</td>
				</tr>
				<tr>
					<td>Bezpečnostný kľúč (key):</td>
					<td> 
						<input type="text" size="30"  name="key" value="<%=Tools.isEmpty(pay24Payment.getKey()) ? "" : pay24Payment.getKey()%>"/><br/>
					</td>
				</tr>
				<tr>
					<td>Mid:</td>	
					<td>
						<input type="text" size="30" name="mid"  value="<%=Tools.isEmpty(pay24Payment.getMid()) ? "" : pay24Payment.getMid() %>" /><br/>
					</td>
				</tr>
				<tr>
					<td>Testovacia prevádzka aktívna:</td><td><input id="testPaymentChckbx" type="checkbox" name="testPayment" <%if("11111111".equals(Constants.getString("24payEshopId"))) out.print("checked"); %>/></td>
				</tr>
				<!--<tr>
					<td>Konštantný symbol:</td><td> <input type="text" size="30" class="required numbers" name="constantSymbol" value="<%--=Constants.getString("basketPaymentVubEplatbyConstantSymbol") --%>"/></td>
				</tr>
				<tr>
					<td>Notifikačný mail:</td><td><input type="text" size="30" class="email" name="notificationEmail" value="<%--=Constants.getString("basketPaymentVubEplatbyNotificationEmail") --%>"/></td>
				</tr> -->
			</table>
		</form>
		</body>
	</div>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>