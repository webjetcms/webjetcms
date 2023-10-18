<%@page import="java.util.HashMap"%>
<%@page import="java.util.TreeMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="net.sourceforge.stripes.action.ActionBean"%>
<%@page import="sk.iway.iwcm.components.news.FieldEnum"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<iwcm:checkLogon admin="true" perms="cmp_news"/><%

request.setAttribute("cmpName", "news");
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

if (Tools.isNotEmpty(paramPageParams)) {
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

pageContext.include("/sk/iway/iwcm/components/news/News.action");
%>
<jsp:include page="/components/top.jsp"/>

<style type="text/css">
.searchBox {margin: 20px 35px;}
</style>

<script>
function doOK() {
	window.close();
}

function filter(tabPage, searchOriginal) {
	tabPage.find('.panel').show();

	search = $.trim(searchOriginal).toLowerCase();
	search = internationalToEnglish(search);

	if (search.length > 0) {
		tabPage.find('.panel').each(function(i,v){
			var panel = $(this);
			var title = internationalToEnglish($.trim(panel.find('.panel-title').text()).toLowerCase());
			var text = internationalToEnglish($.trim(panel.find('.panel-body').text()).toLowerCase());

			if (title.indexOf(search) != -1 || text.indexOf(search) != -1) {
				console.log('show');
				panel.show();
			}
			else {
				panel.hide();
			}
		});
	}

	if (tabPage.find('.panel:visible').length > 0) {
		tabPage.find('.no-records .searchString').text("");
		tabPage.find('.no-records').addClass('hidden');
	}
	else {
		tabPage.find('.no-records .searchString').text(searchOriginal);
		tabPage.find('.no-records').removeClass('hidden');
	}
}

function sort(tabPage, alphabetical) {
	var box = tabPage.find('.panel').parent();
	var panels;

	panels = tabPage.find('.panel').detach().sort(function(a, b){
		if (alphabetical) {
			var aTitle = internationalToEnglish($.trim($(a).text()).toLowerCase());
			var bTitle = internationalToEnglish($.trim($(b).text()).toLowerCase());

			return aTitle.localeCompare(bTitle);
		}
		else {
			var aSort = $(a).data('sort');
			var bSort = $(b).data('sort');

			return aSort > bSort ? 1 : -1;
		}
	});

	box.append(panels);
}

$(function(){
	$('.searchBox :submit').click(function(){

		var el = $(this);
		var tabPage = el.closest('.tab-page');
		var searchText = $('#search').val();

		filter(tabPage, searchText);
		return false;
	});

	$('.searchBox #search').keyup(function(){
		var el = $(this);
		var searchText = el.val();
		var tabPage = el.closest('.tab-page');
		filter(tabPage, searchText);
	});

	$('input[name="sort"]').on('click', function () {

		var el = $(this);
		var tabPage = el.closest('.tab-page');
		var inputs = tabPage.find('input[name="sort"]');

		inputs.closest('label').removeClass('active');
		el.closest('label').addClass('active');

		var alphabet = el.prop('id') == 'sort-priority-alphabet';
		sort(tabPage, alphabet);
	 })
});
</script>

<iwcm:menu name="menuGallery">
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs" style="background-color:transparent;">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1">Web stránka (DocDetails)</a></li>
			<li class=""><a href="#" onclick="showHideTab('2');" id="tabLink2">Adresár (GroupDetails)</a></li>
		</ul>
	</div>
</iwcm:menu>

<iwcm:stripForm action="/admin/editor.do" class="newsForm form-horizontal" name="textForm" beanclass="sk.iway.iwcm.components.news.NewsActionBean">
	<div class="tab-pane toggle_content tab-pane-fullheight">
		<div class="tab-page" id="tabMenu1" style="display: block;">


			<div class="searchBox">
				<div class="form-horizontal">
					<div class="form-group">
    					<label for="search" class="col-sm-2 control-label">Vyhľadávanie</label>
						<input type="text" class="form-control" id="search" placeholder="napr. title">
					</div>

					<div class="form-group">
						<label for="sort" class="col-sm-2 control-label">Zoradenie</label>
						<div class="btn-group" data-toggle="buttons">
							<label class="btn btn-primary active">
								<input type="radio" name="sort" id="sort-priority-default" autocomplete="off" checked> Default
							</label>
							<label class="btn btn-primary">
								<input type="radio" name="sort" id="sort-priority-alphabet" autocomplete="off"> Abecedne
							</label>
						</div>
					</div>
				</div>
			</div>

			<div class="col-xs-12">




				<c:forEach items="${docDetailsHelp}" var="item" varStatus="status">
					<div class="panel panel-default" data-sort="${status.index}">
						<div class="panel-heading">
							<h3 class="panel-title">${item.key}</h3>
						</div>
						<div class="panel-body">
							${item.value}
						</div>
					</div>
				</c:forEach>

				<p class="bg-warning hidden no-records">Žiadny záznam nevyhovuje zadanému vyhľadávaniu (<span class="searchString"></span>)</p>

			</div>
		</div>

		<div class="tab-page" id="tabMenu2">

			<div class="searchBox">
				<div class="form-horizontal">
					<div class="form-group">
    					<label for="search" class="col-sm-2 control-label">Vyhľadávanie</label>
						<input type="text" class="form-control" id="search" placeholder="napr. title">
					</div>

					<div class="form-group">
						<label for="sort" class="col-sm-2 control-label">Zoradenie</label>
						<div class="btn-group" data-toggle="buttons">
							<label class="btn btn-primary active">
								<input type="radio" name="sort" id="sort-priority-default" autocomplete="off" checked> Default
							</label>
							<label class="btn btn-primary">
								<input type="radio" name="sort" id="sort-priority-alphabet" autocomplete="off"> Abecedne
							</label>
						</div>
					</div>
				</div>
			</div>

			<div class="col-xs-12">

				<%
					Map<String, String> groupDetailsHelp = new HashMap<>();

					groupDetailsHelp.put("defaultDocId", "");
					groupDetailsHelp.put("documents", "");
					groupDetailsHelp.put("domainName", "");
					groupDetailsHelp.put("fieldA", "");
					groupDetailsHelp.put("fieldB", "");
					groupDetailsHelp.put("fieldC", "");
					groupDetailsHelp.put("fieldD", "");
					groupDetailsHelp.put("fullPath", "");
					groupDetailsHelp.put("groupId", "");
					groupDetailsHelp.put("groupIdName", "");
					groupDetailsHelp.put("groupName", "");
					groupDetailsHelp.put("groupNameJS", "");
					groupDetailsHelp.put("groupNameShort", "");
					groupDetailsHelp.put("groupNameShortNoJS", "");
					groupDetailsHelp.put("htmlHead", "");
					groupDetailsHelp.put("installName", "");
					groupDetailsHelp.put("linkGroupId", "");
					groupDetailsHelp.put("loggedMenuType", "");
					groupDetailsHelp.put("logonPageDocId", "");
					groupDetailsHelp.put("menuType", "");
					groupDetailsHelp.put("navbar", "");
					groupDetailsHelp.put("navbarName", "");
					groupDetailsHelp.put("navbarNameNoAparam", "");
					groupDetailsHelp.put("newPageDocIdTemplate", "");
					groupDetailsHelp.put("parentFullPath", "");
					groupDetailsHelp.put("parentGroupId", "");
					groupDetailsHelp.put("passwordProtected", "");
					groupDetailsHelp.put("sortPriority", "");
					groupDetailsHelp.put("syncId", "");
					groupDetailsHelp.put("syncStatus", "");
					groupDetailsHelp.put("tempId", "");
					groupDetailsHelp.put("urlDirName", "");
					groupDetailsHelp.put("internal", "");


					request.setAttribute("groupDetailsHelp", groupDetailsHelp);
				%>


				<c:forEach items="${groupDetailsHelp}" var="item" varStatus="status">
					<div class="panel panel-default" data-sort="${status.index}">
						<div class="panel-heading">
							<h3 class="panel-title">${item.key}</h3>
						</div>
						<div class="panel-body">
							${item.value}
						</div>
					</div>
				</c:forEach>

				<p class="bg-warning hidden no-records">Žiadny záznam nevyhovuje zadanému vyhľadávaniu (<span class="searchString"></span>)</p>

			</div>
		</div>
	</div>
</iwcm:stripForm>

<tr id="dialogBottomButtonsRow">
   <td class="footer" colspan="2">
   	  <iwcm:write name="dialogBottomButtons"/>
      <input id="btnOk" type="button" value="<iwcm:text key="button.ok"/>" onClick="doOK();">
	  <input id="btnCancel" type="button" value="<iwcm:text key="button.cancel"/>" onClick="window.close();">
   </td>
</tr>

<jsp:include page="/components/bottom.jsp"/>
