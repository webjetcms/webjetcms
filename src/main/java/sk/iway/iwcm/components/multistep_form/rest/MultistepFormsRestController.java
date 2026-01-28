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
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.i18n.Prop;

@RestController
@RequestMapping("/rest/multistep-form")
public class MultistepFormsRestController {

    private final MultistepFormsService multistepFormsService;
    private final FormHtmlHandler formHtmlHandler;

    @Autowired
    public MultistepFormsRestController(MultistepFormsService multistepFormsService, FormHtmlHandler formHtmlHandler) {
       this.multistepFormsService = multistepFormsService;
       this.formHtmlHandler = formHtmlHandler;
    }

    @PostMapping(value = "/save-form", params={"form-name", "step-id"}, consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> saveForm(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, HttpServletRequest request) {
        JSONObject response = new JSONObject();

        try {
            // Save step form, is its last step, will save whole form answers too
            multistepFormsService.saveFormStep(formName, stepId, request, response);
            return ResponseEntity.ok(response.toString());
        }

        catch (SaveFormException sfe) {
            Logger.error(MultistepFormsRestController.class, "saveForm() failed. " + sfe.getLocalizedMessage(), sfe);
            response.put("err_msg", sfe.getLocalizedMessage());
            response.put("end_try", sfe.isEndUserTry());

            if(Tools.isNotEmpty(sfe.getErrorRedirect()))
                response.put("err_redirect", sfe.getErrorRedirect());

            return ResponseEntity.badRequest().body(response.toString());
        }

        catch (Exception e) {
            Logger.error(MultistepFormsRestController.class, "saveForm() failed. " + e.getLocalizedMessage(), e);
            response.put("err_msg", Prop.getInstance(request).getText("datatable.error.unknown"));
            return ResponseEntity.badRequest().body(response.toString());
        }
    }

    @GetMapping(value="/get-step", params={"form-name", "step-id"}, produces = MediaType.TEXT_HTML)
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, HttpServletRequest request) {
        String encoding = SetCharacterEncodingFilter.getEncoding();
        if (Tools.isEmpty(encoding)) encoding = "UTF-8"; // Fallback
        String contentTypeWithCharset = MediaType.TEXT_HTML + "; charset=" + encoding;

        try {
            // This is call from outside (no admin section) - check that csrf is valid for this form
            if(multistepFormsService.validateFormInfo(formName, stepId, request) == false)
                throw new IllegalStateException("Provided params to get stepHtml are invalid.");

            return ResponseEntity.ok()
                .header("Content-Type", contentTypeWithCharset)
                .body( formHtmlHandler.getFormStepHtml(formName, stepId, request) );
        } catch (Exception e) {
            Logger.error(MultistepFormsRestController.class, "getFormStepHtml() failed. " + e.getLocalizedMessage(), e);
            JSONObject response = new JSONObject();
            response.put("err_msg", Prop.getInstance(request).getText("datatable.error.unknown"));
            return ResponseEntity.badRequest()
                .header("Content-Type", contentTypeWithCharset)
                .body(response.toString());
        }
    }
}
