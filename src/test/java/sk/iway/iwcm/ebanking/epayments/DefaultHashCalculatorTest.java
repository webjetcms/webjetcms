package sk.iway.iwcm.ebanking.epayments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultHashCalculatorTest {
    @Test
    void testCalculateHash() throws Exception {
        String toBeEncrypted = "Hello, World!";
        String privateKey = "0E329232";
        String expectedHash = "DF523EEA1F9C37C8";

        String actualHash = DefaultHashCalculator.calculateHash(toBeEncrypted, privateKey);

        assertEquals(expectedHash, actualHash);
    }
}