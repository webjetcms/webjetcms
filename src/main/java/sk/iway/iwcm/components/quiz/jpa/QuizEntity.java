package sk.iway.iwcm.components.quiz.jpa;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "quiz")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_QUIZ)
public class QuizEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_quiz")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "name")
	@NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "components.quiz.name",
        tab = "main"
    )
    @Size(max = 255)
    private String name;

    @Column(name = "quiz_type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.quiz.option_type",
        tab = "main",
        editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.quiz.type.right_answer", value = "0"),
					@DataTableColumnEditorAttr(key = "components.quiz.type.rated_answer", value = "1")
				}
			)
		}
    )
    @Size(max = 20)
    private String quizType;

    @Column(name = "domain_id")
	private Integer domainId;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "questions",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/quiz/question?quizId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.quiz.jpa.QuizQuestionEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    @JsonIgnore
    @JsonManagedReference(value="quiz")
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade={CascadeType.ALL}, orphanRemoval = true)
	@OrderBy("position ASC")
	private List<QuizQuestionEntity> quizQuestions;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "results",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/quiz/result?quizId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.quiz.jpa.QuizResultEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    @JsonIgnore
    @JsonManagedReference(value="quiz")
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade={CascadeType.ALL}, orphanRemoval = true)
	@OrderBy("position ASC")
	private List<QuizResultEntity> quizResults;

    @JsonIgnore
    @JsonManagedReference(value="quiz")
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade={CascadeType.ALL}, orphanRemoval = true)
	private List<QuizAnswerEntity> quizAnswers;

    public QuizType getQuizTypeEnum() {
        return QuizType.getQuizType(quizType);
    }
}
