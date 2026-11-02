package sk.iway.iwcm.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;

/** Tests database translation compatibility, missing-key reporting and default-language fallback. */
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

    /** Applies legacy DB overrides before migration without changing values or unrelated keys. */
    @ParameterizedTest
    @ValueSource(strings = { "", "example.com-", "42." })
    void normalizesDatabaseOverridesBeforeCaching(String prefix) {
        String legacyKey = prefix + "components.mustistep.form.back_button";
        String currentKey = prefix + "components.multistep.form.back_button";
        String customValue = "Custom components.mustistep.form.back_button Späť";
        IwayProperties defaults = new IwayProperties();
        defaults.setProperty(currentKey, "File default");

        Prop prop = loadDatabaseTranslations(defaults, List.of(
                Map.entry(" " + legacyKey + " ", customValue),
                Map.entry("components.mustistep_extra.label", "Unrelated")));

        assertEquals(customValue, prop.getProperties().getProperty(currentKey));
        assertFalse(prop.getProperties().containsKey(legacyKey));
        assertEquals("Unrelated", prop.getProperties().getProperty("components.mustistep_extra.label"));
    }

    /** Gives the corrected DB key precedence regardless of update order, matching the migration. */
    @ParameterizedTest
    @CsvSource({ "'', true", "'', false", "example.com-, true", "example.com-, false", "42., true", "42., false" })
    void prefersCorrectedDatabaseKeyWhenBothExist(String prefix, boolean correctedFirst) {
        String currentKey = prefix + "components.multistep.form.back_button";
        Map.Entry<String, String> legacy = Map.entry(prefix + "components.mustistep.form.back_button", "Legacy override");
        Map.Entry<String, String> corrected = Map.entry(currentKey, "Corrected override");
        List<Map.Entry<String, String>> rows = List.of(legacy, corrected);
        if (correctedFirst) rows = List.of(corrected, legacy);
        IwayProperties defaults = new IwayProperties();
        defaults.setProperty(currentKey, "File default");

        Prop prop = loadDatabaseTranslations(defaults, rows);

        assertEquals("Corrected override", prop.getProperties().getProperty(currentKey));
    }

    /** Loads through the real language cache path while substituting only file defaults and DB rows. */
    @SuppressWarnings("unchecked")
    private Prop loadDatabaseTranslations(IwayProperties defaults, List<Map.Entry<String, String>> rows) {
        MockServletContext context = new MockServletContext();
        try (var props = mockStatic(Prop.class, CALLS_REAL_METHODS);
                var startup = mockStatic(InitServlet.class);
                var queries = mockConstruction(ComplexQuery.class, withSettings().defaultAnswer(RETURNS_SELF), (query, construction) -> {
                    doAnswer(call -> {
                        Mapper<Void> mapper = call.getArgument(0);
                        for (Map.Entry<String, String> row : rows) {
                            ResultSet result = mock(ResultSet.class);
                            when(result.getString("prop_key")).thenReturn(row.getKey());
                            when(result.getString("prop_value")).thenReturn(row.getValue());
                            mapper.map(result);
                        }
                        return List.of();
                    }).when(query).list(any(Mapper.class));
                })) {
            props.when(() -> Prop.getDefaulProperties("en", "_en", context)).thenReturn(defaults);
            startup.when(InitServlet::isWebjetConfigured).thenReturn(true);

            Prop prop = Prop.getInstance(context, "en", false);

            assertSame(prop, Prop.getInstance(context, "en", false));
            assertEquals(1, queries.constructed().size());
            verify(queries.constructed().get(0)).setParams("en");
            return prop;
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
