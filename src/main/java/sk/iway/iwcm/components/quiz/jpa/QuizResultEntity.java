package sk.iway.iwcm.components.quiz.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

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
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID")
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
