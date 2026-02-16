package sk.iway.iwcm.system.annotations.validations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import sk.iway.iwcm.system.validators.MultipleEmailsValidator;

/**
 * Annotation for validation of emails string. String is parsed by "," and every value is checked using Tools.isEmail method.
 * If only one email is invalid, validation fails for whole string.
 *
 * @author sivan
 *
 */
@Constraint(validatedBy = MultipleEmailsValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MultipleEmails {
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /** */
    String message() default "Invalid text of emails";

    /**
     * If true, then emails are checked against unsubscribed emails (Dmail).
     * Default is false.
     * @return
     */
    boolean checkUnsubscribed() default false;

    /**
     * If true, then emails are checked against duplicity.
     * Default is true.
     * @return
     */
    boolean checkDuplicity() default true;

    /**
     * If true, then string can be empty.
     * Default is false.
     * @return
     */
    boolean canByEmpty() default false;
}