<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*,sk.iway.iwcm.*,org.apache.struts.util.ResponseUtils" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"%><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="menuGallery"/>
<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.gallery.GalleryActionBean" /><%

	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	pageContext.include("/sk/iway/iwcm/gallery/Gallery.action");

	PageParams pageParams = new PageParams(request);

	request.setAttribute("cmpName", "gallery");

	String forward = "/admin/spec/" + Constants.getInstallName() + "/perex_group.jsp";
	File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));

	String componentPath = Tools.getRequestURI(request).replace("editor_component.jsp", "");
%>

<%@page import="sk.iway.iwcm.gallery.*"%>
<%=Tools.insertJQuery(request)%>

<jsp:include page="/components/top.jsp"/>

<%
	if (Tools.isNotEmpty(actionBean.getPerexGroup()))
	{
		String[] perexGroupArray = GalleryDB.convertPerexGroupString(actionBean.getPerexGroup());
		String perexGroupString = GalleryDB.convertPerexGroupString(perexGroupArray);	//aby sa pri editaci skupiny spravne popresuvali, potrebujem
																												//vstup vo specialnom formate ","+groupPerex+","
		request.setAttribute("perexGroup", perexGroupString);
	}

	//out.println("docId="+docId+" groupId="+groupId+" uploadSubdir="+uploadSubdir);
%>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px; background-color: transparent; margin-left: 10px; }
	td.main { padding: 0px; }

.propertiesContent {
	padding-top: 20px;
}
.col-sm-4 {
	text-align: right;
	padding-top: 9px;
}
</style>
<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type='text/javascript'>

$(document).ready(function(){
	showHidePerexRow.call($('#alsoTags'));
	$('#alsoTags').click(showHidePerexRow);

	showHidePagingRow.call($('#pagination'));
	$('#pagination').click(showHidePagingRow);
});

function Ok()
{
	var data = {};

	if ($('#alsoTags').is(':checked')) {
		var perexGroups = [];
		$('#perexRowId select[name="disabledItemsRight"] option').each(function(){
			perexGroups.push($(this).val());
		});

		if (perexGroups.length > 0) {
			data.perexGroup = perexGroups.join("+");
		}
	}

	data.dir = $('#dir').val();
	data.recursive = $('#recursive').is(':checked');
	data.orderBy = $('#orderBy').val();
	data.orderDirection = $('#orderDirection').val();

	data.thumbsShortDescription = $('#thumbsShortDescription').is(':checked');
	data.shortDescription = $('#shortDescription').is(':checked');
	data.longDescription = $('#longDescription').is(':checked');
	data.author = $('#author').is(':checked');

	if ($('#pagination').is(':checked')) {
		data.itemsOnPage= $('#itemsOnPage').val();
	}
	else {
		data.itemsOnPage=0;
	}

	data.style = $('#style').val();

	var html = '!INCLUDE(<%= componentPath %>gallery.jsp';

	$.each(data, function(k, v) {
		html += ", " + k + "=" + v;
	});

	html += ')!';

	oEditor.FCK.InsertHtml(html);

	return true;
} // End function

if (!isFck) {
	resizeDialog(550, 600);
}

function popupExt(url, width, height,left,top)
{
	var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+",left="+left+",top="+top+";"
	popupWindow=window.open(url,"_blank",options);
}

function showHidePerexRow()
{
   if ($(this).is(":checked")) {
	   $('#perexRowId').show();
   }
   else {
	   $('#perexRowId').hide();
   }
}

function showHidePagingRow()
{
   if ($(this).is(":checked")) {
	   $('#paginationRowId').show();
   }
   else {
	   $('#paginationRowId').hide();
   }
}

function loadComponentIframe()
{
	var url = "/admin/v9/apps/gallery/?dir="+encodeURIComponent(document.textForm.elements["dir"].value);
	url = url.replaceAll("//", "/");
	 $("#componentIframeWindowTab").attr("src", url);
}

function setFileBrowserPath(path) {
	$('#dir').val(path);
}
</script>

<iwcm:menu name="menuGallery">
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li class="last"><a href="#" onclick="loadComponentIframe(); showHideTab('2');" id="tabLink2"><iwcm:text key="components.gallery.images"/></a></li>
		</ul>
	</div>
</iwcm:menu>


<div class="tab-pane toggle_content tab-pane-fullheight" style="width:790px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<iwcm:stripForm id="form" class="form-horizontal" action="<%=PathFilter.getOrigPathUpload(request)%>" method="post" name="textForm" beanclass="sk.iway.iwcm.gallery.GalleryActionBean">
			<div class="col-sm-12 propertiesContent">
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.gallery.visual_style"/>:&nbsp;
					</div>
					<div class="col-sm-8">
						<stripes:select name="style" id="style">
							<stripes:options-collection collection="${actionBean.styles}" value="first" label="second"/>
						</stripes:select>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<label for="dir"><iwcm:text key="components.gallery.dir"/></label>:
					</div>
					<div class="col-sm-8">
						<div class="input-group">
							<stripes:text name="dir" id="dir" size="60" class="form-control" />
							<span onclick="popupExt('/admin/dialog_select_dir.jsp?rootDir=/images/',400,700,window.screenX+window.outerWidth,window.screenY)" class="input-group-addon btn green">
								<i class="fa fa-link"></i>
							</span>
						</div>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4"></div>
					<div class="col-sm-8">
						<stripes:checkbox  id="recursive" name="recursive" />
						<label for="recursive"><iwcm:text key="components.gallery.also_subfolders"/></label>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.gallery.sort.mode"/>:&nbsp;
					</div>
					<div class="col-sm-8">
						<stripes:select name="orderBy" id="orderBy">
						 	<stripes:option value="title"><iwcm:text key="components.gallery.sort.alphabet"/></stripes:option>
						 	<stripes:option value="date"><iwcm:text key="components.gallery.sort.date"/></stripes:option>
						 	<stripes:option value="priority"><iwcm:text key="components.gallery.sort.priority"/></stripes:option>
						 </stripes:select>
						 <stripes:select name="orderDirection" id="orderDirection">
						 	<stripes:option value="asc"><iwcm:text key="components.gallery.sort.asc"/></stripes:option>
						 	<stripes:option value="desc"><iwcm:text key="components.gallery.sort.desc"/></stripes:option>
						 </stripes:select>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4"></div>
					<div class="col-sm-8">
						<stripes:checkbox  id="alsoTags" name="alsoTags" />
						<label for="alsoTags"><iwcm:text key="components.gallery.filter.alsoTags"/></label>
					</div>
				</div>
				<div class="form-group" id="perexRowId">
					<div class="col-sm-4">
						<iwcm:text key="components.news.perexGroup"/>:
					</div>
					<div class="col-sm-8">
						<% if (fForward.exists()) {
							pageContext.include(forward);
						}
						else {
							pageContext.include("/admin/spec/gallery_editor_perex_group.jsp");
						}
						%>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.gallery.showOnThumbsPage"/>
					</div>
					<div class="col-sm-8">
						<div>
							<stripes:checkbox  id="thumbsShortDescription" name="thumbsShortDescription" />
				   			<label for="thumbsShortDescription"><iwcm:text key="components.gallery.showShortDescription"/></label>
						</div>
						<div>
							<stripes:checkbox  id="pagination" name="pagination" />
				   			<label for="pagination"><iwcm:text key="components.news.paging"/></label>
						</div>
						<div id="paginationRowId">
							<label for="itemsOnPage"><iwcm:text key="components.news.pageSize"/>:</label>
							<stripes:text name="itemsOnPage" id="itemsOnPage" />
						</div>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.gallery.showOnDetailPage"/>
					</div>
					<div class="col-sm-8">
						<div>
							<stripes:checkbox  id="shortDescription" name="shortDescription" />
				   			<label for="shortDescription"><iwcm:text key="components.gallery.showShortDescription"/></label>
						</div>
						<div>
							<stripes:checkbox  id="longDescription" name="longDescription" />
				   			<label for="longDescription"><iwcm:text key="components.gallery.showLongDescription"/></label>
						</div>
						<div>
							<stripes:checkbox  id="author" name="author" />
				   			<label for="author"><iwcm:text key="components.gallery.showAuthor"/></label>
						</div>
					</div>
				</div>
			</div>
		</iwcm:stripForm>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTab" frameborder="0" name="componentIframeWindowTab" width="100%" height="490" src="/admin/iframe_blank.jsp"></iframe>
	</div>
</div>

<jsp:include page="/components/bottom.jsp"/>
