package sk.iway.iwcm.components.multistep_form.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "form_steps")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INQUIRY)
public class FormStepEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_form_steps")
    @DataTableColumn(inputType = DataTableColumnType.ID, hidden = true)
    @Accessors(chain = false)
    private Long id;

    @Column(name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER, title = "", className = "icon-only", filter = false)
    private Integer sortPriority;

    @Column(name = "form_name")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.DISABLED, title = "components.forms.file_restrictions.form_name", hidden = true)
    private String formName;

    @Column(name = "step_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.banner.primaryHeader")
    private String stepName;

    @Column(name = "step_sub_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.banner.secondaryHeader", hidden = true)
    private String stepSubName;

    // When duplicationg step, we need it's original ID, so we can duplicate binded items too
    @Transient
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private transient Long idForDuplication;

    @Column(name = "domain_id")
    private Integer domainId;
}
