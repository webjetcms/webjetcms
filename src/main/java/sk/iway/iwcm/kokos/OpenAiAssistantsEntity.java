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
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "", tab = "basic")
    @NotBlank
    @Size(max = 255)
    private String name;

    @Column(name = "assistant_key")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "", tab = "basic", className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    @Size(max = 255)
    private String assistantKey;

    @Lob
    @Column(name = "role_description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "&nbsp;",
        tab = "description",
        editor = {
            @DataTableColumnEditor(type = "textarea", attr = {
                @DataTableColumnEditorAttr(key = "class", value = "textarea-code")
            })
        }
    )
    private String roleDescription;

    @Column(name = "model")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "", tab = "basic")
    @Size(max = 255)
    private String model;

    @Column(name = "temperature")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "", tab = "basic", renderFormat = "dt-format-number--decimal")
    private BigDecimal temperature;

    @Column(name = "created_at")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "", tab = "basic", className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private Date created;

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
}