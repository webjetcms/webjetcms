<%@page import="sk.iway.iwcm.tags.CombineTag"%><%@ page import="sk.iway.iwcm.Tools" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%
if ("true".equals(Tools.getRequestParameter(request, sk.iway.iwcm.SetCharacterEncodingFilter.PDF_PRINT_PARAM)))
{
	pageContext.include("/admin/layout_bottom_pdf.jsp");
	return;
}%>

<iwcm:combine type="js" set="adminStandardJs" />
<script src="/admin/scripts/common.jsp?v=<%=CombineTag.getVersion()%>&lng=<%=CombineTag.getLng(pageContext, request) %>" type="text/javascript"></script>

<script type="text/javascript">
var $modal = $('#ajax-modal');

jQuery(document).ready(function() {
	$(".table-wj thead tr").attr("role","row").attr("class","heading" );
	Metronic.init(); // init metronic core components
	Layout.init(); // init current layout
	//UITree.init();
	//TableLon.init();
	ComponentsDropdowns.init();
	ComponentsPickers.init();
	//Index.initMiniCharts();

	$('.portlet a.reload[data-load="true"]').click();
	//componentsEditors.init();
});

if (document.getElementById("waitDiv")!=null) document.getElementById("waitDiv").style.display="none";

</script>

<!-- END JAVASCRIPTS -->
</body>
<!-- END BODY -->
</html>
