package sk.iway.iwcm.system.translation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Tests DeepL translation while preserving embedded WebJET include expressions.
 */
class TestTranslationService extends BaseWebjetTest {

    @BeforeAll
    public static void initJpa()
    {
        Constants.setString("deepl_auth_key", System.getenv("DEEPL_AUTH_KEY"));
        DBPool.getInstance();
        DBPool.jpaInitialize();
    }

    /**
     * Verifies that Slovak text is translated to an accepted English variant without altering includes.
     */
    @Test
    public void checkDeeplTranslation() {
        String inputTextSk = "Toto je test: !INCLUDE(pes.jsp, ja, som, najlepší)!, tento test je povinný. Aj tento by mal fungovať: !INCLUDE(neviem.jsp, skúsim, to, znovu)!, koniec testu.";
        String[] requiredOutputsEn = {
            "This is a test: !INCLUDE(pes.jsp, ja, som, najlepší)!, this test is mandatory. This one should also work: !INCLUDE(neviem.jsp, skúsim, to, znovu)!, end of test.",
            "This is a test: !INCLUDE(pes.jsp, ja, som, najlepší)!, this test is mandatory. This one should work too: !INCLUDE(neviem.jsp, skúsim, to, znovu)!, end of test.",
            "This is a test: !INCLUDE(pes.jsp, ja, som, najlepší)!; this test is required. This one should work too: !INCLUDE(neviem.jsp, skúsim, to, znovu)!; end of test.",
            "This is a test: !INCLUDE(pes.jsp, ja, som, najlepší)!; this test is required. This one should work as well: !INCLUDE(neviem.jsp, skúsim, to, znovu)!; end of test.",
            "This is a test: !INCLUDE(pes.jsp, ja, som, najlepší)!; this test is required. This one should work, too: !INCLUDE(neviem.jsp, skúsim, to, znovu)!; end of test."
        };

        // Init Translate from SK to EN
        TranslationService translator = new TranslationService("SK", "EN");
        String realOutputEn = translator.translate(inputTextSk);

        assertTrue(Arrays.asList(requiredOutputsEn).contains(realOutputEn),
            "Translation from SK to EN failed. Expected one of: " + Arrays.toString(requiredOutputsEn) + ", BUT GOT:\n" + realOutputEn);
    }
}
