package sk.iway.iwcm.components.multistep_form.support;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;

public interface FormProcessorInterface {
    // Validate step data
    public void validateStep(String formName, Long currentStepId, JSONObject stepData, HttpServletRequest request, Map<String, String> errors) throws SaveFormException;

    // Call interceptor after step send - for example, send verify code to email or sms
    public void runStepInterceptor(String formName, Long currentStepId, JSONObject stepData, HttpServletRequest request, Map<String, String> errors) throws SaveFormException;

    // Do custom form save
    // return TRUE - WebJET save will be called also
    //        FALSE - WebJET save will NOT be called
    public boolean handleFormSave(String formName, FormSettingsEntity formSettings, Integer iLastDocId, HttpServletRequest request) throws SaveFormException, IOException;
}
