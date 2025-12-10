package sk.iway.iwcm.components.multistep_form.support;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.iwcm.Tools;

public class CustomStepValidator implements StepValidatorInterface {

    @Override
    public void validateFields(String formName, Long currentStepId, JSONObject currentReceived, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        String wysiwyg = currentReceived.optString("wysiwyg-1", "");
        if(Tools.isEmpty(wysiwyg)) errors.put("wysiwyg-1", "DOPLN TO TY KOKOS");
    }
}
