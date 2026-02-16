package sk.iway.iwcm.system.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import sk.iway.iwcm.BirthNumber;
import sk.iway.iwcm.system.annotations.validations.IdentificationNumberConstraint;

public class IdentificationNumberValidator implements ConstraintValidator<IdentificationNumberConstraint, String> {

    @Override
    public void initialize(IdentificationNumberConstraint contactNumber) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext cxt) {
        BirthNumber birthNumber = new BirthNumber(value);
        return birthNumber.isValid();
    }
}