<%@page import="sk.iway.iwcm.tags.CombineTag"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%
if ("true".equals(Tools.getRequestParameter(request, sk.iway.iwcm.SetCharacterEncodingFilter.PDF_PRINT_PARAM)))
{
	pageContext.include("/admin/layout_bottom_pdf.jsp");
	return;
}%>
		</div>
	</div>
	<!-- END CONTENT -->
</div>
<!-- END CONTAINER -->

<iwcm:combine type="js" set="adminStandardJs" />
<script src="/admin/scripts/common.jsp?v=<%=CombineTag.getVersion()%>&lng=<%=CombineTag.getLng(pageContext, request) %>" type="text/javascript"></script>

<script type="text/javascript">

//prepni bootstrap button na jquery-ui button
//$.fn.bootstrapBtn = $.fn.button.noConflict();

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

	if (document.getElementById("waitDiv")!=null) document.getElementById("waitDiv").style.display="none";

	$("ul.nav-tabs li a").on("click", function(){
		$(this).parents("ul").find("li").removeClass("active");
		$(this).parent().addClass("active");
	});

	//webjet9 wrap na displaytag tabulku
	$("span.pagebanner, span.pagelinks, div.exportlinks").wrapAll("<div class='displaytag-footer'/>'");

	//ak je tam len jedna tabulka placni ju na spodok
	if ($("span.pagebanner").length==1) {
		$("div.displaytag-footer").addClass("displaytag-footer-bottom");
	}
});

//console.log("PARENt WINDOW: "+window.parent+" parent vs top " + ( window.parent != window.top ));
/*
if (window.parent != window.top)
{
	//sme vlozeny do iframe, povypinaj niektore veci
	//console.log("Som vlozeny do iframe");
	$("div.page-header").hide();
	$("body").css("background-image", "none");
	$("body").css("background-color", "white");
	$("div.page-content").css("background-image", "none");
}
*/

</script>

<!-- END JAVASCRIPTS -->
</body>
<!-- END BODY -->
</html>
