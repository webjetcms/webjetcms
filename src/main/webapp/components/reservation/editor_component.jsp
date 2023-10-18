
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%>
<%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.doc.*, java.util.*, sk.iway.iwcm.*"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%>
<iwcm:checkLogon admin="true" perms="cmp_reservation"/>

<%
	request.setAttribute("cmpName", "reservation");
%>
<jsp:include page="/components/top.jsp" />

<script type="text/javascript">
	//<![CDATA[
	function Ok() {
		var htmlCode = "";
		var typeId = $("#reservationTypeId").val();
		if (typeId == 1)
			htmlCode = "!INCLUDE(/components/reservation/reservation_list.jsp)!\n";
		if (typeId == 2)
			htmlCode = "!INCLUDE(/components/reservation/room_list.jsp)!\n";
		//alert(typeId+" : "+htmlCode);
		if (htmlCode != "") {
			oEditor.FCK.InsertHtml(htmlCode);
		}
		return true;
	}

	if (!isFck)
		resizeDialog(370, 200);
	//]]>
	
	function loadListIframe()
	{
		var url = "/components/reservation/admin_reservation_list.jsp";
		 $("#componentIframeWindowTabList").attr("src", url);
	}
	function loadListObjectsIframe()
	{
		var url = "/components/reservation/admin_object_list.jsp";
		 $("#componentIframeWindowTabListObjects").attr("src", url);
	}
</script>

<iwcm:menu name="menuReservation">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li><a href="#" onclick="loadListIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.reservation.reservation_list"/></a></li>
			<li class="last"><a href="#" onclick="loadListObjectsIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.reservation.reservationObjectList"/></a></li>
		</ul>
	</div> 
</iwcm:menu>
<style type="text/css">
	.leftCol {
		text-align: right;
		padding-top: 5px;
	}
	#tabMenu1 {
		margin-top: 15px;
	}
</style>
<div class="tab-pane toggle_content tab-pane-fullheight" style="width:790px;">
	<div class="tab-page" id="tabMenu1" style="display: block; width: 990px;">
		<div class="col-sm-8">
			<div class="col-sm-6 leftCol">
				<label for="reservationTypeId"> <iwcm:text
							key="components.reservation.editor_component.reservation_type" />
				</label>
			</div>
			<div class="col-sm-6">
				<select name="reservationType" id="reservationTypeId">
						<option value='1' selected="selected"><iwcm:text
								key="components.reservation.editor_component.reservation_list" /></option>
						<option value='2'><iwcm:text
								key="components.reservation.editor_component.room_list" /></option>
				</select>
			</div>
		</div>
	</div>
	
	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTabList" frameborder="0" name="componentIframeWindowTabList" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>
	
	<div class="tab-page tab-page-iframe" id="tabMenu3">
		<iframe id="componentIframeWindowTabListObjects" frameborder="0" name="componentIframeWindowTabListObjects" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

</div>
<jsp:include page="/components/bottom.jsp" />
