<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.components.basket.*, sk.iway.iwcm.users.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<iwcm:checkLogon admin="true" perms="cmp_basket"/>

<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%@ include file="/admin/layout_top.jsp" %>

<%
	//presmeruje na /components/INSTALL_NAME/basket/admin_invoices_list.jsp, ak existuje
	String pageURL = "/components/basket/admin_invoices_list.jsp";
	String nahrada = WriteTag.getCustomPage(pageURL, request);
	if (pageURL.equals(nahrada) == false)
	{
		pageContext.include(nahrada);
		return;
	}
%>

<script type="text/javascript">
<!--
	helpLink = "components/basket.jsp&book=components";
//-->
</script>

<div class="row title">
	<h1 class="page-title"><i class="fa icon-basket-loaded"></i><iwcm:text key="components.basket.invoices"/><i class="fa fa-angle-right"></i><iwcm:text key="components.basket.invoices_header" /></h1>
</div>

<%
	//ADMIN - zoznam objednavok
	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user == null)
		return;
	UserDetails ud = null;

	boolean somCestovka = "C".equals(Constants.getString("wjVersion"));

	if("delete".equals(request.getParameter("act")) && request.getParameter("id") != null)
		InvoiceDB.deleteInvoice(Tools.getIntValue(request.getParameter("id"),0));

	int userId = Tools.getIntValue(request.getParameter("userId"), 0);
	if(userId > 0)
		ud = UsersDB.getUser(userId);

	int defaultStatusId = 1;
	if (somCestovka) defaultStatusId = -1;

	int statusId = Tools.getIntValue(request.getParameter("statusId"), defaultStatusId);

	String name = request.getParameter("name");
	if (name == null)
		name = "";

	String email=request.getParameter("email");

	if (email == null)
		email = "";

	String basketInvoiceId = Tools.getStringValue(request.getParameter("basketInvoiceId"),"");

	List<BasketInvoiceBean> invoices = InvoiceDB.searchInvoices(userId, statusId, name, email, Tools.getIntValue(basketInvoiceId,-999));
	if (invoices.size() > 0)
		request.setAttribute("invoices", invoices);

	BasketInvoiceBean i;
%>

<script type="text/javascript">
<!--
	function deleteInvoice(id)
	{
		if(window.confirm('<iwcm:text key="components.basket.invoice.delete_prompt"/>') == true)
		{
			document.actionForm.act.value="delete";
			document.actionForm.id.value = id;
			document.actionForm.submit();
		}
	}
//-->
</script>
<%--
<div class="box_tab box_tab_thin">
	<ul class="tab_menu">
		<li class="first openOnlyFirst"><a class="activeTab" href="#" id="tabLink1" onclick="showHideTab('1');"><iwcm:text key="components.filter" /></a></li>
	</ul>
</div>
 --%>
 <div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.filter" />
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">
			<form action="admin_invoices_list.jsp">
				<input type="hidden" name="userId" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("userId"))%>" />
					<div class="col-md-2 col-sm-6">
	                   	<div class="form-group">
							<label for="statusId1"><iwcm:text key="components.basket.invoice.state" />:</label>
							<select name="statusId" class="form-control" id="statusId1">
								<%
									if (somCestovka)
									{
								%>
								<option value="<%=statusId%>"><iwcm:text key='<%=("components.basket.invoice.status."+statusId)%>'/></option>
								<option value="-1"><iwcm:text key="components.basket.invoice.status.0"/></option>
								<option value="<%=statusId%>">---</option>
								<%
										for(int idStavu : StavyObjednavok.mozneStavy())
										{
								%>
											<option value="<%=idStavu%>"><%=StavyObjednavok.nazovPre(idStavu) %></option>
								<%
										}
									}
									else
									{
								%>
										<option value="<%=statusId%>"><iwcm:text key='<%=("components.basket.invoice.status."+statusId)%>'/></option>
										<option value="<%=statusId%>">---</option>
										<option value="0"><iwcm:text key="components.basket.invoice.status.0"/></option>
										<option value="1"><iwcm:text key="components.basket.invoice.status.1"/></option>
										<option value="2"><iwcm:text key="components.basket.invoice.status.2"/></option>
										<option value="4"><iwcm:text key="components.basket.invoice.status.4"/></option>
										<option value="3"><iwcm:text key="components.basket.invoice.status.3"/></option>
										<option value="<%=BasketInvoiceBean.INVOICE_STATUS_ISSUED%>"><iwcm:text key="components.basket.invoice.status.5"/></option>
								<%
									}
								%>
							</select>
						</div>
					</div>
					<div class="col-md-2 col-sm-6">
                   		<div class="form-group">
                   			<label for="nameId"><iwcm:text key="components.basket.invoice.name" />:</label>
							<input type="text" name="name" id="nameId" size="15" maxlength="128" value="<%=name%>" class="form-control"/>
						</div>
					</div>
					<div class="col-md-2 col-sm-6">
	                   	<div class="form-group">
							<label for="emailId"><iwcm:text key="components.basket.invoice.email" />:</label>
							<input type="text" name="email" id="emailId" size="15" maxlength="128" value="<%=email%>" class="form-control"/>
						</div>
					</div>
					<div class="col-md-2 col-sm-6">
                   		<div class="form-group">
                   			<label for="basketInvoiceId1"><iwcm:text key="components.basket.invoice.number" />:</label>
							<input type="text" name="basketInvoiceId" id="basketInvoiceId1" size="15" maxlength="15" value="<%=basketInvoiceId%>" class="form-control"/>
						</div>
					</div>
					<div class="col-md-2 col-sm-6">
	                   	<div class="form-group">
	                   		<label class="control-label display-block">&nbsp;</label>
	                   		<input type="submit" value="<iwcm:text key="components.basket.invoice.search"/>" class="button50">
	                   	</div>
					</div>
			</form>
		</div>
	</div>
</div>



<%if(ud!=null){%>
	<strong><iwcm:text key="components.basket.invoice.user" />:</strong> <%=ud.getFullName()%><br/><br/>
<%}%>

<display:table uid="row" name="invoices" export="true" class="sort_table" cellspacing="0" cellpadding="0" pagesize="10" excludedParams="act id"><%i=(BasketInvoiceBean)row;%>
	<display:column title="#"><%=pageContext.getAttribute("row_rowNum")%></display:column>
	<display:column titleKey="components.basket.invoice.number" sortable="true" href="javascript:popup2(900,690,'admin_invoice_detail.jsp" paramId="invoiceId" paramProperty="basketInvoiceId" property="basketInvoiceId"/>

	<display:column titleKey="editor_dir.tools" class="sort_td" headerClass="sort_thead_td" sortable="false">
		<a href="javascript:popup2(900,690,'admin_invoice_detail.jsp?invoiceId=<bean:write name="row" property="basketInvoiceId" />');" title='<iwcm:text key="components.basket.invoice.view_detail"/>' class="iconEdit">
			&nbsp;
		</a>

		<%
			if(3==((BasketInvoiceBean)row).getStatusId().intValue())
			{
		%>
				<a href="javascript:deleteInvoice(<bean:write name="row" property="basketInvoiceId" />)" title="<iwcm:text key="components.basket.invoice.delete"/>" class="iconDelete" >&nbsp;</a>
		<%
			}
			else
				out.print("&nbsp;");
		if(((BasketInvoiceBean)row).getHtmlCode() != null)
		{
	%>
			<iwcm:text key="components.basket.invoice.view_detail"/>
	<%
		}
		%>
	</display:column>

	<display:column titleKey="components.basket.invoice.date" sortable="true" href="javascript:popup2(900,690,'admin_invoice_detail.jsp" paramId="invoiceId" paramProperty="basketInvoiceId" decorator="sk.iway.displaytag.DateTimeDecorator" property="createDate"/>

	<display:column titleKey="components.basket.invoice.state" sortable="true">
		<iwcm:text key='<%=("components.basket.invoice.status."+i.getStatusId().intValue())%>'/>
	</display:column>

	<display:column titleKey="components.basket.invoice.payment_method" sortable="true" decorator="sk.iway.displaytag.NbspDecorator" >
		<%String paymentMethodLabelKey = "components.basket.invoice.payment."+i.getPaymentMethod();

		if ( paymentMethodLabelKey.equalsIgnoreCase(Prop.getInstance(request).getText(paymentMethodLabelKey) ) )
			out.println(i.getPaymentMethod() != null ? i.getPaymentMethod() : "");
		else
			out.println( Prop.getInstance(request).getText(paymentMethodLabelKey) );
		%>
	</display:column>

	<display:column titleKey="components.basket.invoice_email.delivery_method" sortable="true" property="deliveryMethod" decorator="sk.iway.displaytag.NbspDecorator"/>

	<display:column titleKey="components.basket.invoice.name" sortable="true" >
		<c:if test="<%=Tools.isNotEmpty(i.getDeliveryName())%>">
			<a href="mailto:<%=i.getContactEmail()%>"><%=i.getDeliveryName() %></a>
		</c:if>
		<c:if test="<%=Tools.isEmpty(i.getDeliveryName())%>">
			<a href="mailto:<%=i.getContactEmail()%>"><%=i.getContactFirstName()+" "+i.getContactLastName() %></a>
		</c:if>
	</display:column>

	<display:column titleKey="components.basket.invoice.items" sortable="true" property="totalItems"/>

	<display:column titleKey="components.basket.invoice.price" sortable="true" comparator="sk.iway.displaytag.NumberComparator" style="text-align: right;">
		 <iway:curr currency="<%=((BasketInvoiceBean)row).getCurrency()%>"><%=((BasketInvoiceBean)row).getTotalPriceVat() %></iway:curr>
	</display:column>

</display:table>

<script type="text/javascript">
<!--
	function popup2( width, height, url)
	{
		popup(url, width, height);
	}
//-->
</script>

<form method="get" action="admin_invoices_list.jsp" name="actionForm">
    <input type="hidden" name="act" />
	<input type="hidden" name="id" />
</form>

<%@ include file="/admin/layout_bottom.jsp" %>