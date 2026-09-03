package sk.iway.iwcm.components.forms;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "forms")
public class FormsEntity extends FormsEntityBasic {

    @Transient
    @DataTableColumnNested
    private transient FormSettingsEntity formSettings;

    public FormSettingsEntity getFormSettings() {
        return formSettings;
    }

    public void setFormSettings(FormSettingsEntity formSettings) {
        this.formSettings = formSettings;
    }

}
