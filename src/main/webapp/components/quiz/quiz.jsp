<%@page import="java.lang.reflect.Method"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.quiz.QuizQuestionBean"%>
<%@page import="sk.iway.iwcm.components.quiz.QuizBean"%>
<%@page import="sk.iway.iwcm.components.quiz.QuizService"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
int quizId = pageParams.getIntValue("quizId", -1);
QuizBean quiz = new QuizService().getById(quizId);
if(quiz == null) return;
List<QuizQuestionBean> quizQuestions = quiz.getQuizQuestions();
int optionsCount = 6;
Map<Integer, String> optionsKeys = new HashMap<Integer, String>();
optionsKeys.put(1, "A");
optionsKeys.put(2, "B");
optionsKeys.put(3, "C");
optionsKeys.put(4, "D");
optionsKeys.put(5, "E");
optionsKeys.put(6, "F");

%>

<link rel="stylesheet" href="/components/quiz/screen.css?v=1.2" type="text/css" />

<div id="quiz">
	<div class="row">
		<div class="container">
			<form id="quizForm">
				<% if(quiz != null && quizQuestions != null) for(int index = 0; index < quizQuestions.size(); index ++) { %>
				<% QuizQuestionBean question = quizQuestions.get(index); %>
					<!-- Question START -->
					<div class="qBox" id="qBox<%=index %>">
						<div class="col-md-2">
							<p class="numb"><%=index + 1 %>.</p>
						</div>

						<div class="col-md-10">
							<h3><%=question.getQuestion() %></h3>

							<% if(Constants.getBoolean("quizAdminShowImageUrl") &&  Tools.isNotEmpty(question.getImage())) { %>
							<img src="<%=question.getImage()%>">
							<% } %>

							<div>

								<% for(int i = 1; i <= optionsCount; i++ ) {
									String option = "";
									Method method = Class.forName("sk.iway.iwcm.components.quiz.QuizQuestionBean").getMethod("getOption" + i);
									option = (String)method.invoke(question);
								%>
									<% if(option != null) { %>
									<p>
										<label>
											<input type="radio" name="<%=question.getId() %>" value="<%=i %>" />
											<span><%=optionsKeys.get(i) %></span> <%=option %>
										</label>
									</p>
									<% } %>
								<% } %>

							</div>

						</div>

					</div>
					<!-- Question END -->
				<% } %>
			</form>
		</div>
	</div>

	<!-- ProgressBar START -->
	<div class="row progressContainer">
		<div class="container">
			<div class="col-md-2">
				<p>
					<a href="javascript:;" class="btn back pull-left">Späť</a>
				</p>
			</div>
			<div class="col-md-7">
				<div class="progress progressBar">
				  <div class="progress-bar" role="progressbar" aria-valuenow="" aria-valuemin="0" aria-valuemax="100" style="">
					<span></span>
				  </div>
				</div>
			</div>
			<div class="col-md-1">
				<div class="count progressNumber"></div>
			</div>
			<div class="col-md-2">
				<p>
					<a href="javascript:;" class="btn next red pull-right">Ďalej</a>
				</p>
			</div>
		</div>
	</div>
	<!-- ProgressBar END -->

	<!-- Result START -->
	<div class="row result resultsPanel">
		<div class="container">
			<div class="col-md-8 col-md-offset-2">
				<h4>Vyhodnotenie testu <%=quiz.getName() %>:</h4>
				<ul>
					<li id="countCorrectAnsw">
						<strong></strong> Počet správnych <br />odpovedí
					</li>
					<li id="countIncorrectAnsw">
						<strong></strong> Počet nesprávnych <br />odpovedí
					</li>
				</ul>
			</div>
		</div>
	</div>

	<div class="row resultsPanel">
		<div class="container">
			<div class="col-md-6 col-md-offset-3">
				<p class="text-center">
					<a href="javascript:;" class="btn correctAnswers yellow">Zobraziť správne odpovede</a>
				</p>
			</div>
			<div class="col-md-3">
				<p>
					<a href="javascript:;" class="btn red doItAgain pull-right">Zopakovať test</a>
				</p>
			</div>
		</div>
	</div>

	<!-- Result END -->


	<!-- Correct answers START -->

	<div class="row" id="correctAnswers">
		<div class="container">

		<% if(quiz != null && quizQuestions != null) for(int index = 0; index < quizQuestions.size(); index ++) { %>
			<% QuizQuestionBean question = quizQuestions.get(index); %>
			<div class="qBox answer" id="answer<%=question.getId() %>">
				<div class="col-md-1">
					<p class="numb"><%=index + 1 %>.</p>
				</div>
				<div class="col-md-10 clearfix">
					<h3><%=question.getQuestion() %></h3>
					<ul>
						<% for(int i = 1; i <= optionsCount; i++ ) {
							String option = "";
							Method method = Class.forName("sk.iway.iwcm.components.quiz.QuizQuestionBean").getMethod("getOption" + i);
							option = (String)method.invoke(question);
						%>
							<% if(option != null) { %>
							<li class="answToQ<%=i %>">
								<label>
									<span><%=optionsKeys.get(i) %></span> <%=option %>
								</label>
							</li>
							<% } %>
						<% } %>
					</ul>
					<p class="text-center"><img src="/images/test.jpg" /></p>
				</div>
			</div>
		<% } %>
		</div>
	</div>

	<!-- Correct answers END -->

</div>
<iwcm:script type="text/javascript" src="/components/quiz/js/jquery.sha1.js"></iwcm:script>
<iwcm:script type="text/javascript" src="/components/quiz/js/quiz.js"></iwcm:script>
<iwcm:script type="text/javascript">
var quiz = new Quiz(<%=quizId %>);

$(function() {
	$('.doItAgain').on('click', function(e) {
		e.preventDefault();
		location.reload();
	});

	$('.btn.back').on('click', function(e) {
		e.preventDefault();
		quiz.goPrev();
	});

	$('.btn.correctAnswers').on('click', function(e) {
		e.preventDefault();
		$('#correctAnswers').show();
		$(this).css('visibility', 'hidden');
	});

	$('.qBox input').change(function() {
		console.log('input was changed');
		quiz.showHideNext();
	});

	$('.btn.next').on('click', function(e) {
		e.preventDefault();

		if($(this).hasClass('sendResults') == true) {
			console.log('sending ajax');
			$.ajax({
				url: '/rest/quiz/saveAnswers/' + quiz.getQuizId() + '/' + quiz.getFormId(),
				type:"POST",
				data: quiz.getAnswersJSONString(),
				contentType:"application/json; charset=utf-8",
				dataType:"json",
				success: function(result) {
			 		if(result) {
			 			for(var i = 0; i < result.length; i++) {
			 				var answer = result[i];
			 				$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .answToQ' + answer.rightAnswer).addClass('correct');
			 				if(!answer.isCorrect) {
			 					$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .answToQ' + answer.answer).addClass('wrong');
			 				}
			 			}

			 			var correctCount = $(result).filter(function(index, item) {return item.isCorrect == true;}).length;
			 			var incorrectCount = result.length - correctCount;

			 			$('#countCorrectAnsw strong').text(correctCount);
			 			$('#countIncorrectAnsw strong').text(incorrectCount);
			 		}
				}
			});
		}

		quiz.goNext();
	});
});
</iwcm:script>