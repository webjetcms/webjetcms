<%@page import="java.lang.reflect.Method"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.quiz.jpa.QuizType"%>
<%@page import="sk.iway.iwcm.components.quiz.jpa.QuizQuestionEntity"%>
<%@page import="sk.iway.iwcm.components.quiz.jpa.QuizEntity"%>
<%@page import="sk.iway.iwcm.components.quiz.jpa.QuizResultEntity"%>
<%@page import="sk.iway.iwcm.components.quiz.jpa.QuizAnswerEntity"%>
<%@page import="sk.iway.iwcm.components.quiz.rest.QuizService"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*, sk.iway.iwcm.i18n.Prop" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
int quizId = pageParams.getIntValue("quizId", -1);
boolean showAllAnswers = pageParams.getBooleanValue("showAllAnswers", false);
QuizEntity quiz = new QuizService().getById(quizId);
if(quiz == null) return;

Prop prop = Prop.getInstance(lng);
final int optionsCount = 6;
List<QuizQuestionEntity> quizQuestions = quiz.getQuizQuestions();
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
				<% QuizQuestionEntity question = quizQuestions.get(index); %>
					<!-- Question START -->
					<div class="qBox" id="qBox<%=index %>">
						<div class="col-md-2">
							<p class="numb"><%=index + 1 %>.</p>
						</div>

						<div class="col-md-10">
							<h3> <%=question.getQuestion() %> </h3>

							<% if(Tools.isNotEmpty(question.getImageUrl())) { %>
								<div class="questionImage">
									<img class="responsive" src="<%=question.getImageUrl()%>" alt=""/>
								</div>
							<% } %>

							<div class="questionOptions">
								<% for(int i = 1; i <= optionsCount; i++ ) {
									String option = question.getOption(i);
								%>
									<% if(Tools.isNotEmpty(option)) { %>
										<label class="quiz-option">
											<input type="radio" name="<%=question.getId() %>" value="<%=i %>" />
											<span class="col-md-1"><%=optionsKeys.get(i) %></span>
											<div class="col-md-11">
												<%=option %>
											</div>
										</label>
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
				<a href="javascript:;" class="btn back pull-left"> <iwcm:text key="components.search.back"/> </a>
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
				<a href="javascript:;" class="btn next red pull-right"> <iwcm:text key="components.search.next"/> </a>
			</div>
		</div>
	</div>
	<!-- ProgressBar END -->

	<!-- Result START -->

	<div class="row result resultsPanel">
		<div class="container">
			<% if(quiz.getQuizTypeEnum().equals(QuizType.RIGHT_ANSWER)) { %>
				<div class="rightResults">
					<h4> <%= prop.getText("componnets.quiz.quiz_evaluation", quiz.getName()) %> </h4>
					<ul style="display: flex; justify-content: center;">
						<li id="countCorrectAnsw">
							<strong></strong> <iwcm:text key="componnets.quiz.number_of_right_answers"/>
						</li>
						<li id="countIncorrectAnsw">
							<strong></strong> <iwcm:text key="componnets.quiz.number_of_wrong_answers"/>
						</li>
					</ul>
				</div>
			<% } %>

			<% if(quiz.getQuizTypeEnum().equals(QuizType.RATED_ANSWER)) { %>
				<div class="ratedResults">
					<h4> <%= prop.getText("componnets.quiz.quiz_evaluation", quiz.getName()) %> </h4>
					<div id="countScore">
						V teste "<%=quiz.getName()%>" ste dosiahli <strong class="b"></strong><strong>b</strong>. Nižšie si môžete prečítať vyhodnotenie.
						<ul>
							<% for(QuizResultEntity qrb : quiz.getQuizResults()) { %>
							<li id="qrb-<%=qrb.getId()%>" data-from="<%=qrb.getScoreFrom()%>" data-to="<%=qrb.getScoreTo()%>">
								<strong>
									<%=qrb.getScoreFrom()%>-<%=qrb.getScoreTo()%><br />
									<span>bodov</span>
								</strong>
								<%=qrb.getDescription()%>
							</li>
							<% } %>
						</ul>
					</div>
				</div>
			<% } %>

			<div class="text-center">
				<a href="javascript:;" class="btn correctAnswers_show">
					<iwcm:text key="componnets.quiz.show_right_answers"/>
				</a>
				<a href="javascript:;" class="btn correctAnswers_hide" style="display: none;">
					<iwcm:text key="componnets.quiz.hide_right_answers"/>
				</a>
				<a href="javascript:;" class="btn red doItAgain pull-right">
					<iwcm:text key="componnets.quiz.redo_quiz"/>
				</a>
			</div>
		</div>
	</div>
	<!-- Result END -->

	<!-- Correct answers START -->
	<div class="row" id="correctAnswers">
		<div class="container">

		<% if(quiz != null && quizQuestions != null) for(int index = 0; index < quizQuestions.size(); index ++) { %>
			<% QuizQuestionEntity question = quizQuestions.get(index); %>
			<div class="qBox answer" id="answer<%=question.getId() %>">
				<div>
					<p class="numb"><%=index + 1 %>.</p>
					<h3><%=question.getQuestion() %></h3>
				</div>

				<% if(Tools.isNotEmpty(question.getImageUrl())) { %>
					<div class="questionImage">
						<img class="responsive" src="<%=question.getImageUrl()%>" alt=""/>
					</div>
				<% } %>

				<div class="questionAnswers">
					<% for(int i = 1; i <= optionsCount; i++ ) {
						String option = "";
						Method method = Class.forName("sk.iway.iwcm.components.quiz.jpa.QuizQuestionEntity").getMethod("getOption" + i);
						option = (String)method.invoke(question);
					%>
						<div class="row">
							<% if(option != null && !("".equals(option))) { %>
									<div class="col-1 iconDiv ">
										<p class="icon icon<%=i %>"> </p>
									</div>
									<div class="quiz-option col-11 answToQ<%=i %>">
										<span ><%=optionsKeys.get(i) %></span>
										<div class="col-md-11">
											<%=option %>
										</div>
									</div>
							<% } %>
						</div>
					<% } %>
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
var quizStatus = new Quiz(<%=quizId %>);

$(function() {
	$('.doItAgain').on('click', function(e) {
		e.preventDefault();
		location.reload();
	});

	$('.btn.back').on('click', function(e) {
		e.preventDefault();
		quizStatus.goPrev();
	});

	$('.btn.correctAnswers_show').on('click', function(e) {
		e.preventDefault();
		$('#correctAnswers').show();
		$(this).css('display', 'none');
		$('.btn.correctAnswers_hide').css('display', 'inline-block');
	});

	$('.btn.correctAnswers_hide').on('click', function(e) {
		e.preventDefault();
		$('#correctAnswers').hide();
		$(this).css('display', 'none');
		$('.btn.correctAnswers_show').css('display', 'inline-block');
	});

	$('.qBox input').change(function() {
		//console.log('input was changed');
		quizStatus.showHideNext();
	});

	$('.btn.next').on('click', function(e) {
		e.preventDefault();

		if($(this).hasClass('sendResults') == true) {
			//console.log('sending ajax');
			$.ajax({
				url: '/rest/quiz/saveAnswers/' + quizStatus.getQuizId() + '/' + quizStatus.getFormId(),
				type:"POST",
				data: quizStatus.getAnswersJSONString(),
				contentType:"application/json; charset=utf-8",
				dataType:"json",
				success: function(result) {
			 		if(result) {

						//console.log(result);

						<% if(quiz.getQuizTypeEnum().equals(QuizType.RIGHT_ANSWER)) { %>
							for(var i = 0; i < result.length; i++) {
								var answer = result[i];
								if(!answer.isCorrect) {
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .answToQ' + answer.answer).addClass('wrong');
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .icon' + answer.answer).addClass('wrong');
								} else {
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .answToQ' + answer.rightAnswer).addClass('correct');
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .icon' + answer.answer).addClass('correct');
								}
							}

							var correctCount = $(result).filter(function(index, item) {return item.isCorrect == true;}).length;
							var incorrectCount = result.length - correctCount;

							$('#countCorrectAnsw strong').text(correctCount);
							$('#countIncorrectAnsw strong').text(incorrectCount);

							<%
								if(showAllAnswers) {
									for(QuizQuestionEntity question : quizQuestions) {
										for(int i = 1; i <= optionsCount; i++) {
											if(question.getRate(i) > 0) {
												%>
													$('#correctAnswers .answer#answer' + <%=question.getId()%> + ' .answToQ' + <%=i%>).addClass('correct');
												<%
											}
										}
									}
								}
							%>

						<% } else if(quiz.getQuizTypeEnum().equals(QuizType.RATED_ANSWER)) { %>
							var sum = 0;
							result.forEach(function(obj) {
								sum += obj.rate
							});

							$('#countScore strong.b').text(sum);

							for(var i = 0; i < result.length; i++) {
								var answer = result[i];
								if(!answer.isCorrect) {
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .answToQ' + answer.answer).addClass('wrong');
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .icon' + answer.answer).addClass('wrong');
								} else {
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .answToQ' + answer.rightAnswer).addClass('correct');
									$('#correctAnswers .answer#answer' + answer.quizQuestionId + ' .icon' + answer.answer).addClass('correct');
								}
							}

							<%
								if(showAllAnswers) {
									for(QuizQuestionEntity question : quizQuestions) {
										for(int i = 1; i <= optionsCount; i++) {
											if(question.getRate(i) > 0) {
												%>
													$('#correctAnswers .answer#answer' + <%=question.getId()%> + ' .answToQ' + <%=i%>).addClass('correct');
												<%
											} else {
												%>
													$('#correctAnswers .answer#answer' + <%=question.getId()%> + ' .answToQ' + <%=i%>).addClass('wrong');
												<%
											}
											%>
												$('#correctAnswers .answer#answer' + <%=question.getId()%> + ' .answToQ' + <%=i%>).append('<p class="optionRate col-md-1"><%=question.getRate(i) + "b"%></p>');
											<%
										}
									}
								}
							%>

						<% } %>
			 		} else {
						window.alert("<iwcm:text key="spam_protection.post_forbidden" param1='<%=""+SpamProtection.getHourlyPostLimit("quiz")%>' param2='<%=""+SpamProtection.getTimeout("quiz")%>'/>");
					}
				},
				error: function() {
					window.alert("<iwcm:text key="spam_protection.post_forbidden" param1='<%=""+SpamProtection.getHourlyPostLimit("quiz")%>' param2='<%=""+SpamProtection.getTimeout("quiz")%>'/>");
				}
			});
		}

		quizStatus.goNext();
	});
});
</iwcm:script>