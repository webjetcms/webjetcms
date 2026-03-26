package sk.iway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class PasswordTest {

    String[] textArr = {"heslo",                               "03101969",                          "ucitel",                           "M@gAZloyiT3;Hes!o"};
    String[] hashArr = {"d7ed8dc6fc9b4a8c3b442c3dcc35bfe4",    "437b9363e304a0a890fa46932bc510f6",  "447144233c3cb432b945dd460f812676", "0b8e3ba673324ac9301622d92487063201e444e264b84372cbf20c600b82f181"};

    @Test
    void testDecrypt() {
        for (int i=0; i<textArr.length; i++) {
            String text = textArr[i];
            String hash = hashArr[i];
            try {
                Password password = new Password();
                String decryptedPassword = password.decrypt(hash);
                assertNotNull(decryptedPassword);
                assertFalse(decryptedPassword.isEmpty());
                assertEquals(text, decryptedPassword);
            } catch (Exception e) {
                fail("An exception occurred: " + e.getMessage());
            }
        }
    }

    @Test
    void testEncrypt() {
        for (int i=0; i<textArr.length; i++) {
            String text = textArr[i];
            String hash = hashArr[i];
            try {
                Password password = new Password();
                String encryptedPassword = password.encrypt(text);
                assertNotNull(encryptedPassword);
                assertFalse(encryptedPassword.isEmpty());
                assertEquals(hash, encryptedPassword);
            } catch (Exception e) {
                fail("An exception occurred: " + e.getMessage());
            }
        }
    }

    @Test
    void testGenerateStrongPassword() {
        String password = Password.generateStrongPassword();

        assertNotNull(password);
        assertFalse(password.isEmpty());

        // Check format: XXXXXX-XXXXXX-XXXXXX (3 parts separated by dash)
        String[] parts = password.split("-");
        assertEquals(3, parts.length);
        assertEquals(6, parts[0].length());
        assertEquals(6, parts[1].length());
        assertEquals(6, parts[2].length());

        // Check total length (18 chars + 2 dashes)
        assertEquals(20, password.length());
    }

    @Test
    void testGenerateStrongPasswordContainsRequiredCharTypes() {
        String password = Password.generateStrongPassword();

        // Remove dashes for character analysis
        String passwordWithoutDashes = password.replace("-", "");

        boolean hasDigit = false;
        boolean hasLowerCase = false;
        boolean hasUpperCase = false;
        boolean hasSpecialChar = false;

        for (char c : passwordWithoutDashes.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else {
                hasSpecialChar = true;
            }
        }

        assertTrue(hasDigit, "Password should contain at least one digit");
        assertTrue(hasLowerCase, "Password should contain at least one lowercase letter");
        assertTrue(hasUpperCase, "Password should contain at least one uppercase letter");
        assertTrue(hasSpecialChar, "Password should contain at least one special character");
    }

    @Test
    void testGenerateStrongPasswordNoConfusableChars() {
        String password = Password.generateStrongPassword();

        assertFalse(password.contains("o"), "Password should not contain letter 'o' (can be confused with 0)");
        assertFalse(password.contains("O"), "Password should not contain letter 'O' (can be confused with 0)");
        assertFalse(password.contains("l"), "Password should not contain letter 'l' (can be confused with 1)");
        assertFalse(password.contains("L"), "Password should not contain letter 'L' (can be confused with 1)");
    }

    @Test
    void testGenerateStrongPasswordMultipleCallsProduceDifferentPasswords() {
        String password1 = Password.generateStrongPassword();
        String password2 = Password.generateStrongPassword();

        assertNotEquals(password1, password2, "Multiple calls should produce different passwords");
    }
}
