<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.*,
sk.iway.iwcm.components.banner.*,
sk.iway.iwcm.components.banner.model.*, sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="menuBanner"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
request.setAttribute("cmpName", "banner");
Identity actualUser = (Identity) session.getAttribute(Constants.USER_KEY);
List bannerGroupNames = BannerDB.getBannerGroupsByUserAllowedCategories(actualUser.getUserId());
if(bannerGroupNames.size() > 0) request.setAttribute("bannerGroupNames", bannerGroupNames);

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);
int displayMode = pageParams.getIntValue("displayMode",1);
%>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>
function showConditionalFields()
{
	if($("#showInIframe").is(":checked")) $(".onlyForIframe").show();
	else
	{
		$(".onlyForIframe").hide();
		$("#refreshRate").val("0");
	}

    if ($("#radio1").is(":checked")) $(".onlyMode1").show();
	else $(".onlyMode1").hide();
}

function addGroup()
{
		document.textForm.bannerGroup.value = document.textForm.group.value;
}

// function showViewType()
// {
// 	if ($('#viewType').val() == 2) {
// 		document.textForm.viewType.value = 2
// 	} else {
// 		document.textForm.viewType.value = 1
// 	}
// }

function Ok()
{
	showInIframe = "false";
	iframeWidth = "";
	iframeHeight = "";

	if (document.textForm.status.checked) status = "enabled";
	if (document.textForm.showInIframe.checked)
	{
			showInIframe = "true";
			iframeWidth = document.textForm.iframeWidth.value;
			iframeHeight = document.textForm.iframeHeight.value;
	}



	refreshRate	= document.textForm.refreshRate.value;
	group = document.textForm.bannerGroup.value;
	bannerIndex = document.textForm.bannerIndex.value;

	for (var i = 0; i < document.textForm.displayMode.length; i++)
	{
			if (document.textForm.displayMode[i].checked)
			{
					displayMode = document.textForm.displayMode[i].value;
			}
	}
	oEditor.FCK.InsertHtml("!INCLUDE(/components/banner/banner.jsp, group=\""+group+"\", status="+status+", displayMode="+displayMode+", refreshRate="+refreshRate+", bannerIndex=\""+bannerIndex+"\", showInIframe="+showInIframe+", iframeWidth="+iframeWidth+", iframeHeight="+iframeHeight+")!");

	return true ;
} // End function

if (isFck)
{

}
else
{
	resizeDialog(600, 700);
}

function loadListIframe()
{
	var url = "/apps/banner/admin/";
	 $("#componentIframeWindowTabList").attr("src", url);
}
function loadStatsIframe()
{
	var url = "/apps/banner/admin/banner-stat/";
	 $("#componentIframeWindowTabStats").attr("src", url);
}

</script>

<iwcm:menu name="menuBasket">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }

		.col-sm-4 {
			text-align: right;
			padding-top: 5px;
			margin-bottom: 5px;
		}
		.col-sm-8, .col-sm-6, .col-sm-2 {
			margin-bottom: 10px;
		}
		.tab-pane:first-child {
			margin-top: 30px;
		}
		input[type="radio"] {
			margin-right: 15px;
			margin-bottom: 4px;
		}
		.col-sm-10 {
			margin-top: 10px;
		}
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li><a href="#" onclick="loadListIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.banner.list_of_banners"/></a></li>
			<li class="last"><a href="#" onclick="loadStatsIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.banner.banners_stat"/></a></li>
		</ul>
	</div>
</iwcm:menu>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block; width: 940px;">

		<form method="get" name="textForm" style="margin: 0px;">
			<div class="col-sm-10">
				<logic:present name="bannerGroupNames">
					<div class="col-sm-12">
						<div class="col-sm-4 leftCol">
							<iwcm:text key="components.banner.select_group"/>:
						</div>
						<div class="col-sm-6">
							<select name="group" onChange="" class="form-control">
								<%
									for(int i=0; i<bannerGroupNames.size(); i++)
									{
										out.println("<option value='" +((BannerGroupBean)bannerGroupNames.get(i)).getBannerGroup()+ "'>" +((BannerGroupBean)bannerGroupNames.get(i)).getBannerGroup()+ "</option>");
									}
								%>
							</select>
						</div>
						<div class="col-sm-2">
							<input type="button" class="btn green" value="<iwcm:text key="button.select"/>" onClick="addGroup()">
						</div>
					</div>
				</logic:present>
				<div class="col-sm-12">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.group"/>:
					</div>
					<div class="col-sm-8">
						<input type="text" name="bannerGroup"  class="form-control" size=20 maxlength="128" value="<%=ResponseUtils.filter(pageParams.getValue("group", ""))%>">
					</div>
				</div>
				<div class="col-sm-12">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.active"/>:
					</div>
					<div class="col-sm-8">
						<input type="checkbox" name="status" checked>
					</div>
				</div>
				<%--<div class="col-sm-12">--%>
					<%--<div class="col-sm-4 leftCol">--%>
						<%--Typ zobrazenia banneru:--%>
					<%--</div>--%>
					<%--<div class="col-sm-8">--%>
						<%--<select name="viewType" id="viewType" onchange="showViewType()">--%>
							<%--<option value="1">Základné zobrazenie</option>--%>
							<%--<option value="2">Zobrazenie obsahových bannerov</option>--%>
						<%--</select>--%>
					<%--</div>--%>
				<%--</div>--%>
				<div class="col-sm-12">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.display_mode"/>:
					</div>
					<div class="col-sm-8">
						<label><input type="radio" name="displayMode" value="1" id="radio1" onclick="showConditionalFields()" <%if (displayMode == 1) out.print("checked='checked'");%>><iwcm:text key="components.banner.display_mode_1"/></label><br>
		  				<label><input type="radio" name="displayMode" value="2" id="radio2" onclick="showConditionalFields()" <%if (displayMode == 2) out.print("checked='checked'");%>><iwcm:text key="components.banner.display_mode_2"/></label><br>
		  				<label><input type="radio" name="displayMode" value="3" id="radio3" onclick="showConditionalFields()" <%if (displayMode == 3) out.print("checked='checked'");%>><iwcm:text key="components.banner.display_mode_3"/></label><br>
		  				<label><input type="radio" name="displayMode" value="4" id="radio4" onclick="showConditionalFields()" <%if (displayMode == 4) out.print("checked='checked'");%>><iwcm:text key="components.banner.display_mode_4"/></label><br>
		  				<label><input type="radio" name="displayMode" value="5" id="radio5" onclick="showConditionalFields()" <%if (displayMode == 5) out.print("checked='checked'");%>><iwcm:text key="components.banner.display_mode_5"/></label>
		  			</div>
				</div>
				<div class="col-sm-12 onlyMode1">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.banner_index"/>:
					</div>
					<div class="col-sm-8">
						<input type="text"  class="form-control" name="bannerIndex" size=5 maxlength="5" value="<%=ResponseUtils.filter(pageParams.getValue("bannerIndex", ""))%>">
						<iwcm:text key="components.banner.jedinecny_index"/>
					</div>
				</div>
				<div class="col-sm-12">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.show_in_iframe"/>:
					</div>
					<div class="col-sm-8">
						<input type="checkbox" name="showInIframe" id="showInIframe" onclick="showConditionalFields()" <%if (pageParams.getBooleanValue("showInIframe", false)) out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-sm-12 onlyForIframe">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.refresh_rate"/> (sec.) :
					</div>
					<div class="col-sm-8">
						<input type="text"  class="form-control" name="refreshRate" id="refreshRate" maxlength="10" value="<%=ResponseUtils.filter(pageParams.getValue("refreshRate", "0"))%>">
						 - <iwcm:text key="components.banner.refresh_rate_desc"/>
					</div>
				</div>
				<div class="col-sm-12 onlyForIframe">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.iframe_width"/>:
					</div>
					<div class="col-sm-8">
						<input type="text" class="form-control" name="iframeWidth" size=5 maxlength="5" value="<%=ResponseUtils.filter(pageParams.getValue("iframeWidth", ""))%>">
					</div>
				</div>
				<div class="col-sm-12 onlyForIframe">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.banner.iframe_height"/>:
					</div>
					<div class="col-sm-8">
						<input type="text" class="form-control" name="iframeHeight" size=5 maxlength="5" value="<%=ResponseUtils.filter(pageParams.getValue("iframeHeight", ""))%>">
					</div>
				</div>
			</div>
		</form>

	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTabList" frameborder="0" name="componentIframeWindowTabList" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu3">
		<iframe id="componentIframeWindowTabStats" frameborder="0" name="componentIframeWindowTabStats" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

</div>

<script type="text/javascript">

$(document).ready(function() {
	<% if (Tools.isNotEmpty(pageParams.getValue("group", ""))) {%>
		try { document.textForm.group.value = "<%=ResponseUtils.filter(pageParams.getValue("group", ""))%>"; } catch (e) {}
	<%}%>
	<% if(displayMode == 1) { %>
		$("#radio1").attr("checked", "checked");
	<%	} else if(displayMode == 2) { %>
		$("#radio2").attr("checked", "checked");
	<%	} else if(displayMode == 3) { %>
		$("#radio3").attr("checked", "checked");
	<%	} else if(displayMode == 4) { %>
		$("#radio4").attr("checked", "checked");
	<%	} else { %>
		$("#radio5").attr("checked", "checked");
	<%	} %>
	showConditionalFields();
});
</script>
<jsp:include page="/components/bottom.jsp"/>
