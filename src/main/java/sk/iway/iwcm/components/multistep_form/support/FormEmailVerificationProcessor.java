package sk.iway.iwcm.components.multistep_form.support;

import java.io.IOException;
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
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.rest.FormMailService;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.i18n.Prop;

/**
 * Processor that handles email verification for multi-step forms.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>After step 1, generates a verification code and sends it to the user's email.</li>
 *   <li>During step 2, validates the code provided by the user and manages attempt limits.</li>
 * </ul>
 * </p>
 *
 * Session keys are namespaced per form to avoid collisions between multiple forms
 * processed in the same session.
 */
public class FormEmailVerificationProcessor implements FormProcessorInterface {

    public static final String SESSION_VERIFY_CODE_KEY = "MULTISTEP_FORM_EMAIL_VERIFY_CODE";
    public static final String SESSION_VERIFY_CODE_ATTEMPTS_KEY = "MULTISTEP_FORM_EMAIL_VERIFY_CODE_ATTEMPTS";
    private static final Integer MAX_VERIFY_ATTEMPTS = 3;

    @Override
    /**
     * Intercepts form processing to trigger email verification after the first step.
     *
     * @param formName the unique form name
     * @param currentStepId the ID of the current step being processed
     * @param stepData JSON payload containing fields submitted in the current step
     * @param request current HTTP request
     * @param errors map to collect validation errors (not used in this step)
     * @throws SaveFormException if email cannot be determined or email sending fails
     */
    public void runStepInterceptor(String formName, FormStepEntity stepEntity, JSONObject stepData, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        if(stepEntity == null) throw new IllegalStateException("FormStepEntity was not provided");

        if(stepEntity.getCurrentPosition() == 1)
            // We want to send verification email at end of first step
            verifyEmailInterceptor(formName, stepData, request);
    }

    @Override
    /**
     * Validates the email verification code during the second step.
     *
     * <p>If the code is invalid, the error is placed into the {@code errors}
     * map for the corresponding field. If the maximum number of attempts is
     * exceeded, a {@link SaveFormException} is thrown to interrupt the
     * processing and redirect to an error page.</p>
     *
     * @param formName the unique form name
     * @param currentStepId the ID of the current step being validated
     * @param stepData JSON payload containing fields submitted in the current step
     * @param request current HTTP request
     * @param errors map to collect validation errors when the code is invalid
     * @throws SaveFormException when verification code attempts exceed the allowed maximum
     */
    public void validateStep(String formName, FormStepEntity stepEntity, JSONObject stepData, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
        if(stepEntity == null) throw new IllegalStateException("FormStepEntity was not provided");

        if(stepEntity.getCurrentPosition() == 2)
            // We want to validate email code in step two
            emaiCodeValidation(formName, stepData, request, errors);
    }

    @Override
    public boolean handleFormSave(String formName, FormSettingsEntity formSettings, Integer iLastDocId, HttpServletRequest request) throws SaveFormException, IOException {
        // We do not want custom save
        // Return TRUE - save basic save will run
        return true;
    }

    /**
     * Extracts the user's email from the current step or session and sends a
     * verification code to that address. Stores the code and attempt counter in
     * the session under a form-specific namespace.
     *
     * @param formName the unique form name
     * @param currentReceived JSON payload from the current step containing submitted fields
     * @param request current HTTP request
     * @throws SaveFormException when no valid email is found or sending the email fails
     */
    private void verifyEmailInterceptor(String formName, JSONObject currentReceived, HttpServletRequest request) throws SaveFormException {
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

        if(Tools.isEmail(email) == false)
            throw new SaveFormException(Prop.getInstance(request).getText("form_email_verification_processor.no_valid_email_found"), false, null);

        String verifyCode = Password.generatePassword(5);
        request.getSession().setAttribute(prefix + SESSION_VERIFY_CODE_KEY, verifyCode);
        request.getSession().setAttribute(prefix + SESSION_VERIFY_CODE_ATTEMPTS_KEY, 0);

        try {
            Prop prop = Prop.getInstance(request);
            String senderName = SendMail.getDefaultSenderName("reservation", Constants.getString("formmailSendUserInfoSenderName"));
		    String senderEmail = SendMail.getDefaultSenderEmail("reservation", Constants.getString("formmailSendUserInfoSenderEmail"));

            String body = prop.getText("form_email_verification_processor.email_body");
            body = Tools.replace(body, "${FORM_NAME}", formName);
            body = Tools.replace(body, "${VERIFY_CODE}", verifyCode);

            SendMail.send(senderName, senderEmail, email, null, null, null, prop.getText("form_email_verification_processor.verify_code.subject", formName), body, null, null);
        } catch (Exception e) {
            throw new SaveFormException(Prop.getInstance(request).getText("form_email_verification_processor.sending_code_failed"), false, null);
        }
    }

    @SuppressWarnings("null")
    /**
     * Validates the verification code provided by the user. Increments the
     * attempt counter and enforces a maximum number of attempts.
     *
     * @param formName the unique form name
     * @param currentReceived JSON payload from the current step containing submitted fields
     * @param request current HTTP request
     * @param errors map to collect validation errors when the code is invalid
     * @throws SaveFormException when max attempts are exceeded, causing processing to stop
     */
    private void emaiCodeValidation(String formName, JSONObject currentReceived, HttpServletRequest request, Map<String, String> errors) throws SaveFormException {
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
        String sessionVerifyCode = (String) request.getSession().getAttribute(sessionKey + "_" + SESSION_VERIFY_CODE_KEY);
        int attempCount = (Integer) request.getSession().getAttribute(sessionKey + "_" + SESSION_VERIFY_CODE_ATTEMPTS_KEY);
        attempCount++;

        if(Tools.isEmpty(verifyCode) || Tools.isEmpty(sessionVerifyCode) || verifyCode.equals(sessionVerifyCode) == false) {
            // BAD CODE
            if(attempCount >= MAX_VERIFY_ATTEMPTS) {
                // Exceeded max attempts, invalidate code -> interrupt form saving and redirect to error page
                request.getSession().removeAttribute(sessionKey + "_" + SESSION_VERIFY_CODE_KEY);
                request.getSession().removeAttribute(sessionKey + "_" + SESSION_VERIFY_CODE_ATTEMPTS_KEY);

                throw new SaveFormException(Prop.getInstance(request).getText("form_email_verification_processor.verify_code_max_attempts"), true, null);
            } else {
                // Update attempts count
                request.getSession().setAttribute(sessionKey + "_" + SESSION_VERIFY_CODE_ATTEMPTS_KEY, attempCount);
                errors.put(foundKey, Prop.getInstance(request).getText("form_email_verification_processor.verify_code_invalid"));
            }
        }
    }
}
