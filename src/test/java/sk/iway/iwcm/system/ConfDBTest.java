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
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.database.SimpleQuery;

class ConfDBTest {

    private static final List<String> CONSOLIDATED_DEFAULT_NAMES = List.of(
            "defaultSkin",
            "auditJpaDisabledEntities",
            "mariaDbDefaultEngine",
            "sk.iway.iwcm.qa.AddAction.sendAdminMail.url",
            "xsrfParamNameExceptionSystem",
            "componentsDirectCallExceptionsSystem",
            "insertScriptCacheMinutes",
            "xssHtmlAllowedFieldsSystem");

    private static final String XSRF_PARAM_NAME_EXCEPTION_SYSTEM =
            "docid,historyid,_logLevel,combineEnabled,combineEnabledRequest,groupid,forward,forceBrowserDetector,_writePerfStat,_disableCache,printTrace,isPopup,isDmail,NO_WJTOOLBAR,userlngr,page,words,datumOd,datumDo,btnSubmit,search,language,__lng,lng,userid,userId,webjetDmsp,formId,hash,invoiceId,auth,loginName,t,f,v,forum,NO_WJTOOLBAR,isPdfVersion,fbclid,utm_source,utm_medium,utm_campaign,utm_term,utm_content,formName,showTextKeys,extURL,id,removePerm,showBanner"
            + ",tempId,redirectId,dir,bid,actualDir,pId,origUrl,week,w,h,ip,c,noip,rnd,login,auth,reservationDate,iID,name,act,datum,basketAct,invoicePaymentId,email,save,scheduleId,rootDir";

    @BeforeEach
    void initializeConfigurationCatalog() {
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    @AfterEach
    void restoreConfigurationCatalog() {
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modernDefaults")
    void modernDefaultIsCurrentAndCatalogued(String name, String expectedValue) {
        Constants.clearValues();

        List<ConfDetails> catalogEntries = Constants.getAllValues().stream()
                .filter(conf -> name.equals(conf.getName()))
                .toList();

        assertTrue(Constants.containsKey(name));
        assertEquals(expectedValue, Constants.getString(name));
        assertEquals(1, catalogEntries.size());
        assertEquals(expectedValue, catalogEntries.get(0).getValue());
    }

    @Test
    void constantsV9DoesNotOverrideDefaultsConsolidatedInConstants() {
        Constants.clearValues();
        List<String> valuesBeforeV9Initialization = CONSOLIDATED_DEFAULT_NAMES.stream()
                .map(Constants::getString)
                .toList();

        assertEquals("60", Constants.getString("insertScriptCacheMinutes"));

        String directCallExceptions = Constants.getString("componentsDirectCallExceptionsSystem");
        for (String legacyException : List.of(
                "/components/cestovka/",
                "/components/magzilla/",
                "/components/helpdesk/",
                "/components/mail/",
                "/components/mcalendar/",
                "/components/chat/js.jsp",
                "/components/user/logon.jsp")) {
            assertFalse(directCallExceptions.contains(legacyException));
        }

        long questionTextOccurrences = Stream.of(Constants.getString("xssHtmlAllowedFieldsSystem").split(","))
                .filter("question_text"::equals)
                .count();
        assertEquals(1, questionTextOccurrences);

        ConstantsV9.clearValuesWebJet9();

        for (int i = 0; i < CONSOLIDATED_DEFAULT_NAMES.size(); i++) {
            String name = CONSOLIDATED_DEFAULT_NAMES.get(i);
            assertEquals(valuesBeforeV9Initialization.get(i), Constants.getString(name));
            List<ConfDetails> catalogEntries = Constants.getAllValues().stream()
                    .filter(conf -> name.equals(conf.getName()))
                    .toList();
            assertEquals(1, catalogEntries.size());
            assertEquals(Constants.getString(name), catalogEntries.get(0).getValue());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consolidatedDefaultNames")
    void deleteNameRestoresModernDefault(String name) throws Exception {
        Constants.clearValues();
        String expectedValue = Constants.getString(name);
        String deleteSql = "DELETE FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(deleteSql)).thenReturn(statement);
        Constants.setString(name, "custom-value");

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
                MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class)) {
            dbPool.when(DBPool::getConnection).thenReturn(connection);

            assertTrue(ConfDB.deleteName(name));

            assertTrue(Constants.containsKey(name));
            assertEquals(expectedValue, Constants.getString(name));
        }

        verify(connection).prepareStatement(deleteSql);
        verify(statement).setString(1, name);
        verify(statement).execute();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consolidatedDefaultNames")
    void refreshVariableRestoresModernDefaultAfterRemoteDelete(String name) {
        Constants.clearValues();
        String expectedValue = Constants.getString(name);
        String valueSql = "SELECT value FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        String countSql = "SELECT COUNT(*) FROM " + ConfDB.CONF_TABLE_NAME + " WHERE name=?";
        Constants.setString(name, "custom-value");

        try (MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
            when(query.forString(valueSql, name)).thenReturn(null);
            when(query.forInt(countSql, name)).thenReturn(0);
        })) {
            ConfDB.refreshVariable(name);

            assertEquals(2, queries.constructed().size());
            verify(queries.constructed().get(0)).forString(valueSql, name);
            verify(queries.constructed().get(1)).forInt(countSql, name);
            assertTrue(Constants.containsKey(name));
            assertEquals(expectedValue, Constants.getString(name));
        }
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
        String partialMatchVariable = "partialModuleNameTest";
        Constants.setString(partialMatchVariable, "", "apps.formmail", "Test variable for a partial module-name match");
        List<ConfDetails> formConfiguration = ConfDB.getConfForJsp("form");

        assertTrue(containsConfiguration(formConfiguration, "formmailAllowedRecipients"));
        assertFalse(containsConfiguration(formConfiguration, partialMatchVariable));
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

    private static Stream<Arguments> modernDefaults() {
        return Stream.of(
                Arguments.of("defaultSkin", "webjet9"),
                Arguments.of("auditJpaDisabledEntities", ""),
                Arguments.of("mariaDbDefaultEngine", "InnoDB"),
                Arguments.of("sk.iway.iwcm.qa.AddAction.sendAdminMail.url", "/apps/qa/admin/"),
                Arguments.of("xsrfParamNameExceptionSystem", XSRF_PARAM_NAME_EXCEPTION_SYSTEM),
                Arguments.of("insertScriptCacheMinutes", "60"));
    }

    private static Stream<String> consolidatedDefaultNames() {
        return CONSOLIDATED_DEFAULT_NAMES.stream();
    }
}
