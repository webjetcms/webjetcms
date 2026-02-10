package sk.iway.iwcm.components.multistep_form.support;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;

/**
 * Defines the contract for custom processing hooks used by multistep forms.
 * Implementations can validate data for individual steps, run post-step
 * interceptors (e.g., send verification codes), and customize the final save
 * process of the form.
 */
public interface FormProcessorInterface {
    /**
     * Validates user-submitted data for a specific step of a multistep form.
     *
     * @param formName logical identifier of the form
     * @param currentStepId ID of the step being validated
     * @param stepData JSON payload containing the step fields and values
     * @param request current HTTP request for context and additional data
     * @param errors mutable map to collect field or general error messages (key → message)
     * @throws SaveFormException when validation fails in a way that should stop processing
     */
    public void validateStep(String formName, FormStepEntity stepEntity, JSONObject stepData, HttpServletRequest request, Map<String, String> errors) throws SaveFormException;

    /**
     * Executes an optional interceptor after a step is submitted, for example sending
     * a verification code to email or SMS.
     *
     * @param formName logical identifier of the form
     * @param currentStepId ID of the step that was submitted
     * @param stepData JSON payload containing the step fields and values
     * @param request current HTTP request for context and additional data
     * @param errors mutable map to collect error messages produced by the interceptor
     * @throws SaveFormException to abort the flow when the interceptor detects blocking issues
     */
    public void runStepInterceptor(String formName, FormStepEntity stepEntity, JSONObject stepData, HttpServletRequest request, Map<String, String> errors) throws SaveFormException;

    /**
     * Performs custom final save logic for the form. The return value controls
     * whether the default WebJET save mechanism is executed.
     *
     * @param formName logical identifier of the form
     * @param formSettings configuration of the form used during saving
     * @param iLastDocId last document ID (if any) associated with the submission
     * @param request current HTTP request for context and additional data
     * @return {@code true} to continue with the default WebJET save; {@code false} to skip it
     * @throws SaveFormException for functional errors during save
     * @throws IOException for I/O related problems that occur during save
     */
    public boolean handleFormSave(String formName, FormSettingsEntity formSettings, Integer iLastDocId, HttpServletRequest request) throws SaveFormException, IOException;
}
