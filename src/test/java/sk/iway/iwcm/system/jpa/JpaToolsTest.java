package sk.iway.iwcm.system.jpa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JpaToolsTest {

    @Test
    void dedicatedRagDatasourceIsExcludedFromLegacyJpaBootstrap() {
        assertFalse(JpaTools.isJPADatasource("rag_jpa"));
        assertTrue(JpaTools.isJPADatasource("iwcm"));
        assertTrue(JpaTools.isJPADatasource("catalog_jpa"));
    }
}
