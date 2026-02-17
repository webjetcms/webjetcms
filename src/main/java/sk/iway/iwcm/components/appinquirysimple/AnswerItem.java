package sk.iway.iwcm.components.appinquirysimple;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class AnswerItem {

    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.inquirysimple.question")
    private String question;

    @DataTableColumn(inputType = DataTableColumnType.DISABLED, title = "components.inquiry_simple.question_id", className = "hide-on-create")
    String questionId;
}
