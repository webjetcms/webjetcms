package sk.iway.iwcm.components.form_settings.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

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

    @Column(name = "form_name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title= "components.formsimple.formName", className = "not-formsimple")
    @NotBlank
    @Size(max = 255)
    private String formName;

    @Column(name = "recipients")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "components.formsimple.recipients")
    @Size(max = 255)
    private String recipients;

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

    @Column(name = "savedb")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.savedb", className = "not-formsimple")
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

    @Column(name = "forward_type")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title= "editor.form.forward_type", editor = {
        @DataTableColumnEditor(options = {
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.default", value = ""),
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.forward", value = "forward"),
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.addParams", value = "addParams")
        })
    })
    @Size(max = 255)
    private String forwardType;

    @Column(name = "use_form_doc_id")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.use_form_doc_id", className = "not-formsimple")
    private Integer useFormDocId;

    @Column(name = "use_form_mail_doc_id")
    private Integer useFormMailDocId;

    @Column(name = "force_text_plain")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.force_text_plain")
    private Boolean forceTextPlain;

    @Column(name = "form_mail_encoding")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.help.form_mail_encoding", className = "not-formsimple")
    private Boolean formMailEncoding;

    @Column(name = "is_pdf")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.help.is_pdf", className = "not-formsimple")
    private Boolean isPdf;

    @Column(name = "allow_only_one_submit")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.help.allow_only_one_submit", className = "not-formsimple")
    private Boolean allowOnlyOneSubmit;

    @Column(name = "overwrite_old_forms")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.help.overwrite_old_forms", className = "not-formsimple")
    private Boolean overwriteOldForms;

    @Column(name = "message_as_attach")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.message_as_attach", className = "not-formsimple")
    private Boolean messageAsAttach;

    @Column(name = "double_opt_in")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.help.doubleOptIn", className = "not-formsimple")
    private Boolean doubleOptIn;

    @Column(name = "add_tech_info")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title= "editor.form.addTechInfo")
    private Boolean addTechInfo;

    @Column(name = "message_as_attach_file_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.message_as_attach_file_name", className = "not-formsimple")
    @Size(max = 255)
    private String messageAsAttachFileName;

    @Column(name = "formmail_send_user_info_doc_id")
    private Integer formmailSendUserInfoDocId;

    @Column(name = "fields_email_header")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.help.fields_emaiL_header", className = "not-formsimple")
    @Size(max = 255)
    private String fieldsEmailHeader;

    @Column(name = "source")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.help.source", className = "not-formsimple")
    @Size(max = 255)
    private String source;

    @Column(name = "after_send_interceptor")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "editor.form.afterSendInterceptor")
    @Size(max = 255)
    private String afterSendInterceptor;

    @Column(name = "encryption_key")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title= "components.form.encryptionKey")
    @Size(max = 1024)
    private String encryptKey;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, title = "components.form.encryptionKey.tooltip", className = "allow-html")
    private transient String encrypKeyInfo;

    @Column(name = "max_size_in_kilobytes")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title= "components.forms.file_restrictions.file_size_in_kilobytes", className = "not-formsimple")
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

    @Column(name = "email_text_before")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title= "components.formsimple.textBefore")
    @Size(max = 1024)
    private String emailTextBefore;

    @Column(name = "email_text_after")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title= "components.formsimple.textAfter")
    @Size(max = 1024)
    private String emailTextAfter;

    @Column(name = "domain_id")
    private Integer domainId;

    //
    @Transient
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.form.use_form_mail_doc_id", className="dt-tree-page-null")
    private transient DocDetailsDto useFormMailDoc;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.form.send_user_info_doc_id", className="dt-tree-page-null")
    private transient DocDetailsDto formmailSendUserInfoDoc;


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
}
