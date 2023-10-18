<%@page import="sk.iway.iwcm.users.SettingsAdminDB"%>
<%@page import="sk.iway.iwcm.calendar.EventTypeDetails"%>
<%@page import="java.util.List"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_calendar"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.calendar.EventTypeDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.gallery.GalleryDB"%>
<%
request.setAttribute("cmpName", "calendar");
Identity actualUser = (Identity) session.getAttribute(Constants.USER_KEY);
List<EventTypeDetails> types = EventTypeDB.getTypes();
types = SettingsAdminDB.filterBeansByUserAllowedCategories(types,"type",actualUser,"cmp_calendar");
request.setAttribute("types", types);

%>
<jsp:include page="/components/top.jsp"/>
<%
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
//out.println("paramPageParams="+paramPageParams);
String jspFileName = ResponseUtils.filter(request.getParameter("jspFileName"));

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("perexGroup", ""))))
{
	String[] perexGroupArray = GalleryDB.convertPerexGroupString(ResponseUtils.filter(pageParams.getValue("perexGroup", "")));
	String perexGroupString = GalleryDB.convertPerexGroupString(perexGroupArray);	//aby sa pri editaci skupiny spravne popresuvali, potrebujem
																										//vstup vo specialnom formate ","+groupPerex+","
	request.setAttribute("perexGroup", perexGroupString);
}
%>
<script type='text/javascript'>


function setParentGroupId(returnValue)
{
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

function changeTyp(value)
{
	if(value == 1)
	{
		document.getElementById('cal_noviniek').style.display = 'none';
		document.getElementById('cal_udalosti').style.display = 'inline';
	}
	else
	{
		document.getElementById('cal_udalosti').style.display = 'none';
		document.getElementById('cal_noviniek').style.display = 'inline';
	}
}

function Ok()
{
	var form = document.textForm;
	var typKalendara = form.typ_kalendara.value;

	if(typKalendara == 1)
	{
		var types = null;
		var options = form.typ;
		for (i=0; i<options.length; i++)
		{
			if (options[i].checked)
			{
				if (types==null) types = options[i].value;
				else types += "+" + options[i].value
			}
		}
		if (types==null) types = "";
		oEditor.FCK.InsertHtml("!INCLUDE(/components/calendar/calendar.jsp, typyNazvy=\""+types+"\")!");
	}
	else
	{
		var groupIds = form.groupIds.value;
		if(groupIds == null || groupIds == '' || groupIds == 'undefined')
		{
			alert("<iwcm:text key='components.calendar.zadajte_id_adresara'/>");
			return;
		}
		var perexGroup = "";
		options = form.perexGroup.options;
		len = options.length;
		for (i=0; i<len; i++)
		{
			if (options[i].selected)
			{
				if (perexGroup == "")
				{
					perexGroup = options[i].value;
				}
				else
				{
					perexGroup = perexGroup + "+" + options[i].value;
				}
			}
		}
		var expandGroupIds = false;
		if (form.expandGroupIds.checked)
			expandGroupIds = true;

		<% if (Tools.isEmpty(paramPageParams)) { //pri prvom vlozeni vlozime rovno aj news komponentu %>
			oEditor.FCK.InsertHtml("!INCLUDE(/components/news/news.jsp, groupIds="+groupIds+(perexGroup != "" ? ", perexGroup="+perexGroup : "")+", expandGroupIds="+expandGroupIds+", orderType=priority, asc=no, publishType=next, noPerexCheck=no, paging=yes, pagingStyle=bottom, pageSize=15, cols=1, image=none, perex=yes, date=yes, place=yes, noData=yes, checkDuplicity=no, truncate=-1)!");
		<% } %>

		oEditor.FCK.InsertHtml("!INCLUDE(/components/calendar/news_calendar.jsp, groupIds="+groupIds+(perexGroup != "" ? ", perexGroup="+perexGroup : "")+", expandGroupIds="+expandGroupIds+")!");
	}
	return true ;
} // End function
function loadListIframe()
{
	var url = "/admin/listevents.do";
	 $("#componentIframeWindowTabList").attr("src", url);
}
function loadTypeIframe()
{
	var url = "/components/calendar/admin_edit_type.jsp";
	 $("#componentIframeWindowTabType").attr("src", url);
}
function loadRejectedIframe()
{
	var url = "/components/calendar/admin_neschvalene_udalosti.jsp";
	 $("#componentIframeWindowTabRejected").attr("src", url);
}
function loadRecommendedIframe()
{
	var url = "/components/calendar/admin_suggest_evens.jsp";
	 $("#componentIframeWindowRecommended").attr("src", url);
}
</script>

<iwcm:menu name="menuCalendar">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li><a href="#" onclick="loadListIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.calendar.list_of_events"/></a></li>
			<li><a href="#" onclick="loadTypeIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="calendar_edit.configType"/></a></li>
			<li><a href="#" onclick="loadRejectedIframe();showHideTab('4');" id="tabLink4"><iwcm:text key="calendar.neschvalene_udalosti"/></a></li>
			<li class="last"><a href="#" onclick="loadRecommendedIframe();showHideTab('5');" id="tabLink5"><iwcm:text key="calendar.suggest_evens"/></a></li>
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
		.formRow {
			margin-bottom: 15px;
		}

	</style>
<div class="tab-pane toggle_content tab-pane-fullheight" style="width:900px !important;">
	<div class="tab-page" id="tabMenu1" style="display: block;">

	<form name="textForm" style="margin: 0px">
		<div class="col-sm-12">
			<div class="col-sm-6 col-sm-offset-3 formRow">
				<div class="col-sm-6 leftCol"><label for="typ_kalendara"><iwcm:text key="components.calendar.vyberte_kalendar"/></label></div>
				<div class="col-sm-6">
					<select name="typ_kalendara" id="typ_kalendara" onchange="changeTyp(this.value);">
						<option value="1"><iwcm:text key="components.calendar.kalendar_udalosti"/></option>
						<option value="2"><iwcm:text key="components.calendar.kalendar_noviniek"/></option>
					</select>
				</div>
			</div>
			<div id="cal_udalosti" class="col-sm-6 col-sm-offset-3 formRow">
				<p><iwcm:text key="components.calendar.zvolte_typy_udalosti"/></p>
				<p>
					<div class="col-sm-8 col-sm-offset-2">
						<logic:iterate id="type" name="types" type="sk.iway.iwcm.calendar.EventTypeDetails">
						   <input type="checkbox" name="typ" value="<bean:write name="type" property="name"/>" <%if (ResponseUtils.filter(pageParams.getValue("typyNazvy", "")).indexOf(type.getName()) != -1)
									out.print("checked='checked'");%>>&nbsp;&nbsp;<bean:write name="type" property="name"/><br/>
						</logic:iterate>
					</div>
				</p>
			</div>
			<div id="cal_noviniek" style="display: none;">
				<div class="col-sm-12 formRow">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.news.groupids"/>
					</div>
					<div class="col-sm-8">
						<input type="text" name="groupIds" size=30 maxlength="255" value="<%=pageParams.getValue("groupIds", "")%>"/>
						<input type="button" class="button50" name="groupSelect" value="<iwcm:text key="components.news.addgroup"/>" onClick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp", 500, 500);'>
					</div>
				</div>
				<div class="col-sm-12 formRow">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.calendar_news.zahrnut_podadresare"/>
					</div>
					<div class="col-sm-8">
						<input type="checkbox" name="expandGroupIds" value="true" <%if (pageParams.getBooleanValue("expandGroupIds", true)) out.print("checked='checked'");%>/>
					</div>
				</div>
				<div class="col-sm-12">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.news.perexGroup"/>
					</div>
					<div class="col-sm-8">
						<%
						//otestuj ci existuje nahrada za tuto stranku
						String forward = "/admin/spec/"+Constants.getInstallName()+"/perex_group.jsp";
						File fForward = new java.io.File(sk.iway.iwcm.Constants.getServletContext().getRealPath(forward));
						if (fForward.exists())
						{
							pageContext.include(forward);
						}
						else
						{
							pageContext.include("/admin/spec/gallery_editor_perex_group.jsp");	//presuva skupiny do praveho stlpca podla perexGroupString
						}
						%>
					</div>
				</div>
			</div>
		</div>

	</form>

	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTabList" frameborder="0" name="componentIframeWindowTabList" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu3">
		<iframe id="componentIframeWindowTabType" frameborder="0" name="componentIframeWindowTabType" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu4">
		<iframe id="componentIframeWindowTabRejected" frameborder="0" name="componentIframeWindowTabRejected" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu5">
		<iframe id="componentIframeWindowRecommended" frameborder="0" name="componentIframeWindowTabRecommended" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

</div>

<% if (jspFileName != null && jspFileName.indexOf("news_calendar")!=-1) { %>
<script type="text/javascript">
	changeTyp(2);
	document.textForm.typ_kalendara.selectedIndex=1;
</script>
<% } %>

<jsp:include page="/components/bottom.jsp"/>
