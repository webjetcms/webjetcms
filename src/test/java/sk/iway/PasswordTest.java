package sk.iway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
