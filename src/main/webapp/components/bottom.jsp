<%@ page import="sk.iway.iwcm.*" %>
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

	appendCommonSettings();
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

<%
	{
		String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
		if (Tools.isNotEmpty(paramPageParams)) { request.setAttribute("includePageParams", paramPageParams); }
		PageParams pageParams = new PageParams(request);
%>
		<script>
			function appendCommonSettings() {
				let tabPane = $("div.tab-pane");
				if(tabPane.length) {
					let advancedSettings = $("#tabMenucommonAdvancedSettings");
					if(advancedSettings.length) {
						tabPane[0].append(advancedSettings[0]);
					}
				}

				console.log(getCommonAdvancedParameters());
			}

			function addAdvancedSettingsTab() {
				let menu = $("ul.tab_menu");
				if(menu.length) {
					menu.append("<li> <a href=\"#\" onclick=\"showHideTab('commonAdvancedSettings');\" id=\"tabLinkcommonAdvancedSettings\"> <iwcm:text key="editor.tab.commonSettings"/> </a> </li>");
				}
			}

			function getCommonAdvancedParameters() {
				let returnPageParamsValue = "";
				let supportedDevices = ["Phone", "Tablet", "Pc"];

				supportedDevices.forEach((device) => {
					let deviceObj = $("#commonAdvancedSettingsDevice" + device);
					if(deviceObj.length && deviceObj.is(':checked')) {
						if(returnPageParamsValue.length < 1) {
							returnPageParamsValue = ", device="+device.toLowerCase();
						} else {
							returnPageParamsValue += "+" + device.toLowerCase();
						}
					}
				});

				//if all are selected consider it as default none
				returnPageParamsValue = returnPageParamsValue.replace(", device=phone+tablet+pc", "");

				return returnPageParamsValue;
			}
		</script>

		<div class="tab-page" id="commonAdvancedSettings">
			<div class="tab-page" id="tabMenucommonAdvancedSettings" style="width: 800px; height: 1000px; max-height: 1000px;">
				<div class="wrapper">
					<div class="row">
						<div class="col-sm-4 leftCol">
							<iwcm:text key="apps.devices.title"/>:
						</div>
						<div class="col-sm-8">
							<label><input type="checkbox" name="device" id="commonAdvancedSettingsDevicePhone" <%if (pageParams.getValue("device", "phone+tablet+pc").contains("phone")) out.print("checked='checked'");%>> <iwcm:text key="apps.devices.phone"/></label><br>
							<label><input type="checkbox" name="device" id="commonAdvancedSettingsDeviceTablet" <%if (pageParams.getValue("device", "phone+tablet+pc").contains("tablet")) out.print("checked='checked'");%>> <iwcm:text key="apps.devices.tablet"/></label><br>
							<label><input type="checkbox" name="device" id="commonAdvancedSettingsDevicePc" <%if (pageParams.getValue("device", "phone+tablet+pc").contains("pc")) out.print("checked='checked'");%>> <iwcm:text key="apps.devices.pc"/></label><br>
						</div>
					</div>
				</div>
			</div>
		</div>
<%
	}
%>

</body>
</html>
