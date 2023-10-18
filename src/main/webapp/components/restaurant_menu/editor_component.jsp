<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<iwcm:checkLogon admin="true" perms="cmp_restaurant_menu"/>
<%@page import="sk.iway.iwcm.gallery.*"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
request.setAttribute("cmpName", "restaurant_menu");
request.setAttribute("descKey", "components.restaurant_menu.desc");
Prop prop = Prop.getInstance(request);
%>
<jsp:include page="/components/top.jsp"/>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px !important; }
	td.main { padding: 0px; }
</style>

<%
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

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
String style = pageParams.getValue("style", "01");
%>
<script type='text/javascript'>
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



function Ok()
{
	var mena = document.textForm.mena.value;
	var typMenu = $(".styleBox input:checked").val();
	var includeText = "!INCLUDE(/components/restaurant_menu/menu.jsp, style="+typMenu+", mena="+mena+")!";
	oEditor.FCK.InsertHtml(includeText);

	return true ;
} // End function

function loadListMealsIframe()
{
	var url = "/components/restaurant_menu/admin_list_meals.jsp";
 	$("#listMealsIframeWindowTab").attr("src", url);
}
function loadListMenuIframe()
{
	var url = "/components/restaurant_menu/admin_list_menu.jsp";
 	$("#listMenuIframeWindowTab").attr("src", url);
}
function loadNewMenuIframe()
{
	var url = "/components/restaurant_menu/admin_new_menu.jsp";
 	$("#newMenuIframeWindowTab").attr("src", url);
}
</script>
<style type="text/css">

	.styleBox {display: block; position: relative; width: 480px; height: 160px; background: #fff; margin: 3px; padding: 10px; border: 1px solid #bcbcbc; border-radius: 4px;}
	* HTML BODY .styleBox {width: 402px; height: 180px;}

	.boxes .styleBox {height: 110px;}
	* HTML BODY .boxes .styleBox {height: 130px;}

	.styleBox .radioSelect { position: absolute; left: 0; top: 0; text-align: left; width: 100%; height: 100%;}
	.styleBox .radioSelect input {position: absolute; left: 10px; top: 80px; border: 0px none;}
	.boxes .styleBox .radioSelect input  {top: 55px;}
	.styleBox img  {position: absolute; top: 10px; left: 42px;}

	div.colBox {display: block; float: left; margin: 10px 10px 0 0; padding: 0;  width: 408px; overflow: auto;}

	div.clearer {width: 100%; clear: both; height: 0; line-height: 0; font-size: 0; display: block; visibility: hidden;}

	select { width: 300px; }
	input { padding-left: 4px; }
</style>
<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.settings"/></a></li>
		<li><a href="#" onclick="loadListMealsIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.restaurant_menu.mealsList"/></a></li>
		<li><a href="#" onclick="loadNewMenuIframe();showHideTab('4');" id="tabLink4"><iwcm:text key="components.restaurant_menu.newMenu"/></a></li>
		<li class="last"><a href="#" onclick="loadListMenuIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.restaurant_menu.listMenu"/></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content" style="height:840px; overflow: auto; width:1090px;">

	<div class="tab-page" id="tabMenu1" style="display: block; padding: 15px;">

		<form name="textForm" style="margin: 0px">

		<div id="cal_udalosti" style="width: 900px; height: 900px;">
		<table>
		<tr><th colspan="2" > <iwcm:text key="components.gis.settingsInfo"/>  </th></tr>
		<tr>
			<td><label for="menaId"><iwcm:text key="components.restaurant_menu.mena"/>:</label></td>
			<td>
				<input type="text" id="menaId" name="mena" value="<%=pageParams.getValue("mena", "€") %>" />
			</td>
		</tr>
		</table>
		<table border="0">
			<tr>
				<td valign="top">
					<strong><iwcm:text key="components.restaurant_menu.visualSettings"/>:</strong>

					<div id="styleSelectArea" style="height: 470px; width: 430px; overflow: auto;">

						<%
						int checkedInputPosition = 0;
						IwcmFile stylesDir = new IwcmFile(Tools.getRealPath("/components/restaurant_menu/menu-styles"));
						if (stylesDir.exists() && stylesDir.canRead())
						{
							IwcmFile styleFiles[] = stylesDir.listFiles();
							styleFiles = FileTools.sortFilesByName(styleFiles);
							int counter = 0;
							for (IwcmFile file : styleFiles)
							{
								if (file.getName().endsWith(".png")==false) continue;
								if (file.getName().contains("bootstrap") && sk.iway.iwcm.common.CloudToolsForCore.isBootstrap(request)==false) continue;

								String styleValue = file.getName().substring(0, file.getName().lastIndexOf("."));

								if (styleValue.equals(style)) checkedInputPosition = counter;
								%>

									<div class="styleBox">
										<label class="image" for="style-<%=styleValue%>">
											<img src="<%=file.getVirtualPath() %>" alt="<%=styleValue%>" />
											<div class="radioSelect">
			  									<input type="radio" name="style" id="style-<%=styleValue%>" value="<%=styleValue%>" <%= styleValue.equals(style) ? " checked=\"checked\"" : "" %> />
			  									<% if ("iwcm.interway.sk".equals(request.getServerName())) out.print(styleValue); %>
			  								</div>
										</label>
									</div>
								<%
								counter++;
							}
						}
						%>
					</div>
				</td>
				<td valign="top" style="padding-left: 20px; width:100%;">
					<table style="width:460px;" border="0" cellspacing="0" cellpadding="1">
					<tr>
                    <td style="width:460px;">
	<h2><iwcm:text key="components.restaurant_menu.tutorial"/></h2>
	<ul>
	<li style="margin-bottom:7px"><iwcm:text key="components.restaurant_menu.tutorial_step1"/></li>
	<li style="margin-bottom:7px"><iwcm:text key="components.restaurant_menu.tutorial_step2"/></li>
	<li style="margin-bottom:7px"><iwcm:text key="components.restaurant_menu.tutorial_step3"/></li>
	<li style="margin-bottom:7px"><iwcm:text key="components.restaurant_menu.tutorial_step4"/></li>
	</ul>
    </td>
	</tr>
					</table>
				</td>
			</tr>
		</table>
		</div>
		</form>
		</div>

	<div class="tab-page" id="tabMenu2">
		<iframe id="listMealsIframeWindowTab" frameborder="0" name="listMealsIframeWindowTab" width="960" height="490" src="/admin/iframe_blank.jsp"></iframe>
	</div>
	<div class="tab-page" id="tabMenu3">
		<iframe id="listMenuIframeWindowTab" frameborder="0" name="listMenuIframeWindowTab" width="960" height="490" src="/admin/iframe_blank.jsp"></iframe>
	</div>
	<div class="tab-page" id="tabMenu4">
		<iframe id="newMenuIframeWindowTab" frameborder="0" name="newMenuIframeWindowTab" width="960" height="490" src="/admin/iframe_blank.jsp"></iframe>
	</div>
</div>

<jsp:include page="/components/bottom.jsp"/>
