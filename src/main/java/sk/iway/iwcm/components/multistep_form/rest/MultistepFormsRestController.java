package sk.iway.iwcm.components.multistep_form.rest;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.form.FormAttributeDB;
import sk.iway.iwcm.i18n.Prop;

@RestController
@RequestMapping("/rest/multistep-form")
public class MultistepFormsRestController {

    private final FormItemsRepository formItemsRepository;
    private final FormStepsRepository formStepsRepository;

    @Autowired
    public MultistepFormsRestController(FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
    }

    @GetMapping(value="/get-step", params={"form-name", "step-id"}, produces = MediaType.TEXT_HTML)
    public ResponseEntity<String> computeHoursReservationPrice(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, HttpServletRequest request) {
        StringBuilder HTML = new StringBuilder();
        boolean isEmailRender = false;
        boolean rowView = false;

        FormStepEntity currentStep = formStepsRepository.findByFormNameAndId(formName, stepId).orElse(null);

        Prop prop = Prop.getInstance(request);

        String requiredLabelAdd = prop.getText("components.formsimple.requiredLabelAdd");
        Map<String, String> attributes = new FormAttributeDB().load(DocTools.removeChars(formName, true));


        List<FormItemEntity> stepItems = formItemsRepository.findAllByStepIdAndDomainIdOrderBySortPriorityAsc(currentStep.getId(), CloudToolsForCore.getDomainId());

        String recipients = "";
        if (attributes!=null && Tools.isNotEmpty(attributes.get("recipients"))) recipients = attributes.get("recipients");

        //tu evidujeme nadpisy nad pole, ktore sme uz raz zobrazili
        Set<String> firstTimeHeadingSet = new HashSet<String>();

        HTML.append(FormsService.replaceFields(prop.getText("components.formsimple.form.start"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop));
        if (rowView) HTML.append("<div class=\"row\">");

        for(int i = 0; i < stepItems.size(); i++)
        {
            JSONObject item = new JSONObject(stepItems.get(i));

            String fieldType = item.getString("fieldType");

            String input = prop.getText("components.formsimple.input."+fieldType);
            item.put("labelOriginal", item.getString("label"));
            if (Tools.isEmpty(item.getString("label")))
            {
                String label = prop.getText("components.formsimple.label."+fieldType);
                item.put("label", label);
            }

            String html = FormsService.replaceFields(input, formName, recipients, item, requiredLabelAdd, isEmailRender, rowView, firstTimeHeadingSet, prop);
            if (html.contains("!INCLUDE")) {
                html = EditorToolsForCore.renderIncludes(html, true, request);
            }

            HTML.append(html);
        }

        if (rowView) HTML.append("</div>");
        HTML.append( FormsService.replaceFields(prop.getText("components.formsimple.form.end"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop) );


        return ResponseEntity.ok().body(HTML.toString());
    }
}
