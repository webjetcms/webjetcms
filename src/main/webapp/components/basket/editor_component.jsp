<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<iwcm:checkLogon admin="true" perms="cmp_basket|menuWebpages"/>

<%@page import="java.io.File" %>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Collections"%>
<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<%@page import="sk.iway.iwcm.gallery.*"%>

<%
	String lng = PageLng.getUserLng(request);
	request.setAttribute("cmpName", "basket");
	pageContext.setAttribute("lng", lng);

	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	String jspFileName = request.getParameter("jspFileName");
	int groupId = Tools.getIntValue(request.getParameter("groupId"), 0);

	if (Tools.isNotEmpty(jspFileName) && jspFileName.contains("product_perex")) {
		pageContext.include("editor_component_perex.jsp");
		return;
	}


	if(Tools.isNotEmpty(jspFileName)){
		int slash = jspFileName.lastIndexOf("/");
		int dot = jspFileName.lastIndexOf(".");

		if(slash > 0 && dot < jspFileName.length()) jspFileName = jspFileName.substring(slash+1, dot);	//ziskam len nazov komponenty bez koncovky
	}
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String style =  pageParams.getValue("style", "01");

	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("perexGroup", ""))))
	{
		String[] perexGroupArray = GalleryDB.convertPerexGroupString(ResponseUtils.filter(pageParams.getValue("perexGroup", "")));
		String perexGroupString = GalleryDB.convertPerexGroupString(perexGroupArray);	//aby sa pri editaci skupiny spravne popresuvali, potrebujem
																												//vstup vo specialnom formate ","+groupPerex+","
		request.setAttribute("perexGroup", perexGroupString);
	}

	Identity user = UsersDB.getCurrentUser(request);
%>

<%--
	Okno komponenty sluziacej na vkladanie stranok suvisiacich so shopom.
	Mozno vlozit: obsah kosiku, zoznam produktov, maly kosik a objednavkovy formular.

	Pouziva: Tools, Constants

	Suvisiace stranky: products.jsp, order_form.jsp, basket.jsp, basket_small.jsp
 --%>

<jsp:include page="/components/top.jsp"/>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<script src="/components/form/check_form.js" type="text/javascript"></script>
<script src="/components/form/fix_e.js" type="text/javascript"></script>

<%=Tools.insertJQuery(request) %>

<script src="/components/form/event_attacher.js" type="text/javascript"></script>
<script src="/components/form/class_magic.js" type="text/javascript"></script>
<script src="/components/form/check_form_impl.jsp" type="text/javascript"></script>
<script src="/admin/scripts/common.jsp" type="text/javascript"></script>




<script type="text/javascript">
<!--

function changeDisplayedDiv()
{
    select = $('#mainSelect').get(0);
    selectedValue = select.options[ select.selectedIndex ].value;
    $('#productsDiv').hide();
    $('#basketDiv').hide();
    $('#addbasketDiv').hide();
    $('#basketSmallDiv').hide();
    $('#orderDiv').hide();
    //$('#mojeObjednavkyDiv').hide();

    if ("basket_page" == selectedValue)
    {
        $('#basketDiv').show();
        $('#orderDiv').show();
        $('#anotherDocTable').show();
        $('#mojeObjednavkyDiv').show()
        if ($('#orderFormDocTarget').attr("checked") == true) {
            $('#orderDiv').show();$('#anotherDocTable').hide();$('#mojeObjednavkyDiv').hide();
        }
        else {
            $('#orderDiv').get(0).style.display = 'none';$('#anotherDocTable').show();$('#mojeObjednavkyDiv').hide();
        }
    }
    if ("basket_small" == selectedValue)
        $('#basketSmallDiv').show();
    if ("addbasket" == selectedValue)
        $('#addbasketDiv').show();
    if ("bootstrap_products" == selectedValue)
        $('#productsDiv').show();
    if ("order_form" == selectedValue)
        $('#orderDiv').show();
}


$(document).ready(function(){
	<% if(Tools.isNotEmpty(pageParams.getValue("groupIds", ""))){ %>
	document.textForm.groupIds.value = "<%=ResponseUtils.filter(pageParams.getValue("groupIds", ""))%>";
<%} %>
});
	<%-- musime si vediet dynamicky ziskat zoznam podporovanych elektronickych platieb--%>
	var modeTransport="";

	function checkChanges()
	{
		try

		{
			var changed = window.frames.componentIframeWindowTab.isUnsavedForm();
			if (changed == true)
			{
				if (window.confirm("<iwcm:text key="components.youHaveUnsavedItems"/>")==false)
				{
					showHideTab('2');
					return false;
				}
			}
		}
		catch (e) { }

		return true;
	}

	function Cancel()
	{
		if (checkChanges()==false) return false;

		return true;
	}

	function Ok()
	{
        select = $('#mainSelect').get(0);
        selectedValue = select.options[ select.selectedIndex ].value;

        if (selectedValue == "bootstrap_products")
            return doOkProducts();

        if ("basket_page" == selectedValue)
        {
            //ak sme si vybrali, ze budeme posielat obsah kosiku na inu stranku, nie na samu seba

                orderDoc = $('#orderDocId').get(0).value;
                oEditor.FCK.InsertHtml("!INCLUDE(/components/basket/basket_page.jsp"+(orderDoc.match("^[0-9]+$")?",orderFormDocId="+orderDoc:"")+")!");


        }

        if ("basket_small" == selectedValue)
        {
            iconProperties = !$("#basketSmallIcon").get(0).checked
                ? ",withIcon=false"
                : ',withIcon=true,iconClickTarget="'+$("#referencedPage").get(0).value+'"';
            oEditor.FCK.InsertHtml("!INCLUDE(/components/basket/basket_small.jsp"+iconProperties+")!");
        }

        if ("addbasket" == selectedValue)
        {
            oEditor.FCK.InsertHtml("!INCLUDE(/components/basket/addbasket.jsp)!");
        }
        if ("order_form" == selectedValue)
        {
            return doOkOrderForm();
        }
        if ("invoices_list" == selectedValue)
        {
            return doOkMojeObjednavkyForm();
        }
        return true;

	} // End function

function doOkOrderForm()
{
    oEditor.FCK.InsertHtml("!INCLUDE(/components/basket/order_form.jsp"+", "+getOrderFormParametersString()+")!");
    return true;
}

function getOrderFormParametersString()
{
    var notifyEmail = $('#notifyEmail').get(0).value;
    //zistime si, ake sposoby platby nam zaskrtal, a spravime z nich zoznam
    var account = "";
    if($('#allowPartialPayments').get(0).checked){
        account = "allowPartialPayments=true";
    }
    //odrezeme prvu cairku a pridame na koniec
    notifyEmail = (notifyEmail.length > 2 ? ", notifyEmail=\""+notifyEmail+"\"" : "" );

    return account+", displayedPayments="+notifyEmail;
}

	function setParentGroupId(returnValue)
	{
		//var returnValue = showModalDialog("<iwcm:cp/>/admin/grouptree.jsp", "", "dialogHeight: 500px; resizable: Yes;") + " ";
		if (returnValue.length > 15)
		{
			var groupid = returnValue.substr(0,15);
			var groupname = returnValue.substr(15);
			groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
			if (document.textForm.groupIds.value=="")
			{
				document.textForm.groupIds.value = groupid;
			}
			else
			{
				document.textForm.groupIds.value = document.textForm.groupIds.value + "+"+groupid;
			}
		}
	}

	function setRootGroup(returnValue)
	{
		//var returnValue = showModalDialog("<iwcm:cp/>/admin/grouptree.jsp", "", "dialogHeight: 500px; resizable: Yes;") + " ";
		if (returnValue.length > 15)
		{
			var groupid = returnValue.substr(0,15);
			var groupname = returnValue.substr(15);
			groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");

			document.MenuULForm.rootGroupId.value = groupid;

		}
	}



	function getIncludeTextProducts()
	{
		groupIds = document.textForm.groupIds.value;

		orderType = document.textForm.orderType.value;
		asc = "no";
		if (document.textForm.asc.checked)
		{
			asc = "yes";
		}
		publishType = document.textForm.publishType.value;
		paging = "no";
		if (document.textForm.paging.checked)
		{
			paging = "yes";
		}
		pageSize = document.textForm.pageSize.value;

		var showCategory = "no";
		if (document.textForm.showCategory.checked) showCategory = "yes";

		var showSort = "no";
		if (document.textForm.showSort.checked) showSort = "yes";

		var testRun = "no";
		if (document.textForm.testRun.checked) testRun = "yes";

		var katalogProduktov = "no";
		if (document.textForm.katalogProduktov.checked) katalogProduktov = "yes";

		var pagingPosition = $('#paging_position').val();
		var style = $('input[name=style]:checked').val();

		return "!INCLUDE(/components/basket/bootstrap_products.jsp, style="+style+" ,groupIds="+groupIds+", orderType="+orderType+", asc="+asc+", publishType="+publishType+", paging="+paging+", pageSize="+pageSize+",pagingPosition="+pagingPosition+", thumbWidth="+document.textForm.thumbWidth.value+", thumbHeight="+document.textForm.thumbHeight.value+", showCategory="+showCategory+", showSort="+showSort+", testRun="+testRun+", katalogProduktov="+katalogProduktov+", overeneZakaznikmi="+document.textForm.overeneZakaznikmi.value+")!";
	}

	function doOkProducts()
	{
		var showCategory = "no";
		if (document.textForm.showCategory.checked) showCategory = "yes";

		var showSort = "no";
		if (document.textForm.showSort.checked) showSort = "yes";

		var testRun = "no";
		if (document.textForm.testRun.checked) testRun = "yes";


		try
		{
			$.ajax({
	            type:'POST',
	            url: '/components/basket/admin_change_shop_settings_ajax.jsp',
	            data: {showCategory: showCategory, showSort: showSort, testRun: testRun, rootId: document.textForm.groupIds.value},
	            success:function(data)
	            {
	            	oEditor.FCK.InsertHtml(getIncludeTextProducts());

	            	if (window.parent.location.href.indexOf("inline")==-1)
	            	{
	            		parent.setComponentData();
	            		parent.Cancel();
	            	}
	            }
	        });
		}
		catch (e) { window.alert("<iwcm:text key='editor.ajax.save.error'/>"); }


		return false;
	} // End function

	function loadComponentIframe()
	{
		var url = "/components/basket/admin_products_list.jsp?groupId=<%=groupId%>&include="+encodeURIComponent(getIncludeTextProducts());
	 	$("#componentIframeWindowTab").attr("src", url);
	}

	function loadInvoicesIframe()
	{
		var url = "/components/basket/admin_invoices_list.jsp";
	 	$("#invoicesIframeWindowTab").attr("src", url);
	}

	function loadTransportsIframe()
	{
		var url = "/components/basket/admin_transports_list.jsp?include="+encodeURIComponent(getIncludeTextProducts());
	 	$("#transportIframeWindowTab").attr("src", url);
	}




//-->


</script>

<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="loadTransportsIframe();showHideTab('1');" id="tabLink1"><iwcm:text key="components.settings"/></a></li>
				<li class=""><a href="#" onclick="showHideTab('4');" id="tabLink4"><iwcm:text key="components.basket.visualStyle"/></a></li>

		<li class="noperms-cmp_basket"><a href="#" onclick="loadInvoicesIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.basket.lis_of_invoices"/></a></li>
		<li class="last"><a href="#" onclick="loadComponentIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.news.items"/></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content" style="height:540px; overflow: auto; width:990px;">

	<div class="tab-page" id="tabMenu1" style="display: block; padding: 15px;">

			<form action="/admin/editor.do" onsubmit="return false" name="textForm">
<div class="clearfix">
			<h2><iwcm:text key="basket.shopSettings"/></h2>
				<div class="form-group hidden">
					<div class="row">
						<div class="col-sm-4 leftCol text-left">
							<iwcm:text key="components.basket.editor.insert_component"/>:
						</div>
						<div class="col-sm-8" style="padding-left: 20px;">
							<select id="mainSelect" onchange="changeDisplayedDiv();" name="mainSelect">
								<option value="bootstrap_products" selected="selected"><iwcm:text key="components.basket.editor.products" /></option>
								<option value="basket_page"><iwcm:text key="components.basket.editor.basket" /></option>
								<option value="basket_small"><iwcm:text key="components.basket.editor.small_basket" /></option>
								<option value="order_form"><iwcm:text key="components.basket.editor.order" /></option>
							</select>
						</div>
					</div>
				</div>


			<table id="productsDiv" border="0" cellspacing="0" cellpadding="1">
				<%
				int rootGroupId = sk.iway.iwcm.common.CloudToolsForCore.getRootGroupId(request);
				List<GroupDetails> availableGroups = GroupsDB.getInstance().getGroupsTree(rootGroupId, false, true);
				request.setAttribute("groupsTree", availableGroups);
				%>
				<tr>
					<td><label for="groupIds1"><iwcm:text key="components.news.groupids"/>:</label></td>
					<td>
						<div class="input-group">
							<input type="text" name="groupIds" size=30 maxlength="255" id="groupIds1" class="form-control"/>
							<input type="button" class="btn btn-primary" name="groupSelect" value="<iwcm:text key="components.news.addgroup"/>" onclick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp", 500, 500);' />
						</div>
					</td>
				</tr>
				<tr>
					<td><label for="orderTypeId"><iwcm:text key="components.news.ordertype"/>:</label></td>
					<td nowrap="nowrap">
						<select name="orderType" id="orderTypeId">
							<option value="priority"><iwcm:text key="components.news.ORDER_PRIORITY"/></option>
							<option value="date"><iwcm:text key="components.news.ORDER_DATE"/></option>
							<option value="saveDate"><iwcm:text key="components.news.ORDER_SAVE_DATE"/></option>
							<option value="title"><iwcm:text key="components.news.ORDER_TITLE"/></option>
							<option value="id"><iwcm:text key="components.news.ORDER_ID"/></option>
							<option value="price"><iwcm:text key="components.basket.invoice.price"/></option>
						</select>
						<label>
							<iwcm:text key="components.news.asc"/>
							<input type="checkbox" name="asc" <%if (pageParams.getBooleanValue("asc", true))
						out.print("checked='checked'");%> value="yes" />
						</label>
					</td>
				</tr>
				<tr style="display: none;">
					<td valign="top"><label for="publishTypeId"><iwcm:text key="components.news.publishtype"/>:</label></td>
					<td>
						<select name="publishType" id="publishTypeId">
							<option value="all"><iwcm:text key="components.news.PUBLISH_ALL"/></option>
							<option value="new"><iwcm:text key="components.news.PUBLISH_NEW"/></option>
							<option value="old"><iwcm:text key="components.news.PUBLISH_OLD"/></option>
							<option value="next"><iwcm:text key="components.news.PUBLISH_NEXT"/></option>
						</select>
					</td>
				</tr>
				<tr>
					<td nowrap="nowrap"><label for="pagingId"><iwcm:text key="components.news.paging"/>:</label></td>
					<td>
						<input type="checkbox" name="paging" <%if (pageParams.getBooleanValue("paging", true))
						out.print("checked='checked'");%> value="yes" id="pagingId" />
					</td>
				</tr>
				<tr>
					<td><label for="pageSizeId"><iwcm:text key="components.news.pageSize"/>:</label></td>
					<td>
						<input type="text" id="pageSizeId" name="pageSize" size=5 maxlength="5" value="<%=pageParams.getIntValue("pageSize", 15) %>" />
					</td>
				</tr>
				<tr>
					<td><label for="paging_position"><iwcm:text key="components.basket.paging_position"/>:</label></td>
					<td>
						<select name="paging_position" id="paging_position">
						<option <% if(pageParams.getValue("pagingPosition", "both").equals("top")) out.print("selected"); %> value="top"><iwcm:text key="components.basket.paging_top"/></option>
						<option <% if(pageParams.getValue("pagingPosition", "both").equals("bottom")) out.print("selected"); %> value="bottom"><iwcm:text key="components.basket.paging_bottom"/></option>
						<option <% if(pageParams.getValue("pagingPosition", "both").equals("both")) out.print("selected"); %> value="both"><iwcm:text key="components.basket.paging_both"/></option>
						</select>
					</td>
				</tr>
				<tr>
					<td><label for="thumbWidthId"><iwcm:text key="components.basket.thumbWidth"/>:</label></td>
					<td>
						<input type="text" id="thumbWidthId" name="thumbWidth" size=5 maxlength="5" value="<%=pageParams.getIntValue("thumbWidth", 190) %>" />
					</td>
				</tr>
				<tr>
					<td width="170" nowrap="nowrap"><label for="thumbHeightId"><iwcm:text key="components.basket.thumbHeight"/>:</label></td>
					<td width="80">
						<input type="text" id="thumbHeightId" name="thumbHeight" size=5 maxlength="5" value="<%=pageParams.getIntValue("thumbHeight", 190) %>" />
					</td>
				</tr>
				<tr>
					<td><label for="showCategoryId"><iwcm:text key="basket.showCategory"/>:</label></td>
					<td>
						<input type="checkbox" name="showCategory" <%if (pageParams.getBooleanValue("showCategory", true))
						out.print("checked='checked'");%> value="yes" id="showCategoryId" />
					</td>
				</tr>
				<tr>
					<td><label for="showSortId"><iwcm:text key="basket.showSort"/>:</label></td>
					<td>
						<input type="checkbox" name="showSort" <%if (pageParams.getBooleanValue("showSort", true))
						out.print("checked='checked'");%> value="yes" id="showSortId" />
					</td>
				</tr>
				<tr>
					<td><label for="testRunId"><iwcm:text key="basket.testRun"/>:</label></td>
					<td>
						<input type="checkbox" name="testRun" <%if (pageParams.getBooleanValue("testRun", false))
						out.print("checked='checked'");%> value="yes" id="testRunId" />
					</td>
				</tr>
				<tr>
					<td><label for="katalogProduktovId"><iwcm:text key="basket.katalogProduktov"/>:</label></td>
					<td>
						<input type="checkbox" name=katalogProduktov <%if (pageParams.getBooleanValue("katalogProduktov", false))
						out.print("checked='checked'");%> value="yes" id="katalogProduktovId" />
					</td>
				</tr>
				<% if (!Tools.isEmpty(pageParams.getValue("groupIds", ""))){%>
					<tr>
						<td><iwcm:text key="components.basket.heurekaXML"/>:</td>
						<iwcm:menu notName="anyPremiumService">
							<td>
									<jsp:include page="/components/cloud/_common/admin/premium_required.jsp"><jsp:param value="true" name="inline"/></jsp:include>
							</td>
						</iwcm:menu>
						<iwcm:menu name="anyPremiumService">
							<td nowrap="nowrap">
								<%=Tools.getBaseHref(request) %>/components/basket/heureka_ajax.jsp?groupId=<%=pageParams.getValue("groupIds", "") %>
							</td>
						</iwcm:menu>
					</tr>
					<tr>
						<td><iwcm:text key="components.basket.najnakupXML"/>:</td>
						<td nowrap="nowrap">
							<iwcm:menu name="anyPremiumService">
								<%=Tools.getBaseHref(request) %>/components/basket/najnakup_ajax.jsp?groupId=<%=pageParams.getValue("groupIds", "") %>
							</iwcm:menu>
						</td>
					</tr>
				<%} %>
				<tr>
					<td><label for="overeneZakaznikmiId"><iwcm:text key="components.basket.overeneZakaznikmi"/>:</label></td>
					<td>
						<input type="text" id="overeneZakaznikmiId" <% if (user.isEnabledItem("anyPremiumService")==false) out.print("style='display: none;'"); %> size="30" name="overeneZakaznikmi" value="<%=pageParams.getValue("overeneZakaznikmi", "") %>" />
					</td>
				</tr>
			</table>

				<%-------------------------OBSAH KOSIKU------------------------ --%>

				<div id="basketDiv" class="col-sm-10 col-sm-offset-1" style="display: none;">
					<div class="col-sm-12 form-group">
						<iwcm:text key="components.basket.editor.basket_desc"/>
					</div>
					<div class="col-sm-8 col-sm-offset-2 form-group">



					</div>
					<div id="anotherDocTable3" class="col-sm-12 form-group">
						<div class="col-sm-8">
							<iwcm:text key="components.basket.editor.order_form_doc"/>
						</div>
						<div class="col-sm-4">
							<input type="text" class="numbers" id="orderDocId" size="3" value="<%=ResponseUtils.filter(pageParams.getValue("orderFormDocId", "")) %>"/>
							<input type="button" value="<iwcm:text key="groupedit.select"/>" name="bSelDoc" onClick='popupFromDialog("/admin/user_adddoc.jsp", 450, 340);' class="button50" />
						</div>
					</div>
				</div>

				<%------------------------ MALY KOSIK--------------------%>

				<div id="basketSmallDiv" style="display: none;">
					<div class="col-sm-10 col-sm-offset1">
						<div class="form-group col-sm-12 text-center">
							<iwcm:text key="components.basket.editor.basket_small_desc"/>
						</div>
						<div class="form-group col-sm-12 hidden">
							<div class="col-sm-9" style="width:70%;">
								<input type="checkbox" id="basketSmallIcon" <%if (pageParams.getBooleanValue("withIcon", false))
									out.print("checked='checked'");%>/>
								<iwcm:text key="components.basket.editor.basket_small_icon" />
							</div>
							<div class="col-sm-3" style="width:30%;">
								<input type="text" id="referencedPage" name="referencedPage" size="3" class="numbers" value="<%=ResponseUtils.filter(pageParams.getValue("iconClickTarget", "")) %>"/>
								<input type="button" class="button50" value="<iwcm:text key="button.select"/>" onClick='popupFromDialog("/admin/user_adddoc.jsp", 450, 340);' />
							</div>
						</div>
					</div>
				</div>

				<%------------------ VLOZENIE DO KOSIKA -------------%>

				<div id="addbasketDiv" class="col-sm-10 col-sm-offset-1" style="display: none;">
					<iwcm:text key="components.basket.editor.addbasket_desc"/>
				</div>

				<%------------------ OBJEDNAVKOVY FORMULAR -------------%>

				<div id="orderDiv">
					<div class="col-sm-12">
						<div class="col-sm-10 col-sm-offset-1 form-group">
							<iwcm:text key="components.basket.editor.order_desc"/>
						</div>
						<div class="col-sm-10 col-sm-offset-1 form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.basket.editor.allow_partial_payments"/>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" id="allowPartialPayments" <%if (pageParams.getBooleanValue("allowPartialPayments", false))
									out.print("checked='checked'");%>/>
							</div>
						</div>
						<div class="col-sm-10 col-sm-offset-1 form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.basket.editor.notify_email"/>
							</div>
							<div class="col-sm-6">
								<input type="text" class="email" id="notifyEmail" style="width: 100%; float:right;" value="<%=ResponseUtils.filter(pageParams.getValue("notifyEmail", "")) %>"/>
							</div>
						</div>
					</div>
				</div>
			</form></div>
<div class="clearfix">
			<h2><iwcm:text key="components.basket.mode_of_transport"/></h2>
			<div>
				<iframe id="transportIframeWindowTab" frameborder="0" name="transportIframeWindowTab" width="100%" height="700" src="/admin/iframe_blank.jsp"></iframe>
			</div>
	</div></div>

	<div class="tab-page" id="tabMenu2">
		<iframe id="componentIframeWindowTab" frameborder="0" name="componentIframeWindowTab" width="100%" height="530" src="/admin/iframe_blank.jsp"></iframe>
	</div>
	<div class="tab-page" id="tabMenu3">
		<iframe id="invoicesIframeWindowTab" frameborder="0" name="invoicesIframeWindowTab" width="100%" height="530" src="/admin/iframe_blank.jsp"></iframe>
	</div>

		<div class="tab-page" id="tabMenu4">

					<div id="styleSelectArea" style="height: 450px; width: 100%; overflow: auto;">

						<%
						int checkedInputPosition = 0;
						IwcmFile stylesDir = new IwcmFile(Tools.getRealPath("/components/basket/admin-styles/"));
						if (stylesDir.exists() && stylesDir.canRead())
						{
							IwcmFile styleFiles[] = stylesDir.listFiles();
							styleFiles = FileTools.sortFilesByName(styleFiles);
							int counter = 0;
							for (IwcmFile file : styleFiles)
							{
								if (file.getName().endsWith(".png")==false) continue;

								// tuto kontrolu pravdepodobne netreba kedze bude bootstrap vzdy
								//if (file.getName().contains("bootstrap") && sk.iway.iwcm.common.CloudToolsForCore.isBootstrap(request)==false) continue;

								String styleValue = file.getName().substring(0, file.getName().lastIndexOf("."));

								if (styleValue.equals(style)) checkedInputPosition = counter;
								%>

									<div class="styleBox">
										<label class="image" for="style-<%=styleValue%>">
											<img src="<%=file.getVirtualPath() %>" alt="<%=styleValue%>" style="margin-left: 19px;" />
											<div class="radioSelect" style="position:relative; top:-87px;">
			  									<input type="radio" name="style" id="style-<%=styleValue%>" value="<%=styleValue%>" <%= styleValue.equals(style) ? " checked=\"checked\"" : "" %>/>
			  									<% //if ("iwcm.interway.sk".equals(request.getServerName())) out.print(styleValue); %>
			  								</div>
										</label>
									</div>
								<%
								counter++;
							}
						}
						%>
					</div>
	</div>
</div>


<script type="text/javascript">
<!--
	loadTransportsIframe();

//inicializacia poloziek
	if (isFck)
	{
		var oEditor = window.parent.InnerDialogLoaded();
		var FCK		= oEditor.FCK ;
		<% if(Tools.isNotEmpty(pageParams.getValue("groupIds", "")))
		{ %>
			$("#groupIds").val("<%=ResponseUtils.filter(pageParams.getValue("groupIds", ""))%>");
			//document.textForm.groupIds.value = "<%=ResponseUtils.filter(pageParams.getValue("groupIds", ""))%>";
	<%
		}
		else
		{ %>
			$("#groupIds").val(FCK.LinkedField.form.groupId.value);
			//document.textForm.groupIds.value = FCK.LinkedField.form.groupId.value;
	<% } %>
	}
	else
	{
		<% if(Tools.isNotEmpty(pageParams.getValue("groupIds", "")))
		{ %>
			$("#groupIds").val("<%=ResponseUtils.filter(pageParams.getValue("groupIds", ""))%>");
	<%
		}
		else
		{ %>
			$("#groupIds").val(window.opener.textForm.groupId.value);
	<% } %>

	}
//-->

	<%	if(Tools.isEmpty(ResponseUtils.filter(pageParams.getValue("orderFormDocId", "")))) 	//orderFormDocId je v parametroch iba ak je vybrany druhy radio button
	{ %>
		$("#orderFormDocTarget").attr("checked", "checked");
<%	}
	else
	{ %>
		$("#orderFormDocTarget2").attr("checked", "checked");
<%	} %>

	<%
	if (Tools.isNotEmpty(jspFileName)) {%>
		document.textForm.mainSelect.value = "<%=jspFileName%>";
		changeDisplayedDiv();
		<%}
if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("orderType", "")))) {%>
		document.textForm.orderType.value = "<%=ResponseUtils.filter(pageParams.getValue("orderType", ""))%>";
	<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("publishType", "")))) {%>
		document.textForm.publishType.value = "<%=ResponseUtils.filter(pageParams.getValue("publishType", ""))%>";
	<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("thumbWidth", "")))) {%>
		document.textForm.thumbWidth.value = "<%=ResponseUtils.filter(pageParams.getValue("thumbWidth", ""))%>";
	<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("thumbHeight", "")))) {%>
		document.textForm.thumbHeight.value = "<%=ResponseUtils.filter(pageParams.getValue("thumbHeight", ""))%>";
	<%}
	%>
</script>

<jsp:include page="/components/bottom.jsp"/>
