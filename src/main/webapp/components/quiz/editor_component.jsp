
<%@page import="java.util.Collection"%>
<%@page import="sk.iway.iwcm.components.quiz.jpa.QuizEntity"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.quiz.rest.QuizService"%>
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<iwcm:checkLogon admin="true" perms="cmp_quiz"/>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	request.setAttribute("cmpName", "cmp_quiz");
	request.setAttribute("titleKey", "components.quiz.title");
	request.setAttribute("descKey", "components.quiz.desc");
	request.setAttribute("iconLink", "/components/quiz/editoricon.png");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	Collection<QuizEntity> quizes = new QuizService().getAll();
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>
<iwcm:menu notName="cmp_quiz">
	<iwcm:text key="components.permsDenied"/>
	<% if (1==1) return; %>
</iwcm:menu>
<jsp:include page="/components/top.jsp" />

<script type="text/javascript">
	//<![CDATA[
	var selectedQuizId = <%=pageParams.getIntValue("quizId", 0) %>;


	function getQuiz() {
		return "!INCLUDE(/components/quiz/quiz.jsp, quizId=" + document.textForm.quizId.value + ", showAllAnswers=" + document.textForm.showAllAnswers.checked + ")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getQuiz());
		return true;
	}

    function loadComponentIframe()
    {
        var url = "admin_list.jsp";
        $("#componentIframeWindowTab").attr("src", url);
    }

    function reloadSelect()
	{
	    $.get('/admin/rest/quiz/all', function(data) {
	        //console.log('reloadSelect');
	        //console.log(data);

            selectedQuizId = $('#quizIdSelect').val();

	        $('#quizIdSelect option').each(function() {
	            if($(this).val() > 0) {
	                $(this).remove();
				}
			});

	        for(var i = 0; i < data.length; i++) {
	            var selected = '';
	            if(data[i].id == selectedQuizId) {
	                selected = ' selected="selected"';
				}
	            $('#quizIdSelect').append('<option value="' + data[i].id + '"' + selected + '>' + data[i].name + '</option>');
			}
		});
	}
	//]]>
</script>
<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs" style="background-color:transparent;">
		<li class="first openFirst"><a href="#" onclick="reloadSelect();showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
		<li class="last"><a href="#" onclick="loadComponentIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.quiz.title"/></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
	<div class="tab-page" id="tabMenu1" style="display: block; width:840px;">
		<form name="textForm" style="padding: 10px; margin: 0px;">
			<table border="0" cellspacing="0" cellpadding="5">
				<tr>
					<td width="195px"><iwcm:text
							key="components.quiz.editorComponent.quiz" />:</td>
					<td>
						<select id="quizIdSelect" name="quizId">
						<%
						for(QuizEntity q : quizes) {
						%>
							<option value="<%=q.getId() %>" <%=(q.getId() == pageParams.getIntValue("quizId", 0) ? "selected=\"selected\"" : "") %>><%=q.getName() %></option>
						<% } %>
						</select>
					</td>
				</tr>

			</table>
			<div style="display: flex; padding-top: 20px;">
				<input type="checkbox" id="showAllAnswers"/>
				<label for="showAllAnswers"> <iwcm:text key="components.quiz.editor_component.show_all"/> </label>
			</div>
		</form>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTab" frameborder="0" name="componentIframeWindowTab" width="100%" height="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>
</div>

<jsp:include page="/components/bottom.jsp" />
