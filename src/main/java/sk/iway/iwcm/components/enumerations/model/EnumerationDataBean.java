package sk.iway.iwcm.components.enumerations.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
    @DataTableColumn(inputType = DataTableColumnType.DISABLED, visible = false)
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

    //BACKWARD API COMPATIBILITY
    public int getEnumerationDataId() {
        if(id == null) return 0;
        return id.intValue();
    }

    public void setEnumerationDataId(int enumerationDataId) {
        if (enumerationDataId==0) this.id = null;
        else this.id = Long.valueOf(enumerationDataId);
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
