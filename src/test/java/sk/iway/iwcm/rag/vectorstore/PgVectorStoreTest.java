package sk.iway.iwcm.rag.vectorstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.rag.vectorjpa.PgvectorJpaConfig;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/** Verifies that shared vector schema initialization ignores tenant dimension overrides. */
class PgVectorStoreTest {

    /** Checks generated table dimensions and context restoration on successful and failed initialization. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void initializesGlobalDimensionsAndRestoresTenantContext(boolean failInitialization) {
        String domainName = "schema-test.example";
        String dimensionsKey = "ragEmbeddingDimensions";
        String tenantDimensionsKey = domainName + "-" + dimensionsKey;
        boolean originalAliasSearch = Constants.isConstantsAliasSearch();
        Constants.setConstantsAliasSearch(false);
        Map<String, Integer> originalValues = new HashMap<>();
        for (String key : List.of(dimensionsKey, tenantDimensionsKey)) {
            originalValues.put(key, Constants.containsKey(key) ? Constants.getInt(key) : null);
        }

        try (DomainRequestBeanScope tenantScope = DomainRequestBeanScope.open(domainName);
             MockedStatic<PgvectorJpaConfig> configuration = mockStatic(PgvectorJpaConfig.class);
             MockedStatic<Logger> logger = mockStatic(Logger.class);
             MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
                 if (failInitialization) {
                     doThrow(new IllegalStateException("Schema creation failed"))
                         .when(query).execute(startsWith("CREATE TABLE IF NOT EXISTS rag_embedding_chunks"));
                 }
             })) {
            Constants.setInt(dimensionsKey, 1536);
            Constants.setInt(tenantDimensionsKey, 768);
            Constants.setConstantsAliasSearch(true);
            configuration.when(PgvectorJpaConfig::getRagDataSourceName).thenReturn("rag_jpa");

            RequestBean tenantRequest = SetCharacterEncodingFilter.getCurrentRequestBean();
            assertEquals(768, Constants.getInt(dimensionsKey));

            assertEquals(failInitialization == false, new PgVectorStore().initializeSchema());

            assertSame(tenantRequest, SetCharacterEncodingFilter.getCurrentRequestBean());
            assertEquals(768, Constants.getInt(dimensionsKey));
            verify(queries.constructed().get(0)).execute(argThat(sql ->
                sql.startsWith("CREATE TABLE IF NOT EXISTS rag_embedding_chunks")
                    && sql.contains("vector(1536)")
            ));
        } finally {
            originalValues.forEach((key, value) -> {
                if (value == null) Constants.deleteConstant(key);
                else Constants.setInt(key, value);
            });
            Constants.setConstantsAliasSearch(originalAliasSearch);
        }
    }
}
