package sk.iway.iwcm.components.enumerations.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
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

@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_UPDATEDB)
public class EnumerationTypeBean extends ActiveRecordRepository implements Serializable {

    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(generator = "WJGen_enumeration_type")
    @TableGenerator(name = "WJGen_enumeration_type", pkColumnValue = "enumeration_type")
    @Column(name = "enumeration_type_id")
    @DataTableColumn(inputType = DataTableColumnType.ID, tab="basic")
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
}