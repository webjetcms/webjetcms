package sk.iway.iwcm.components.multistep_form.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp")
@WebjetAppStore(nameKey = "multistep_form.title", descKey="multistep_form.desc", imagePath = "ti ti-route", galleryImages = "", commonSettings = true)
@Getter
@Setter
public class MultistepFormApp extends WebjetComponentAbstract {

    @JsonIgnore
    private final FormItemsRepository formItemsRepository;

    @JsonIgnore
    private final FormStepsRepository formStepsRepository;

    private static final String VIEW_PATH = "/apps/form/mvc/multistep-form"; //NOSONAR

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "", tab = "basic")
    private String formName;

    @Autowired
    public MultistepFormApp(FormItemsRepository formItemsRepository, FormStepsRepository formStepsRepository) {
        this.formItemsRepository = formItemsRepository;
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
        model.addAttribute("stepId", steps.get(0).getId());

        return VIEW_PATH;
    }

    // @DefaultHandler
	// public String view(Model model, HttpServletRequest request) {
    //     List<FormStepEntity> steps = formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId());
    //     model.addAttribute("currentStepCnt", 1);
    //     model.addAttribute("allStepsCnt", steps.size());

    //     FormStepEntity currentStep = steps.get(0);
    //     prepareModel(model, currentStep, request);
    //     return VIEW_PATH;
	// }

    // public String saveForm(@Valid @ModelAttribute("entity") MultistepFormDto entity, BindingResult result, Model model, HttpServletRequest request) {
    //     FormStepEntity currentStep = formStepsRepository.findByFormNameAndId(formName, entity.getStepId()).orElse(null);
    //     if(currentStep == null) return ERROR_VIEW_PATH;

    //     //
    //     List<String> errors = validateStep(currentStep.getId(), entity, request);
    //     if(errors != null && errors.isEmpty() == false) {
    //         model.addAttribute("errors", errors);
    //         return VIEW_PATH;
    //     }

    //     FormStepEntity nextStep = null;
    //     List<FormStepEntity> steps = formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(formName, CloudToolsForCore.getDomainId());
    //     model.addAttribute("allStepsCnt", steps.size());

    //     for(int i = 0; i < steps.size(); i++) {
    //         if(steps.get(i).getId() == entity.getStepId()) {
    //             //This step is current - set next one if is there
    //             if((i + 1) < steps.size()) {
    //                 model.addAttribute("currentStepCnt", i + 2);
    //                 nextStep = steps.get(i + 1);
    //             }
    //             else break; // there is no next step
    //         }
    //     }

    //     if(nextStep == null) {
    //         //This is the end
    //         return DONE_VIEW_PATH;
    //     }

    //     prepareModel(model, nextStep, request);

    //     return VIEW_PATH;
    // }

    // private List<String> validateStep(Long stepId, MultistepFormDto entity, HttpServletRequest request) {
    //     List<String> errors = new ArrayList<>(); // Step is accesses via ID but just in case add fomrName into select

    //     //Step fields
    //     List<FormItemEntity> stepItems = formItemsRepository.findAllByStepIdAndDomainIdOrderBySortPriorityAsc(stepId, CloudToolsForCore.getDomainId());
    //     for(FormItemEntity stepItem : stepItems) {
    //         if(Tools.isTrue(stepItem.getRequired())) {
    //             String id = DocTools.removeChars(stepItem.getLabel() , true);
    //             String value = Tools.getStringValue(request.getParameter(id), null);
    //             if(Tools.isEmpty(value)) {
    //                 errors.add(stepItem.getFieldType() + " - EMPTY");
    //             }
    //         }
    //     }

    //     return errors;
    // }

    // private void prepareModel(Model model, FormStepEntity currentStep, HttpServletRequest request) {
    //     model.addAttribute("heading", currentStep.getStepName());
    //     model.addAttribute("sub-heading", currentStep.getStepSubName());

    //     //
    //     model.addAttribute("stepItems", getFieldsHtml(currentStep, request));

    //     //
    //     MultistepFormDto dto = new MultistepFormDto();
    //     dto.setStepId(currentStep.getId());
    //     model.addAttribute("entity", dto);
    // }

    // private List<String> getFieldsHtml(FormStepEntity currentStep, HttpServletRequest request) {
    //     List<FormItemEntity> stepItems = formItemsRepository.findAllByStepIdAndDomainIdOrderBySortPriorityAsc(currentStep.getId(), CloudToolsForCore.getDomainId());
    //     Prop prop = Prop.getInstance(request);
    //     List<String> fieldsHtml = new ArrayList<>();
    //     for(FormItemEntity stepItem : stepItems) fieldsHtml.add( FormsService.getFieldHtml(stepItem, prop) );
    //     return fieldsHtml;
    // }


    // @Override
    // public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
    //     Map<String, List<OptionDto>> options = new HashMap<>();
    //     options.put("formName", getMultistepFormsOptions());
    //     return options;
    // }

    // private final List<OptionDto> getMultistepFormsOptions() {
    //     List<String> multistepFormNames = formStepsRepository.getMultistepFormNames(CloudToolsForCore.getDomainId());

    //     //TODO : later get allso real formNames ... this is saniotazed value
    //     List<OptionDto> options = new ArrayList<>();
    //     for(String formName : multistepFormNames) {
    //         options.add( new OptionDto(formName, formName, "") );
    //     }

    //     return options;
    // }
}
