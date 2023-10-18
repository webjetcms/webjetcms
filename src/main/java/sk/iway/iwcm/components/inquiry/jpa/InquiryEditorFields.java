package sk.iway.iwcm.components.inquiry.jpa;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class InquiryEditorFields {

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "answers",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/inquiry-answer?questionId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true")
            }
        )
    })
    private List<InquiryAnswerEntity> answers;

    public void fromInquiryEntity(InquiryEntity originalEntity, Prop prop, ProcessItemAction action) {

        if(action == ProcessItemAction.CREATE) {
            originalEntity.setAnswerTextOk(prop.getText("inquiry.answer_text_ok_default"));
            originalEntity.setAnswerTextFail(prop.getText("inquiry.answer_text_fail_default"));
            originalEntity.setQuestionActive(true);
            originalEntity.setHours(24);
            originalEntity.setQuestionGroup("default");
        }

        originalEntity.setEditorFields(this);
    }

}
