package sk.iway.iwcm.components.formsimple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@WebjetComponent("sk.iway.iwcm.components.formsimple.FormSimpleApp")
@WebjetAppStore(
    nameKey = "components.formsimple.title",
    descKey = "components.formsimple.desc",
    itemKey = "formsimple",
    imagePath = "ti ti-forms",
    galleryImages = "/components/formsimple/screenshot-1.jpg,/components/formsimple/screenshot-2.jpg,/components/formsimple/screenshot-3.jpg,/components/formsimple/screenshot-4.jpg",
    componentPath = "/components/formsimple/form.jsp",
    customHtml = "/apps/formsimple/admin/editor-component.html"
)
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
        @DataTableTab(id = "advanced", title = "datatable.tab.advanced"),
        @DataTableTab(id = "items", title = "components.news.items")
})
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormSimpleApp extends WebjetComponentAbstract {

    @JsonIgnore
    private static final String ITEM_KEY_LABEL_PREFIX = "components.formsimple.label.";
    private static final String ITEM_KEY_HIDE_FIELDS_PREFIX = "components.formsimple.hide.";

    @JsonIgnore
    public static final String ATTRIBUTE_PREFIX = "attribute_";

    @Autowired
    @JsonIgnore
    private FormAttributesRepository formAttributesRepository;

    /* TAB BASIC */

    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.formsimple.formName")
    private String formName;

    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.formsimple.recipients")
    private String attribute_recipients;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "components.formsimple.rowView")
    private Boolean rowView;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, tab = "basic", title = "components.formsimple.textBefore")
    private String attribute_emailTextBefore;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, tab = "basic", title = "components.formsimple.textAfter")
    private String attribute_emailTextAfter;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "editor.form.force_text_plain")
    private Boolean attribute_forceTextPlain;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "editor.form.addTechInfo")
    private Boolean attribute_addTechInfo;

    /* TAB ADVANCED */

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "advanced", title = "editor.form.cc_emails")
    private String attribute_ccEmails;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "advanced", title = "editor.form.bcc_emails")
    private String attribute_bccEmails;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "advanced", title = "editor.form.reply_to_emails")
    private String attribute_replyTo;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "advanced", title = "editor.form.subject")
    private String attribute_subject;

    @DataTableColumn(inputType=DataTableColumnType.ELFINDER, tab="advanced", title="editor.form.forward", className="dt-tree-page-null")
    private String attribute_forward;

    @DataTableColumn(inputType=DataTableColumnType.ELFINDER, tab="advanced", title="editor.form.forward_fail", className="dt-tree-page-null")
    private String attribute_forwardFail;

    @DataTableColumn(inputType=DataTableColumnType.SELECT, tab="advanced", title="editor.form.forward_type", editor = {
        @DataTableColumnEditor(options = {
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.default", value = ""),
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.forward", value = "forward"),
                @DataTableColumnEditorAttr(key = "editor.form.forward_type.option.addParams", value = "addParams")
        })
    })
    private String attribute_forwardType;

    @DataTableColumn(inputType=DataTableColumnType.JSON, tab="advanced", title="editor.form.send_user_info_doc_id", className="dt-tree-page-null")
    private DocDetailsDto attribute_formmail_sendUserInfoDocId;

    @DataTableColumn(inputType=DataTableColumnType.JSON, tab="advanced", title="editor.form.use_form_mail_doc_id", className="dt-tree-page-null")
    private DocDetailsDto attribute_useFormMailDocId;

    @DataTableColumn(inputType=DataTableColumnType.JSON, tab="advanced", title="editor.form.use_form_doc_id", className="dt-tree-page-null", editor = {
        @DataTableColumnEditor(
            message="editor.form.help.use_form_doc_id"
        )
    })
    private DocDetailsDto attribute_useFormDocId;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "advanced", title = "editor.form.afterSendInterceptor")
    private String attribute_afterSendInterceptor;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, tab = "advanced", title = "components.form.encryptionKey")
    private String attribute_encryptKey;

    @JsonIgnore
    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "advanced", title = "components.form.encryptionKey.tooltip", className = "allow-html")
    private String encrypKeyInfo;

    /* TAB ITEMS */

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "items", title="&nbsp;", className = "dt-json-editor",editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.formsimple.FormSimpleItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-localJson", value = "true")
            }
        )})
    private String editorData = null;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        options.put("optionsTypeVisibility", getFiledTypeVisibility(request));
        return options;
    }

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        String formName = "";

        if(Tools.isNotEmpty(componentRequest.getParameters())) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[\\\"]*formName[\\\"]*=([^,]+)")
                .matcher(componentRequest.getParameters());

            formName = matcher.find() ? matcher.group(1) : null;
        }

        //Set formAttributes into params
        if(Tools.isNotEmpty(formName)) {
            StringBuilder sb = new StringBuilder();

            // formName is sanitazed
            // app include contains original text like "Formulár ľahko" BUT attribues have value "formular-lahko"
            for(FormAttributesEntity attribute : formAttributesRepository.findAllByFormNameAndDomainId(DocTools.removeChars(formName, true), CloudToolsForCore.getDomainId())) {
                sb.append(", ").append(ATTRIBUTE_PREFIX).append(attribute.getParamName());
                sb.append("=\"").append(attribute.getValue()).append("\"");
            }

            String newParams = componentRequest.getParameters().replaceFirst("editorData=", sb.toString() + ", editorData=");
            componentRequest.setParameters(newParams);
        }

        //Set defautl values into params, when creating new app
        if(isNewApp(componentRequest)) {
            this.attribute_forceTextPlain = false;
            this.attribute_addTechInfo = true;
            this.attribute_forwardType = "";

            UserDetails currentUser =  UsersDB.getCurrentUser(request);
            if(currentUser != null && Tools.isNotEmpty(currentUser.getEmail())) this.attribute_recipients = currentUser.getEmail();
            else this.attribute_recipients = "";

            if(componentRequest.getDocId() < 1) {
                // New page, set default value
                String defaultFormName = Prop.getInstance(request).getText("components.formsimple.title") + " " + Tools.formatDate(Tools.getNow());
                this.formName = defaultFormName;
            } else {
                // Page do exist, use values from page
                DocDetails doc = DocDB.getInstance().getDoc(componentRequest.getDocId());
                this.formName = doc.getTitle();
            }
        }

        super.initAppEditor(componentRequest, request);
    }

    private boolean isNewApp(ComponentRequest componentRequest) {
        return Tools.isEmpty(componentRequest.getParameters()) && Tools.isEmpty(componentRequest.getOriginalComponentName());
    }

    public static List<OptionDto> getFiledTypeVisibility(HttpServletRequest request) {

        Prop prop = Prop.getInstance(request);
        Map<String,String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_HIDE_FIELDS_PREFIX);

        List<OptionDto> options = new ArrayList<>();
        for(Entry<String, String> entry : formsimpleFields.entrySet())
            options.add(new OptionDto(entry.getValue(), entry.getKey().substring(ITEM_KEY_HIDE_FIELDS_PREFIX.length()), ""));

        return options;
    }

    //Called by @DataTableOptionMethod in FormSimpleItem, for field fieldType
    public static List<LabelValue> getFieldTypes() {
        Prop prop = Prop.getInstance();

        List<LabelValue> options = new ArrayList<>();
        Map<String,String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_LABEL_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet())
            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length())));

        //sort options by label
        options.sort((o1, o2) -> o1.getLabel().compareToIgnoreCase(o2.getLabel()));

        return options;
    }
}