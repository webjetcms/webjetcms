<%@ page import="sk.iway.iwcm.Tools" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
		</div>
   </td>
</tr>
</table>
<script type="text/javascript">
// zavola funkciu na resize okna, len ak je okno otvorene ako popup, respektive ak existuje funkcia
if (typeof parent.SetAutoSize !== 'undefined' && $.isFunction(parent.SetAutoSize)) {
	parent.SetAutoSize(true);
}
</script>
<script type="text/javascript">

//prepni bootstrap button na jquery-ui button
$.fn.bootstrapBtn = $.fn.button.noConflict();

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

	<%
	{
	    String pageParamsX = Tools.getRequestParameterUnsafe(request, "pageParams");
	    System.out.println(pageParamsX);
	    if (sk.iway.iwcm.Tools.isNotEmpty(pageParamsX) && pageParamsX.length()>10)
	    {
	        %>
            $("ul.tab_menu li a").each(function() {

                var onClick = $(this).attr("onclick");
                if (onClick != null && onClick.indexOf("loadComponentIframe")!=-1)
                {
                    $(this).click();
                }

            });
            <%
	    }
	}
	%>
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
</body>
</html>
