package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PathFilter;
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
        assertEquals(String.valueOf(Constants.LINK_TYPE_HTML), ConfDB.normalizeRuntimeValue("linkType", String.valueOf(Constants.LINK_TYPE_HTML)));
        assertEquals(String.valueOf(Constants.LINK_TYPE_DOCID), ConfDB.normalizeRuntimeValue("linkType", String.valueOf(Constants.LINK_TYPE_DOCID)));

        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            ConfDB.setRuntimeValue("linkType", "html");
            ConfDB.setRuntimeValue("linkType", "docid");

            constants.verify(() -> Constants.setInt("linkType", Constants.LINK_TYPE_HTML));
            constants.verify(() -> Constants.setInt("linkType", Constants.LINK_TYPE_DOCID));
        }
    }

    @Test
    void deleteNameRestoresDefaultThroughRuntimeSideEffects() throws Exception {
        String name = "pathFilterBlockedPaths";
        String defaultValue = ConfDB.getOldValue(name);
        String deleteSql = "DELETE FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(deleteSql)).thenReturn(statement);
        Constants.setString(name, "custom-blocked-path");

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
                MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class);
                MockedStatic<PathFilter> pathFilter = mockStatic(PathFilter.class)) {
            dbPool.when(DBPool::getConnection).thenReturn(connection);

            assertTrue(ConfDB.deleteName(name));

            pathFilter.verify(PathFilter::resetBlockedPaths);
            assertEquals(defaultValue, Constants.getString(name));
        }

        verify(connection).prepareStatement(deleteSql);
        verify(statement).setString(1, name);
        verify(statement).execute();
    }

    @Test
    void deleteNameRemovesRuntimeValueWithoutDefault() throws Exception {
        String name = "configurationDatabaseOnlyResetTest";
        String deleteSql = "DELETE FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(deleteSql)).thenReturn(statement);
        Constants.setString(name, "database-only-value");
        assertTrue(Constants.containsKey(name));

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
                MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class)) {
            dbPool.when(DBPool::getConnection).thenReturn(connection);

            assertTrue(ConfDB.deleteName(name));

            assertFalse(Constants.containsKey(name));
            assertFalse(Constants.getAllKeys().contains(name));
        }

        verify(connection).prepareStatement(deleteSql);
        verify(statement).setString(1, name);
        verify(statement).execute();
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
    void refreshVariableRestoresDefaultThroughRuntimeSideEffects() {
        String name = "pathFilterBlockedPaths";
        String defaultValue = ConfDB.getOldValue(name);
        String valueSql = "SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        String countSql = "SELECT COUNT(*) FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        Constants.setString(name, "custom-blocked-path");

        try (MockedStatic<PathFilter> pathFilter = mockStatic(PathFilter.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
                    when(query.forString(valueSql, name)).thenReturn(null);
                    when(query.forInt(countSql, name)).thenReturn(0);
                })) {
            ConfDB.refreshVariable(name);

            assertEquals(2, queries.constructed().size());
            verify(queries.constructed().get(0)).forString(valueSql, name);
            verify(queries.constructed().get(1)).forInt(countSql, name);
            pathFilter.verify(PathFilter::resetBlockedPaths);
            assertEquals(defaultValue, Constants.getString(name));
        }
    }

    @Test
    void refreshVariablePreservesExplicitEmptyDefaultAfterRemoteClusterReset() {
        String name = "configurationEmptyDefaultTest";
        String valueSql = "SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        String countSql = "SELECT COUNT(*) FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        ConfDetails defaultConf = new ConfDetails(name, "");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
                    when(query.forString(valueSql, name)).thenReturn(null);
                    when(query.forInt(countSql, name)).thenReturn(0);
                })) {
            constants.when(Constants::getAllValues).thenReturn(List.of(defaultConf));

            ConfDB.refreshVariable(name);

            constants.verify(() -> Constants.setString(name, ""));
            constants.verify(() -> Constants.deleteConstant(name), never());
        }
    }

    @Test
    void refreshVariableRemovesRuntimeValueWithoutDefaultAfterRemoteClusterReset() {
        String name = "pathFilterBlockedPaths";
        String valueSql = "SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        String countSql = "SELECT COUNT(*) FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<PathFilter> pathFilter = mockStatic(PathFilter.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
                    when(query.forString(valueSql, name)).thenReturn(null);
                    when(query.forInt(countSql, name)).thenReturn(0);
                })) {
            constants.when(Constants::getAllValues).thenReturn(List.of());

            ConfDB.refreshVariable(name);

            pathFilter.verify(PathFilter::resetBlockedPaths);
            constants.verify(() -> Constants.deleteConstant(name));
            constants.verify(() -> Constants.setString(name, ""), never());
        }
    }

    @Test
    void refreshVariableRestoresLinkTypeDefaultAfterRemoteClusterReset() {
        String name = "linkType";
        String defaultValue = String.valueOf(Constants.LINK_TYPE_HTML);
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
            constants.verify(() -> Constants.setInt(name, Constants.LINK_TYPE_HTML));
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
