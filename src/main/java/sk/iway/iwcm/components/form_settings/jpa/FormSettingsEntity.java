package sk.iway.iwcm.components.form_settings.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "form_settings")
@Getter
@Setter
public class FormSettingsEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_form_settings")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    //Not showing for safety reason...it set from form that is edited
    @Column(name = "form_name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title= "components.formsimple.formName", className = "not-formsimple not-form")
    @NotBlank
    @Size(max = 255)
    private String formName;

    @Column(name = "recipients")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "components.formsimple.recipients")
    @Size(max = 255)
    private String recipients;

    /* STYLES in multistep form */
        @Column(name = "row_view")
        @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "components.formsimple.rowView", className = "not-form",
            editor = {
                @DataTableColumnEditor(
                    attr = { @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.form.form_style") }
                )
            }
        )
        private Boolean rowView;

        @Column(name = "form_add_classes")
        @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "editor.form.add_classes", className = "not-formsimple not-form")
        @Size(max = 255)
        private String formAddClasses;

        @Column(name = "form_css")
        @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title= "editor.form.add_css_styles", className = "not-formsimple not-form",
            editor = {
                @DataTableColumnEditor(
                    attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after") }
                )
            }
        )
        @Size(max = 1024)
        private String formCss;

    @Column(name = "cc_emails")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.cc_emails")
    @Size(max = 255)
    private String ccEmails;

    @Column(name = "bcc_emails")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.bcc_emails")
    @Size(max = 255)
    private String bccEmails;

    @Column(name = "reply_to")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.reply_to_emails")
    @Size(max = 255)
    private String replyTo;

    @Column(name = "subject")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.subject")
    @Size(max = 255)
    private String subject;

    /* Deprecated, that contained formName in the past */
    @Column(name = "savedb")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, title= "editor.form.savedb")
    @Size(max = 255)
    private String savedb;

    @Column(name = "forward")
    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, title="editor.form.forward", className="dt-tree-page-null")
    @Size(max = 255)
    private String forward;

    @Column(name = "forward_fail")
    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, title="editor.form.forward_fail", className="dt-tree-page-null")
    @Size(max = 255)
    private String forwardFail;

    /* Old forms used selection of forward types, because they wanted for example add all params to request we do NOT do this */
    @Column(name = "forward_type")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title= "editor.form.forward_type", className = "not-multistep", editor = {
        @DataTableColumnEditor(options = {
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.default", value = ""),
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.forward", value = "forward"),
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.addParams", value = "addParams")
        })
    })
    @Size(max = 255)
    private String forwardType;

    @Column(name = "force_text_plain")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.force_text_plain")
    private Boolean forceTextPlain;

    @Column(name = "form_mail_encoding")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.form_mail_encoding", className = "not-formsimple")
    private Boolean formMailEncoding;

    @Column(name = "add_tech_info")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.addTechInfo", className = "not-form")
    private Boolean addTechInfo;

    @Column(name = "is_pdf")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.is_pdf", className = "not-formsimple")
    private Boolean isPdf;

    @Column(name = "allow_only_one_submit")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.allow_only_one_submit", className = "not-formsimple")
    private Boolean allowOnlyOneSubmit;

    @Column(name = "overwrite_old_forms")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.overwrite_old_forms", className = "not-formsimple")
    private Boolean overwriteOldForms;

    @Column(name = "double_opt_in")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.doubleOptIn", className = "not-formsimple")
    private Boolean doubleOptIn;

    @Column(name = "message_as_attach")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.message_as_attach", className = "not-formsimple")
    private Boolean messageAsAttach;

    @Column(name = "message_as_attach_file_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.message_as_attach_file_name", className = "not-formsimple")
    @Size(max = 255)
    private String messageAsAttachFileName;

    /* We are re-using this in multistep forms (just using old value, but functionality is different) */
    @Column(name = "after_send_interceptor")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.afterSendInterceptor", className = "ai-off")
    @Size(max = 255)
    private String afterSendInterceptor;

    @Transient
        @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.mustistep.processor", className = "ai-off not-formsimple",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/form-settings/autocomplete-formProcessor"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    private String formProcessor;

    @Column(name = "encryption_key")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title= "components.form.encryptionKey")
    @Size(max = 1024)
    private String encryptKey;

    @Column(name = "email_text_before")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title= "components.formsimple.textBefore", className = "not-form")
    @Size(max = 1024)
    private String emailTextBefore;

    @Column(name = "email_text_after")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title= "components.formsimple.textAfter", className = "not-form")
    @Size(max = 1024)
    private String emailTextAfter;

    /* You can set special header in email eg. for Call Center email parsing */
    @Column(name = "fields_email_header")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.fields_email_header", className = "not-formsimple not-multistep")
    @Size(max = 255)
    private String fieldsEmailHeader;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, title = "components.form.encryptionKey.tooltip", className = "allow-html")
    private transient String encrypKeyInfo;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.form.use_form_doc_id", className="dt-tree-page-null")
    private transient DocDetailsDto useFormDoc;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.form.send_user_info_doc_id", className="dt-tree-page-null")
    private transient DocDetailsDto formmailSendUserInfoDoc;

    /* FILE PROPERTIES */
    @Column(name = "max_size_in_kilobytes")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title= "components.forms.file_restrictions.file_size_in_kilobytes", className = "not-formsimple",
        editor = {
			@DataTableColumnEditor(
				attr = { @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.form.file_limits") }
			)
		}
    )
    private Integer maxSizeInKilobytes;

    @Column(name = "allowed_extensions")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "components.forms.file_restrictions.allowed_extensions", className = "not-formsimple")
    @Size(max = 255)
    private String allowedExtensions;

    @Column(name = "picture_height")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title= "components.forms.file_restrictions.image_height", className = "not-formsimple")
    private Integer pictureHeight;

    @Column(name = "picture_width")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title= "components.forms.file_restrictions.image_width", className = "not-formsimple")
    private Integer pictureWidth;

    @Column(name = "use_form_doc_id")
    private Integer useFormDocId;

    @Column(name = "formmail_send_user_info_doc_id")
    private Integer formMailSendUserInfoDocId;

    @Column(name = "use_form_mail_doc_id")
    private Integer useFormMailDocId;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.form.use_form_mail_doc_id", className="dt-tree-page-null")
    private transient DocDetailsDto useFormMailDoc;

    @Column(name = "domain_id")
    private Integer domainId;

    /**
     * Converts snake_case to camelCase (pascalCase with lowercase first letter)
     * @param input
     * @return
     */
    public static String toPascalCase(String input) {
        if(Tools.isEmpty(input)) return "";
        if (!input.contains("_")) return Character.toLowerCase(input.charAt(0)) + input.substring(1);

        StringBuilder result = new StringBuilder();
        String[] parts = input.split("_");

        result.append(parts[0].toLowerCase());

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                result.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    /**
     * Converts camelCase to snake_case
     * @param input
     * @return
     */
    public static String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) return input;

        return input
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }
}
