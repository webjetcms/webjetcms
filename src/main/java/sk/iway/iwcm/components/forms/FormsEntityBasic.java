package sk.iway.iwcm.components.forms;

import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@MappedSuperclass
@Setter
@Getter
public class FormsEntityBasic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_forms")
    @DataTableColumn(inputType = DataTableColumnType.ID, tab = "basic")
    private Long id;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "formslist.tools",
        tab = "basic",
        renderFormatLinkTemplate = "javascript:openFormHtml('{{id}}');",
        renderFormatPrefix = "<i class=\"ti ti-eye\"></i>",
        orderable = false,
        hiddenEditor = true
    )
    private String preview;

    @Column(name = "form_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "formslist.nazov_formularu", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String formName;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title="formslist.pocet_zaznamov", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private transient Integer count;

    @Lob
    private String data;

    @Column(name = "create_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title="formslist.createDate", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private Date createDate;

    @Column(name = "last_export_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title="formlist.export.lastExportDate", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private Date lastExportDate;

    @Lob
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title="formslist.note", tab = "basic")
    private String note;

    @Lob
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA, tab = "basic",
        title="formslist.attachments", className = "cell-not-editable"
    )
    private String files;

    @Lob
    private String html;

    // Numeric value of the same column (user_id)
    @Column(name = "user_id")
    private Long userId;

    // Relation to users table; load lazily, only readable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @DataTableColumnNested
    private UserDetailsEntity userDetails;

    @Column(name = "doc_id")
    private int docId;

    @Column(name = "domain_id")
    private int domainId;

    @Column(name = "double_optin_confirmation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date doubleOptinConfirmationDate;

    @Column(name = "double_optin_hash")
    private String doubleOptinHash;

    @Transient
    private Map<String, String> columnNamesAndValues;
}
