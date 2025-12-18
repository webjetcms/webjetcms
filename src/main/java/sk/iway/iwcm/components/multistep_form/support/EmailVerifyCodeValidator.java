package sk.iway.iwcm.components.multistep_form.support;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.i18n.Prop;

public class EmailVerifyCodeValidator implements StepValidatorInterface {

    private static final Integer MAX_VERIFY_ATTEMPTS = 3;

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

        String sessionKey = MultistepFormsService.getSessionKey(formName, request);
        String sessionVerifyCode = (String) request.getSession().getAttribute(sessionKey + "_" + EmailVerifyCodeInterceptor.SESSION_VERIFY_CODE_KEY);
        int attempCount = (Integer) request.getSession().getAttribute(sessionKey + "_" + EmailVerifyCodeInterceptor.SESSION_VERIFY_CODE_ATTEMPTS_KEY);
        attempCount++;

        if(Tools.isEmpty(verifyCode) || Tools.isEmpty(sessionVerifyCode) || verifyCode.equals(sessionVerifyCode) == false) {
            // BAD CODE
            if(attempCount >= MAX_VERIFY_ATTEMPTS) {
                // Exceeded max attempts, invalidate code -> interrupt form saving and redirect to error page
                request.getSession().removeAttribute(sessionKey + "_" + EmailVerifyCodeInterceptor.SESSION_VERIFY_CODE_KEY);
                request.getSession().removeAttribute(sessionKey + "_" + EmailVerifyCodeInterceptor.SESSION_VERIFY_CODE_ATTEMPTS_KEY);

                throw new SaveFormException(Prop.getInstance(request).getText("components.multistep_form.verify_code_max_attempts"), true, null);
            } else {
                // Update attempts count
                request.getSession().setAttribute(sessionKey + "_" + EmailVerifyCodeInterceptor.SESSION_VERIFY_CODE_ATTEMPTS_KEY, attempCount);
                errors.put(foundKey, Prop.getInstance(request).getText("components.multistep_form.verify_code_invalid"));
            }
        }
    }
}