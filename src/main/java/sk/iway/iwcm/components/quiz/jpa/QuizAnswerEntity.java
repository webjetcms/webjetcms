package sk.iway.iwcm.components.quiz.jpa;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "quiz_answers")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_QUIZ)
public class QuizAnswerEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_quiz_answers")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "form_id")
    @NotNull
    @Size(max = 255)
	private String formId;

    //
        @Column(name = "quiz_id")
        @DataTableColumn( inputType = DataTableColumnType.HIDDEN )
        private Integer quizId;

        //JOIN with QuizEntity (tabel quiz)
        @ManyToOne
        @JoinColumn(name="quiz_id", insertable=false, updatable=false)
        @JsonIgnore
        private QuizEntity quiz;

    //
        @Column(name = "quiz_question_id")
        @DataTableColumn( inputType = DataTableColumnType.HIDDEN )
        private Integer quizQuestionId;

        //JOIN with QuizEntity (tabel quiz)
        @ManyToOne
        @JoinColumn(name="quiz_question_id", insertable=false, updatable=false)
        @JsonIgnore
        private QuizQuestionEntity quizQuestion;

    @Column(name = "answer")
	private Integer answer;

    @Column(name = "is_correct")
	private Boolean isCorrect;

    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "right_answer")
	private Integer rightAnswer;

    @Column(name = "rate")
	private Integer rate;
}
