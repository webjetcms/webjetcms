package sk.iway.iwcm.headless.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.headless.dto.FieldError;
import sk.iway.iwcm.headless.dto.FormResult;
import sk.iway.iwcm.headless.dto.FormSubmitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for processing form submissions through the headless API.
 * Reuses existing WebJET form processing logic where possible.
 */
@Service("HeadlessFormActionService")
public class HeadlessFormActionService {

    /**
     * Processes a form submission request.
     *
     * @param request     the form submission request
     * @param httpRequest the HTTP request
     * @param response    the HTTP response
     * @return FormResult with success/error status
     */
    public FormResult processFormSubmission(FormSubmitRequest request, 
                                           HttpServletRequest httpRequest, 
                                           HttpServletResponse response) {
        FormResult formResult = new FormResult();
        formResult.setSuccess(false);

        // Validate required fields
        List<FieldError> validationErrors = validateFormRequest(request);
        if (!validationErrors.isEmpty()) {
            formResult.setFieldErrors(validationErrors);
            formResult.setMessage("Validation failed.");
            return formResult;
        }

        try {
            // Identify the form by formId, formName, or componentKey
            String formId = request.getFormId();
            String formName = request.getFormName();
            String componentKey = request.getComponentKey();

            // Build a Map<String, String> from the form fields for processing
            Map<String, String> formFields = request.getFields();

            // Set up request attributes for existing form processing
            if (Tools.isNotEmpty(formId)) {
                httpRequest.setAttribute("formId", formId);
            }
            if (Tools.isNotEmpty(formName)) {
                httpRequest.setAttribute("formName", formName);
            }
            if (Tools.isNotEmpty(componentKey)) {
                httpRequest.setAttribute("componentKey", componentKey);
            }
            if (Tools.isNotEmpty(request.getPagePath())) {
                httpRequest.setAttribute("pagePath", request.getPagePath());
            }
            if (Tools.isNotEmpty(request.getLocale())) {
                httpRequest.setAttribute("locale", request.getLocale());
            }

            // Store form fields as request attributes for existing form processing
            if (formFields != null) {
                for (Map.Entry<String, String> entry : formFields.entrySet()) {
                    httpRequest.setAttribute("field_" + entry.getKey(), entry.getValue());
                }
            }

            // TODO: Integrate with existing form processing components
            // This is a placeholder that returns success for POC validation.
            // In production, this would call the existing form processing pipeline
            // (e.g., InquiryComponent, MultiStepFormComponent, etc.)

            formResult.setSuccess(true);
            formResult.setMessage("Form submitted successfully.");

        } catch (Exception e) {
            sk.iway.iwcm.Logger.error("HeadlessFormActionService.processFormSubmission", e);
            formResult.setMessage("Form processing error: " + e.getMessage());
        }

        return formResult;
    }

    /**
     * Validates the form submission request.
     */
    private List<FieldError> validateFormRequest(FormSubmitRequest request) {
        List<FieldError> errors = new ArrayList<>();

        // Check form identification
        if (Tools.isEmpty(request.getFormId()) 
                && Tools.isEmpty(request.getFormName()) 
                && Tools.isEmpty(request.getComponentKey())) {
            errors.add(new FieldError("formId/formName/componentKey", 
                    "One of formId, formName, or componentKey is required."));
        }

        // Check fields payload
        if (request.getFields() == null || request.getFields().isEmpty()) {
            errors.add(new FieldError("fields", "Form fields payload is required."));
        } else {
            // Validate that required fields are present
            // This is a placeholder - actual validation depends on the specific form
            for (Map.Entry<String, String> entry : request.getFields().entrySet()) {
                if (Tools.isEmpty(entry.getKey())) {
                    errors.add(new FieldError("field", "Field name cannot be empty."));
                }
            }
        }

        return errors;
    }
}
