package sk.iway.iwcm.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Tests compatibility of legacy multistep translation keys when loading property files. */
class IwayPropertiesTest extends BaseWebjetTest {

    private static final String LEGACY_KEY = "components.mustistep.form.back_button";
    private static final String CURRENT_KEY = "components.multistep.form.back_button";

    @TempDir
    Path temporaryDirectory;

    /** Preserves file precedence and customer encoding for plain, domain and template-prefixed keys. */
    @ParameterizedTest
    @ValueSource(strings = { "", "example.com-", "42." })
    void preservesCustomerOverridesAcrossFiles(String prefix) throws IOException {
        IwayProperties properties = new IwayProperties("utf-8");
        properties.load(writeProperties("text-webjet9.properties", prefix + CURRENT_KEY + "=Default\n"));

        String customHtml = "<button class=\"customer-back\">Späť</button>";
        properties.load(writeProperties("text-customer.properties", prefix + LEGACY_KEY + "=" + customHtml + "\n"));

        assertEquals(customHtml, properties.getProperty(prefix + CURRENT_KEY));
        assertFalse(properties.containsKey(prefix + LEGACY_KEY));

        properties.load(writeProperties("text-tenant.properties", prefix + CURRENT_KEY + "=Tenant override\n"));

        assertEquals("Tenant override", properties.getProperty(prefix + CURRENT_KEY));
    }

    /** Uses the last entry when a file contains both spellings of the same translation key. */
    @ParameterizedTest
    @CsvSource({ "mustistep, multistep", "multistep, mustistep" })
    void preservesEntryOrderWithinFile(String firstSpelling, String lastSpelling) throws IOException {
        IwayProperties properties = new IwayProperties();
        properties.load(writeProperties("text-customer.properties",
                "components." + firstSpelling + ".form.back_button=First\n"
                + "components." + lastSpelling + ".form.back_button=Last\n"));

        assertEquals("Last", properties.getProperty(CURRENT_KEY));
        assertEquals(1, properties.size());
    }

    /** Keeps translation values and unrelated property names unchanged. */
    @Test
    void preservesValuesAndUnrelatedKeys() throws IOException {
        IwayProperties properties = new IwayProperties();
        String value = "<span>components.mustistep.form.back_button Späť</span>";
        properties.load(writeProperties("text-customer.properties",
                LEGACY_KEY + "=" + value + "\n"
                + "components.mustistep_extra.label=Unrelated\n"));

        assertEquals(value, properties.getProperty(CURRENT_KEY));
        assertEquals("Unrelated", properties.getProperty("components.mustistep_extra.label"));
    }

    /** Recognizes legacy names after decoding Unicode escapes in a property key. */
    @Test
    void normalizesDecodedKey() throws IOException {
        IwayProperties properties = new IwayProperties();
        properties.load(writeProperties("text-customer.properties",
                "components.musti\\u0073tep.form.back_button=Custom\n"));

        assertEquals("Custom", properties.getProperty(CURRENT_KEY));
    }

    private IwcmFile writeProperties(String fileName, String content) throws IOException {
        Path file = temporaryDirectory.resolve(fileName);
        Files.writeString(file, content, Charset.forName("windows-1250"));
        return new IwcmFile(file);
    }
}
