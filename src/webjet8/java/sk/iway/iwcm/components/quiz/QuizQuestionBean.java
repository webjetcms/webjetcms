package sk.iway.iwcm.components.quiz;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestionBean extends ActiveRecord implements Serializable {
	
	private static final long serialVersionUID = -860332849238382776L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz_questions")
	private int id;
	@ManyToOne
    @JoinColumn(name="quiz_id")
	@JsonIgnore
	private QuizBean quiz;
	@Column(name="sort_order")
	private int sortOrder;
	@Column
	private String question;
	@Column(name="image_url")
	private String image;
	@Column
	private String option1;
	@Column
	private String option2;
	@Column
	private String option3;
	@Column
	private String option4;
	@Column
	private String option5;
	@Column
	private String option6;
	@Column(name="right_answer")
	private int rightAnswer;
	@Column
	private int rate1;
	@Column
	private int rate2;
	@Column
	private int rate3;
	@Column
	private int rate4;
	@Column
	private int rate5;
	@Column
	private int rate6;

	public int getId() 
	{
		return id;
	}
	
	public void setId(int id) 
	{
		this.id = id;
	}
	
	public QuizBean getQuiz() 
	{
		return quiz;
	}
	
	public void setQuiz(QuizBean quiz) 
	{
		this.quiz = quiz;
	}
	
	public int getSortOrder() 
	{
		return sortOrder;
	}
	
	public void setSortOrder(int sortOrder) 
	{
		this.sortOrder = sortOrder;
	}
	
	public String getQuestion() 
	{
		return question;
	}
	
	public void setQuestion(String question) 
	{
		this.question = question;
	}
	
	public String getOption1() 
	{
		return option1;
	}
	
	public void setOption1(String option1) 
	{
		this.option1 = option1;
	}
	
	public String getOption2() 
	{
		return option2;
	}
	
	public void setOption2(String option2) 
	{
		this.option2 = option2;
	}
	
	public String getOption3() 
	{
		return option3;
	}
	
	public void setOption3(String option3) 
	{
		this.option3 = option3;
	}
	
	public String getOption4() 
	{
		return option4;
	}
	
	public void setOption4(String option4) 
	{
		this.option4 = option4;
	}
	
	public String getOption5() 
	{
		return option5;
	}
	
	public void setOption5(String option5) 
	{
		this.option5 = option5;
	}
	
	public String getOption6() 
	{
		return option6;
	}
	
	public void setOption6(String option6) 
	{
		this.option6 = option6;
	}
	
	public int getRightAnswer() 
	{
		return rightAnswer;
	}
	
	public void setRightAnswer(int rightAnswer) 
	{
		this.rightAnswer = rightAnswer;
	}

	public int getRate1() {
		return rate1;
	}

	public void setRate1(int rate1) {
		this.rate1 = rate1;
	}

	public int getRate2() {
		return rate2;
	}

	public void setRate2(int rate2) {
		this.rate2 = rate2;
	}

	public int getRate3() {
		return rate3;
	}

	public void setRate3(int rate3) {
		this.rate3 = rate3;
	}

	public int getRate4() {
		return rate4;
	}

	public void setRate4(int rate4) {
		this.rate4 = rate4;
	}

	public int getRate5() {
		return rate5;
	}

	public void setRate5(int rate5) {
		this.rate5 = rate5;
	}

	public int getRate6() {
		return rate6;
	}

	public void setRate6(int rate6) {
		this.rate6 = rate6;
	}

	public int getRate(int index)
	{
		switch (index)
		{
			case 1: return getRate1();
			case 2: return getRate2();
			case 3: return getRate3();
			case 4: return getRate4();
			case 5: return getRate5();
			case 6: return getRate6();
			default: return 0;
		}
	}

	public String getImage() {
		return image;
	}

	public void setImage(String imageUrl) {
		this.image = imageUrl;
	}
}
