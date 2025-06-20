<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_basket"/>

<%
	request.setAttribute("cmpName", "basket");
	//je to tu takto, aby sa spravne zobrazila ikona
	request.setAttribute("dialogTitleKey", "components.basket.price_import.dialog_title");
	request.setAttribute("dialogDescKey", "components.basket.price_import.dialog_desc");
	//budeme importovat podla partNo? ak nie, tak podla docId...
	boolean displayByPartNo = !"".equals( Constants.getString("basketPartNoField") );
%>

<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<script src="/components/form/check_form.js" type="text/javascript"></script>
<script src="/components/form/fix_e.js" type="text/javascript"></script>

<%=Tools.insertJQuery(request) %>

<script src="/components/form/event_attacher.js" type="text/javascript"></script>
<script src="/components/form/class_magic.js" type="text/javascript"></script>
<script src="/components/form/check_form_impl.jsp" type="text/javascript"></script>

<script type="text/javascript">
<!--
	document.body.style.cursor = "default";
	function Ok()
	{
		//musi to ist takto, inak by sa nezavolal check form
		document.xlsImportForm.submit.click();
	}
//-->
</script>
<div class="padding10">
	<div class="col-sm-12 col-xs-12 form-group">

		<%
			if (displayByPartNo)
			{
		%>
				<iwcm:text key="components.basket.price_import.help" param1='<%=Prop.getInstance(request).getText("components.basket.price_list.part_number")%>'/>
		<%
			}
			else
			{
		%>
				<iwcm:text key="components.basket.price_import.help" param1='<%=Prop.getInstance(request).getText("components.basket.price_list.map_number")%>'/>
		<%
			}
		%>
	</div>
	<form method="post" action="/admin/import/excel/" name="xlsImportForm" id="xlsImportForm" enctype="multipart/form-data">
		<div class="col-sm-12 col-xs-12 form-group">
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6">
					<iwcm:text key="components.basket.price_import.file_label"/>:
				</div>
				<div class="col-sm-6 col-xs-6">
					<input type="file" name="file" class="input" />
					<input type="hidden" value="sk.iway.iwcm.components.basket.PricelistExcelImport" name="type" class="required" />
				</div>
			</div>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6">
					<label for="titleId"><iwcm:text key="components.basket.price_import.title_label"/>:</label>
				</div>
				<div class="col-sm-6 col-xs-6">
					<input type="text" id="titleId" name="title" class="required" value="<iwcm:text key="components.basket.price_import.default_title_header"/>" />
				</div>
			</div>
            <div class="col-sm-10 col-xs-10 form-group">
                <div class="col-sm-6 col-xs-6">
                    <label for="quantityId"><iwcm:text key="components.basket.price_import.quantity_label"/>:</label>
                </div>
                <div class="col-sm-6 col-xs-6">
                    <input type="text" id="quantityId" name="quantity" class="required" value="<iwcm:text key="components.basket.price_import.default_quantity_header"/>" />
                </div>
            </div>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6">
					<label for="priceId"><iwcm:text key="components.basket.price_import.price_label"/></label>
				</div>
				<div class="col-sm-6 col-xs-6">
					<input type="text" name="price" id="priceId" class="required" value="<iwcm:text key="components.basket.price_import.default_price_header"/>"/>
				</div>
			</div>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6">
					<label for="currencyId"><iwcm:text key="components.basket.price_import.currency_label"/></label>
				</div>
				<div class="col-sm-6 col-xs-6">
					<input type="text" name="currency" id="currencyId" class="required" value="<iwcm:text key="components.basket.price_import.default_currency_header"/>"/>
				</div>
			</div>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6">
					<label for="vatId"><iwcm:text key="components.basket.price_import.vat_label"/>:</label>
				</div>
				<div class="col-sm-6 col-xs-6">
					<input type="text" name="vat" id="vatId" class="required" value="<iwcm:text key="components.basket.price_import.default_vat_header"/>"/>
				</div>
			</div>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6"></div>
				<div class="col-sm-6 col-xs-6"></div>
			</div>
			<%
				if(displayByPartNo)
				{
			%>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6">
					<label for="partNumberId"><iwcm:text key="components.basket.price_import.map_number_label"/></label>
				</div>
				<div class="col-sm-6 col-xs-6">
					<input type="text" id="partNumberId" name="partNumber" class="required" value="<iwcm:text key="components.basket.price_import.default_map_number_header"/>"/>
				</div>
			</div>
			<%
				}
				else
				{
			%>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6">
					<label for="docId1"><iwcm:text key="components.basket.price_import.docid_number_label"/></label>
				</div>
				<div class="col-sm-6 col-xs-6">
					<input type="text" name="docId" id="docId1" class="required" value="<iwcm:text key="components.basket.price_import.default_docid_header"/>"/>
				</div>
			</div>
			<%
				}
			%>
			<div class="col-sm-10 col-xs-10 form-group">
				<div class="col-sm-6 col-xs-6"></div>
				<div class="col-sm-6 col-xs-6" align="right">
					<input type="submit" class="button50" name="submit" id="submit" value="<iwcm:text key="components.basket.price_import.ok_button"/>" style="display: none;" />
				</div>
			</div>
		</div>
	</form>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>