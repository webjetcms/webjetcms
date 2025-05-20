<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
int orderType = DocDB.ORDER_PRIORITY; // 3

int publishType = 103;//DocDB.PUBLISH_ALL; // 1

boolean ascending = pageParams.getBooleanValue("asc", true);
String groupIds = pageParams.getValue("groupIds", "");

//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

DocDB docDB = DocDB.getInstance();
docDB.getDocPerex(groupIds, orderType, ascending, publishType, 1000, "novinky", "pages", request);
%>
<script type="text/javascript">
$(document).ready(function(){
	//vyexportujem si zoznam produktov do excelu
	$('.export.excel').click();
});
</script>
<div style="display:none;">
	<display:table name="novinky" export="true" id="row" pagesize="1000" requestURI="<%=PathFilter.getOrigPath(request)%>">
		<display:column titleKey="components.cloud.basket.sku" property="fieldA"/>
		<display:column titleKey="editor.title" property="title"/>
		<display:column titleKey="components.cloud.basket.product_in_stock" property="fieldB"/>
		<display:column titleKey="components.catalog.price_without_vat" property="fieldK"/>
		<display:column titleKey="components.basket.invoice.currency" property="fieldJ"/>
		<display:column titleKey="components.catalog.vat" property="fieldL"/>
		<display:column titleKey="components.basket.old_price" property="fieldM"/>
	</display:table>
</div>