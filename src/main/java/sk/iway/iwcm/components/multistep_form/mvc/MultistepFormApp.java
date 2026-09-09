package sk.iway.iwcm.components.multistep_form.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

/**
 * Renders and configures the multistep form application.
 *
 * <p>The component prepares request-scoped form state, enforces the single-submission
 * setting, exposes editor options, and resolves optional CSS templates.</p>
 */
@WebjetComponent("sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp")
@WebjetAppStore(
    nameKey = "multistep_form.title",
    descKey="components.multistep_form.desc",
    itemKey= "cmp_form",
    imagePath = "ti ti-route",
    galleryImages = "/components/multistep-form/",
    customHtml = "/apps/form/mvc/editor-component.html"
)
@Getter
@Setter
public class MultistepFormApp extends WebjetComponentAbstract {

    @JsonIgnore
    private final FormStepsRepository formStepsRepository;

    @JsonIgnore
    private final FormSettingsRepository formSettingsRepository;

    @JsonIgnore
    private final FormsRepository formsRepository;

    private static final String VIEW_PATH = "/apps/form/mvc/multistep-form"; //NOSONAR
    private static final String ERROR_PATH = "/apps/form/mvc/error"; //NOSONAR
    private static final String CSS_TEMPLATES_PATH = "/apps/form/mvc/styles/";

    /* Its importtant, that we use "-" and not "_" */
    public static final String DOC_ID = "-docid";
    public static final String PERMITTED = "-permitted";
    public static final String START_TIME = "-starttime";
    public static final String COUNTER = "-counter";
    private static final String REQUEST_COUNTER_ATTR = "webjet_multistep_form_counter";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "formslist.nazov_formularu", tab = "basic")
    private String formName;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.multistep_form.form-content.css-template", tab = "basic")
    private String cssTemplate;

    @Autowired
    public MultistepFormApp(FormStepsRepository formStepsRepository, FormSettingsRepository formSettingsRepository, FormsRepository formsRepository) {
        this.formStepsRepository = formStepsRepository;
        this.formSettingsRepository = formSettingsRepository;
        this.formsRepository = formsRepository;
    }

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(MultistepFormApp.class, "Init of MultistepFormApp app");
    }

    /**
     * Prepares the first step of the configured form for rendering.
     *
     * <p>The handler rejects duplicate submissions when required, initializes the
     * session state used by subsequent steps, and records the form view.</p>
     *
     * @param model  MVC model populated for the form view
     * @param request  current HTTP request used to initialize form and session state
     * @return path to the multistep form view, or the error view when another submission is not allowed
     */
    @DefaultHandler
	public String view(Model model, HttpServletRequest request) {
        //Check first, if user can fill form
        Identity currentcUser = UsersDB.getCurrentUser(request);
        if(currentcUser != null && currentcUser.getUserId() > 0) {
            FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
            if(formSettings != null && Tools.isTrue(formSettings.getAllowOnlyOneSubmit())) {
                //Did user allready submitted ?
                Integer count = formsRepository.getNumberOfSubmitted(formName, CloudToolsForCore.getDomainId());
                if(count != null && count > 0) {
                    model.addAttribute("err_msg", Prop.getInstance(request).getText("checkform.formIsAllreadySubmitted"));
                    return ERROR_PATH;
                }
            }
        }

        model.addAttribute("stepPath", "/rest/multistep-form/get-step");
        model.addAttribute("formName", formName);
        model.addAttribute("cssTemplatePath", getValidCssTemplatePath(cssTemplate));

        Logger.debug(MultistepFormApp.class, "Generating CSRF token for multistep form, formName= " + formName + " actual datetime is : " + Tools.getNow());

        String csrf = CSRF.getCsrfToken(request.getSession(), true);
        model.addAttribute("csrf", csrf);

        // Count how many multistep forms are on this page (request-scoped)
        int formCounter = Tools.getIntValue(String.valueOf(request.getAttribute(REQUEST_COUNTER_ATTR)), 0) + 1;
        request.setAttribute(REQUEST_COUNTER_ATTR, formCounter);

        // Set docId of current page
        String sessionKey = MultistepFormsService.getNewSessionKey(formName, csrf);
        request.getSession().setAttribute(sessionKey + DOC_ID, Tools.getIntValue(request.getParameter("docid"), -1));
        request.getSession().setAttribute(sessionKey + PERMITTED, Boolean.TRUE);
        request.getSession().setAttribute(sessionKey + START_TIME, Tools.getNow());
        request.getSession().setAttribute(sessionKey + COUNTER, formCounter);

        //Get and set first step id
        model.addAttribute("stepId", formStepsRepository.getFirstStepId(formName, CloudToolsForCore.getDomainId()).orElse(-1L));

        // user lng
        model.addAttribute("language", PageLng.getUserLng(request));

        // Increment form view count
        formSettingsRepository.incrementFormViewCount(formName, CloudToolsForCore.getDomainId());

        return VIEW_PATH;
    }

    /**
     * Builds editor options for available multistep forms and CSS templates.
     *
     * @param componentRequest  current component editor request
     * @param request  current HTTP request used to localize option labels
     * @return options keyed by {@code formName} and {@code cssTemplate}
     */
    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        List<String> multistepFormNames = formStepsRepository.getMultistepFormNames(CloudToolsForCore.getDomainId());

        List<OptionDto> formNameOption = new ArrayList<>();
        for(String formName : multistepFormNames) {
            formNameOption.add( new OptionDto(formName, formName, "") );
        }

        Prop prop = Prop.getInstance(request);
        List<OptionDto> cssTemplateOptions = new ArrayList<>();
        cssTemplateOptions.add(new OptionDto(prop.getText("components.multistep_form.form-content.css-template-none"), "", null));
        for(LabelValue template : getCssTemplates()) {
            cssTemplateOptions.add(new OptionDto(template.getLabel(), template.getValue(), null));
        }

        Map<String, List<OptionDto>> options = new HashMap<>();
        options.put("formName", formNameOption);
        options.put("cssTemplate", cssTemplateOptions);
        return options;
    }

    /**
     * Lists readable CSS template files available to the multistep form component.
     *
     * @return sorted label-value pairs containing each template file name and public path,
     *         or an empty list when the template directory is unavailable
     */
    public static List<LabelValue> getCssTemplates() {
        List<LabelValue> templates = new ArrayList<>();
        IwcmFile directory = new IwcmFile(Tools.getRealPath(CSS_TEMPLATES_PATH));
        if(directory.exists() == false || directory.isDirectory() == false || directory.canRead() == false) return templates;

        IwcmFile[] files = FileTools.sortFilesByName(directory.listFiles());
        if(files == null) return templates;

        for(IwcmFile file : files) {
            if(file.isFile() == false || file.canRead() == false || file.getName().toLowerCase(Locale.ROOT).endsWith(".css") == false) continue;
            templates.add(new LabelValue(file.getName(), CSS_TEMPLATES_PATH + file.getName()));
        }
        return templates;
    }

    private static String getValidCssTemplatePath(String cssTemplate) {
        if(Tools.isEmpty(cssTemplate)) return "";

        for(LabelValue template : getCssTemplates()) {
            if(cssTemplate.equals(template.getValue())) return template.getValue();
        }
        return "";
    }
}
