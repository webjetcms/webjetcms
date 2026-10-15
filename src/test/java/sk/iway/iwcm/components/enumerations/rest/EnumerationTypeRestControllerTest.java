package sk.iway.iwcm.components.enumerations.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataRepository;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;

/**
 * Tests deleted-type safeguards, data deletion and restoration, and synchronization and duplication of string field settings.
 */
class EnumerationTypeRestControllerTest {

    private static final int COMMON_DOMAIN_ID = 7;
    private static final long ENUMERATION_TYPE_ID = 42L;

    /** Rejects operations on deleted types even when the request claims the type is active. */
    @Test
    void deletedTypesCannotBeDuplicatedOrDeleted() {
        EnumerationTypeRepository repository = mock(EnumerationTypeRepository.class);
        EnumerationDataRepository dataRepository = mock(EnumerationDataRepository.class);
        EnumerationTypeRestController controller = spy(new EnumerationTypeRestController(repository, dataRepository, mock(CustomFieldsRepository.class)));
        doThrow(new IllegalStateException()).when(controller).throwError("config.not_permitted_action_err");
        EnumerationTypeBean deleted = new EnumerationTypeBean();
        deleted.setHidden(true);
        when(repository.findById(ENUMERATION_TYPE_ID)).thenReturn(Optional.of(deleted));
        EnumerationTypeBean submitted = new EnumerationTypeBean();
        submitted.setId(ENUMERATION_TYPE_ID);

        assertThrows(IllegalStateException.class, () -> controller.beforeDuplicate(submitted, ENUMERATION_TYPE_ID));
        assertThrows(IllegalStateException.class, () -> controller.deleteItem(submitted, ENUMERATION_TYPE_ID));
        verify(repository, never()).deleteEnumTypeById((int) ENUMERATION_TYPE_ID, true);
        verifyNoInteractions(dataRepository);
    }

    /** Propagates deletion and restoration to data records, but leaves data unchanged on ordinary edits. */
    @ParameterizedTest
    @CsvSource({
        "false, true, 1",
        "true, false, 1",
        "false, false, 0",
        "true, true, 0"
    })
    void editUpdatesDataOnlyWhenTypeDeletionStateChanges(boolean wasHidden, boolean hidden, int expectedUpdates) {
        EnumerationTypeRepository repository = mock(EnumerationTypeRepository.class);
        EnumerationDataRepository dataRepository = mock(EnumerationDataRepository.class);
        EnumerationTypeRestController controller = spy(new EnumerationTypeRestController(repository, dataRepository, mock(CustomFieldsRepository.class)));
        EnumerationTypeBean type = new EnumerationTypeBean();
        type.setId(ENUMERATION_TYPE_ID);
        type.setHidden(hidden);
        doReturn(type).when(controller).getOne(ENUMERATION_TYPE_ID);
        when(repository.getHiddenByEnumTypeId((int) ENUMERATION_TYPE_ID)).thenReturn(wasHidden);
        when(repository.save(type)).thenReturn(type);

        controller.editItem(type, ENUMERATION_TYPE_ID);

        verify(dataRepository, times(expectedUpdates)).deleteAllEnumDataByEnumTypeId((int) ENUMERATION_TYPE_ID, hidden);
        verifyNoMoreInteractions(dataRepository);
    }

    /**
     * Verifies that common enumeration field settings are loaded from the configured common domain.
     */
    @Test
    void afterSaveUsesCommonDomainForEnumerationCustomFields() {
        EnumerationTypeRepository enumerationTypeRepository = mock(EnumerationTypeRepository.class);
        EnumerationDataRepository enumerationDataRepository = mock(EnumerationDataRepository.class);
        CustomFieldsRepository customFieldsRepository = mock(CustomFieldsRepository.class);
        EnumerationTypeRestController controller = new EnumerationTypeRestController(
            enumerationTypeRepository,
            enumerationDataRepository,
            customFieldsRepository
        );
        EnumerationTypeBean saved = new EnumerationTypeBean();
        saved.setId(ENUMERATION_TYPE_ID);

        when(customFieldsRepository.findAllByClassNameAndEntityId(
            EnumerationDataBean.class.getName(),
            ENUMERATION_TYPE_ID,
            COMMON_DOMAIN_ID
        )).thenReturn(Collections.emptyList());

        Cache enumerationCache = mock(Cache.class);
        try (MockedStatic<Cache> cache = mockStatic(Cache.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cache.when(Cache::getInstance).thenReturn(enumerationCache);
            constants.when(() -> Constants.getInt("domainIdCommon")).thenReturn(COMMON_DOMAIN_ID);

            controller.afterSave(saved, saved);

            verify(enumerationCache).removeObjectStartsWithName("enumeration.");
            verify(customFieldsRepository).findAllByClassNameAndEntityId(
                EnumerationDataBean.class.getName(),
                ENUMERATION_TYPE_ID,
                COMMON_DOMAIN_ID
            );
            cloudTools.verify(CloudToolsForCore::getDomainId, never());
        }
    }

    /**
     * Verifies that duplicating an enumeration type creates independent copies of its string field settings.
     */
    @Test
    @SuppressWarnings("unchecked")
    void afterDuplicateCopiesEnumerationStringFieldSettings() {
        long originalEnumerationTypeId = 41L;
        long duplicatedEnumerationTypeId = 84L;
        EnumerationTypeRepository enumerationTypeRepository = mock(EnumerationTypeRepository.class);
        EnumerationDataRepository enumerationDataRepository = mock(EnumerationDataRepository.class);
        CustomFieldsRepository customFieldsRepository = mock(CustomFieldsRepository.class);
        EnumerationTypeRestController controller = new EnumerationTypeRestController(
            enumerationTypeRepository,
            enumerationDataRepository,
            customFieldsRepository
        );

        EnumerationTypeBean duplicatedType = new EnumerationTypeBean();
        duplicatedType.setId(duplicatedEnumerationTypeId);

        CustomFieldsEntity sourceA = createSourceField(
            77L,
            originalEnumerationTypeId,
            "A",
            "text",
            Boolean.TRUE,
            "text-255, warningLength-220"
        );
        CustomFieldsEntity sourceL = createSourceField(
            78L,
            originalEnumerationTypeId,
            "L",
            "select",
            Boolean.FALSE,
            "|First|Second"
        );
        CustomFieldsEntity outsideStringFieldRange = createSourceField(
            79L,
            originalEnumerationTypeId,
            "M",
            "text",
            Boolean.FALSE,
            null
        );
        CustomFieldsEntity bonusField = createSourceField(
            80L,
            originalEnumerationTypeId,
            "B",
            "text",
            Boolean.FALSE,
            null
        );
        bonusField.setBonusEntityId(99L);

        when(customFieldsRepository.findAllByClassNameAndEntityId(
            EnumerationDataBean.class.getName(),
            originalEnumerationTypeId,
            COMMON_DOMAIN_ID
        )).thenReturn(List.of(sourceA, sourceL, outsideStringFieldRange, bonusField));

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            constants.when(() -> Constants.getInt("domainIdCommon")).thenReturn(COMMON_DOMAIN_ID);

            controller.afterDuplicate(duplicatedType, originalEnumerationTypeId);

            ArgumentCaptor<List<CustomFieldsEntity>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
            verify(customFieldsRepository).saveAll(fieldsCaptor.capture());
            List<CustomFieldsEntity> duplicatedFields = fieldsCaptor.getValue();

            assertEquals(2, duplicatedFields.size());
            assertCopiedField(
                sourceA,
                getFieldByAlphabet(duplicatedFields, "A"),
                duplicatedEnumerationTypeId
            );
            assertCopiedField(
                sourceL,
                getFieldByAlphabet(duplicatedFields, "L"),
                duplicatedEnumerationTypeId
            );

            assertEquals(77L, sourceA.getId());
            assertEquals(originalEnumerationTypeId, sourceA.getEntityId());
            assertEquals(COMMON_DOMAIN_ID, sourceA.getDomainId());
            assertEquals(78L, sourceL.getId());
            assertEquals(originalEnumerationTypeId, sourceL.getEntityId());
            assertEquals(COMMON_DOMAIN_ID, sourceL.getDomainId());
            cloudTools.verify(CloudToolsForCore::getDomainId, never());
        }
    }

    /**
     * Verifies that field settings cannot be copied before the duplicated type has a persistent ID.
     */
    @Test
    void afterDuplicateRejectsUnsavedTarget() {
        EnumerationTypeRepository enumerationTypeRepository = mock(EnumerationTypeRepository.class);
        EnumerationDataRepository enumerationDataRepository = mock(EnumerationDataRepository.class);
        CustomFieldsRepository customFieldsRepository = mock(CustomFieldsRepository.class);
        EnumerationTypeRestController controller = new EnumerationTypeRestController(
            enumerationTypeRepository,
            enumerationDataRepository,
            customFieldsRepository
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> controller.afterDuplicate(new EnumerationTypeBean(), ENUMERATION_TYPE_ID)
        );

        assertEquals(
            "Duplicated enumeration type must be saved before copying its field settings.",
            exception.getMessage()
        );
        verifyNoInteractions(customFieldsRepository);
    }

    private static CustomFieldsEntity createSourceField(
            long id,
            long enumerationTypeId,
            String alphabet,
            String type,
            Boolean required,
            String value) {
        CustomFieldsEntity field = new CustomFieldsEntity();
        field.setId(id);
        field.setClassName(EnumerationDataBean.class.getName());
        field.setAlphabet(alphabet);
        field.setEntityId(enumerationTypeId);
        field.setType(type);
        field.setLabel("Original field " + alphabet);
        field.setTooltip("Tooltip " + alphabet);
        field.setRequired(required);
        field.setBonusClassName("");
        field.setBonusEntityId(0L);
        field.setDomainId(COMMON_DOMAIN_ID);
        field.setValue(value);
        field.setWarningText("Warning " + alphabet);
        return field;
    }

    private static CustomFieldsEntity getFieldByAlphabet(List<CustomFieldsEntity> fields, String alphabet) {
        return fields.stream()
            .filter(field -> alphabet.equals(field.getAlphabet()))
            .findFirst()
            .orElseThrow();
    }

    private static void assertCopiedField(
            CustomFieldsEntity source,
            CustomFieldsEntity copy,
            long duplicatedEnumerationTypeId) {
        assertNotSame(source, copy);
        assertNull(copy.getId());
        assertEquals(EnumerationDataBean.class.getName(), copy.getClassName());
        assertEquals(source.getAlphabet(), copy.getAlphabet());
        assertEquals(duplicatedEnumerationTypeId, copy.getEntityId());
        assertEquals(source.getType(), copy.getType());
        assertEquals(source.getLabel(), copy.getLabel());
        assertEquals(source.getTooltip(), copy.getTooltip());
        assertEquals(source.getRequired(), copy.getRequired());
        assertEquals(source.getBonusClassName(), copy.getBonusClassName());
        assertEquals(source.getBonusEntityId(), copy.getBonusEntityId());
        assertEquals(source.getDomainId(), copy.getDomainId());
        assertEquals(source.getValue(), copy.getValue());
        assertEquals(source.getWarningText(), copy.getWarningText());
    }
}
