package sk.iway.iwcm.components.file_archiv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;

/**
 * Verifies that the file archive cache time is declared under the name
 * {@link FileArchivatorDB#getCacheTime(String)} reads, with the default it used before.
 */
class FileArchivatorCacheTimeTest {

    private static final String KEY = "fileArchivCacheTime";

    @AfterEach
    void restoreDefaults() {
        Constants.deleteConstant(KEY + "-getAllFiles");
        Constants.clearValues();
    }

    @Test
    void cacheTimeIsDeclaredUnderTheNameTheArchiveReads() {
        Constants.clearValues();

        assertTrue(Constants.containsKey(KEY));
        assertFalse(Constants.containsKey("fileArchivatorCacheTime"));
        assertEquals(120, FileArchivatorDB.getCacheTime("getAllFiles"), "default keeps the previous effective value");

        Constants.setInt(KEY, 30);
        assertEquals(30, FileArchivatorDB.getCacheTime("getAllFiles"), "the declared option changes the cache time");

        Constants.setInt(KEY + "-getAllFiles", 5);
        assertEquals(5, FileArchivatorDB.getCacheTime("getAllFiles"), "method specific value still wins");
    }
}
