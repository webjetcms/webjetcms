package sk.iway.iwcm.components.enumerations.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

import lombok.Getter;
import lombok.Setter;
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

@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_UPDATEDB)
public class EnumerationDataBean extends ActiveRecordRepository implements Serializable {

    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue(generator = "WJGen_enumeration_data")
    @TableGenerator(name = "WJGen_enumeration_data", pkColumnValue = "enumeration_data")
    @Column(name = "enumeration_data_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column (name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER,
        className = "dt-row-edit",
        title = "components.enumerations.sort_priority",
        renderFormatLinkTemplate = "javascript:;")
    private int sortPriority;

    @Column(name = "string1")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string1")
    private String fieldA;

    @Column(name = "string2")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string2")
    private String fieldB;

    @Column(name = "string3")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string3")
    private String fieldC;

    @Column(name = "string4")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string4")
    private String fieldD;

    @Column(name = "string5")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string5")
    private String fieldE;

    @Column(name = "string6")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string6")
    private String fieldF;

    @Column(name = "string7")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string7")
    private String fieldG;

    @Column(name = "string8")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string8")
    private String fieldH;

    @Column(name = "string9")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string9")
    private String fieldI;

    @Column(name = "string10")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string10")
    private String fieldJ;

    @Column(name = "string11")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string11")
    private String fieldK;

    @Column(name = "string12")
    @DataTableColumn(inputType = DataTableColumnType.TEXT)
    @JsonAlias("string12")
    private String fieldL;

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
    @DataTableColumn(inputType = DataTableColumnType.DISABLED, visible = false)
    @Column(name = "enumeration_type_id", insertable=false, updatable=false)
    private Integer typeId;

    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
    @Column (name= "date1")
    @DataTableColumn(inputType = DataTableColumnType.DATE)
    private Date date1;

    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
    @Column (name = "date2")
    @DataTableColumn(inputType = DataTableColumnType.DATE)
    private Date date2;

    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
    @Column (name = "date3")
    @DataTableColumn(inputType = DataTableColumnType.DATE)
    private Date date3;

    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
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

    //BACKWARD API COMPATIBILITY
    public int getEnumerationDataId() {
        if(id == null) return 0;
        return id.intValue();
    }

    public void setEnumerationDataId(int enumerationDataId) {
        if (enumerationDataId==0) this.id = null;
        else this.id = Long.valueOf(enumerationDataId);
    }

    @JsonIgnore
    public String getString1() {
        return fieldA;
    }

    public void setString1(String string1) {
        this.fieldA = string1;
    }

    @JsonIgnore
    public String getString2() {
        return fieldB;
    }

    public void setString2(String string2) {
        this.fieldB = string2;
    }

    @JsonIgnore
    public String getString3() {
        return fieldC;
    }

    public void setString3(String string3) {
        this.fieldC = string3;
    }

    @JsonIgnore
    public String getString4() {
        return fieldD;
    }

    public void setString4(String string4) {
        this.fieldD = string4;
    }

    @JsonIgnore
    public String getString5() {
        return fieldE;
    }

    public void setString5(String string5) {
        this.fieldE = string5;
    }

    @JsonIgnore
    public String getString6() {
        return fieldF;
    }

    public void setString6(String string6) {
        this.fieldF = string6;
    }

    @JsonIgnore
    public String getString7() {
        return fieldG;
    }

    public void setString7(String string7) {
        this.fieldG = string7;
    }

    @JsonIgnore
    public String getString8() {
        return fieldH;
    }

    public void setString8(String string8) {
        this.fieldH = string8;
    }

    @JsonIgnore
    public String getString9() {
        return fieldI;
    }

    public void setString9(String string9) {
        this.fieldI = string9;
    }

    @JsonIgnore
    public String getString10() {
        return fieldJ;
    }

    public void setString10(String string10) {
        this.fieldJ = string10;
    }

    @JsonIgnore
    public String getString11() {
        return fieldK;
    }

    public void setString11(String string11) {
        this.fieldK = string11;
    }

    @JsonIgnore
    public String getString12() {
        return fieldL;
    }

    public void setString12(String string12) {
        this.fieldL = string12;
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
}
