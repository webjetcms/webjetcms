package sk.iway.iwcm.components.multistep_form.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
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
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

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

    /* Its importtant, that we use "-" and not "_" */
    public static final String DOC_ID = "-docid";
    public static final String PERMITTED = "-permitted";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "formslist.nazov_formularu", tab = "basic")
    private String formName;

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

    @DefaultHandler
	public String view(Model model, HttpServletRequest request) {
        //Check first, if user can fill form
        Identity currentcUser = UsersDB.getCurrentUser(request);
        if(currentcUser != null && currentcUser.getUserId() > 0) {
            FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
            if(Tools.isTrue(formSettings.getAllowOnlyOneSubmit())) {
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

        Logger.debug(MultistepFormApp.class, "Generating CSRF token for multistep form, formName= " + formName + " actual datetime is : " + Tools.getNow());

        String csrf = CSRF.getCsrfToken(request.getSession(), true);
        model.addAttribute("csrf", csrf);

        // Set docId of current page
        String sessionKey = MultistepFormsService.getNewSessionKey(formName, csrf);
        request.getSession().setAttribute(sessionKey + DOC_ID, Tools.getIntValue(request.getParameter("docid"), -1));
        request.getSession().setAttribute(sessionKey + PERMITTED, Boolean.TRUE);

        //Get and set first step id
        model.addAttribute("stepId", formStepsRepository.getFirstStepId(formName, CloudToolsForCore.getDomainId()).orElse(-1L));

        return VIEW_PATH;
    }

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
       List<String> multistepFormNames = formStepsRepository.getMultistepFormNames(CloudToolsForCore.getDomainId());

        List<OptionDto> formNameOption = new ArrayList<>();
        for(String formName : multistepFormNames) {
            formNameOption.add( new OptionDto(formName, formName, "") );
        }

        Map<String, List<OptionDto>> options = new HashMap<>();
        options.put("formName", formNameOption);
        return options;
    }
}
