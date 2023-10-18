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
@Table(name = "quiz_results")
public class QuizResultBean extends ActiveRecord implements Serializable {
    private static final long serialVersionUID = -860332849238382175L;

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY, generator="S_quiz_results")
    private int id;

    @ManyToOne
    @JoinColumn(name="quiz_id")
    @JsonIgnore
    private QuizBean quiz;

    @Column(name="sort_order")
    private int sortOrder;

    @Column(name="score_from")
    private int from;

    @Column(name="score_to")
    private int to;

    @Column
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public QuizBean getQuiz() {
        return quiz;
    }

    public void setQuiz(QuizBean quiz) {
        this.quiz = quiz;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
