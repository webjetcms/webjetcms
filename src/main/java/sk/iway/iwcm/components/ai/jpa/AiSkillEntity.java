package sk.iway.iwcm.components.ai.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

/** Reusable instructions shared by assistants in one domain. */
@Entity
@Table(name = "ai_skills")
@Getter
@Setter
@EntityListeners(AuditEntityListener.class)
@EntityListenersType(Adminlog.TYPE_AI)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
    @DataTableTab(id = "instructions", title = "components.ai_assistants.instruction_tab")
})
public class AiSkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_ai_skills")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "components.ai_assistants.skills.name", tab = "basic")
    private String name;

    @NotBlank
    @Lob
    @Column(nullable = false)
    @Convert(converter = AllowHtmlAttributeConverter.class)
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.skills.instructions",
        tab = "instructions", hidden = true, ai = false,
        editor = @DataTableColumnEditor(attr = @DataTableColumnEditorAttr(key = "class", value = "textarea-code")))
    private String instructions;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.group",
        tab = "instructions", hidden = true, export = false, ai = false,
        editor = @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/ai-skills/autocomplete-tool-group"),
            @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
            @DataTableColumnEditorAttr(key = "placeholder", value = "components.ai_assistants.tools.find_group")
        }))
    private String toolGroup;

    @Column(name = "domain_id", nullable = false, updatable = false)
    private Integer domainId;
}
