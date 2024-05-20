package sk.iway.iwcm.users.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Sha512Test {

    String password = "password";
    String salt = "1234567890abcdef";
    String expectedHash = "7685b2432d2310c906cef0d9ed844874ea03505ad26be5354326780f72c706de5212cb633000477f87eb2c28581a52215c11e902e539e302e409910fe2161241";

    @Test
    void testCalculateHash() {
        Sha512 sha512 = new Sha512();

        String actualHash = sha512.calculateHash(password, salt);

        assertEquals(expectedHash, actualHash);
    }

    @Test
    void testIsPasswordCorrect() {
        Sha512 sha512 = new Sha512();
        assertTrue(sha512.isPasswordCorrect(password, salt, expectedHash));
    }

    @Test
    void testGenerateSalt() {
        Sha512 sha512 = new Sha512();
        String salt = sha512.generateSalt();

        assertNotNull(salt);
        assertEquals(16, salt.length());
        assertTrue(salt.matches("[0-9a-f]+"));
    }

}