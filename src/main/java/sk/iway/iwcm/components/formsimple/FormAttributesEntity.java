package sk.iway.iwcm.components.formsimple;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "form_attributes")
@Getter
@Setter
public class FormAttributesEntity {

    /*
     * This entity is retained for backward compatibility. The application now uses
     * FormSettingsEntity, which operates on the remastered form_attributes table (exposed as form_settings).
     * Since the form_attributes table still exists, both this entity and its repository are preserved
     * for potential future use.
     * #58161 - sivan
     */

    public FormAttributesEntity() {}

    public FormAttributesEntity(Long id, String formName, String paramName, Integer domainId) {
        this.id = id;
        this.formName = formName;
        this.paramName = paramName;
        this.domainId = domainId;
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_form_attributes")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Size(max = 1024)
    @Column(name = "value")
    private String value;

    @NotBlank
    @Size(max = 255)
    @Column(name = "form_name")
    private String formName;

    @NotBlank
    @Size(max = 64)
    @Column(name = "param_name")
    private String paramName;

    @Column(name = "domain_id")
    private Integer domainId;
}