package sk.iway.iwcm.components.ai.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "ai_assistants")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_AI)
public class AssistantDefinitionEntity implements Serializable {

    @Id
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    /* BASIC tab */

    //Unique name
    @Column(name = "name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "components.ai_assistants.name", tab = "basic")
    @NotBlank
    @Size(max = 255)
    private String name;

    //User visible name
    @Column(name = "description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.ai_assistants.description",
        tab = "basic",
        hidden = true
    )
    private String description;

    @Column(name = "icon")
    @DataTableColumn(inputType = DataTableColumnType.ICON, title = "components.ai_assistants.icon", tab = "basic")
    private String icon;

    @Column(name = "group_name")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.group", tab = "basic")
    private String groupName;

    @Column(name = "created_at")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "components.ai_assistants.created_at", tab = "basic", className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private Date created;

    @Column(name="active")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.ai_assistants.active", visible = false, tab = "basic")
	private Boolean active;

    /* ACTION tab */

    @Column(name = "action")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.action", tab = "action")
    @Size(max = 255)
    private String action;

    @Column(name = "class_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.class_name", tab = "action",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/ai/assistant-definition/autocomplete-class"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    @NotBlank
    @Size(max = 255)
    private String className;

    @Column(name = "field_from")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.field_from", tab = "action",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/ai/assistant-definition/autocomplete-field"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className")
				}
			)
		}
    )
    @Size(max = 255)
    private String fieldFrom;

    @Column(name = "field_to")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.field_to", tab = "action",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/ai/assistant-definition/autocomplete-field"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className")
				}
			)
		}
    )
    @NotBlank
    @Size(max = 255)
    private String fieldTo;

    /* PROVIDER tab */

    @Column(name = "provider")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.provider", tab = "provider")
    private String provider;

    @Column(name = "model")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.model", visible = false, className = "hideOnCreate hideOnEdit", tab = "provider",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/ai/assistant-definition/autocomplete-model"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_provider")
				}
			)
		}
    )
    @Size(max = 255)
    private String model;

    /* INSTRUCTIONS tab */

    @Lob
    @Column(name = "instructions")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "&nbsp;",
        tab = "instructions",
        className = "wrap",
        editor = {
            @DataTableColumnEditor(type = "textarea", attr = {
                @DataTableColumnEditorAttr(key = "class", value = "textarea-code")
            })
        }
    )
    @NotBlank
    private String instructions;

    /* ADVANCED tab */

    @Column(name = "keep_html")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.ai_assistants.keep_html", tab = "advanced", visible = false)
    private Boolean keepHtml;

    @Column(name="use_streaming")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.ai_assistants.use_streaming", visible = false, className = "hideOnCreate hideOnEdit", tab = "advanced")
	private Boolean useStreaming;

    @Column(name="use_temporal")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.ai_assistants.useTemporal", visible = false, className = "hideOnCreate hideOnEdit", tab = "advanced")
	private Boolean useTemporal;

    @Column(name = "user_prompt_enabled")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.ai_assistants.user_prompt.enabled", tab = "advanced", visible = false)
    private Boolean userPromptEnabled;

    @Lob
    @Column(name = "user_prompt_label")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.user_prompt.label", tab = "advanced", className = "wrap", visible = false)
    private String userPromptLabel;

    @Transient
	@DataTableColumnNested
	private AssistantDefinitionEditorFields editorFields = null;

    @Column(name="domain_id")
	private Integer domainId;
}