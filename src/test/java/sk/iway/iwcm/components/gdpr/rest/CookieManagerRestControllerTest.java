package sk.iway.iwcm.components.gdpr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.gdpr.CookieManagerBean;
import sk.iway.iwcm.components.gdpr.CookieManagerDB;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyEntity;
import sk.iway.iwcm.components.translation_keys.rest.TranslationKeyService;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

/** Tests cookie translation scoping, fallback, and domain access checks. */
class CookieManagerRestControllerTest {

    /** Verifies domain-scoped saves without alias search, preserving language and reloading after the last value. */
    @Test
    void savesDescriptionsInCurrentDomain() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("breadcrumbLanguage")).thenReturn("en");
        Identity user = mock(Identity.class);
        RequestBean requestBean = new RequestBean();
        requestBean.setDomain("tenant.example");
        TranslationKeyService service = mock(TranslationKeyService.class);
        List<SavedTranslation> saved = new ArrayList<>();
        doAnswer(call -> {
            TranslationKeyEntity translation = call.getArgument(1);
            saved.add(new SavedTranslation(translation.getKey(), translation.getLng(),
                    translation.getValue(), call.getArgument(2)));
            return translation;
        }).when(service).saveTranslation(same(user), any(TranslationKeyEntity.class), anyBoolean());

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class);
                MockedStatic<MultiDomainFilter> domains = mockStatic(MultiDomainFilter.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class)) {
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(requestBean);
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            CookieManagerRestController controller = new CookieManagerRestController(service);
            controller.setRequest(request);
            CookieManagerBean cookie = new CookieManagerBean();
            cookie.setCookieName("lng");
            cookie.setProvider("Website operator");
            cookie.setPurpouse("Remember the selected language");
            cookie.setValidity("One year");

            controller.createEditTranslationKeysFromEntity(cookie);

            String keyPrefix = "tenant.example-components.gdpr.cookies.lng.";
            assertEquals(List.of(
                    new SavedTranslation(keyPrefix + "provider", "en", "Website operator", false),
                    new SavedTranslation(keyPrefix + "purpouse", "en", "Remember the selected language", false),
                    new SavedTranslation(keyPrefix + "validity", "en", "One year", true)), saved);
        }
    }

    /** Verifies domain overrides, shared descriptions, and default-language fallback during cookie loading. */
    @Test
    void readsDescriptionsWithSharedAndLanguageFallback() {
        String baseKey = "components.gdpr.cookies.regression-language.";
        RequestBean requestBean = new RequestBean();
        requestBean.setDomain("tenant.example");
        requestBean.setParameters(Map.of());
        IwayProperties translations = new IwayProperties();
        translations.setProperty(baseKey + "provider", "Shared provider");
        translations.setProperty(baseKey + "purpouse", "Shared purpose");
        translations.setProperty(baseKey + "validity", "Shared validity");
        translations.setProperty("tenant-" + baseKey + "provider", "Current domain provider");
        IwayProperties defaults = new IwayProperties();
        defaults.setProperty("tenant-" + baseKey + "validity", "Current domain default-language validity");
        Prop prop = mock(Prop.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(prop, "properties", translations);
        ReflectionTestUtils.setField(prop, "language", "en");
        doReturn(defaults).when(prop).getRes("sk");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class);
                MockedStatic<MultiDomainFilter> domains = mockStatic(MultiDomainFilter.class)) {
            constants.when(Constants::isConstantsAliasSearch).thenReturn(true);
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("sk");
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(requestBean);
            domains.when(() -> MultiDomainFilter.getDomainAlias("tenant.example")).thenReturn("tenant");
            CookieManagerRestController controller = new CookieManagerRestController(mock(TranslationKeyService.class));
            CookieManagerBean cookie = new CookieManagerBean();
            cookie.setCookieName("regression-language");

            controller.setTranslationKeysIntoEntity(cookie, prop);

            assertEquals("Current domain provider", cookie.getProvider());
            assertEquals("Shared purpose", cookie.getPurpouse());
            assertEquals("Current domain default-language validity", cookie.getValidity());
        } finally {
            if (Prop.getMissingTexts("en") != null) {
                Prop.getMissingTexts("en").removeIf(text -> text.getKey().endsWith(baseKey + "purpouse")
                        || text.getKey().endsWith(baseKey + "validity"));
            }
        }
    }

    /** Verifies that existing cookie access uses the persisted domain even when the submitted domain is forged. */
    @ParameterizedTest
    @CsvSource({ "7, true", "8, false", "-1, false" })
    void checksPersistedDomainBeforeChangingCookies(int storedDomain, boolean expected) {
        CookieManagerBean submitted = new CookieManagerBean();
        submitted.setId(42);
        submitted.setDomainId(7);
        CookieManagerBean stored = new CookieManagerBean();
        stored.setDomainId(storedDomain);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
                MockedConstruction<CookieManagerDB> databases = mockConstruction(CookieManagerDB.class,
                        (database, context) -> when(database.getById(42L)).thenReturn(storedDomain < 0 ? null : stored))) {
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(7);
            CookieManagerRestController controller = new CookieManagerRestController(mock(TranslationKeyService.class));

            assertEquals(expected, controller.checkItemPerms(submitted, 42L));
        }
    }

    private record SavedTranslation(String key, String language, String value, boolean reload) {
    }
}
