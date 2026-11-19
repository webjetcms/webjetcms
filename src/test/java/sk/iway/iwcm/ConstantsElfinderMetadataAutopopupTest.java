package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the elfinder metadata autopopup option is declared under the name
 * {@code ElfinderListener} reads, so changing it in the administration takes effect.
 */
class ConstantsElfinderMetadataAutopopupTest {

    private static final String KEY = "elfinderMetadataAutopopup";

    @AfterEach
    void restoreDefaults() {
        Constants.clearValues();
    }

    @Test
    void autopopupIsDeclaredUnderTheNameTheListenerReads() {
        Constants.clearValues();

        assertTrue(Constants.containsKey(KEY));
        assertFalse(Constants.containsKey("elfinerMetadataAutopopup"));
        assertFalse(Constants.getBoolean(KEY), "default stays false");
        assertEquals(1, Constants.getAllValues().stream().filter(conf -> KEY.equals(conf.getName())).count(),
                "the option is listed in the administration");

        Constants.setBoolean(KEY, true);
        assertTrue(Constants.getBoolean(KEY));
    }
}
