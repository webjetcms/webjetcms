package sk.iway.iwcm.components.translation_keys.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyEntity;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyRepository;
import sk.iway.iwcm.i18n.Prop;

/** Tests restricted-user sanitization for trusted saves and permission checks for arbitrary translation keys. */
class TranslationKeyServiceTest {

    /** Verifies that restricted users cannot edit cookie keys directly and trusted saves still sanitize their values. */
    @Test
    void trustedSaveSanitizesRestrictedValuesWhileEditorRejectsKey() {
        Identity user = mock(Identity.class);
        TranslationKeyRepository repository = mock(TranslationKeyRepository.class);
        TranslationKeyService service = new TranslationKeyService(repository, null);
        TranslationKeyEntity translation = new TranslationKeyEntity();
        translation.setKey("tenant.example-components.gdpr.cookies.lng.provider");
        translation.setLng("en");
        translation.setValue("<script>alert(1)</script>");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Prop> props = mockStatic(Prop.class)) {
            constants.when(() -> Constants.getStringExecuteMacro("propertiesEnabledKeys"))
                    .thenReturn("multiweb.,components.multiweb,default.,checkform.");
            constants.when(() -> Constants.getString("propAllowedTags")).thenReturn("-");
            props.when(Prop::getInstance).thenReturn(mock(Prop.class));

            assertThrows(IllegalArgumentException.class,
                    () -> service.createOrEditTranslationKeySingleLanguage(user, translation, true));
            verifyNoInteractions(repository);

            TranslationKeyEntity saved = service.saveTranslation(user, translation, false);

            assertEquals("&lt;script&gt;alert&#x28;1&#x29;&lt;/script&gt;", saved.getValue());
            verify(repository).save(saved);
        }
    }
}
