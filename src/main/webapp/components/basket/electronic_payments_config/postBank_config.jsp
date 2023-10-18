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
page import="sk.iway.iwcm.system.ConfDB"%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
request.setAttribute("cmpName", "basket.editor.payments_setup");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %><%
//formularik uz bol odoslany
if (request.getParameter("mid")!=null)
{
	ConfDB.setName("basketPaymentPostBankMid",request.getParameter("mid"));
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
	window.resizeTo(400,350);

	function Ok()
	{
		if( checkForm.recheckAjax(document.bankForm) != false);
			document.bankForm.submit();
	}
</script>

<h2><iwcm:text key="components.basket.editor.payments_setup.title" /> <iwcm:text key="components.basket.order_form.postBank" /> </h2>

<body>
<form method="post" action="" name="bankForm">
	<table>
		<tr>
			<td>Merchant id:</td><td> <input type="text" size="30" class="required" name="mid" value="<%=Constants.getString("basketPaymentPostBankMid") %>"/></td>
		</tr>
	</table>
</form>
</body>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>