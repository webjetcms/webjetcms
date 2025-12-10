package sk.iway.iwcm.components.multistep_form.support;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

public interface StepInterceptorInterface {
    public void runInterceptor(String formName, Long currentStepId, JSONObject currentReceived, HttpServletRequest request, Map<String, String> errors) throws SaveFormException;
}
