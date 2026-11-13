package sk.iway.iwcm.system.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

/** Covers failure propagation and compatibility of translation callers without an external provider. */
class TranslationServiceFailureTest extends BaseWebjetTest {
    private final TranslationEngine engine = mock(TranslationEngine.class, CALLS_REAL_METHODS);

    private TranslationService translator() {
        TranslationService translator = mock(TranslationService.class, CALLS_REAL_METHODS);
        translator.setFromLanguage("sk");
        translator.setToLanguage("en");
        translator.setTranslationEngine(engine);
        return translator;
    }

    /** Mirroring can detect provider failures while legacy callers retain the original fallback. */
    @Test
    void exposesFailureWithoutBreakingLegacyFallback() {
        when(engine.translate(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("Fixture provider unavailable"));
        TranslationService translator = translator();

        assertThrows(TranslationException.class, () -> translator.translateOrThrow("Slovenský text"));
        assertEquals("Slovenský text", translator.translate("Slovenský text"));
    }

    /** Empty provider responses must not erase a translated page. */
    @Test
    void rejectsEmptyTranslation() {
        when(engine.translate(anyString(), anyString(), anyString())).thenReturn("");
        assertThrows(TranslationException.class, () -> translator().translateOrThrow("Slovenský text"));
    }

    /** An unchanged brand name is a valid successful translation, not a failure signal. */
    @Test
    void acceptsUnchangedTranslation() {
        when(engine.translate("Orange", "sk", "en")).thenReturn("Orange");
        assertEquals("Orange", translator().translateOrThrow("Orange"));
    }

    /** Include expressions containing replacement metacharacters survive a successful translation unchanged. */
    @Test
    void preservesIncludeExpressions() {
        String include = "!INCLUDE(test.jsp, value=$5, path=C:\\images)!";
        when(engine.translate("Text __INCLUDE_PLACEHOLDER_1__", "sk", "en"))
            .thenReturn("Translated __INCLUDE_PLACEHOLDER_1__");
        assertEquals("Translated " + include, translator().translateOrThrow("Text " + include));
    }

    /** Provider errors during include translation must not trigger another billable request. */
    @Test
    void doesNotRetryFailedIncludeTranslation() {
        when(engine.translate(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("Fixture quota exhausted"));
        assertThrows(TranslationException.class, () -> translator().translateOrThrow("Text !INCLUDE(test.jsp)!"));
        verify(engine, times(1)).translate(anyString(), anyString(), anyString());
    }

    /** Ordinary mirroring still copies content when no translation provider is configured. */
    @Test
    void preservesCopyWithoutConfiguredProvider() {
        TranslationService translator = translator();
        translator.setTranslationEngine(null);
        assertEquals("Slovenský text", translator.translateOrThrow("Slovenský text"));
    }
}
