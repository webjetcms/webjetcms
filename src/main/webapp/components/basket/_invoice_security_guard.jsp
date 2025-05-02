<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.helpers.RequestHelper"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%
	BasketInvoiceEntity _invoice = (BasketInvoiceEntity)request.getAttribute("invoice");

	String _authKey2 = request.getParameter("auth");
	if (_authKey2 == null || _authKey2.equals(_invoice.getAuthorizationToken())==false)
	{
		UserDetails _user = new RequestHelper(request).getUser();
		if (_user == null) 
			return;
	
		if (!_user.isAdmin() && _invoice.getLoggedUserId() != _user.getUserId())
		{
			out.println(Prop.getInstance(request).getText("components.basket.invoice_detail.zadana_objednavka_neexistuje"));
			return;
		}
	}
%>