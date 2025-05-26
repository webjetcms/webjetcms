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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_QUIZ)
public class QuizQuestionEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_quiz_questions")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "question")
	@NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "components.quiz.statistics.table.question",
        editor = {
			@DataTableColumnEditor(
				type = "quill"
			)
		}
    )
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    @Size(max = 500)
    private String question;

    @Column(name = "right_answer")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.quiz.type.right_answer",
        editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.quiz.option_1", value = "1"),
					@DataTableColumnEditorAttr(key = "components.quiz.option_2", value = "2"),
					@DataTableColumnEditorAttr(key = "components.quiz.option_3", value = "3"),
					@DataTableColumnEditorAttr(key = "components.quiz.option_4", value = "4"),
					@DataTableColumnEditorAttr(key = "components.quiz.option_5", value = "5"),
					@DataTableColumnEditorAttr(key = "components.quiz.option_6", value = "6")
				}
			)
		}
    )
    @NotNull
	private Integer rightAnswer;

    @Column(name = "image_url")
    @DataTableColumn(inputType = DataTableColumnType.ELFINDER,
        title = "components.quiz.result.imageUrl",
		className = "image",
		renderFormat = "dt-format-image"
	)
	@jakarta.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
    @Size(max = 255)
    private String imageUrl;

    @Column(name = "sort_order")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.position",
        editor = {
			@DataTableColumnEditor(
				attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }
			)
		}
    )
    @Min(0)
    @NotNull
	private Integer position;

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
        @JsonIgnore
        @JsonManagedReference(value="quizQuestion")
        @OneToMany(mappedBy = "quizQuestion", fetch = FetchType.LAZY, cascade={CascadeType.ALL}, orphanRemoval = true)
        private List<QuizAnswerEntity> quizAnswers;

    //Options and rates (aka answers and points)
    @Column(name = "option1")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title = "components.quiz.option_1"
    )
    @Size(max = 255)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String option1;

    @Column(name = "rate1")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.rate_1",
        editor = {
			@DataTableColumnEditor(
				attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }
			)
		}
    )
    @Min(0)
	private Integer rate1;

    @Column(name = "option2")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title = "components.quiz.option_2"
    )
    @Size(max = 255)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String option2;

    @Column(name = "rate2")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.rate_2",
        editor = {
			@DataTableColumnEditor(
				attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }
			)
		}
    )
    @Min(0)
	private Integer rate2;

    @Column(name = "option3")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title = "components.quiz.option_3"
    )
    @Size(max = 255)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String option3;

    @Column(name = "rate3")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.rate_3",
        editor = {
			@DataTableColumnEditor(
				attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }
			)
		}
    )
    @Min(0)
	private Integer rate3;

    @Column(name = "option4")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title = "components.quiz.option_4"
    )
    @Size(max = 255)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String option4;

    @Column(name = "rate4")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.rate_4",
        editor = {
			@DataTableColumnEditor(
				attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }
			)
		}
    )
    @Min(0)
	private Integer rate4;

    @Column(name = "option5")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title = "components.quiz.option_5"
    )
    @Size(max = 255)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String option5;

    @Column(name = "rate5")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.rate_5",
        editor = {
			@DataTableColumnEditor(
				attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }
			)
		}
    )
    @Min(0)
	private Integer rate5;

    @Column(name = "option6")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title = "components.quiz.option_6"
    )
    @Size(max = 255)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String option6;

    @Column(name = "rate6")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.rate_6"
    )
    @Min(0)
	private Integer rate6;

    public int getMaxRate() {
        int maxRate = 0;
        for(int i = 1; i <= 6; i++) {
            if(maxRate < getRate(i)) {
                maxRate = getRate(i);
            }
        }
        return maxRate;
    }

    public int getRate(int index) {
        Integer rate;
		switch (index) {
			case 1: rate = getRate1(); break;
			case 2: rate = getRate2(); break;
			case 3: rate = getRate3(); break;
			case 4: rate = getRate4(); break;
			case 5: rate = getRate5(); break;
			case 6: rate = getRate6(); break;
			default: return 0;
		}
        return rate == null ? 0 : rate;
	}

    public String getOption(int index) {
        switch (index) {
            case 1: return getOption1();
            case 2: return getOption2();
            case 3: return getOption3();
            case 4: return getOption4();
            case 5: return getOption5();
            case 6: return getOption6();
            default: return "";
        }
    }

    public int getSortOrder() {
        return position == null ? 0 : position;
    }
}
