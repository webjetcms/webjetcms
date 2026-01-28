package sk.iway.iwcm.components.forms;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import javax.persistence.Transient;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "forms")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORMMAIL)
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