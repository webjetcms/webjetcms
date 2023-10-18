<%@page import="sk.iway.iwcm.components.quiz.QuizService"%>
<%@page import="sk.iway.iwcm.components.quiz.QuizBean"%>
<%@page import="java.util.Collection"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_quiz"/>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<iwcm:menu notName="cmp_quiz">
	<iwcm:text key="components.permsDenied"/>
	<% if (1==1) return; %>
</iwcm:menu>
<%@ include file="/admin/layout_top.jsp" %>
<%=Tools.insertJQuery(request)%>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);
request.setAttribute("cmpName", "cmp_quiz");

QuizService quizService = new QuizService();
Collection<QuizBean> items = quizService.getAll();
request.setAttribute("items", items);
%>
<style>
	.FarebnaTabulka {
		width:100%;
	}
</style>
<script type=text/javascript>

	helpLink = "components/forms.jsp&book=components";

	function deleteOK(text, id) {
		var url = '/admin/rest/quiz/remove/' + id;
		if (confirm(text)) {
			$.post(url, function(result) {
				if(JSON.parse(result) === true) {
					location.reload();
				} else {
					alert('<%=prop.getText("components.quiz.deleteErrorMsg") %>');
				}
			});
		}
	}

</script>
<div class="row title">
    <h1 class="page-title"><i class="icon-question"></i><iwcm:text key="components.quiz.title"/></h1>
</div>

<div class="tab-content">
	<div id="tabMenu1" class="tab-pane clearfix active">
		<div class="col-lg-2 col-md-12 col-sm-6">
			<button onclick="javascript:openPopupDialogFromLeftMenu('/components/quiz/admin_edit.jsp');" type="button" class="btn default imageButton">
				<i class="wjIconBig wjIconBig-uploadDoc"></i>
				<iwcm:text key="components.quiz.create_button"/>
			</button>
		</div>
	</div>
</div>


<display:table class="sort_table FarebnaTabulka" cellspacing="0" export="false" cellpadding="0" name="items" id="item" pagesize="25">
	<% QuizBean q = (QuizBean)item; %>
		<display:column title='<%=prop.getText("components.quiz.id") %>'>
			<a href="javascript:openPopupDialogFromLeftMenu('/components/quiz/admin_edit.jsp?id=<%=q.getId() %>');">
	        	<%=q.getId() %>
	        </a>
		</display:column>
		<display:column title='<%=prop.getText("components.quiz.name") %>'>
			<a href="javascript:openPopupDialogFromLeftMenu('/components/quiz/admin_edit.jsp?id=<%=q.getId() %>');">
	        	<%=q.getName() %>
	        </a>
		</display:column>
		<display:column titleKey="components.quiz.statistics.pocet_zaznamov">
			<div>
				<%=quizService.getSubmittedQuizCount(q.getId(), null, null) %>
			</div>
		</display:column>
		<display:column titleKey="components.table.column.tools">
			<a href="/components/quiz/<%=q.getId() %>/showStatistics" title='<iwcm:text key="components.redirect.admin_list.zobrazit_statistiku"/>' class="iconStat" style="margin: 11px 9px 11px 9px;" aria-hidden="true">&nbsp;</a>
			<a href="javascript:openPopupDialogFromLeftMenu('/components/quiz/admin_edit.jsp?id=<%=q.getId() %>');" title='<iwcm:text key="components.redirect.admin_list.edituj"/>'><span class="glyphicon glyphicon-pencil" aria-hidden="true"></span></a>
			<a href="javascript:deleteOK('<%=prop.getText("components.quiz.deleteConfirmation") %>',<%=q.getId() %>);" class="confirmation" title='<iwcm:text key="button.delete"/>'><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>
		</display:column>
</display:table>

<%@ include file="/admin/layout_bottom.jsp" %>