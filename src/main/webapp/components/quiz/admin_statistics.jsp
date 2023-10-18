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
%>

<link rel="stylesheet" href="/components/quiz/css/admin_quiz_statistics.css">
<script>var pageLng = '<%=lng%>';</script>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-notebook"></i><iwcm:text key="components.quiz.title"/>
    <i class="fa fa-angle-right"></i><iwcm:text key="components.quiz.statistics.title"/> ${quiz.name}
    </h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.filter"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">


    <div class="row quizAnswerStat_div">
        <div class="col-md-12">
            <div class="col-md-3 col-sm-6 col-xs-12 col-lg-3" >
                <div class="dashboard-stat green">
                    <div class="visual">
                        <i class="fa fa-file-text-o"></i>
                    </div>
                    <div class="details">
                        <div class="number2">
                            ${submittedQuizesCount}
                        </div>
                        <div class="desc">
                            <iwcm:text key="components.quiz.statistics.submitted"/>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-3 col-sm-6 col-xs-12 col-lg-3">
                <div class="dashboard-stat yellow">
                    <div class="visual">
                        <i class="fa fa-file-text-o"></i>
                    </div>
                    <div class="details">
                        <div id="submittedQuizesRange" class="number2">
                            ${submittedQuizesCount}
                        </div>
                        <div class="desc">
                            <iwcm:text key="components.quiz.statistics.submitted_range"/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>   

    <div class="col-md-12">
        <form name="statisticsRange" class="form-inline" action="/components/quiz/1/showStatistics" method="POST">
            <div class="form-group">
                <label for="startDateId" class="form-label"><iwcm:text key="components.adminlog.from"/>:</label>
                <div class="input-group date date-picker" data-date-format="dd.mm.yyyy">
                    <input class="form-control datepicker" type="text" size="10" maxlength="10" name="startDate" id="startDateId" onblur="checkDate(this);return false;">
                    <span class="input-group-btn"><button class="btn default" type="button"><i class="fa fa-calendar"></i></button></span>
                </div>
            </div>

            <div class="form-group">
                <label for="endDateId" class="form-label"><iwcm:text key="components.adminlog.to"/>:</label>
                <div class="input-group date date-picker" data-date-format="dd.mm.yyyy">
                    <input class="form-control datepicker" type="text" size="10" maxlength="10" name="endDate" id="endDateId" onblur="checkDate(this);return false;">
                    <span class="input-group-btn"><button class="btn default" type="button"><i class="fa fa-calendar"></i></button></span>
                </div>
            </div>
            <div class="form-group">
                <button id="filterDatesButton" class="btn btn-default" type="button"><iwcm:text key="components.tips.view"/></button>
            </div>
        </form>
    </div>
    </div>
</div>
<div class="row col-md-12">
    <table id="quizAnswerStatistics" class="table table-striped table-bordered nowrap table-ninja-style dataTable" data-quizIdValue="${quiz.id}">
        <thead>
            <tr role="row" style="height: 64px;">
                <th class="not-allow-hide row-controller-and-settings not-export" rowspan="1" colspan="1" style="width: 227px;" aria-label='<iwcm:text key="components.quiz.statistics.table.question"/>' data-statLabel='<iwcm:text key="components.quiz.statistics.table.question"/>'></th>
                <th class="not-allow-hide row-controller-and-settings not-export" rowspan="1" colspan="1" style="width: 227px;" aria-label='<iwcm:text key="components.quiz.statistics.table.right_answers"/>' data-statLabel='<iwcm:text key="components.quiz.statistics.table.right_answers"/>'></th>
                <th class="not-allow-hide row-controller-and-settings not-export" rowspan="1" colspan="1" style="width: 227px;" aria-label='<iwcm:text key="components.quiz.statistics.table.wrong_answers"/>' data-statLabel='<iwcm:text key="components.quiz.statistics.table.wrong_answers"/>'></th>
            </tr>
        </thead>
    </table>
</div>


<script type="text/javascript" src="/admin/scripts/dateTime.jsp"></script>
<script type="text/javascript" src="/components/quiz/js/admin_quiz_statistics.js"></script>
<%@ include file="/admin/layout_bottom.jsp" %>