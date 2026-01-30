package sk.iway.iwcm.components.formsimple;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "form_attributes")
@Getter
@Setter
public class FormAttributesEntity {

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
