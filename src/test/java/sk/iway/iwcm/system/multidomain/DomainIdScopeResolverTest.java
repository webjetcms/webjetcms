package sk.iway.iwcm.system.multidomain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;

/**
 * Tests domain ID resolution from the common-domain marker annotation.
 */
class DomainIdScopeResolverTest {

    private static final int CURRENT_DOMAIN_ID = 9;
    private static final int COMMON_DOMAIN_ID = 17;

    /**
     * Verifies that a common-scoped entity uses the configured common domain.
     */
    @Test
    void resolvesConfiguredCommonDomainForAnnotatedEntity() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getInt("domainIdCommon")).thenReturn(COMMON_DOMAIN_ID);

            int resolvedDomainId = DomainIdScopeResolver.resolve(EnumerationDataBean.class);

            assertEquals(COMMON_DOMAIN_ID, resolvedDomainId, "Annotated entities must use the configured common domain");
        }
    }

    /**
     * Verifies that the default domain is used when the common domain is not configured.
     */
    @Test
    void fallsBackToDefaultCommonDomain() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getInt("domainIdCommon")).thenReturn(0);

            int resolvedDomainId = DomainIdScopeResolver.resolve(EnumerationDataBean.class.getName());

            assertEquals(1, resolvedDomainId, "An unset common domain must fall back to domain 1");
        }
    }

    /**
     * Verifies that an entity without a scope annotation remains in the current domain.
     */
    @Test
    void resolvesCurrentDomainForUnannotatedEntity() {
        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            int resolvedDomainId = DomainIdScopeResolver.resolve(CustomFieldsEntity.class.getName());

            assertEquals(CURRENT_DOMAIN_ID, resolvedDomainId, "Unannotated entities must use the current domain");
        }
    }

    /**
     * Verifies that an unknown class name safely remains in the current domain.
     */
    @Test
    void resolvesCurrentDomainForUnknownEntityClass() {
        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            int resolvedDomainId = DomainIdScopeResolver.resolve("invalid.missing.Entity");

            assertEquals(CURRENT_DOMAIN_ID, resolvedDomainId, "Unknown entity classes must use the current domain");
        }
    }

    /**
     * Verifies common-domain marker detection through class and class-name overloads.
     */
    @Test
    void detectsCommonDomainMarker() {
        assertTrue(DomainIdScopeResolver.isCommon(EnumerationDataBean.class), "The annotated class must use the common domain");
        assertTrue(DomainIdScopeResolver.isCommon(EnumerationDataBean.class.getName()), "The annotated class name must use the common domain");
        assertFalse(DomainIdScopeResolver.isCommon(CustomFieldsEntity.class), "An unannotated class must not use the common domain");
        assertFalse(DomainIdScopeResolver.isCommon(CustomFieldsEntity.class.getName()), "An unannotated class name must not use the common domain");
        assertFalse(DomainIdScopeResolver.isCommon("invalid.missing.Entity"), "An unknown class name must not use the common domain");
    }
}
