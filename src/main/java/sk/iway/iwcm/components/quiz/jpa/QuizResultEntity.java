package sk.iway.iwcm.components.quiz.jpa;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "quiz_results")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_QUIZ)
public class QuizResultEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_quiz_results")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "score_from")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.result.score_from"
    )
    @Min(0)
    @NotNull
	private Integer scoreFrom;

    @Column(name = "score_to")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.quiz.result.score_to"
    )
    @Min(0)
	private Integer scoreTo;

    @Column(name = "description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "components.quiz.result.description"
    )
    private String description;

    @Column(name = "quiz_id")
    @DataTableColumn( inputType = DataTableColumnType.HIDDEN )
    private Integer quizId;

    @ManyToOne
    @JoinColumn(name="quiz_id", insertable=false, updatable=false)
    @JsonIgnore
	private QuizEntity quiz;

    @Column(name = "sort_order")
    @DataTableColumn( inputType = DataTableColumnType.NUMBER, title = "components.quiz.position")
	private Integer position;

    public int getSortOrder() {
        return position == null ? 0 : position;
    }
}
