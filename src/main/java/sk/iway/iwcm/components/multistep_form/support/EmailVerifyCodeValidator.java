package sk.iway.iwcm.components.multistep_form.support;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.i18n.Prop;

public class EmailVerifyCodeValidator implements StepValidatorInterface {

    @Override
    public void validateFields(String formName, Long currentStepId, JSONObject currentReceived, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        String verifyCode = null;
        String foundKey = null;
        for(String key : currentReceived.keySet()) {
            foundKey = new String(key);
            //remove key postfix
            key = key.replaceFirst("-\\d+$", "");
            if(key.equalsIgnoreCase("verify_code")) {
                verifyCode = currentReceived.getString(foundKey);
                break;
            }
        }

        String sessionVerifyCode = (String) request.getSession().getAttribute(MultistepFormsService.getSessionKey(formName, request) + "_" + EmailVerifyCodeInterceptor.SESSION_VERIFY_CODE_KEY);
        if(Tools.isEmpty(verifyCode) || Tools.isEmpty(sessionVerifyCode) || verifyCode.equals(sessionVerifyCode) == false) {
            errors.put(foundKey, Prop.getInstance(request).getText("components.multistep_form.verify_code_invalid"));
        }
    }
}
