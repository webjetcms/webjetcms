package sk.iway.iwcm.components.forms;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

import javax.persistence.*;

@Entity
@Table(name = "forms")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORMMAIL)
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
