package sk.iway.iwcm.system.annotations.validations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import sk.iway.iwcm.system.validators.IdentificationNumberValidator;

@Constraint(validatedBy = IdentificationNumberValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentificationNumberConstraint {
    String message() default "Invalid identification number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
