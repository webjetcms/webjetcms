package sk.iway.iwcm.components.quiz;
import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name = "quiz")
public class QuizBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -860332849238982776L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz")
	private int id;
	@Column
	private String name;

	@Column(name="domain_id")
	private int domainId;

	@Column(name="quiz_type")
	private QuizType quizType;

	@OneToMany(fetch = FetchType.LAZY, mappedBy="quiz", cascade=CascadeType.ALL)
	@OrderBy("sortOrder ASC")
	private List<QuizQuestionBean> quizQuestions;

	@OneToMany(fetch = FetchType.LAZY, mappedBy="quiz", cascade=CascadeType.ALL)
	@OrderBy("sortOrder ASC")
	private List<QuizResultBean> quizResults;

	public QuizBean()
	{
		this.domainId = CloudToolsForCore.getDomainId();
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<QuizQuestionBean> getQuizQuestions()
	{
		return quizQuestions;
	}

	public void setQuizQuestions(List<QuizQuestionBean> quizQuestions)
	{
		for(int i = 0; i < quizQuestions.size(); i++)
		{
			quizQuestions.get(i).setSortOrder(i);
		}
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
		for(int i = 0; i < quizResults.size(); i++)
		{
			quizResults.get(i).setSortOrder(i);
		}
		this.quizResults = quizResults;
	}
}
