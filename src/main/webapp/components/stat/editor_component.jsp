<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_stat"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
request.setAttribute("cmpName", "stat.heat_map");
%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.Tools"%>
<jsp:include page="/components/top.jsp"/>
<script src="/components/form/check_form.js" type="text/javascript"></script>
<%=Tools.insertJQuery(request) %>
<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	//out.println(paramPageParams);
	//System.out.println(paramPageParams);
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>
<script type='text/javascript'>
function Ok()
{
	//enable tracking if it was disabled before and used decided otherwise
	if ($("#enableClickTracking:checked").size() == 1){
		alert(1)
		$.post("/admin/conf_editor_popup.jsp", {setName : "update", prefix : "statEnableClickTracking", value : "true"})
	}
	oEditor.FCK.InsertHtml("!INCLUDE(/components/stat/heat_map_tracker.jsp, container=\""+$('#container').val()+"\")!");
	return true ;
} // End function

if (!isFck)
	resizeDialog(700, 700);

</script>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px; }
	td.main { padding: 0px; }

	.leftCol {
		text-align: right;
		padding-top: 5px;
	}
	.col-sm-12 {
		margin-bottom: 15px;
	}
	#tabMenu1 {
		margin-top: 15px;
	}
</style>


<div class="tab-pane toggle_content tab-pane-fullheight" style="width:680px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">

		<form method="get" style="margin: 0px;" id="heatMapForm">
			<div class="col-sm-10 col-sm-offset-1">
				<p>
					<em>
						<iwcm:text key="components.stat.heat_map.warning" />
					</em>
				</p>
				<div class="col-sm-12">
					<div class="col-sm-6 leftCol">
						<c:if test='<%=Constants.getBoolean("statEnableClickTracking") == false %>'>
							<label>
								<input type="checkbox" id="enableClickTracking" /> <iwcm:text key="components.stat.heat_map.enable_tracking"/>
							</label>
						</c:if>
					</div>
				</div>
				<div class="col-sm-12">
					<div class="col-sm-6 leftCol">
						<label for="container">
							<iwcm:text key="components.stat.heat_map.container"/>
						</label>
					</div>
					<div class="col-sm-6">
						<input type="text" class="required" value="<%=ResponseUtils.filter(pageParams.getValue("container", Constants.getString("statHeatMapMainContainerSelector")))%>" id="container" autofocus="true" />
					</div>
				</div>
			</div>
		</form>
	</div>
</div>
<jsp:include page="/components/bottom.jsp"/>
