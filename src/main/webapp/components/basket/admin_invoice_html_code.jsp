<%@ page language="java" import="java.lang.*,java.util.*,sk.iway.iwcm.components.basket.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="cmp_basket"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
	BasketInvoiceBean bi = InvoiceDB.getInvoiceById(Integer.parseInt(request.getParameter("invoiceId")));
%>
<%=bi.getHtmlCode()%>
<%@ include file="/admin/layout_bottom.jsp" %>
