package sk.iway.iwcm.components.multistep_form.rest;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

/**
 * Exposes public HTTP endpoints used while completing a multistep form.
 *
 * <p>The controller delegates validation and session handling to
 * {@link MultistepFormsService} and converts processing failures into localized JSON
 * responses.</p>
 */
@RestController
@RequestMapping("/rest/multistep-form")
public class MultistepFormsRestController {

    private final MultistepFormsService multistepFormsService;

    @Autowired
    public MultistepFormsRestController(MultistepFormsService multistepFormsService) {
       this.multistepFormsService = multistepFormsService;
    }

    /**
     * Validates and stores the submitted step, completing the form on its final step.
     *
     * @param formName logical form name
     * @param stepId submitted step identifier
     * @param language language used to localize validation messages
     * @param request request containing the JSON payload and form session
     * @return JSON result with the next step or localized error details
     */
    @PostMapping(value = "/save-form", params={"form-name", "step-id", "language"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveForm(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, @RequestParam("language") String language, HttpServletRequest request) { //NOSONAR language is consumed implicitly by PageLng.getUserLng(request)
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

            Adminlog.add(Adminlog.TYPE_MULTISTEP_FORM_USERS, "Error while saving form (or form step):" + formName + ". Error: " + sfe.getLocalizedMessage(), multistepFormsService.getFormId(formName), FormStatService.AUDIT_SUBID_ERROR_STEP_SAVE);

            return ResponseEntity.badRequest().body(response.toString());
        }

        catch (Exception e) {
            Logger.error(MultistepFormsRestController.class, "saveForm() failed. " + e.getLocalizedMessage(), e);
            response.put("err_msg", Prop.getInstance( PageLng.getUserLng(request) ).getText("datatable.error.unknown"));

            Adminlog.add(Adminlog.TYPE_MULTISTEP_FORM_USERS, "Error while saving form (or form step):" + formName + ". Error: " + e.getLocalizedMessage(), multistepFormsService.getFormId(formName), FormStatService.AUDIT_SUBID_ERROR_STEP_SAVE);

            return ResponseEntity.badRequest().body(response.toString());
        }
    }

    /**
     * Returns rendered content and saved state for a permitted form step.
     *
     * @param formName logical form name
     * @param stepId requested step identifier
     * @param language language used to render localized form content
     * @param request request containing the form session
     * @return JSON containing step HTML, conditions, and saved values, or an error response
     */
    @GetMapping(value="/get-step", params={"form-name", "step-id", "language"})
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, @RequestParam("language") String language, HttpServletRequest request) { //NOSONAR language is consumed implicitly by PageLng.getUserLng(request)
        String encoding = SetCharacterEncodingFilter.getEncoding();
        if (Tools.isEmpty(encoding)) encoding = "UTF-8"; // Fallback
        String contentTypeWithCharset = MediaType.APPLICATION_JSON_VALUE + "; charset=" + encoding;

        try {
            // This is call from outside (no admin section) - check that csrf is valid for this form
            if(multistepFormsService.validateFormInfo(formName, stepId, request) == false)
                throw new IllegalStateException("Provided params to get stepHtml are invalid.");

            FormHtmlHandler formHtmlHandler = new FormHtmlHandler(formName, request);
            FormConditionsHandler formConditionsHandler = new FormConditionsHandler(formName, request);
            Pair<JSONObject, JSONObject> savedStepData = multistepFormsService.getSavedStepData(formName, stepId, request);
            request.setAttribute("multistepFormPrefix", formHtmlHandler.getDomIdPrefix());

            JSONObject result = new JSONObject();
            result.put("html", formHtmlHandler.getFormStepHtml(stepId, request));
            result.put("domIdPrefix", formHtmlHandler.getDomIdPrefix());
            result.put("visibilityConditions", formConditionsHandler.getVisibilityConditions(stepId));
            result.put("requirementConditions", formConditionsHandler.getRequirementConditions(stepId));
            result.put("savedValues", savedStepData.getFirst());
            result.put("savedFiles", savedStepData.getSecond());

            return ResponseEntity.ok()
                .header("Content-Type", contentTypeWithCharset)
                .body( result.toString() );
        } catch (Exception e) {
            Logger.error(MultistepFormsRestController.class, "getFormStepHtml() failed. " + e.getLocalizedMessage(), e);
            JSONObject response = new JSONObject();
            response.put("err_msg", Prop.getInstance( PageLng.getUserLng(request) ).getText("datatable.error.unknown"));

            Adminlog.add(Adminlog.TYPE_MULTISTEP_FORM_USERS, "Error while getting next form step. formName:" + formName + ", stepId:" + stepId + ". Error: " + e.getLocalizedMessage() , multistepFormsService.getFormId(formName), FormStatService.AUDIT_SUBID_ERROR_STEP_GET);

            return ResponseEntity.badRequest()
                .header("Content-Type", contentTypeWithCharset)
                .body(response.toString());
        }
    }

    @GetMapping(value = "/autocomplete", params = {"step-id", "item-id", "term"})
    public List<LabelValue> getAutocompleteOptions(
        @RequestParam(value = "form-name", required = false) String formName,
        @RequestParam("step-id") Long stepId,
        @RequestParam("item-id") Long itemId,
        @RequestParam String term,
        HttpServletRequest request
    ) {
        return multistepFormsService.getAutocompleteOptions(formName, stepId, itemId, term, request);
    }

    /**
     * Streams a temporary image that belongs to an upload field in the current form session.
     *
     * @param formName logical form name
     * @param fileKey temporary upload key
     * @param request request containing the form session
     * @param response response receiving the image or a not-found status
     * @throws java.io.IOException when the response cannot be written
     */
    @GetMapping(value = "/temp-file-preview", params = {"form-name", "file-key"})
    public void getTempFilePreview(@RequestParam("form-name") String formName, @RequestParam("file-key") String fileKey, HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        IwcmFile file = multistepFormsService.getSavedTempFilePreview(formName, fileKey, request);
        if(file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if(FilePathTools.writeFileOut(file, request, response) == false) response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
