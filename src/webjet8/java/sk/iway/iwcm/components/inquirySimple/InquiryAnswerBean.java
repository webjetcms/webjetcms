package sk.iway.iwcm.components.inquirySimple;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name = "inquirysimple_answers")
public class InquiryAnswerBean extends ActiveRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY, generator = "S_inquirysimple_answers")
    private int id;

    @Column(name="form_id")
    String formId;

    @Column(name="question_id")
    String questionId;

    @Column(name="user_id")
    String userId;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
