package sk.iway.iwcm.kokos;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "open_ai_assistants")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
//@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CLIENT_SPECIFIC)
public class OpenAiAssistantsEntity {

    @Id
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "components.ai_assistants.name", tab = "basic")
    @NotBlank
    @Size(max = 255)
    private String name;

    @Column(name = "class_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.class_name", tab = "basic",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/openai-assistants/autocomplete-class"),
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
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.field_from", tab = "basic",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/openai-assistants/autocomplete-field"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className")
				}
			)
		}
    )
    @NotBlank
    @Size(max = 255)
    private String fieldFrom;

    @Column(name = "field_to")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.field_to", tab = "basic",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/openai-assistants/autocomplete-field"),
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

    @Column(name = "model")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.model", tab = "basic")
    @Size(max = 255)
    private String model = "gpt-3.5-turbo";

    @Column(name = "assistant_key")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.assistant_key", tab = "basic", className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    @Size(max = 255)
    private String assistantKey;

    @Lob
    @Column(name = "description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "components.ai_assistants.description",
        tab = "basic"
    )
    private String description;

    @Lob
    @Column(name = "instructions")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "&nbsp;",
        tab = "instructions",
        editor = {
            @DataTableColumnEditor(type = "textarea", attr = {
                @DataTableColumnEditorAttr(key = "class", value = "textarea-code")
            })
        }
    )
    @NotBlank
    private String instructions;

    @Column(name = "created_at")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "components.ai_assistants.created_at", tab = "basic", className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private Date created;

    @Column(name = "temperature")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.ai_assistants.temperature", tab = "advanced", renderFormat = "dt-format-number--decimal")
    private BigDecimal temperature = BigDecimal.ONE;

    @Column(name = "keep_html")
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.ai_assistants.keep_html", tab = "advanced")
    private Boolean keepHtml;

    @Column(name = "top_p")
    private BigDecimal topP ;

    @Column(name = "reasoning_effort")
    private String reasoningEffort;

    @Column(name="domain_id")
	private Integer domainId;

    public String getName() {
        //Cut prefix from name
        String prefix = OpenAiAssistantsService.getAssitantPrefix();
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    public String getFullName() {
        return name;
    }

    public void setNameAddPrefix(String name) {
        String prefix = OpenAiAssistantsService.getAssitantPrefix();
        //Just in case, so we dont set prefix 2x
        if(name.startsWith(prefix)) return;
        this.name = prefix + name;
    }
}