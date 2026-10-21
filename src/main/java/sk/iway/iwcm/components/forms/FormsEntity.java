package sk.iway.iwcm.components.forms;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@Entity
@Table(name = "forms")
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
    @DataTableTab(id = "settings-basic", title = "editor.form_setting.basic_tab"),
    @DataTableTab(id = "settings-email", title = "editor.form_setting.email_tab"),
    @DataTableTab(id = "settings-advanced", title = "datatable.tab.advanced")
})
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
