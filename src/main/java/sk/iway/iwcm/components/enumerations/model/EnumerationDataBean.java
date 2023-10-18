package sk.iway.iwcm.components.enumerations.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "enumeration_data")
//@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue( "default" )

@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_UPDATEDB)
public class EnumerationDataBean extends ActiveRecordRepository implements Serializable {

    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(generator = "WJGen_enumeration_data")
    @TableGenerator(name = "WJGen_enumeration_data", pkColumnValue = "enumeration_data")
    @Column(name = "enumeration_data_id")
    @DataTableColumn(
        inputType = DataTableColumnType.ID,
        title="ID"
    )
    private Long id;

    @Column (name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER,
        className = "dt-row-edit",
        title = "components.enumerations.sort_priority",
        renderFormatLinkTemplate = "javascript:;")
    private int sortPriority;

    @Column(name = "string1")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string1;

    @Column(name = "string2")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string2;

    @Column(name = "string3")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string3;

    @Column(name = "string4")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string4;

    @Column(name = "string5")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string5;

    @Column(name = "string6")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string6;

    @Column(name = "string7")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string7;

    @Column(name = "string8")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string8;

    @Column(name = "string9")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string9;

    @Column(name = "string10")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string10;

    @Column(name = "string11")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string11;

    @Column(name = "string12")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    private String string12;

    @Column(name = "decimal1")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER)
    private BigDecimal decimal1 = BigDecimal.valueOf(0);

    @Column(name = "decimal2")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER)
    private BigDecimal decimal2 = BigDecimal.valueOf(0);

    @Column(name = "decimal3")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER)
    private BigDecimal decimal3 = BigDecimal.valueOf(0);

    @Column(name = "decimal4")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER)
    private BigDecimal decimal4 = BigDecimal.valueOf(0);

    @Column (name = "boolean1")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN)
    private boolean boolean1;

    @Column (name = "boolean2")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN)
    private boolean boolean2;

    @Column (name = "boolean3")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN)
    private boolean boolean3;

    @Column (name = "boolean4")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN)
    private boolean boolean4;

    // pomocna premenna aby som v spring data mohol vyhladavat podla typeId (autokupa)
    @Column(name = "enumeration_type_id", insertable=false, updatable=false)
    private Integer typeId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column (name= "date1")
    @DataTableColumn(inputType = DataTableColumnType.DATE)
    private Date date1;

    @Temporal(TemporalType.TIMESTAMP)
    @Column (name = "date2")
    @DataTableColumn(inputType = DataTableColumnType.DATE)
    private Date date2;

    @Temporal(TemporalType.TIMESTAMP)
    @Column (name = "date3")
    @DataTableColumn(inputType = DataTableColumnType.DATE)
    private Date date3;

    @Temporal(TemporalType.TIMESTAMP)
    @Column (name = "date4")
    @DataTableColumn(inputType = DataTableColumnType.DATE)
    private Date date4;

    @Column (name = "hidden")
    private boolean hidden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enumeration_type_id")
    private EnumerationTypeBean type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_enumeration_type_id")
    private EnumerationTypeBean childEnumerationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "parent_enumeration_data_id")
    private EnumerationDataBean parentEnumerationData;

    //Bind editor fields
    @Transient
    @DataTableColumnNested
    private transient EnumerationDataEditorFields editorFields = null;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public int getEnumerationDataId() {
        if(id == null) return 0;
        return id.intValue();
    }

    public void setEnumerationDataId(int enumerationDataId) {
        if (enumerationDataId==0) this.id = null;
        else this.id = Long.valueOf(enumerationDataId);
    }

    public EnumerationTypeBean getType() {
        return type;
    }

    public void setType(EnumerationTypeBean type) {
        this.type = type;
    }

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public String getString2() {
        return string2;
    }

    public void setString2(String string2) {
        this.string2 = string2;
    }

    public String getString3() {
        return string3;
    }

    public void setString3(String string3) {
        this.string3 = string3;
    }

    public BigDecimal getDecimal1() {
        return decimal1;
    }

    public void setDecimal1(BigDecimal decimal1) {
        this.decimal1 = decimal1;
    }

    public BigDecimal getDecimal2() {
        return decimal2;
    }

    public void setDecimal2(BigDecimal decimal2) {
        this.decimal2 = decimal2;
    }

    public BigDecimal getDecimal3() {
        return decimal3;
    }

    public void setDecimal3(BigDecimal decimal3) {
        this.decimal3 = decimal3;
    }

    public BigDecimal getDecimal4() {
        return decimal4;
    }

    public void setDecimal4(BigDecimal decimal4) {
        this.decimal4 = decimal4;
    }

    public String getString4() {
        return string4;
    }

    public void setString4(String string4) {
        this.string4 = string4;
    }

    public boolean isBoolean1() {
        return boolean1;
    }

    public void setBoolean1(boolean boolean1) {
        this.boolean1 = boolean1;
    }

    public boolean isBoolean2() {
        return boolean2;
    }

    public void setBoolean2(boolean boolean2) {
        this.boolean2 = boolean2;
    }

    public boolean isBoolean3() {
        return boolean3;
    }

    public void setBoolean3(boolean boolean3) {
        this.boolean3 = boolean3;
    }

    public boolean isBoolean4() {
        return boolean4;
    }

    public void setBoolean4(boolean boolean4) {
        this.boolean4 = boolean4;
    }

    public int getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(int sortPriority) {
        this.sortPriority = sortPriority;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public EnumerationTypeBean getChildEnumerationType() {
        return childEnumerationType;
    }

    public void setChildEnumerationType(EnumerationTypeBean childEnumerationType) {
        this.childEnumerationType = childEnumerationType;
    }

    public String getString5() {
        return string5;
    }

    public void setString5(String string5) {
        this.string5 = string5;
    }

    public String getString6() {
        return string6;
    }

    public void setString6(String string6) {
        this.string6 = string6;
    }

    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
    }

    public Date getDate2() {
        return date2;
    }

    public void setDate2(Date date2) {
        this.date2 = date2;
    }

    public Date getDate3() {
        return date3;
    }

    public void setDate3(Date date3) {
        this.date3 = date3;
    }

    public Date getDate4() {
        return date4;
    }

    public void setDate4(Date date4) {
        this.date4 = date4;
    }

    public String getDate1Str() {
        return Tools.formatDateTimeSeconds(this.date1);
    }

    public String getDate2Str() {
        return Tools.formatDateTimeSeconds(this.date2);
    }

    public String getDate3Str() {
        return Tools.formatDateTimeSeconds(this.date3);
    }

    public String getDate4Str() {
        return Tools.formatDateTimeSeconds(this.date4);
    }

    public EnumerationDataBean getParentEnumerationData() {
        return parentEnumerationData;
    }

    public void setParentEnumerationData(EnumerationDataBean parentEnumerationData) {
        this.parentEnumerationData = parentEnumerationData;
    }

    public String getString7() {
        return string7;
    }

    public void setString7(String string7) {
        this.string7 = string7;
    }

    public String getString8() {
        return string8;
    }

    public void setString8(String string8) {
        this.string8 = string8;
    }

    public String getString9() {
        return string9;
    }

    public void setString9(String string9) {
        this.string9 = string9;
    }

    public String getString10() {
        return string10;
    }

    public void setString10(String string10) {
        this.string10 = string10;
    }

    public String getString11() {
        return string11;
    }

    public void setString11(String string11) {
        this.string11 = string11;
    }

    public String getString12() {
        return string12;
    }

    public void setString12(String string12) {
        this.string12 = string12;
    }

    public EnumerationDataEditorFields getEditorFields() {
        return this.editorFields;
    }

    public void setEditorFields(EnumerationDataEditorFields editorFields) {
        this.editorFields = editorFields;
    }
}
