package sk.iway.iwcm.system.validators;

import java.util.Set;
import java.util.HashSet;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.dmail.DmailUtil;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.validations.MultipleEmails;

/**
 * Validator for email strings. It checks if the string contains valid email addresses.
 *
 * param checkUnsubscribed - if true, it checks if the email is not in the list of unsubscribed emails
 * param checkDuplicity - if true, it checks if the email is not in the list of emails already in the string
 * param canByEmpty - if true, the string can be empty
 *
 * Example of usage:
 *  ",,," - it's NOT valid, there is not a single good email
 *  "a@b.sk,,,," - it's valid, there is one GOOD email (redundant "," are ignored)
 *  "a@b.sk,,asd189/,," - it's NOT valid, there is BAD email (soo whole string is invalid)
 *
 * Duplicity and unsubscribed emails are checked only if the email is valid and we want it.
 */
public class MultipleEmailsValidator implements ConstraintValidator<MultipleEmails, String> {

    private boolean checkUnsubscribed;
    private boolean checkDuplicity;
    private boolean canByEmpty;

    @Override
    public void initialize(MultipleEmails parameters) {
        checkUnsubscribed = parameters.checkUnsubscribed();
        checkDuplicity = parameters.checkDuplicity();
        canByEmpty = parameters.canByEmpty();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext cxt) {
        //Empty validation
        if(Tools.isEmpty(value)) {
            if(canByEmpty == true) return true;
            else {
                return throwError(cxt, "email_validation.error.empty");
            }
        }

        Set<String> unsubscribedEmails = null;
        Set<String> emailsTable = new HashSet<>();

        String[] emails = Tools.getTokens(value, ",", true);
        for(String email : emails) {

            //Get rid of white-spaces (for safety reason)
            email = email.replaceAll("\\s+","");

            if(Tools.isEmpty(email) == true) continue;

            if(checkUnsubscribed) {
                if (unsubscribedEmails == null) unsubscribedEmails = DmailUtil.getUnsubscribedEmails();
                //Protection against unsubscribed email addresses
                if(unsubscribedEmails.contains(email.toLowerCase())) continue;
            }

            //All emails must be valid
            if(Tools.isEmail(email) == false) return throwError(cxt, "email_validation.error.invalid", email);
            else {

                if(checkDuplicity == true) {
                    //Protection against duplicity (email is already in the table)
                    if(emailsTable.contains(email)) {
                        return throwError(cxt, "email_validation.error.duplicity");
                    }
                }

                emailsTable.add(email);
            }
        }

        //String must contain at least one good email
        if(emailsTable.size() < 1) return throwError(cxt, "email_validation.error.no_email");
        else return true;
    }

    private boolean throwError(ConstraintValidatorContext cxt, String key) {
        return throwError(cxt, key, null);
    }

    private boolean throwError(ConstraintValidatorContext cxt, String key, String wrongValue) {
        String errMessage;
        if(Tools.isEmpty(wrongValue) == false) {
            errMessage = Prop.getInstance().getText(key, wrongValue);
        } else {
            errMessage = Prop.getInstance().getText(key);
        }

        cxt.disableDefaultConstraintViolation();
        cxt
        .buildConstraintViolationWithTemplate(errMessage)
        .addConstraintViolation();

        return false;
    }
}
