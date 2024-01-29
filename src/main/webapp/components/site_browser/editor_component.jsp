<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<% request.setAttribute("cmpName", "site_browser"); %>
<jsp:include page="/components/top.jsp"/>

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
var pathChanged = false;
function setFileBrowserPath(path)
{
	pathChanged = true;
	document.getElementById("rootDir").value=path;
}
function popupExt(url, width, height,left,top)
{
      var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+",left="+left+",top="+top+";"
      popupWindow=window.open(url,"_blank",options);
}

function Ok()
{


	showActualDir = false;
	if($('#showActualDir').is(':checked')){showActualDir = true;}

	rootDir = $('#rootDir').val();
	target = $('#target').val();

	oEditor.FCK.InsertHtml("!INCLUDE(/components/site_browser/site_browser.jsp, rootDir=\""+rootDir+"\", target="+target+", showActualDir="+showActualDir+")!");
	return true ;
} // End function

if (isFck)
{

}
else
{
	resizeDialog(400, 300);
}

function loadComponentIframe()
{
	var rootDir = document.textForm.elements["rootDir"].value;
	var url = "/admin/elfinder/dialog.jsp";
	url += "?inIframe=true&hideLinkInput=true&selectMode=directory&link="+encodeURIComponent(rootDir);
	$("#componentIframeWindowTab").attr("src", url);
}
</script>

<iwcm:menu name="site_browser">
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li class="last"><a href="#" onclick="loadComponentIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="menu.fbrowser"/></a></li>
		</ul>
	</div>
</iwcm:menu>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<table border="0" cellspacing="0" cellpadding="1">
			<form method=get name=textForm>
			<tr>
				<td><iwcm:text key="components.sitemap.root_group"/>:&nbsp;&nbsp;</td>
				<td nowrap="nowrap"><input type="text" id="rootDir" name="rootDir" size="40" maxlength="128" value="<%=ResponseUtils.filter(pageParams.getValue("rootDir", ""))%>">&nbsp;
					<a href="javascript:void(0);" onclick="popupExt('/admin/dialog_select_dir.jsp?rootDir=',400,700,window.screenX+window.outerWidth,window.screenY)" >
						<img border="0" src="/admin/images/icon_edit.gif" alt="<iwcm:text key="components.gallery.dialog.set.path"/>" title="<iwcm:text key="components.gallery.dialog.set.path"/>" />
					</a>
				</td>
			</tr>
			<tr>
				<td><iwcm:text key="components.site_browser.target"/>:&nbsp;&nbsp;</td>
				<td><input type=text name=target size=30 maxlength="128" id="target" value="<%=ResponseUtils.filter(pageParams.getValue("target", "_blank"))%>">&nbsp;&nbsp;</td>
			</tr>
			<tr>
				<td nowrap="nowrap"><iwcm:text key="components.site_browser.show_actual_dir"/>:&nbsp;&nbsp;</td>
				<td><input type=checkbox id="showActualDir" name=showActualDir <%if (pageParams.getBooleanValue("showActualDir", true))
						out.print("checked='checked'");%>>&nbsp;&nbsp;</td>
			</tr>
			</form>
		</table>
	</div>
	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTab" frameborder="0" name="componentIframeWindowTab" width="100%" style="height: 500px;" src="/admin/iframe_blank.jsp"></iframe>
	</div>
</div>

<jsp:include page="/components/bottom.jsp"/>
