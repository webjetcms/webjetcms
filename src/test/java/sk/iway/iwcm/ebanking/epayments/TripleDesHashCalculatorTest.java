package sk.iway.iwcm.ebanking.epayments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;

class TripleDesHashCalculatorTest {
    @Test
    void testCalculateHash() {
        String stringToEncrypt = "Hello, World!";
        String privateKey = "30b13309ca691de1";
        String expectedHash = "FD2/IsDHN0owI5H/GpR+OF8X5+NFcz7L";

        String actualHash = TripleDesHashCalculator.calculateHash(stringToEncrypt, Base64.getEncoder().encodeToString(privateKey.getBytes()));

        assertEquals(expectedHash, actualHash);
    }
}