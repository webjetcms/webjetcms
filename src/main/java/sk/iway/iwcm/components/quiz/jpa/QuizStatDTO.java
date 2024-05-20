package sk.iway.iwcm.components.quiz.jpa;

import java.util.Date;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

@Getter
@Setter
public class QuizStatDTO {
    
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.quiz.statistics.table.question")
    private String question;

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, 
        title = "components.quiz.result.imageUrl",
		className = "image",
		renderFormat = "dt-format-image"
	)
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
    private String imageUrl;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.quiz.statistics.table.right_answers")
	private Integer numberOfRightAnswers;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.quiz.statistics.table.wrong_answers")
	private Integer numberOfWrongAnswers;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER, 
        renderFormat = "dt-format-number--decimal",
        title = "components.quiz.stat.percentage_of_right_answers"
    )
    private Float percentageOfRightAnswers;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title = "Priemerný počet získaných bodov na otázku"
    )
    private Float averageGainedPoints;

        @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title = "Maximálny počet bodov za otázku"
    )
    private Integer questionMaxPoints;

    //Date created
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="editor.date",
        visible = false
    )
    @Temporal(TemporalType.TIMESTAMP)
    private Date dayDate;

    //Value used in chart
    private Integer chartValue;
}
