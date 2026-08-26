package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.database.SimpleQuery;

class ConfDBTest {

    @BeforeEach
    void initializeConfigurationCatalog() {
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    @Test
    void setRuntimeValueAppliesSpecialLinkTypeRepresentation() {
        assertEquals(String.valueOf(Constants.LINK_TYPE_HTML), ConfDB.normalizeRuntimeValue("linkType", "html"));
        assertEquals(String.valueOf(Constants.LINK_TYPE_DOCID), ConfDB.normalizeRuntimeValue("linkType", "docid"));

        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            ConfDB.setRuntimeValue("linkType", "html");
            ConfDB.setRuntimeValue("linkType", "docid");

            constants.verify(() -> Constants.setInt("linkType", Constants.LINK_TYPE_HTML));
            constants.verify(() -> Constants.setInt("linkType", Constants.LINK_TYPE_DOCID));
        }
    }

    @Test
    void getConfForJspMatchesHierarchicalPathsAndKnownFileExtensions() {
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("apps.gallery"), "galleryImageQuality"));
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("gallery.jsp"), "galleryImageQuality"));
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("/components/gallery/gallery.jsp"), "galleryImageQuality"));
    }

    @Test
    void getConfForJspMatchesAnyCompleteHierarchySegment() {
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("security"), "oauth2_githubClientId"));
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("oauth2"), "oauth2_githubClientId"));
    }

    @Test
    void getConfForJspDoesNotMatchPartialModuleNames() {
        List<ConfDetails> formConfiguration = ConfDB.getConfForJsp("form");

        assertTrue(containsConfiguration(formConfiguration, "formmailAllowedRecipients"));
        assertFalse(containsConfiguration(formConfiguration, "formMailFixedSenderEmail"));
    }

    @Test
    void getConfForJspPreservesLegacyAliases() {
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("forms"), "formmailAllowedRecipients"));
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("reservations"), "reservationAllDayStartTime"));
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("related_pages"), "RelatedPagesDBCacheMinutes"));
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("dynamic_tags"), "navbarSeparator"));
        assertTrue(containsConfiguration(ConfDB.getConfForJsp("banner_system"), "bannerCampaignParamName"));
    }

    @Test
    void refreshVariableRestoresDefaultAfterRemoteClusterReset() {
        String name = "configurationRemoteResetTest";
        String defaultValue = "default-value";
        String valueSql = "SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        String countSql = "SELECT COUNT(*) FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        ConfDetails defaultConf = new ConfDetails(name, defaultValue);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
                    when(query.forString(valueSql, name)).thenReturn(null);
                    when(query.forInt(countSql, name)).thenReturn(0);
                })) {
            constants.when(Constants::getAllValues).thenReturn(List.of(defaultConf));

            ConfDB.refreshVariable(name);

            assertEquals(2, queries.constructed().size());
            verify(queries.constructed().get(0)).forString(valueSql, name);
            verify(queries.constructed().get(1)).forInt(countSql, name);
            constants.verify(() -> Constants.setString(name, defaultValue));
        }
    }

    @Test
    void refreshVariablePreservesEmptyRuntimeValueForNullDatabaseRow() {
        String name = "configurationNullDatabaseValueTest";
        String valueSql = "SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        String countSql = "SELECT COUNT(*) FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
                    when(query.forString(valueSql, name)).thenReturn(null);
                    when(query.forInt(countSql, name)).thenReturn(1);
                })) {
            ConfDB.refreshVariable(name);

            assertEquals(2, queries.constructed().size());
            constants.verify(() -> Constants.setString(name, ""));
        }
    }

    private boolean containsConfiguration(List<ConfDetails> configuration, String name) {
        return configuration.stream().anyMatch(item -> name.equals(item.getName()));
    }
}
