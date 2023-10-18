package sk.iway.iwcm.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.form.validators.PhoneValidator;

public class PhoneValidatorTest extends BaseWebjetTest {

    @BeforeAll
    public static void init() {
        //Constants.setString("jpaAddPackages", "sk.iway.iwcm.components.gdpr.model");
        DBPool.getInstance();
        DBPool.jpaInitialize();
    }

    @Test
    public void validateTest() {
        PhoneValidator validator = PhoneValidator.getInstance();
        validator.onlyMobilePhone = false;

        List<String> classes = new ArrayList<>();
        classes.add("phone");
        // SK
        assertTrue(validator.isValid(classes,"+421903759841"));
        assertTrue(validator.isValid(classes,"0903759841"));

        // CZ
        assertTrue(validator.isValid(classes,"00420903759841"));
        assertTrue(validator.isValid(classes,"+420903759841"));

        // Google US
        assertTrue(validator.isValid(classes,"0019254056495"));
        assertTrue(validator.isValid(classes,"+19254056495"));

        // Orange FR
        assertTrue(validator.isValid(classes,"0033969363900"));
        assertTrue(validator.isValid(classes,"+33969363900"));

        // Telekom DE
        assertTrue(validator.isValid(classes,"004922818188899"));
        assertTrue(validator.isValid(classes,"+4922818188899"));

        // Gazprom RU
        assertTrue(validator.isValid(classes,"0074957193001"));
        assertTrue(validator.isValid(classes,"+74957193001"));


        // zla predvolba, moze byt len + a 00, alebo pri +421 moze zacinat na 0
        //assertFalse(validator.isValid(classes,"0174957193001"));
        // prilis dlhe
        //assertFalse(validator.isValid(classes,"+00174957193001"));

        //assertFalse(validator.isValid(classes,"1903759841"));
        /*assertFalse(validator.isValid(classes,"1"));
        assertFalse(validator.isValid(classes,"12"));
        assertFalse(validator.isValid(classes,"123"));
        assertFalse(validator.isValid(classes,"1234"));
        assertFalse(validator.isValid(classes,"12345"));
        assertFalse(validator.isValid(classes,"123456"));
        assertFalse(validator.isValid(classes,"1234567"));
        assertFalse(validator.isValid(classes,"12345678"));
        assertFalse(validator.isValid(classes,"123456789"));*/

        validator.addPhoneCode("+421");

        // SK
        assertTrue(validator.isValid(null,"00421903759841"));
        assertTrue(validator.isValid(null,"+421903759841"));
        assertTrue(validator.isValid(null,"0903759841"));

        // CZ
        assertFalse(validator.isValid(null,"00420903759841"));
        assertFalse(validator.isValid(null,"+420903759841"));

        // Google US
        assertFalse(validator.isValid(null,"0019254056495"));
        assertFalse(validator.isValid(null,"+19254056495"));

        validator.addPhoneCode("+420");

        // SK
        assertTrue(validator.isValid(null,"00421903759841"));
        assertTrue(validator.isValid(null,"+421903759841"));
        assertTrue(validator.isValid(null,"0903759841"));

        // CZ
        assertTrue(validator.isValid(null,"00420903759841"));
        assertTrue(validator.isValid(null,"+420903759841"));

        // Google US
        assertFalse(validator.isValid(null,"0019254056495"));
        assertFalse(validator.isValid(null,"+19254056495"));
    }
}
