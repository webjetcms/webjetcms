package sk.iway.iwcm.doc;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * TemplatesGroupBean.java
 *
 * Class TemplatesGroupBean is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * author       $Author: mhruby $
 * version      $Revision: 1.0 $
 * created      12.6.2018 17:24
 * modified     12.6.2018 17:21
 */

@Entity
@Table(name = "templates_group")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_TEMPLATE_GROUP)
public class TemplatesGroupBean extends ActiveRecordRepository implements Serializable {

    private static final long serialVersionUID = -1L;

    @Id
    @Column(name = "templates_group_id")
    @GeneratedValue(generator = "WJGen_templates_group")
    @TableGenerator(name = "WJGen_templates_group", pkColumnValue = "templates_group")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long templatesGroupId;

    @Column(name = "name")
    @NotBlank
    @DataTableColumn(
        inputType = {DataTableColumnType.OPEN_EDITOR},
        title = "admin.temp_group_list.name",
        tab = "basic"
    )
    private String name;

    @Column(name = "directory")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "admin.temp_group_list.directory",
        tab = "basic"
    )
    private String directory;

    @Column(name = "inline_editing_mode")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "admin.temp_group_list.inline_editing_mode",
        tab = "basic"
    )
    private String inlineEditingMode;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_name",
        tab = "metadata"
    )
    private String projectName;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_autor",
        tab = "metadata"
    )
    private String projectAuthor;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_copyright",
        tab = "metadata"
    )
    private String projectCopyright;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_developer",
        tab = "metadata"
    )
    private String projectDeveloper;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_generator",
        tab = "metadata"
    )
    private String projectGenerator;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_field_a",
        tab = "fields"
    )
    private String projectFieldA;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_field_b",
        tab = "fields"
    )
    private String projectFieldB;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_field_c",
        tab = "fields"
    )
    private String projectFieldC;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.project_field_d",
        tab = "fields"
    )
    private String projectFieldD;

    @Transient
    @DataTableColumn(
            inputType = DataTableColumnType.DISABLED,
            tab = "basic",
            title = "admin.temps_list.pocet_pouziti",
            editor = {
                @DataTableColumnEditor(
                        type = "text",
                        attr = @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
                )
            }
    )
    private Integer count;

    @Column(name = "key_prefix")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "admin.temp_group_list.key_prefix",
        tab = "metadata"
    )
    private String keyPrefix;

    public Long getTemplatesGroupId() {
        return templatesGroupId;
    }

    public void setTemplatesGroupId(Long groupTemplateId) {
        this.templatesGroupId = groupTemplateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public Long getId() {
        return getTemplatesGroupId();
    }

    @Override
    public void setId(Long id) {
        this.setTemplatesGroupId(id);
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectAuthor() {
        return projectAuthor;
    }

    public void setProjectAuthor(String projectAuthor) {
        this.projectAuthor = projectAuthor;
    }

    public String getProjectCopyright() {
        return projectCopyright;
    }

    public void setProjectCopyright(String projectCopyright) {
        this.projectCopyright = projectCopyright;
    }

    public String getProjectDeveloper() {
        return projectDeveloper;
    }

    public void setProjectDeveloper(String projectDeveloper) {
        this.projectDeveloper = projectDeveloper;
    }

    public String getProjectGenerator() {
        return projectGenerator;
    }

    public void setProjectGenerator(String projectGenerator) {
        this.projectGenerator = projectGenerator;
    }

    public String getProjectFieldA() {
        return projectFieldA;
    }

    public void setProjectFieldA(String projectFieldA) {
        this.projectFieldA = projectFieldA;
    }

    public String getProjectFieldB() {
        return projectFieldB;
    }

    public void setProjectFieldB(String projectFieldB) {
        this.projectFieldB = projectFieldB;
    }

    public String getProjectFieldC() {
        return projectFieldC;
    }

    public void setProjectFieldC(String projectFieldC) {
        this.projectFieldC = projectFieldC;
    }

    public String getProjectFieldD() {
        return projectFieldD;
    }

    public void setProjectFieldD(String projectFieldD) {
        this.projectFieldD = projectFieldD;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getInlineEditingMode() { return inlineEditingMode; }

    public void setInlineEditingMode(String inlineEditingMode) { this.inlineEditingMode = inlineEditingMode; }
}