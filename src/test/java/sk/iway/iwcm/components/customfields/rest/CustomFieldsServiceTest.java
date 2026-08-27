package sk.iway.iwcm.components.customfields.rest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;

/**
 * Tests runtime custom-field lookup domain selection.
 */
class CustomFieldsServiceTest {

    private static final int CURRENT_DOMAIN_ID = 9;
    private static final int COMMON_DOMAIN_ID = 17;
    private static final long ENTITY_ID = 42L;

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
