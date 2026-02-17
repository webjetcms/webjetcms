package sk.iway.iwcm.components.forms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "forms")
public class FormsEntity extends FormsEntityBasic {

    @Column(name = "form_type")
    @DataTableColumn(inputType = DataTableColumnType.SELECT,
        title = "components.form.form_type.title", tab = "basic", sortAfter = "formName"
    )
    private String formType;

    @Transient
    @DataTableColumnNested
    private transient FormSettingsEntity formSettings;

    public FormSettingsEntity getFormSettings() {
        return formSettings;
    }

    public void setFormSettings(FormSettingsEntity formSettings) {
        this.formSettings = formSettings;
    }

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }
}