package sk.iway.iwcm.components.abtesting;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;

import static org.junit.jupiter.api.Assertions.*;

class ABTestingTest {

    @Test
    void testGenerateVariant() {
        // Test case 1: Single ratio
        Constants.setString("ABTestingRatio", "100");
        String variant1 = ABTesting.generateVariant();
        assertEquals("a", variant1);

        // Test case 2: Multiple ratios
        Constants.setString("ABTestingRatio", "50:300:20");
        String variant2 = ABTesting.generateVariant();
        assertTrue(variant2.matches("[a-c]"));

        // Test case 3: Exception handling
        Constants.setString("ABTestingRatio", "invalid_ratio");
        String variant3 = ABTesting.generateVariant();
        assertEquals("a", variant3);

        //repeat the test 100 times and verify there are a and also b variants
        Constants.setString("ABTestingRatio", "50:300");
        boolean a = false;
        boolean b = false;
        for (int i = 0; i < 50; i++) {
            String variant = ABTesting.generateVariant();
            if (variant.equals("a")) {
                a = true;
            } else if (variant.equals("b")) {
                b = true;
            }
        }
        assertTrue(a);
        assertTrue(b);

    }
}