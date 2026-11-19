package sk.iway.iwcm.system.datatable.settings.customfields.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.settings.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.system.datatable.settings.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.system.datatable.settings.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.system.datatable.settings.fields.rest.FieldsSettingsService;

/**
 * Tests custom-field lookup, optional translation fallback and shared class suggestions.
 */
class CustomFieldsServiceTest {

    private static final int CURRENT_DOMAIN_ID = 9;
    private static final int COMMON_DOMAIN_ID = 17;
    private static final long ENTITY_ID = 42L;

    /** Verifies optional field types and labels retain Slovak fallback without registering missing keys. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void resolvesOptionalTranslationsWithoutMissingKeyReporting(boolean hasSlovakTranslation) {
        String labelKey = "regression.custom-fields.field_a";
        String typeKey = labelKey + ".type";
        String fieldType = "regression-jsoneditor";
        String typeLabelKey = "settings.custom-fields.type." + fieldType;
        List<String> keys = List.of(typeKey, typeLabelKey);
        IwayProperties german = new IwayProperties();
        IwayProperties slovak = new IwayProperties();
        if (hasSlovakTranslation) {
            slovak.setProperty(typeKey, "jsoneditor");
            slovak.setProperty(typeLabelKey, "Editor JSON");
        }

        Prop prop = mock(Prop.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(prop, "properties", german);
        ReflectionTestUtils.setField(prop, "language", "de");
        doReturn(german).when(prop).getRes("de");
        doReturn(slovak).when(prop).getRes("sk");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("de");

            assertEquals(hasSlovakTranslation ? "jsoneditor" : typeKey,
                CustomFieldsService.getConfiguredFieldType(null, labelKey, prop));
            assertEquals(hasSlovakTranslation ? "Editor JSON" : typeLabelKey,
                CustomFieldsService.getFieldTypeLabel(prop, fieldType));
            assertTrue(Prop.getMissingTexts("de") == null
                || Prop.getMissingTexts("de").stream().noneMatch(text -> keys.contains(text.getKey())),
                "Optional custom-field keys must not be reported as missing.");
        } finally {
            if (Prop.getMissingTexts("de") != null) {
                Prop.getMissingTexts("de").removeIf(text -> keys.contains(text.getKey()));
            }
        }
    }

    /** Verifies that custom-field autocomplete uses the shared discovery service. */
    @Test
    void usesSharedClassOptions() {
        FieldsSettingsService fieldsSettings = mock(FieldsSettingsService.class);
        CustomFieldsService service = new CustomFieldsService(
            mock(CustomFieldsRepository.class), mock(EnumerationTypeRepository.class), fieldsSettings);
        List<String> options = List.of(EnumerationDataBean.class.getName());
        when(fieldsSettings.getCustomFieldClassOptions("Enumeration")).thenReturn(options);

        assertSame(options, service.getClassOptions("Enumeration"));
        verify(fieldsSettings).getCustomFieldClassOptions("Enumeration");
    }

    /**
     * Verifies that runtime lookups use the entity's resolved domain scope.
     */
    @Test
    void usesResolvedDomainForRuntimeLookup() {
        CustomFieldsRepository repository = mock(CustomFieldsRepository.class);
        when(repository.findAllGlobalCustomFields(anyString(), anyInt())).thenReturn(List.of());
        when(repository.findAllByClassNameAndEntityId(anyString(), anyLong(), anyInt()))
            .thenAnswer(invocation -> new ArrayList<CustomFieldsEntity>());

        try (MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            tools.when(() -> Tools.getSpringBean("customFieldsRepository", CustomFieldsRepository.class)).thenReturn(repository);
            constants.when(() -> Constants.getInt("domainIdCommon")).thenReturn(COMMON_DOMAIN_ID);
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            List<CustomFieldsEntity> enumerationFields = CustomFieldsService.getCustomFields(
                new CustomFieldsSearchDto(EnumerationDataBean.class.getName(), ENTITY_ID)
            );
            List<CustomFieldsEntity> regularFields = CustomFieldsService.getCustomFields(
                new CustomFieldsSearchDto(CustomFieldsEntity.class.getName(), ENTITY_ID)
            );

            assertTrue(enumerationFields.isEmpty(), "The mocked enumeration lookup must return an empty result");
            assertTrue(regularFields.isEmpty(), "The mocked regular lookup must return an empty result");
            verify(repository).findAllGlobalCustomFields(EnumerationDataBean.class.getName(), COMMON_DOMAIN_ID);
            verify(repository).findAllByClassNameAndEntityId(EnumerationDataBean.class.getName(), ENTITY_ID, COMMON_DOMAIN_ID);
            verify(repository).findAllGlobalCustomFields(CustomFieldsEntity.class.getName(), CURRENT_DOMAIN_ID);
            verify(repository).findAllByClassNameAndEntityId(CustomFieldsEntity.class.getName(), ENTITY_ID, CURRENT_DOMAIN_ID);
        }
    }
}
