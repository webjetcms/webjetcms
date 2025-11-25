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
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp")
@WebjetAppStore(
    nameKey = "multistep_form.title",
    descKey="multistep_form.desc",
    itemKey= "cmp_app-cookiebar",
    imagePath = "ti ti-route",
    galleryImages = "",
    commonSettings = true
)
@Getter
@Setter
public class MultistepFormApp extends WebjetComponentAbstract {

    @JsonIgnore
    private final FormStepsRepository formStepsRepository;

    private static final String VIEW_PATH = "/apps/form/mvc/multistep-form"; //NOSONAR

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "", tab = "basic")
    private String formName;

    @Autowired
    public MultistepFormApp(FormStepsRepository formStepsRepository) {
        this.formStepsRepository = formStepsRepository;
    }

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(MultistepFormApp.class, "Init of MultistepFormApp app");
    }

    @DefaultHandler
	public String view(Model model, HttpServletRequest request) {
        model.addAttribute("stepPath", "/rest/multistep-form/get-step");
        model.addAttribute("formName", formName);

        //Get first step id
        List<FormStepEntity> steps = formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId());
        if(steps == null || steps.size() < 1)
            model.addAttribute("stepId", -1L);
        else
            model.addAttribute("stepId", steps.get(0).getId());

        return VIEW_PATH;
    }

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
       List<String> multistepFormNames = formStepsRepository.getMultistepFormNames(CloudToolsForCore.getDomainId());

        //TODO : later get allso real formNames ... this is saniotazed value
        List<OptionDto> formNameOption = new ArrayList<>();
        for(String formName : multistepFormNames) {
            formNameOption.add( new OptionDto(formName, formName, "") );
        }

        Map<String, List<OptionDto>> options = new HashMap<>();
        options.put("formName", formNameOption);
        return options;
    }
}
