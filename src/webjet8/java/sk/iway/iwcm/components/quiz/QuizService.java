package sk.iway.iwcm.components.quiz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpSession;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.quiz.statistics.model.QuizAnswersDto;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;

@RestController
public class QuizService {

	public QuizService() {
		//
	}

	@GetMapping(path = "/admin/rest/quiz/all")
	@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
	public Collection<QuizBean> getAll() {
		Collection<QuizBean> result = new JpaDB<QuizBean>(QuizBean.class).find("domainId",
				CloudToolsForCore.getDomainId());
		return result;
	}

	@PostMapping(path = "/rest/quiz/saveAnswers/{quizId}/{formId}")
	public Collection<QuizAnswerBean> saveAnswersAndGetResult(@PathVariable int quizId, @PathVariable String formId,
			@RequestBody List<NameValueBean> answers) {
		QuizBean quiz = getById(quizId);
		List<QuizQuestionBean> quizQuestions = quiz.getQuizQuestions();

		if (quizQuestions == null || quizQuestions.size() < 1) {
			return new ArrayList<>();
		}

		Map<Integer, QuizQuestionBean> quizQuestionsByQuestionId = new HashMap<>();
		for (QuizQuestionBean item : quizQuestions) {
			quizQuestionsByQuestionId.put(item.getId(), item);
		}

		for (NameValueBean answer : answers) {
			QuizAnswerBean existingAnswer = JpaTools.findFirstByProperties(QuizAnswerBean.class,
					new Pair<String, Integer>("quizId", quizId), new Pair<String, String>("formId", formId),
					new Pair<String, Integer>("quizQuestionId", answer.getName()));
			if (existingAnswer != null) {
				break;
			}
			QuizAnswerBean newAnswer = new QuizAnswerBean();

			newAnswer.setQuizId(quizId);
			newAnswer.setFormId(formId);

			newAnswer.setQuizQuestionId(answer.getName());
			newAnswer.setAnswer(answer.getValue());
			newAnswer.setRate(quizQuestionsByQuestionId.get(answer.getName()).getRate(answer.getValue()));

			int rightAnswer = quizQuestionsByQuestionId.get(answer.getName()).getRightAnswer();
			boolean isCorrect = rightAnswer == answer.getValue();
			newAnswer.setIsCorrect(isCorrect);
			newAnswer.setRightAnswer(rightAnswer);

			newAnswer.save();
		}

		return new JpaDB<QuizAnswerBean>(QuizAnswerBean.class).find("formId", formId);
	}

	@GetMapping(path = "/admin/rest/quiz/{id}")
	@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
	public QuizBean getById(@PathVariable int id) {
		if (id <= 0) {
			return new QuizBean();
		}
		QuizBean result = JpaTools.findFirstByProperties(QuizBean.class, new Pair<String, Integer>("id", id),
				new Pair<String, Integer>("domainId", CloudToolsForCore.getDomainId())); // .getById(id);
		return result;
	}

	@PostMapping(path = "/admin/rest/quiz/save")
	@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
	public Boolean updateQuiz(@RequestBody QuizTransferBean quiz, HttpSession session) {
		if (!hasUserPermissionsToChange(session)) {
			return false;
		}

		return quiz.merge(new QuizBean()).save();
	}

	@PostMapping(path = "/admin/rest/quiz/remove/{id}")
	@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
	public boolean removeQuiz(@PathVariable int id, HttpSession session) {
		if (!hasUserPermissionsToChange(session)) {
			return false;
		}
		QuizBean result = getById(id);
		if (result == null) {
			return false;
		}
		return result.delete();
	}

	protected boolean hasUserPermissionsToChange(HttpSession session) {
		Identity user = UsersDB.getCurrentUser(session);
		return user != null && user.isAdmin() && user.isEnabledItem("cmp_quiz");
	}

	/**
	 * Metoda vrati zoznam otazok, pocet spravnych odpovedi, pocet nespravnych
	 * odpovedi pre zadane quizId
	 *
	 * @param quizId
	 * @param dateFrom - optional
	 * @param dateTo   - optional
	 * @return vlastny zoznam typu List<QuizAnswersDto>
	 */
	@PostMapping(path = "/admin/rest/quiz/{quizId}/answers")
	@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
	public List<QuizAnswersDto> getQuizAnswersByQuizId(@PathVariable int quizId,
			@RequestParam(name = "fromDate", required = false) String dateFrom,
			@RequestParam(name = "toDate", required = false) String dateTo) {

		 EntityManager entityManager = JpaTools.getEntityManager();
		 entityManager.getTransaction().begin();
		 List<QuizAnswersDto> resultList = new ArrayList<>();
		 try
		 {
			  if(Tools.isEmpty(dateFrom)) dateFrom = "01.01.1900 00:00:00";
			  if(Tools.isEmpty(dateTo)) dateTo = Tools.formatDate(Tools.getNow())+" 23:59:59";

			  //IntelliJ hlasi nespravne chybu pri SUM+CASE - ')' expected, got 'WHEN', ale zbehne to v poriadku pri volani query
			  Query queryAnswers = entityManager.createQuery("SELECT qq.question, "
						  + "SUM(CASE WHEN qa.isCorrect = 1 THEN 1 ELSE 0 END), "
						  + "SUM(CASE WHEN qa.isCorrect = 0 THEN 1 ELSE 0 END) FROM QuizBean q "
						  + "JOIN QuizQuestionBean qq on q.id = qq.quiz.id JOIN QuizAnswerBean qa ON qq.id = qa.quizQuestionId "
						  + "WHERE q.id = :quizId AND qa.created BETWEEN :dateFrom AND :dateTo GROUP BY qq.question")
						  .setParameter("quizId", quizId)
						  .setParameter("dateFrom", new Date(DB.getTimestamp(dateFrom)), TemporalType.TIMESTAMP)
						  .setParameter("dateTo", new Date(DB.getTimestamp(dateTo)), TemporalType.TIMESTAMP);

			  @SuppressWarnings("unchecked")
			  List<Object[]> answersList = queryAnswers.getResultList();

			  Iterator<Object[]> answersIt = answersList.iterator();
			  while (answersIt.hasNext()) {
					Object[] answerObject = answersIt.next();
					resultList.add(new QuizAnswersDto((String) answerObject[0], ((Long)answerObject[1]).intValue(), ((Long)answerObject[2]).intValue()));
			  }
			  entityManager.getTransaction().commit();
		 }
		 catch (Exception e)
		 {
			  try
			  {
					entityManager.getTransaction().rollback();
			  }
			  catch (Exception ignore)
			  {
			  }
			  sk.iway.iwcm.Logger.error(e);
		 }
		 finally
		 {
			  entityManager.close();
		 }

		 return resultList;
	}

	/**
	 * Metoda vrati pocet odoslanych dotaznikov za obdobie od a do
	 *
	 * @param quizId
	 * @param dateFrom - optional
	 * @param dateTo   - optional
	 * @return pocet odoslanych dotaznikov za obdobie dateFrom - dateTo
	 */
	@GetMapping(path = "/admin/rest/quiz/{quizId}/answers/count")
	@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
	public int getSubmittedQuizCount(@PathVariable int quizId,
			@RequestParam(name = "fromDate", required = false) String dateFrom,
			@RequestParam(name = "toDate", required = false) String dateTo) {

		 EntityManager entityManager = JpaTools.getEntityManager();
		 entityManager.getTransaction().begin();
		 try
		 {
			  if(Tools.isEmpty(dateFrom)) dateFrom = "01.01.1900 00:00:00";
			  if(Tools.isEmpty(dateTo)) dateTo = Tools.formatDate(Tools.getNow())+" 23:59:59";

			  Query queryAnswers = entityManager.createQuery("SELECT count(distinct q.formId) FROM QuizAnswerBean q WHERE q.quizId = :quizId AND q.created BETWEEN :dateFrom AND :dateTo")
						  .setParameter("quizId", quizId)
						  .setParameter("dateFrom", new Date(DB.getTimestamp(dateFrom)), TemporalType.TIMESTAMP)
						  .setParameter("dateTo", new Date(DB.getTimestamp(dateTo)), TemporalType.TIMESTAMP);
			  Object result = queryAnswers.getSingleResult();
			  entityManager.getTransaction().commit();

			  if (result != null) return ((Long)result).intValue();
		 }
		 catch (Exception e)
		 {
			  try
			  {
					entityManager.getTransaction().rollback();
			  }
			  catch (Exception ignore)
			  {
			  }
			  sk.iway.iwcm.Logger.error(e);
		 }
		 finally
		 {
			  entityManager.close();
		 }

		 return 0;
	}
}
