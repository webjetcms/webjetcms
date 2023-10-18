package sk.iway.iwcm.components.quiz;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name = "quiz_answers")
public class QuizAnswerBean extends ActiveRecord implements Serializable {
	
	private static final long serialVersionUID = -860332849238382775L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz_answers")
	private int id;
	@Column(name="form_id")
	private String formId;
	@Column(name="quiz_id")
	private int quizId;
	@Column(name="quiz_question_id")
	private int quizQuestionId;
	@Column
	private int answer;
	@Column(name="right_answer")
	private int rightAnswer;
	@Column(name="is_correct")
	private boolean isCorrect;
	@Column
	private int rate;
	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date created;
	
	public int getRightAnswer() {
		return rightAnswer;
	}

	public void setRightAnswer(int rightAnswer) {
		this.rightAnswer = rightAnswer;
	}
	
	public int getId() 
	{
		return id;
	}
	
	public void setId(int id) 
	{
		this.id = id;
	}
	
	public String getFormId() 
	{
		return formId;
	}
	
	public void setFormId(String formId) 
	{
		this.formId = formId;
	}
	
	public int getQuizId() 
	{
		return quizId;
	}
	
	public void setQuizId(int quizId) 
	{
		this.quizId = quizId;
	}
	
	public int getQuizQuestionId() 
	{
		return quizQuestionId;
	}
	
	public void setQuizQuestionId(int quizQuestionId) 
	{
		this.quizQuestionId = quizQuestionId;
	}
	
	public int getAnswer() 
	{
		return answer;
	}
	
	public void setAnswer(int answer) 
	{
		this.answer = answer;
	}
	
	public boolean getIsCorrect() 
	{
		return isCorrect;
	}
	
	public void setIsCorrect(boolean isCorrect) 
	{
		this.isCorrect = isCorrect;
	}
	
	public Date getCreated() 
	{
		return created;
	}
	
	public void setCreated(Date created) 
	{
		this.created = created;
	}

	public boolean isCorrect() {
		return isCorrect;
	}

	public void setCorrect(boolean correct) {
		isCorrect = correct;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

	@Override
	public boolean save()
	{
		if(getCreated() == null)
		{
			setCreated(new Date());			
		}
		return super.save();
	}
}
