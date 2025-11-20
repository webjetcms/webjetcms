package sk.iway.iwcm.components.multistep_form.mvc;

import java.util.List;

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
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
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
}
