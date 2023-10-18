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
<iwcm:checkLogon admin="true" perms="cmp_basket"/><%@
page import="sk.iway.iwcm.system.ConfDB"%><%@page import="sk.iway.Password"%>
<%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
request.setAttribute("cmpName", "basket.editor.payments_setup");%>
<%@ include file="/admin/layout_top_dialog.jsp" %><%
//formularik uz bol odoslany
Password password = new Password();
if (request.getParameter("mid") != null && request.getParameter("key") != null && request.getParameter("url") != null)
{
	ConfDB.setName("basketPaymentVubEplatbyMid",request.getParameter("mid"));
	if(request.getParameter("encrypt")!=null)
		ConfDB.setName("basketPaymentVubEplatbyKey",password.encrypt(request.getParameter("key")));
	else
		ConfDB.setName("basketPaymentVubEplatbyKey",request.getParameter("key"));
	ConfDB.setName("basketPaymentVubEplatbyUrl",request.getParameter("url"));
	ConfDB.setName("basketPaymentVubEplatbyConstantSymbol",request.getParameter("constantSymbol"));
	ConfDB.setName("basketPaymentVubEplatbyNotificationEmail",request.getParameter("notificationEmail"));
	%>
		<body>Údaje úspešne uložené</body>
	<%@ include file="/admin/layout_bottom_dialog.jsp" %>
	<script type="text/javascript">
	function Ok(){
		window.opener.location.reload();
		setTimeout('window.opener.changeDisplayedDiv();', 1000);
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
		if( checkForm.recheckAjax(document.getElementById('vubBankForm')) != false)
			document.getElementById('vubBankForm').submit();
	}
</script>

<h2><iwcm:text key="components.basket.editor.payments_setup.title" /> <iwcm:text key="components.basket.order_form.vubEplatby" /> </h2>

<body>
<div class="tab-pane toggle_content tab-pane-fullheight tab-pane-single" style="max-height: 500px;<!-- height: 500px; -->">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<form method="post" action="" id="vubBankForm" name="vubBankForm">
			<table>
				<tr>
					<td>Merchant id:</td><td> <input type="text" size="30" class="required" name="mid" value="<%=Constants.getString("basketPaymentVubEplatbyMid") %>"/></td>
				</tr>
				<tr>
					<td>Privátny kľúč:</td>
					<td> 
						<input type="text" size="30" class="required" name="key" value="<%=Constants.getString("basketPaymentVubEplatbyKey") %>"/><br/>
					</td>
				</tr>
				<tr>
					<td></td>	
					<td>
						<input type="checkbox" size="30" name="encrypt" /> Šifrovať privátny kľúč (zaškrtnúť len pri zadávaní nového, alebo pri zmene) <br/><br/>
					</td>
				</tr>
				<tr>
					<td>URL Banky:</td><td><input type="text" size="30" class="required" name="url" value="<%=Constants.getString("basketPaymentVubEplatbyUrl") %>"/></td>
				</tr>
				<tr>
					<td>Konštantný symbol:</td><td> <input type="text" size="30" class="required numbers" name="constantSymbol" value="<%=Constants.getString("basketPaymentVubEplatbyConstantSymbol") %>"/></td>
				</tr>
				<tr>
					<td>Notifikačný mail:</td><td><input type="text" size="30" class="email" name="notificationEmail" value="<%=Constants.getString("basketPaymentVubEplatbyNotificationEmail") %>"/></td>
				</tr>
			</table>
		</form>
	</div>
</div>
</body>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>