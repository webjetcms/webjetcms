<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<iwcm:checkLogon admin="true" perms='<%=Constants.getString("webpagesFunctionsPerms")%>'/>
<%@page import="sk.iway.iwcm.gallery.*"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
request.setAttribute("cmpName", "news");
String paramPageParams = Tools.getStringValue(Tools.getRequestParameterUnsafe(request, "pageParams"), "");
String jspFileName = Tools.getRequestParameter(request, "jspFileName");

if (Tools.isEmpty(jspFileName) || jspFileName.contains("velocity")) {
	pageContext.include("editor_component_velocity.jsp");
	return;
}

String jspFileNameKalendar = "/components/calendar/news_calendar.jsp";
if (Tools.isNotEmpty(jspFileName))
{
	if (jspFileName.startsWith("!INCLUDE(")) jspFileName = jspFileName.substring(9);
	jspFileNameKalendar = jspFileName;
	request.setAttribute("descKey", jspFileName);
}
else
{
	jspFileName = "/components/news/news.jsp";
}

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
<jsp:include page="/components/top.jsp"/>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type='text/javascript'>
$(document).ready(function(){
	$('input#paging').click(function(){
		if ($('input#paging:checked').length == 0) {
			$('tr.pagingStyle').css("display","none");
		}
		else {
			$('tr.pagingStyle').css("display","table-row");
		}
	});

	//window.parent.parent.CKEDITOR.dialog.getCurrent().resize(860, 600);
});

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


function showHelp(select)
{
	size = select.options.length;
	for (i=0; i<size; i++)
	{
		el = document.getElementById("help_"+i);
		if (el!=null)
		{
			if (i==select.selectedIndex)
			{
				el.style.display = "block";
			}
			else
			{
				el.style.display = "none";
			}
		}
	}
}

function getIncludeTextNews()
{
	var typNoviniek = document.getElementById('typ_noviniek').value;

	var jspFileName = "<%=jspFileName%>";
	if (typNoviniek.indexOf(".jsp")!=-1) jspFileName = typNoviniek;

	groupIds = document.textForm.groupIds.value;
	perexGroup = "";
	options = document.textForm.perexGroup.options;
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

			orderType = document.textForm.orderType.value;
			asc = "no";
	if (document.textForm.asc.checked)
	{
		asc = "yes";
	}
	publishType = document.textForm.publishType.value;
	noPerexCheck = "no";
	if (document.textForm.noPerexCheck.checked)
	{
		noPerexCheck = "yes";
	}
	paging = "no";
	if (document.textForm.paging.checked)
	{
		paging = "yes";
	}
	pageSize = document.textForm.pageSize.value;
	cols = document.textForm.cols.value;
	image = document.textForm.image.value;
	pagingStyle = document.textForm.pagingStyle.value;
	perex = "no";
	var truncate = -1;
	if (document.textForm.perex.checked)
	{
		perex = "yes";
		truncate = document.textForm.truncate.value;
	}
	date = "no";
	if (document.textForm.date.checked)
	{
		date = "yes";
	}
	place = "no";
	if (document.textForm.place.checked)
	{
		place = "yes";
	}

	if (perexGroup != "")
	{
		perexGroup = ", perexGroup="+perexGroup;
	}

	noData = "no";
	if (document.textForm.noData.checked)
	{
		noData = "yes";
	}

	checkDuplicity = "no";
	if (document.textForm.checkDuplicity.checked)
	{
		checkDuplicity = "yes";
	}

	var expandGroupIds = false;
	if (document.textForm.expandGroupIds.checked)
		expandGroupIds = true;

	return "!INCLUDE("+jspFileName+", groupIds="+groupIds+perexGroup+", orderType="+orderType+", asc="+asc+", publishType="+publishType+", noPerexCheck="+noPerexCheck+", paging="+paging+ ", pagingStyle="+pagingStyle+", pageSize="+pageSize+", cols="+cols+", image="+image+", perex="+perex+", date="+date+", place="+place+", noData="+noData+", checkDuplicity="+checkDuplicity+", truncate="+truncate+", expandGroupIds="+expandGroupIds+")!";
}

function doOKNewsFCK()
{
	var typNoviniek = document.getElementById('typ_noviniek').value;
	if(typNoviniek.indexOf("news_calendar.jsp")==-1 && (typNoviniek == 'vypis' || typNoviniek.indexOf(".jsp")!=-1))
	{
		var includeText = getIncludeTextNews();
		oEditor.FCK.InsertHtml(includeText);
	}
	else if(typNoviniek == 'kalendar')
	{
		var form = document.textForm;
		var groupIds = form.groupIds.value;
		if(groupIds == null || groupIds == '' || groupIds == 'undefined')
		{
			alert("<iwcm:text key='components.calendar.zadajte_id_adresara'/>");
			return false;
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

		oEditor.FCK.InsertHtml("!INCLUDE(<%=jspFileNameKalendar%>, groupIds="+groupIds+(perexGroup != "" ? ", perexGroup="+perexGroup : "")+", expandGroupIds="+expandGroupIds+")!");
	}

	return true ;
} // End function


function Ok()
{
	if (doOKNewsFCK()) return true;
	else return false;
} // End function


function addOption(name, id)
{
	var form = document.textForm;
	var option = new Option(name, id, true, true);

	form.perexGroup.options[form.perexGroup.length] = option;
	//form.perexGroup.selectedIndex = form.perexGroup.length-1;
	if (document.textForm.perexGroupSearch)
		initializeDisabledItems(document.textForm, 'perexGroup', 'disabledItems');
}

function changeOption(name, id)
{
	var form = document.textForm;

	for (i=0; i < form.perexGroup.length; i++)
	{
		if (form.perexGroup.options[i].value == id)
		{
			form.perexGroup.options[i].text = name;
			if (document.textForm.perexGroupSearch)
				initializeDisabledItems(document.textForm, 'perexGroup', 'disabledItems');
			return;
		}
	}
}

function removeOption()
{
	if (document.textForm.perexGroupSearch)
	{
		var form = document.textForm;
		var selected = form.disabledItemsLeft.value;
		for (i=0; i<form.perexGroup.options.length; i++)
		{
			if(form.perexGroup.options[i].value==selected)
			{
				form.perexGroup.options[i] = null;
				i = form.perexGroup.options.length;
			}
		}
		initializeDisabledItems(document.textForm, 'perexGroup', 'disabledItems');
	}
	else
	{
		var form = document.textForm;
		var selected = form.perexGroup.selectedIndex;
		form.perexGroup.options[form.perexGroup.selectedIndex] = null;
	}
}



if (isFck)
{

}
else
{
	resizeDialog(570, 750);
}

function changeTyp(sel)
{
	if(sel.value == 'vypis' || sel.value.indexOf(".jsp")!=-1)
	{
		$(".novinky").show();
		$(".kalendar").hide();
	}
	else if(sel.value == 'kalendar')
	{
		$(".kalendar").show();
		$(".novinky").hide();
	}
}

function loadComponentIframe()
{
	var url = "/apps/news/admin/?include="+encodeURIComponent(getIncludeTextNews());
 	$("#componentIframeWindowTab").attr("src", url);
}
</script>

<iwcm:menu name="cmp_news">
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs" style="background-color:transparent;">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li class="last"><a href="#" onclick="loadComponentIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.news.title"/></a></li>
		</ul>
	</div>
</iwcm:menu>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block; width:840px;">

		<table border="0" cellspacing="0" cellpadding="1">

			<tr>
				<td><iwcm:text key="components.news.vyberte_typ_noviniek"/>:</td>
				<td>
					<select name="typ_noviniek" id="typ_noviniek" onchange="changeTyp(this);" class="form-control">
						<option value="/components/news/news.jsp"><iwcm:text key="components.news.vypis_noviniek"/></option>
						<%
						String newsComponentsDirs = "/components/"+Constants.getInstallName()+"/news/";
						if (Tools.isNotEmpty(Constants.getString("newsComponentsDirs"))) newsComponentsDirs+=","+Constants.getString("newsComponentsDirs");

						//prejdi vsetky adresare rekurzivne a vypis z nich komponenty zacinajuce na news-
						String newsComponentsDirsArr[] = Tools.getTokens(newsComponentsDirs, ",");
						if (Tools.isNotEmpty(jspFileName)) jspFileName = jspFileName.trim();
						for (String dirName : newsComponentsDirsArr)
						{
							IwcmFile dir = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(dirName));
							if (dir.exists() == false || dir.canRead()==false) continue;

							IwcmFile files[] = FileTools.sortFilesByName(dir.listFiles());
							boolean fileFound = false;
							for (IwcmFile file : files)
							{
								if (file.getName().startsWith("news-")==false && file.getName().startsWith("news_")==false) continue;
								if (file.canRead()==false) continue;

								String nameForUser = file.getVirtualPath();
								if (nameForUser.startsWith("/components/")) nameForUser = nameForUser.substring(11);
								if (nameForUser.startsWith("/"+Constants.getInstallName()+"/")) nameForUser = nameForUser.substring(Constants.getInstallName().length()+1);
								if (nameForUser.startsWith("/news/")) nameForUser = nameForUser.substring("news".length()+2);
								if (nameForUser.endsWith(".jsp")) nameForUser = nameForUser.substring(0, nameForUser.lastIndexOf(".jsp"));

								String virtualPath = file.getVirtualPath();
								if (virtualPath.startsWith("/components/"+Constants.getInstallName())) virtualPath = "/components/"+virtualPath.substring(Constants.getInstallName().length()+13);

								out.print("<option value=\""+Tools.escapeHtml(virtualPath)+"\"");
								if (file.getVirtualPath().equals(jspFileName) || virtualPath.equals(jspFileName))
								{
									out.print(" selected='selected'");
									fileFound = true;
								}
								out.println(">"+Tools.escapeHtml(nameForUser)+"</option>");
							}
							if (fileFound == false && Tools.isNotEmpty(jspFileName) && jspFileName.equals("/components/news/news.jsp")==false)
							{
								out.print("<option value=\""+Tools.escapeHtml(jspFileName)+"\"");
								out.print(" selected='selected'");
								out.println(">"+Tools.escapeHtml(jspFileName)+"</option>");
							}

						}
						%>
						<option value="kalendar"><iwcm:text key="components.news.kalendar_noviniek"/></option>
					</select>
				</td>
			</tr>
		</table>



		<form action="/admin/editor.do" onsubmit="return false" name="textForm" type="sk.iway.iwcm.editor.EditorForm">
		<table border="0" cellspacing="0" cellpadding="1">
			<tr>
				<td style="width:200px !important;"><iwcm:text key="components.news.groupids"/>:</td>
				<td>
					<div class="input-group">
						<input type="text" name="groupIds" class="form-control" size=30 maxlength="255">
						<span class="input-group-btn">
							<input type="button" class="btn green" name="groupSelect" value="<iwcm:text key="components.news.addgroup"/>" onClick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp", 500, 500);'>
                  </span>
					</div>
				</td>
			</tr>
			<tr>
				<td></td>
				<td>

				</td>
			</tr>
			<tr>
				<td><iwcm:text key="components.calendar_news.zahrnut_podadresare"/>:</td>
				<td colspan="2"><input type="checkbox" name="expandGroupIds" value="true" <%if (pageParams.getBooleanValue("expandGroupIds", true)) out.print("checked='checked'");%>/></td>
			</tr>
			<tr>
				<td style="width:200px" valign="top"><iwcm:text key="components.news.perexGroup"/>:</td>
				<td style="width: 600px;">
					<%
						//otestuj ci existuje nahrada za tuto stranku
						String forward = "/admin/spec/"+Constants.getInstallName()+"/perex_group.jsp";
						File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
						if (fForward.exists())
						{
							pageContext.include(forward);
						}
						else
						{
							pageContext.include("/admin/spec/gallery_editor_perex_group.jsp");	//presuva skupiny do praveho stlpca podla perexGroupString
						}
						%>
				</td>
			</tr>
			<tr class="novinky">
				<td><iwcm:text key="components.news.ordertype"/>:</td>
				<td>
					<select name="orderType" class="form-control">
						<option value="priority"><iwcm:text key="components.news.ORDER_PRIORITY"/></option>
						<option value="date"><iwcm:text key="components.news.ORDER_DATE"/></option>
						<option value="eventDate"><iwcm:text key="components.news.ORDER_EVENT_DATE"/></option>
						<option value="saveDate"><iwcm:text key="components.news.ORDER_SAVE_DATE"/></option>
						<option value="title"><iwcm:text key="components.news.ORDER_TITLE"/></option>
						<option value="place"><iwcm:text key="components.news.ORDER_PLACE"/></option>
						<option value="id"><iwcm:text key="components.news.ORDER_ID"/></option>
					</select>

				</td>
			</tr>
			<tr>
				<td><iwcm:text key="components.news.asc"/></td>
				<td>
					 <input type="checkbox" name="asc"  value="yes" <%if (pageParams.getBooleanValue("asc", false))
						out.print("checked='checked'");%> />
				</td>
			</tr>

			<tr class="novinky">
				<td valign="top"><iwcm:text key="components.news.publishtype"/>:</td>
				<td>
					<select name="publishType" class="form-control">
						<option value="new"><iwcm:text key="components.news.PUBLISH_NEW"/></option>
						<option value="old"><iwcm:text key="components.news.PUBLISH_OLD"/></option>
						<option value="all"><iwcm:text key="components.news.PUBLISH_ALL"/></option>
						<option value="next"><iwcm:text key="components.news.PUBLISH_NEXT"/></option>
					</select>
					<br>

				</td>
			</tr>
			<tr>
				<td><iwcm:text key="components.news.noPerexCheck"/>&nbsp; &nbsp;</td>
				<td>
					<input type="checkbox" name="noPerexCheck" value="yes" <%if (pageParams.getBooleanValue("noPerexCheck", false))
						out.print("checked='checked'");%>>
				</td>
			</tr>

			<tr class="novinky">
				<td nowrap><iwcm:text key="components.news.paging"/>:</td>
				<td>
					<input id="paging" type="checkbox"  name="paging" <%if (pageParams.getBooleanValue("paging", true)) out.print("checked='checked'");%> value="yes" >
				</td>
				<td rowspan=8 align="right" valign="top">

				</td>
			</tr>
			<tr class="novinky pagingStyle">
				<td style="width:200px"><iwcm:text key="components.news.paging.style.title"/>:</td>
				<td>
					<select name="pagingStyle" class="form-control" class="form-control">
						<option value="bottom"><iwcm:text key="components.news.paging.style.bottom"/></option>
						<option value="top"><iwcm:text key="components.news.paging.style.top"/></option>
						<option value="both"><iwcm:text key="components.news.paging.style.both"/></option>
					</select>
				</td>
			</tr>
			<tr class="novinky">
				<td width="140"><iwcm:text key="components.news.pageSize"/>:</td>
				<td width="80">
					<input type="text" class="form-control" name="pageSize" size=5 maxlength="5" value="<%=pageParams.getIntValue("pageSize", 15)%>">
				</td>
			</tr>

			<tr class="novinky">
				<td><iwcm:text key="components.news.cols"/>:</td>
				<td>
					<input type="text" class="form-control" name="cols" size=5 maxlength="5" value="<%=pageParams.getIntValue("cols", 1)%>">
				</td>
			</tr>
			<tr class="novinky">
				<td><iwcm:text key="components.news.image"/>:</td>
				<td>
					<select name="image" class="form-control">
						<option value="none"><iwcm:text key="components.news.img_none"/></option>
						<option value="top"><iwcm:text key="components.news.img_left"/></option>
						<option value="bottom"><iwcm:text key="components.news.under_header"/></option>
					</select>
				</td>
			</tr>
			<tr class="novinky">
				<td nowrap><iwcm:text key="components.news.perex"/>:</td>
				<td>
					<input type="checkbox" id="perex" name="perex" <%if (pageParams.getBooleanValue("perex", true)) out.print("checked='checked'");%> value="yes" onchange="showHideRow('truncate', $('#perex').is(':checked'))">
				</td>
			</tr>
			<tr class="novinky" id="truncate">
				<td nowrap><iwcm:text key="components.news.truncate"/>:</td>
				<td>
					<input type="text" class="form-control" name="truncate" size=5 maxlength="5" value="<%=pageParams.getIntValue("truncate", -1)%>">
				</td>
			</tr>
			<tr class="novinky">
				<td><iwcm:text key="components.news.date"/>:</td>
				<td>
					<input type="checkbox" name="date" <%if (pageParams.getBooleanValue("date", true)) out.print("checked='checked'");%> value="yes">
				</td>
			</tr>
			<tr class="novinky">
				<td><iwcm:text key="components.news.place"/>:</td>
				<td>
					<input type="checkbox" name="place" <%if (pageParams.getBooleanValue("place", true)) out.print("checked='checked'");%> value="yes">
				</td>
			</tr>
			<tr class="novinky">
				<td><iwcm:text key="components.news.no_data"/>:</td>
				<td>
					<input type="checkbox" name="noData" value="true" <%if (pageParams.getBooleanValue("noData", true)) out.print("checked='checked'");%>>
				</td>
			</tr>
			<tr class="novinky">
				<td><iwcm:text key="components.news.check_duplicty"/>:</td>
				<td>
					<input type="checkbox" name="checkDuplicity" value="true" <%if (pageParams.getBooleanValue("checkDuplicity", false)) out.print("checked='checked'");%>>
				</td>
			</tr>
			<tr>
				<td>&nbsp;</td>
				<td>&nbsp;</td>
			</tr>
		</table>
		</form>

	</div>
	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTab" frameborder="0" name="componentIframeWindowTab" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>
</div>

<script type="text/javascript">
//inicializacia poloziek
if (isFck)
{
	var oEditor = window.parent.InnerDialogLoaded();
	var FCK		= oEditor.FCK ;
<% if(Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("groupIds", ""))))
	{ %>
		document.textForm.groupIds.value = "<%=ResponseUtils.filter(pageParams.getValue("groupIds", ""))%>";
<%
	}
	else
	{ %>
		document.textForm.groupIds.value = FCK.LinkedField.form.groupId.value;
<% } %>
}
else
{
	<% if(Tools.isNotEmpty(pageParams.getValue("groupIds", "")))
	{ %>
		document.textForm.groupIds.value = "<%=ResponseUtils.filter(pageParams.getValue("groupIds", ""))%>";
<%
	}
	else
	{ %>
		document.textForm.groupIds.value = window.opener.textForm.groupId.value;
<% } %>
}

<% if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("orderType", "")))) {%>
document.textForm.orderType.value = "<%=ResponseUtils.filter(pageParams.getValue("orderType", ""))%>";
<%}

	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("image", "")))) {%>
document.textForm.image.value = "<%=ResponseUtils.filter(pageParams.getValue("image", ""))%>";
<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("pagingStyle", "")))) {%>
	document.textForm.pagingStyle.value = "<%=ResponseUtils.filter(pageParams.getValue("pagingStyle", ""))%>";
<%}
if (Tools.isNotEmpty(pageParams.getValue("publishType", ""))) {%>
	document.textForm.publishType.value = "<%=ResponseUtils.filter(pageParams.getValue("publishType", ""))%>";
<%}%>

showHideRow('truncate', $('#perex').is(':checked'));
</script>

<jsp:include page="/components/bottom.jsp"/>
