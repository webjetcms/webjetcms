package sk.iway.iwcm.components.multistep_form.jpa;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

@Entity
@Table(name = "form_steps")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORMMAIL)
public class FormStepEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_form_steps")
    @DataTableColumn(inputType = DataTableColumnType.ID, hidden = true)
    @Accessors(chain = false)
    private Long id;

    @Column(name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER, title = "", tab = "main", className = "icon-only", filter = false)
    private Integer sortPriority;

    @Column(name = "form_name")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.DISABLED, title = "components.forms.file_restrictions.form_name", tab = "main", hidden = true)
    private String formName;

    @Lob
    @Column(name = "header")
    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.banner.primaryHeader", tab = "main", renderFunction = "renderStepName")
    private String header;

    @Column(name = "next_step_btn_label")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.mustistep.form.next_step.title", tab = "advanced", hidden = true)
    private String nextStepBtnLabel;

    @Column(name = "back_step_btn_label")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.mustistep.form.back_step.title", tab = "advanced", hidden = true)
    private String backStepBtnLabel;

    @Lob
    @Column(name="step_bonus_html")
    @Convert(converter = AllowHtmlAttributeConverter.class)
    @DataTableColumn(inputType=DataTableColumnType.TEXTAREA, tab="stepBonusHtml", title="components.insert_script.body", className = "textarea-code show-html", hidden = true)
    private String stepBonusHtml;

    @Column(name = "current_position")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer currentPosition;

    @Column(name = "max_position")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer maxPosition;

    // When duplicationg step, we need it's original ID, so we can duplicate binded items too
    @Transient
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private transient Long idForDuplication;

    @Column(name = "domain_id")
    private Integer domainId;

    public boolean isLastStep() {
        return currentPosition == maxPosition;
    }
}
