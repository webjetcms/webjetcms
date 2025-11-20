package sk.iway.iwcm.components.multistep_form.rest;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
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

    @PostMapping(value = "/save-form", params={"form-name", "step-id"}, consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> saveForm(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, HttpServletRequest request) {
        JSONObject response = new JSONObject();
        try {
            MultistepFormsService.validateStepAndSetNextStep(formName, stepId, request, response, formItemsRepository, formStepsRepository);
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            Logger.error(MultistepFormsRestController.class, "saveForm() failed. " + e.getLocalizedMessage());

            response.put("message", Prop.getInstance(request).getText("datatable.error.unknown"));
            return ResponseEntity.badRequest().body(response.toString());
        }
    }

    @GetMapping(value="/get-step", params={"form-name", "step-id"}, produces = MediaType.TEXT_HTML)
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, HttpServletRequest request) {
        String encoding = SetCharacterEncodingFilter.getEncoding();
        if (Tools.isEmpty(encoding)) encoding = "UTF-8"; // Fallback
        String contentTypeWithCharset = MediaType.TEXT_HTML + "; charset=" + encoding;

        try {
            String stepHtml = MultistepFormsService.getFormStepHtml(formName, stepId, request, formStepsRepository, formItemsRepository);

            return ResponseEntity.ok()
                .header("Content-Type", contentTypeWithCharset)
                .body(stepHtml);
        } catch (Exception e) {
            Logger.error(MultistepFormsRestController.class, "getFormStepHtml() failed. " + e.getLocalizedMessage());

            JSONObject response = new JSONObject();
            response.put("message", Prop.getInstance(request).getText("datatable.error.unknown"));
            return ResponseEntity.badRequest()
                .header("Content-Type", contentTypeWithCharset)
                .body(response.toString());
        }
    }
}
