package sk.iway.iwcm.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SetCharacterEncodingFilter;

/** Tests independent missing-key reporting and default-language fallback. */
class PropTest {

    /** Verifies all combinations of reporting and fallback without coupling the two flags. */
    @ParameterizedTest
    @CsvSource({ "true, true", "true, false", "false, true", "false, false" })
    void controlsReportingAndFallbackIndependently(boolean checkMissing, boolean tryDefaultLanguage) {
        String key = "regression.prop.independent-flags";
        Prop prop = createProp();
        prop.getRes("de").setProperty(key, "Default-language translation");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("de");

            String expected = key;
            if (tryDefaultLanguage) expected = "Default-language translation";
            assertEquals(expected, prop.getText(key, checkMissing, tryDefaultLanguage));
            assertEquals(checkMissing, isMissingKeyRecorded(key),
                    "Missing-key reporting must depend only on checkMissing.");
        } finally {
            removeMissingKey(key);
        }
    }

    /** Verifies that the existing two-argument overload still enables or disables both behaviors together. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void preservesTwoArgumentBehavior(boolean checkMissing) {
        String key = "regression.prop.legacy-flags";
        Prop prop = createProp();
        prop.getRes("de").setProperty(key, "Default-language translation");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("de");

            String expected = key;
            if (checkMissing) expected = "Default-language translation";
            assertEquals(expected, prop.getText(key, checkMissing));
            assertEquals(checkMissing, isMissingKeyRecorded(key));
        } finally {
            removeMissingKey(key);
        }
    }

    /** Verifies configured-language precedence and Slovak fallback without registering optional keys. */
    @ParameterizedTest
    @CsvSource({ "de, de", "de, sk", "sk, sk" })
    void preservesLanguageFallbackOrder(String defaultLanguage, String availableLanguage) {
        String key = "regression.prop.language-order";
        Prop prop = createProp();
        prop.getRes("sk").setProperty(key, "Slovak translation");
        if ("de".equals(availableLanguage)) {
            prop.getRes("de").setProperty(key, "Configured-language translation");
        }

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn(defaultLanguage);

            String expected = "Slovak translation";
            if ("de".equals(availableLanguage)) expected = "Configured-language translation";
            assertEquals(expected, prop.getText(key, false, true));
            assertEquals(false, isMissingKeyRecorded(key));
        } finally {
            removeMissingKey(key);
        }
    }

    private Prop createProp() {
        Prop prop = mock(Prop.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(prop, "properties", new IwayProperties());
        ReflectionTestUtils.setField(prop, "language", "en");
        doReturn(new IwayProperties()).when(prop).getRes("de");
        doReturn(new IwayProperties()).when(prop).getRes("sk");
        return prop;
    }

    private boolean isMissingKeyRecorded(String key) {
        return Prop.getMissingTexts("en") != null
                && Prop.getMissingTexts("en").stream().anyMatch(text -> text.getKey().equals(key));
    }

    private void removeMissingKey(String key) {
        if (Prop.getMissingTexts("en") != null) {
            Prop.getMissingTexts("en").removeIf(text -> text.getKey().equals(key));
        }
    }
}
