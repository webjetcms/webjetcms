package sk.iway.iwcm.components.quiz;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Query;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.utils.Pair;

public class QuizTransferBean {
	private int id;
	private String name;
	private QuizType quizType;
	private List<QuizQuestionBean> quizQuestions;
	private List<QuizResultBean> quizResults;

	public QuizBean merge(QuizBean quiz) {
		quiz.setId(id);
		quiz.setName(name);
		quiz.setQuizType(quizType);

		deleteQuestions(quiz);
		for(int i = 0; i < quizQuestions.size(); i++) {
			quizQuestions.get(i).setQuiz(quiz);
		}
		quiz.setQuizQuestions(quizQuestions);

		deleteResults(quiz);
		for(int i = 0; i < quizResults.size(); i++) {
			quizResults.get(i).setQuiz(quiz);
		}
		quiz.setQuizResults(quizResults);
		return quiz;
	}

	protected int deleteQuestions(QuizBean quiz)
	{
		@SuppressWarnings("unchecked")
		List<QuizQuestionBean> quests = new JpaDB<QuizQuestionBean>(QuizQuestionBean.class).findByProperties(new Pair<>("quiz", quiz));

		List<Integer> existingQQIds = quests != null ? quests.stream().map(item -> item.getId()).collect(Collectors.toList()) : new ArrayList<Integer>();
		List<Integer> newQQIds = quizQuestions != null ? quizQuestions.stream().map(item -> item.getId()).collect(Collectors.toList()) : new ArrayList<Integer>();
		List<Integer> toDeleteQQIds = existingQQIds.stream().filter(item -> newQQIds.indexOf(item) < 0).collect(Collectors.toList());

		if(toDeleteQQIds.size() <= 0)
		{
			return 0;
		}
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery("delete from QuizQuestionBean where id in :ids");
		query.setParameter("ids", toDeleteQQIds);
		int result = query.executeUpdate();
		em.getTransaction().commit();
		return result;
	}

	protected int deleteResults(QuizBean quiz)
	{
		@SuppressWarnings("unchecked")
		List<QuizResultBean> quests = new JpaDB<QuizResultBean>(QuizResultBean.class).findByProperties(new Pair<>("quiz", quiz));

		List<Integer> existingQQIds = quests != null ? quests.stream().map(item -> item.getId()).collect(Collectors.toList()) : new ArrayList<Integer>();
		List<Integer> newQQIds = quizResults != null ? quizResults.stream().map(item -> item.getId()).collect(Collectors.toList()) : new ArrayList<Integer>();
		List<Integer> toDeleteQQIds = existingQQIds.stream().filter(item -> newQQIds.indexOf(item) < 0).collect(Collectors.toList());

		if(toDeleteQQIds.size() <= 0)
		{
			return 0;
		}
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery("delete from QuizResultBean where id in :ids");
		query.setParameter("ids", toDeleteQQIds);
		int result = query.executeUpdate();
		em.getTransaction().commit();
		return result;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<QuizQuestionBean> getQuizQuestions() {
		return quizQuestions;
	}
	public void setQuizQuestions(List<QuizQuestionBean> quizQuestions) {
		this.quizQuestions = quizQuestions;
	}

	public QuizType getQuizType() {
		return quizType;
	}

	public void setQuizType(QuizType quizType) {
		this.quizType = quizType;
	}

	public List<QuizResultBean> getQuizResults() {
		return quizResults;
	}

	public void setQuizResults(List<QuizResultBean> quizResults) {
		this.quizResults = quizResults;
	}
}
