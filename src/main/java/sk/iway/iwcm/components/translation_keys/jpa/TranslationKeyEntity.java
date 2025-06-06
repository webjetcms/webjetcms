package sk.iway.iwcm.components.translation_keys.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.tags.support_logic.ResponseUtils;

@Entity
@Table(name = "_properties_")
//auditujeme manualne, kvoli importu a poctu zaznamov @EntityListeners(AuditEntityListener.class)
//@EntityListenersType(Adminlog.TYPE_PROP_UPDATE)
@Setter
@Getter
public class TranslationKeyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_webjet_properties")
    @DataTableColumn(inputType = DataTableColumnType.ID, className = "not-export", filter = false)
    private Long id;

    @Column(name = "prop_key")
    @NotBlank
    @DataTableColumn(inputType = {DataTableColumnType.OPEN_EDITOR})
    private String key;

    //Field is not showed but used to get translation key value from DB
    @Column(name = "prop_value")
    private String value;

    //Field is not showed but used to get translation key language
    @Column(name = "lng")
    @Size(min = 2, max = 3)
    private String lng;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(renderFormat = "dt-format-date-time", editor = {
            @DataTableColumnEditor(
                    type = "datetime",
                    attr = {
                            @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                    }
            )
    })
    private Date updateDate;

    @Transient
    @DataTableColumnNested
    private TranslationKeyEditorFields editorFields = null;

    /*
     * CUSTOM-FIELDS representing translation key value in specific language.
     * fieldA is ALWAYS representing default language and NEVER null or empty
     * originalValue's represent's original value of transation key load from file (can be same)
    */

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
    )
    private String fieldA = ""; //aby sme pri exporte nemali Nevyplnene ale prazdnu hodnotu v jazykoch kde kluc nie je zadany

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueA;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
    )
    private String fieldB = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueB;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldC = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueC;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldD = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueD;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldE = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueE;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldF = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueF;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldG = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueG;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldH = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueH;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldI = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueI;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "wrap show-html",
        filter = true
        )
    private String fieldJ = "";

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        className = "hideOnCreate not-export",
        filter = true,
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String originalValueJ;

    public String getKey() {
        if (key==null) return null;
        //ochrana pred XSS ak kluc nie je citany z DB nie je chraneny
        if (key.contains("<") || key.contains(">")) return ResponseUtils.filter(key).trim();
        return key.trim();
    }

    public String getOriginalValue(char a) {
        switch (a) {
            case 'A':
                return originalValueA;
            case 'B':
                return originalValueB;
            case 'C':
                return originalValueC;
            case 'D':
                return originalValueD;
            case 'E':
                return originalValueE;
            case 'F':
                return originalValueF;
            case 'G':
                return originalValueG;
            case 'H':
                return originalValueH;
            case 'I':
                return originalValueI;
            case 'J':
                return originalValueJ;
            default: return null;
        }
    }
}
