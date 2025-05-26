package sk.iway.iwcm.doc.attributes.jpa;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "doc_atr_def")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_DOC_ATTRIBUTES)
public class DocAtrDefEntity extends ActiveRecordRepository {

    @PrePersist
    public void prePersist() {
        if (domainId==null) domainId = CloudToolsForCore.getDomainId();
    }

    @Id
    @Column(name = "atr_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_doc_atr_def")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "atr_name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="user.admin_edit.atrName"
    )
    @NotBlank
    @Size(max = 255)
    private String name;

    @Column(name = "order_priority")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="user.admin_edit.orderPriority"
    )
    private Integer orderPriority;

    @Column(name = "atr_description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="user.admin_edit.atrDescription"
    )
    @Size(max = 255)
    private String description;

    @Column(name = "atr_default_value")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="user.admin_edit.atrDefaultValue"
    )
    private String defaultValue;

    @Column(name = "atr_type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="user.admin_edit.atrType",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "STRING", value = "0"),
                    @DataTableColumnEditorAttr(key = "INT", value = "1"),
                    @DataTableColumnEditorAttr(key = "BOOL", value = "2"),
                    @DataTableColumnEditorAttr(key = "DOUBLE", value = "3")
                }
            )
        }
    )
    private Integer type;

    @Column(name = "atr_group")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="user.admin_edit.atrGroup",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/webpages/attributes/def/autocomplete"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1")
				}
			)
		}
    )
    @Size(max = 32)
    private String group;

    @Column(name = "true_value")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="user.admin_edit.trueValue"
    )
    @Size(max = 255)
    private String trueValue;

    @Column(name = "false_value")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="user.admin_edit.falseValue"
    )
    @Size(max = 255)
    private String falseValue;

    @Column(name = "domain_id")
    private Integer domainId;

    public void setId(Long id) {
		this. id = id;
	}

    /**
     * We don't want to send docAtrEntities to frontend, because it's not needed to be there in attrDefinition page
     */
    @JsonManagedReference(value="atrDef")
    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "atrDef")
    private List<DocAtrEntity> docAtrEntities;

    /**
     * For webPage we need to send DocAtrEntity value, as we have @JsonIgnore on docAtrEntities
     * we need to fill this field with first value from docAtrEntities on backend
     */
    @Transient
    private DocAtrEntity docAtrEntityFirst;
}
