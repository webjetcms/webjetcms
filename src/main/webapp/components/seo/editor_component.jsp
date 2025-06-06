<%@page import="sk.iway.iwcm.components.seo.*,java.util.List,sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_seo"/>
<% request.setAttribute("cmpName", "seo"); %>
<%=Tools.insertJQuery(request)%>
<jsp:include page="/components/top.jsp"/>

<%
List<String> seoKeywords = SeoManager.getDistinctSeoKeywords();
String keywords = String.join(",", seoKeywords);
%>

<script type="text/javascript">

function Ok()
{
	try
	{
		odosliData();
	} catch (e) { console.log(e); }
	return false;
}

function loadBotsIframe()
{
	var url = "/components/seo/admin_bots.jsp";
	 $("#componentIframeWindowTabBots").attr("src", url);
}
function loadEnginesIframe()
{
	var url = "/components/stat/stat_searchengines.jsp";
	 $("#componentIframeWindowTabEngines").attr("src", url);
}
function loadManagementIframe()
{
	var url = "/components/seo/admin_management_keywords.jsp";
	 $("#componentIframeWindowTabManagement").attr("src", url);
}
function loadStatsIframe()
{
	var url = "/components/seo/admin_stat_keywords.jsp";
	 $("#componentIframeWindowTabStats").attr("src", url);
}
function loadPositionIframe()
{
	var url = "/components/seo/admin_positions.jsp";
	 $("#componentIframeWindowTabPosition").attr("src", url);
}
function loadNumberIframe()
{
	var url = "/components/seo/admin_number_keywords.jsp";
	 $("#componentIframeWindowTabNumber").attr("src", url);
}
</script>

<iwcm:menu name="menuSeo">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.seo.pageAnalytics"/></a></li>
			<li><a href="#" onclick="loadBotsIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.seo.se_bots.list.short"/></a></li>
			<li><a href="#" onclick="loadEnginesIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.seo.se_searching.short"/></a></li>
			<li><a href="#" onclick="loadManagementIframe();showHideTab('4');" id="tabLink4"><iwcm:text key="components.seo.management_keywords.short"/></a></li>
			<li><a href="#" onclick="loadStatsIframe();showHideTab('5');" id="tabLink5"><iwcm:text key="components.seo.stat_keywords.short"/></a></li>
			<li><a href="#" onclick="loadPositionIframe();showHideTab('6');" id="tabLink6"><iwcm:text key="components.seo.leftMenu.positions.short"/></a></li>
			<li class="last"><a href="#" onclick="loadNumberIframe();showHideTab('7');" id="tabLink7"><iwcm:text key="components.seo.keywords.stat.number.short"/></a></li>
		</ul>
	</div>
</iwcm:menu>
<style type="text/css">
	.leftCol {
		text-align: right;
		padding-top: 8px;
	}
	.col-sm-3, .col-sm-9 {
		margin-bottom: 10px;
	}
	#tabMenu1 {
		margin-top: 15px;
	}
</style>
<div class="tab-pane toggle_content tab-pane-fullheight" style="width:790px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">

		<form onsubmit="return false" name="editorForm">

			<div class="">
				<div class="row" id="fieldTrS">
					<div class="col-sm-3 leftCol">
						<label for="seoKeywordsInput"><iwcm:text key="components.seo.keywords.keywords"/></label>
					</div>
					<div class="col-sm-9" id="fieldTdS">
						<input type="text" id="fieldInputS" name="fieldS" class="form-control" value="<%=ResponseUtils.filter(keywords)%>"/>
					</div>
				</div>
				<div class="row">
					<div class="col-sm-3 leftCol">
						<label for="seoLongSentence"><iwcm:text key="components.seo.keywords.long_sentence"/> (<iwcm:text key="components.seo.keywords.word_count"/>)</label>
					</div>
					<div class="col-sm-9">
						<iwcm:text key="components.seo.keywords.max"/>:
						<input type="text" size="4" name="maxCountSentence" value="22"/>

						<iwcm:text key="components.seo.keywords.middle"/>:
						<input type="text" size="4" name="middleCountSentence" value="16"/>
					</div>
				</div>
				<div class="row">
					<div class="col-sm-3 leftCol">
						<label for="seoLongWords"><iwcm:text key="components.seo.keywords.long_words"/> (<iwcm:text key="components.seo.keywords.letter_count"/>)</label>
					</div>
					<div class="col-sm-9">
						<iwcm:text key="components.seo.keywords.max_word"/>:
						<input type="text" size="4" name="maxCountWord" value="22"/>

						<iwcm:text key="components.seo.keywords.middle_word"/>:
						<input type="text" size="4" name="middleCountWord" value="16"/>
					</div>
				</div>
				<div class="row" style="margin-top: 20px">
					<div class="col-sm-3 leftCol">
						<strong><iwcm:text key="components.seo.pageAnalytics"/></strong>
					</div>
					<div class="col-sm-9">
						<div class="seoData" style="margin-top: 8px"></div>
					</div>
				</div>
			</div>


		</form>

	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTabBots" frameborder="0" name="componentIframeWindowTabBots" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu3">
		<iframe id="componentIframeWindowTabEngines" frameborder="0" name="componentIframeWindowTabEngines" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu4">
		<iframe id="componentIframeWindowTabManagement" frameborder="0" name="componentIframeWindowTabManagement" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu5">
		<iframe id="componentIframeWindowTabStats" frameborder="0" name="componentIframeWindowTabStats" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu6">
		<iframe id="componentIframeWindowTabPosition" frameborder="0" name="componentIframeWindowTabPosition" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu7">
		<iframe id="componentIframeWindowTabNumber" frameborder="0" name="componentIframeWindowTabNumber" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

</div>

<script type="text/javascript">

	function odosliData()
	{
		//var data = $('#data___Frame').contents().find('iframe').contents().find('#WebJETEditorBody').html();
		var data = ""
		try {
			data = window.parent.parent.parent.webpagesDatatable.EDITOR.field("data").s.opts.wjeditor.getData()
		} catch (e) {
			console.log(e);
			data = window.parent.parent.ckEditorInstance.getData();
		}
		//data = data.replace(/&/g,"");
		var keyWord = document.editorForm.fieldS.value;
		var middleSentencesCount = document.editorForm.middleCountSentence.value;
		var maxSentencesCount = document.editorForm.maxCountSentence.value;
		var middleWordsCount = document.editorForm.middleCountWord.value;
		var maxWordsCount = document.editorForm.maxCountWord.value;
		$.ajax({
			   type: "POST",
			   url: "/components/seo/admin_keyword_counter.jsp",
			   data: ( {data: data, keyWord: keyWord, middleSentencesCount: middleSentencesCount, maxSentencesCount: maxSentencesCount,
			   middleWordsCount: middleWordsCount, maxWordsCount: maxWordsCount} ),
			   success: function(msg)
			   {

			   	$(".seoData").html(msg).hide().fadeIn();
			   	//alert( "Data Saved: " + data );
			   }
		});

		return false;
	}

</script>

<%@ include file="/components/bottom.jsp" %>

<script>
	$(document).ready(()=> {
		odosliData();
	});
</script>