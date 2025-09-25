package sk.iway.iwcm.components.ai.jpa;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class AssistantDefinitionEditorFields extends BaseEditorFields {

    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title = "components.ai_assistants.description",
        tab = "basic",
        filter = false,
        sortAfter = "description"
    )
    private String translatedDescription;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.user_prompt.label", tab = "advanced",
        className = "wrap", filter = false, sortAfter = "userPromptLabel", visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String translatedUserPromptLabel;


    public void fromAssistantDefinition(AssistantDefinitionEntity assistant, boolean isInit, Prop prop) {
        if(Tools.isNotEmpty(assistant.getDescription())) {
            setTranslatedDescription( prop.getText(assistant.getDescription()) );
        }

        if(Tools.isNotEmpty(assistant.getUserPromptLabel())) {
            setTranslatedUserPromptLabel( prop.getText(assistant.getUserPromptLabel()) );
        }

        if(Tools.isFalse(assistant.getActive())) {
            addRowClass("not-active");
        } else if(isInit == false) {
            addRowClass("not-init");
        }

        assistant.setEditorFields(this);
    }
}
