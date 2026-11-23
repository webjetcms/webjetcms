package sk.iway.iwcm.components.file_archiv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.system.ConfDetails;

/**
 * Verifies that the file archive cache time is declared under the name
 * {@link FileArchivatorDB#getCacheTime(String)} reads, with the default it used before.
 */
@Isolated
class FileArchivatorCacheTimeTest {

    private static final String KEY = "fileArchivCacheTime";
    private static final String METHOD = "getByReferenceId-";
    private static final String METHOD_KEY = KEY + "-" + METHOD;

    @Test
    void cacheTimeIsDeclaredUnderTheNameTheArchiveReads() {
        var declarations = Constants.getAllValues();
        ConfDetails cacheTime = declarations.stream()
            .filter(conf -> KEY.equals(conf.getName()))
            .findFirst().orElseThrow(() -> new AssertionError("Missing declaration for " + KEY));
        assertEquals("120", cacheTime.getValue());
        assertFalse(declarations.stream().anyMatch(conf -> "fileArchivatorCacheTime".equals(conf.getName())));
    }

    @Test
    void methodCacheTimeOverridesTheGlobalValue() {
        // The public getters convert values. Preserve the original types as well as absent keys.
        @SuppressWarnings("unchecked")
        Map<String, Object> values = (Map<String, Object>) ReflectionTestUtils.getField(Constants.class, "constantsMap");
        Object previousGlobal = values.get(KEY);
        Object previousMethod = values.get(METHOD_KEY);
        try {
            Constants.deleteConstant(METHOD_KEY);
            Constants.deleteConstant(KEY);
            assertEquals(120, FileArchivatorDB.getCacheTime(METHOD), "default keeps the previous effective value");

            Constants.setInt(KEY, 30);
            assertEquals(30, FileArchivatorDB.getCacheTime(METHOD), "the declared option changes the cache time");

            Constants.setInt(METHOD_KEY, 5);
            assertEquals(5, FileArchivatorDB.getCacheTime(METHOD), "method specific value still wins");
        } finally {
            values.compute(KEY, (key, current) -> previousGlobal);
            values.compute(METHOD_KEY, (key, current) -> previousMethod);
        }
    }
}
