package sk.iway.iwcm.components.enumerations.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "enumeration_type")
//@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue( "default" )

@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_UPDATEDB)
public class EnumerationTypeBean extends ActiveRecordRepository implements Serializable {

    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(generator = "WJGen_enumeration_type")
    @TableGenerator(name = "WJGen_enumeration_type", pkColumnValue = "enumeration_type")
    @Column(name = "enumeration_type_id")
    @DataTableColumn(
        inputType = DataTableColumnType.ID,
        title="ID",
        tab="basic"
    )
    private Long id;

    @Column(name = "name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.enumerations.typeName",
        tab="basic"
    )
    @Size(max = 255)
    private String typeName;

    @Column(name = "allow_child_enumeration_type")
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="components.enumerations.allow_child_enumeration_type",
        hidden = true,
        tab="basic"
    )
    private boolean allowChildEnumerationType;

    @Column(name = "allow_parent_enumeration_data")
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="components.enumerations.allow_parent_enumeration_data",
        hidden = true,
        tab="basic"
    )
    private boolean allowParentEnumerationData;

    @Column(name = "string1_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;1",
        tab="strings"
    )
    @Size(max = 255)
    private String string1Name;

    @Column(name = "string2_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;2",
        tab="strings"
    )
    @Size(max = 255)
    private String string2Name;

    @Column(name = "string3_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;3",
        tab="strings"
    )
    @Size(max = 255)
    private String string3Name;

    @Column(name = "string4_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;4",
        tab="strings"
    )
    @Size(max = 255)
    private String string4Name;

    @Column(name = "string5_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;5",
        tab="strings"
    )
    @Size(max = 255)
    private String string5Name;

    @Column(name = "string6_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;6",
        tab="strings"
    )
    @Size(max = 255)
    private String string6Name;

    @Column(name = "string7_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;7",
        tab="strings"
    )
    @Size(max = 255)
    private String string7Name;

    @Column(name = "string8_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;8",
        tab="strings"
    )
    @Size(max = 255)
    private String string8Name;

    @Column(name = "string9_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;9",
        tab="strings"
    )
    @Size(max = 255)
    private String string9Name;

    @Column(name = "string10_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;10",
        tab="strings"
    )
    @Size(max = 255)
    private String string10Name;

    @Column(name = "string11_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;11",
        tab="strings"
    )
    @Size(max = 255)
    private String string11Name;

    @Column(name = "string12_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.retazec;12",
        tab="strings"
    )
    @Size(max = 255)
    private String string12Name;

    @Column(name = "decimal1_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.cislo;1",
        tab="numbers"
    )
    @Size(max = 255)
    private String decimal1Name;

    @Column(name = "decimal2_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.cislo;2",
        tab="numbers"
    )
    @Size(max = 255)
    private String decimal2Name;

    @Column(name = "decimal3_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.cislo;3",
        tab="numbers"
    )
    @Size(max = 255)
    private String decimal3Name;

    @Column(name = "decimal4_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.cislo;4",
        tab="numbers"
    )
    @Size(max = 255)
    private String decimal4Name;

    @Column(name = "boolean1_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.boolean;1",
        tab="booleans"
    )
    @Size(max = 255)
    private String boolean1Name;

    @Column(name = "boolean2_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.boolean;2",
        tab="booleans"
    )
    @Size(max = 255)
    private String boolean2Name;

    @Column(name = "boolean3_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.boolean;3",
        tab="booleans"
    )
    @Size(max = 255)
    private String boolean3Name;

    @Column(name = "boolean4_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.boolean;4",
        tab="booleans"
    )
    @Size(max = 255)
    private String boolean4Name;

    @Column(name = "date1_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.date;1",
        tab="dates"
    )
    @Size(max = 255)
    private String date1Name;

    @Column(name = "date2_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.date;2",
        tab="dates"
    )
    @Size(max = 255)
    private String date2Name;

    @Column(name = "date3_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.date;3",
        tab="dates"
    )
    @Size(max = 255)
    private String date3Name;

    @Column(name = "date4_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.enumerations.admin_enum_list.date;4",
        tab="dates"
    )
    @Size(max = 255)
    private String date4Name;

    @Column(name = "hidden")
    private boolean hidden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_enumeration_type_id")
    private EnumerationTypeBean childEnumerationTypeBean;

    //Bind editor fields
    @Transient
    @DataTableColumnNested
	private EnumerationTypeEditorFields editorFields = null;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumerationTypeBean that = (EnumerationTypeBean) o;
        return id == that.id &&
                Objects.equals(id, that.id) &&
                Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, id, typeName);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Integer getEnumerationTypeId() {
       if(id == null) return null;
       return id.intValue();
    }

    public void setEnumerationTypeId(Integer enumerationTypeId) {
        this.id = Long.valueOf(enumerationTypeId);
    }

    public void setEnumerationTypeId(int enumerationTypeId) {
        this.id = Long.valueOf(enumerationTypeId);
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String type_name) {
        this.typeName = type_name;
    }

    public String getString1Name() {
        return string1Name;
    }

    public void setString1Name(String string1Name) {
        this.string1Name = string1Name;
    }

    public String getString2Name() {
        return string2Name;
    }

    public void setString2Name(String string2Name) {
        this.string2Name = string2Name;
    }

    public String getString3Name() {
        return string3Name;
    }

    public void setString3Name(String string3Name) {
        this.string3Name = string3Name;
    }

    public String getDecimal1Name() {
        return decimal1Name;
    }

    public void setDecimal1Name(String decimal1Name) {
        this.decimal1Name = decimal1Name;
    }

    public String getDecimal2Name() {
        return decimal2Name;
    }

    public void setDecimal2Name(String decimal2Name) {
        this.decimal2Name = decimal2Name;
    }

    public String getDecimal3Name() {
        return decimal3Name;
    }

    public void setDecimal3Name(String decimal3Name) {
        this.decimal3Name = decimal3Name;
    }

    public String getString4Name() {
        return string4Name;
    }

    public void setString4Name(String string4Name) {
        this.string4Name = string4Name;
    }

    public String getDecimal4Name() {
        return decimal4Name;
    }

    public void setDecimal4Name(String decimal4Name) {
        this.decimal4Name = decimal4Name;
    }

    public String getBoolean1Name() {
        return boolean1Name;
    }

    public void setBoolean1Name(String boolean1Name) {
        this.boolean1Name = boolean1Name;
    }

    public String getBoolean2Name() {
        return boolean2Name;
    }

    public void setBoolean2Name(String boolean2Name) {
        this.boolean2Name = boolean2Name;
    }

    public String getBoolean3Name() {
        return boolean3Name;
    }

    public void setBoolean3Name(String boolean3Name) {
        this.boolean3Name = boolean3Name;
    }

    public String getBoolean4Name() {
        return boolean4Name;
    }

    public void setBoolean4Name(String boolean4Name) {
        this.boolean4Name = boolean4Name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public EnumerationTypeBean getChildEnumerationTypeBean() {
        return childEnumerationTypeBean;
    }

    public void setChildEnumerationTypeBean(EnumerationTypeBean childEnumerationTypeBean) {
        this.childEnumerationTypeBean = childEnumerationTypeBean;
    }

    public boolean isAllowChildEnumerationType() {
        return allowChildEnumerationType;
    }

    public void setAllowChildEnumerationType(boolean allowChildEnumerationType) {
        this.allowChildEnumerationType = allowChildEnumerationType;
    }

    public String getString5Name() {
        return string5Name;
    }

    public void setString5Name(String string5Name) {
        this.string5Name = string5Name;
    }

    public String getString6Name() {
        return string6Name;
    }

    public void setString6Name(String string6Name) {
        this.string6Name = string6Name;
    }

    public String getDate1Name() {
        return date1Name;
    }

    public void setDate1Name(String date1Name) {
        this.date1Name = date1Name;
    }

    public String getDate2Name() {
        return date2Name;
    }

    public void setDate2Name(String date2Name) {
        this.date2Name = date2Name;
    }

    public String getDate3Name() {
        return date3Name;
    }

    public void setDate3Name(String date3Name) {
        this.date3Name = date3Name;
    }

    public String getDate4Name() {
        return date4Name;
    }

    public void setDate4Name(String date4Name) {
        this.date4Name = date4Name;
    }

    public boolean isAllowParentEnumerationData() {
        return allowParentEnumerationData;
    }

    public void setAllowParentEnumerationData(boolean allowParentEnumerationData) {
        this.allowParentEnumerationData = allowParentEnumerationData;
    }

    public String getString7Name() {
        return string7Name;
    }

    public void setString7Name(String string7Name) {
        this.string7Name = string7Name;
    }

    public String getString8Name() {
        return string8Name;
    }

    public void setString8Name(String string8Name) {
        this.string8Name = string8Name;
    }

    public String getString9Name() {
        return string9Name;
    }

    public void setString9Name(String string9Name) {
        this.string9Name = string9Name;
    }

    public String getString10Name() {
        return string10Name;
    }

    public void setString10Name(String string10Name) {
        this.string10Name = string10Name;
    }

    public String getString11Name() {
        return string11Name;
    }

    public void setString11Name(String string11Name) {
        this.string11Name = string11Name;
    }

    public String getString12Name() {
        return string12Name;
    }

    public void setString12Name(String string12Name) {
        this.string12Name = string12Name;
    }

    public void setEditorFields(EnumerationTypeEditorFields editorFields) {
        this.editorFields = editorFields;
    }

    public EnumerationTypeEditorFields getEditorFields() {
        return this.editorFields;
    }
}