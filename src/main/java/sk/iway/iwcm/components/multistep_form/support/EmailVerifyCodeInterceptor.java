package sk.iway.iwcm.components.multistep_form.support;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.multistep_form.rest.FormMailService;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.i18n.Prop;

public class EmailVerifyCodeInterceptor implements StepInterceptorInterface {

    public static final String SESSION_VERIFY_CODE_KEY = "MULTISTEP_FORM_EMAIL_VERIFY_CODE";
    public static final String SESSION_VERIFY_CODE_ATTEMPTS_KEY = "MULTISTEP_FORM_EMAIL_VERIFY_CODE_ATTEMPTS";

    @Override
    public void runInterceptor(String formName, Long currentStepId, JSONObject currentReceived, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        //Try get email from current json
        List<String> emailFieldsNames = Arrays.stream( Constants.getArray(FormMailService.EMAIL_FIELD_KEY) ).map(s -> s.toLowerCase()).toList();

        String email = null;
        for(String key : currentReceived.keySet()) {
            String originalKey = null;
            originalKey = new String(key);
            //remove key postfix
            key = key.replaceFirst("-\\d+$", "");
            if(emailFieldsNames.contains(key.toLowerCase())) {
                email = currentReceived.getString(originalKey);
                if(Tools.isEmail(email)) break;
            }
        }

        // If email is still empty, try find email in session
        String prefix = MultistepFormsService.getSessionKey(formName, request) + "_";
        if(Tools.isEmail(email) == false) {
            @SuppressWarnings("unchecked")
            Enumeration<String> e = request.getSession().getAttributeNames();
            while(e.hasMoreElements()) {
                String originalKey = e.nextElement();
                String key = new String(originalKey);
                // Remove prefix
                key = key.replaceFirst(prefix, "");
                // Remove key postfix
                key = key.replaceFirst("-\\d+$", "");
                if(emailFieldsNames.contains(key.toLowerCase())) {
                    email = (String) request.getSession().getAttribute(originalKey);
                    if(Tools.isEmail(email)) break;
                }
            }
        }

        if(Tools.isEmail(email) == false) {
            throw new SaveFormException("E-mail address not found or is invalid.", false, null);
        }

        String verifyCode = Password.generatePassword(5);
        request.getSession().setAttribute(prefix + SESSION_VERIFY_CODE_KEY, verifyCode);
        request.getSession().setAttribute(prefix + SESSION_VERIFY_CODE_ATTEMPTS_KEY, 0);

        try {
            Prop prop = Prop.getInstance(request);
            String senderName = SendMail.getDefaultSenderName("reservation", Constants.getString("formmailSendUserInfoSenderName"));
		    String senderEmail = SendMail.getDefaultSenderEmail("reservation", Constants.getString("formmailSendUserInfoSenderEmail"));

            String body = prop.getText("components.multistep_form.verify_code.email_body");
            body = Tools.replace(body, "${FORM_NAME}", formName);
            body = Tools.replace(body, "${VERIFY_CODE}", verifyCode);

            SendMail.send(senderName, senderEmail, email, null, null, null, prop.getText("components.multistep_form.verify_code.subject", formName), body, null, null);
        } catch (Exception e) {
            throw new SaveFormException("Failed to send verify code e-mail.", false, null);
        }
    }
}