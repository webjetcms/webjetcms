package sk.iway.iwcm.components.enumerations.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataRepository;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;

/**
 * Tests synchronization of custom field settings after saving an enumeration type.
 */
class EnumerationTypeRestControllerTest {

    private static final int COMMON_DOMAIN_ID = 7;
    private static final long ENUMERATION_TYPE_ID = 42L;

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

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            constants.when(() -> Constants.getInt("domainIdCommon")).thenReturn(COMMON_DOMAIN_ID);

            controller.afterSave(saved, saved);

            verify(customFieldsRepository).findAllByClassNameAndEntityId(
                EnumerationDataBean.class.getName(),
                ENUMERATION_TYPE_ID,
                COMMON_DOMAIN_ID
            );
            cloudTools.verify(CloudToolsForCore::getDomainId, never());
        }
    }
}
