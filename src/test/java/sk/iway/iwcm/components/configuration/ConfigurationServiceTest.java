package sk.iway.iwcm.components.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.cluster.ClusterDB;

class ConfigurationServiceTest {

    @Test
    void getAllShowsCurrentAndDatabaseValuesWhileDetailsRemainUnformatted() {
        String changedName = "configurationServiceChangedValueTest";
        String sameName = "configurationServiceSameValueTest";
        String encryptedName = "configurationServiceEncryptedValueTest";
        String linkTypeName = "linkType";
        String databaseValue = "database-value";
        String currentValue = "current-value";
        String sameValue = "same-value";
        String encryptedValue = "encrypted:0123456789012345678901234567890123456789";
        String decryptedValue = "decrypted-value";
        Identity user = mock(Identity.class);

        ConfDetails changed = new ConfDetails(changedName, databaseValue);
        ConfDetails same = new ConfDetails(sameName, sameValue);
        ConfDetails encrypted = new ConfDetails(encryptedName, encryptedValue);
        ConfDetails linkType = new ConfDetails(linkTypeName, "html");
        List<ConfDetails> databaseData = List.of(changed, same, encrypted, linkType);

        ConfDetails defaults = new ConfDetails(changedName, "default-value");
        defaults.setDescription("description");
        List<ConfDetails> defaultData = List.of(defaults);

        ConfigurationService service = new ConfigurationService(new ConfDetailsMapper());

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class)) {
            confDb.when(ConfDB::getConfig).thenReturn(databaseData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, defaultData)).thenReturn(defaultData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, databaseData)).thenReturn(databaseData);
            confDb.when(() -> ConfDB.normalizeRuntimeValue(changedName, databaseValue)).thenReturn(databaseValue);
            confDb.when(() -> ConfDB.normalizeRuntimeValue(sameName, sameValue)).thenReturn(sameValue);
            confDb.when(() -> ConfDB.normalizeRuntimeValue(encryptedName, encryptedValue)).thenReturn(decryptedValue);
            confDb.when(() -> ConfDB.normalizeRuntimeValue(linkTypeName, "html")).thenReturn("2");
            constants.when(Constants::getAllValues).thenReturn(defaultData);
            constants.when(() -> Constants.getString(changedName)).thenReturn(currentValue);
            constants.when(() -> Constants.getString(sameName)).thenReturn(sameValue);
            constants.when(() -> Constants.getString(encryptedName)).thenReturn(decryptedValue);
            constants.when(() -> Constants.getString(linkTypeName)).thenReturn("2");

            List<ConfDetailsDto> displayed = service.getAll(user);

            assertEquals(databaseValue, displayed.get(0).getValue());
            assertEquals(currentValue, displayed.get(0).getDisplayValue());
            assertTrue(displayed.get(0).isRuntimeValueDifferent());
            assertEquals("default-value", displayed.get(0).getOldValue());
            assertEquals("description", displayed.get(0).getDescription());
            assertTrue(displayed.get(0).isDatabaseValuePresent());
            assertEquals(sameValue, displayed.get(1).getDisplayValue());
            assertFalse(displayed.get(1).isRuntimeValueDifferent());
            assertEquals(encryptedValue, displayed.get(2).getDisplayValue());
            assertFalse(displayed.get(2).isRuntimeValueDifferent());
            assertEquals("html", displayed.get(3).getDisplayValue());
            assertFalse(displayed.get(3).isRuntimeValueDifferent());
            long changedId = displayed.get(0).getId();
            assertTrue(changedId > 0 && changedId <= 9_007_199_254_740_991L);

            constants.when(() -> Constants.getString(encryptedName)).thenReturn("changed-secret");
            ConfDetailsDto changedEncrypted = service.getAll(user).get(2);
            assertEquals("********", changedEncrypted.getDisplayValue());
            assertTrue(changedEncrypted.isRuntimeValueDifferent());

            ConfDetails inserted = new ConfDetails("configurationServiceAInsertedTest", "inserted-value");
            List<ConfDetails> shiftedDatabaseData = List.of(inserted, changed, same, encrypted, linkType);
            confDb.when(ConfDB::getConfig).thenReturn(shiftedDatabaseData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, shiftedDatabaseData)).thenReturn(shiftedDatabaseData);

            ConfDetailsDto detail = service.getOne(user, changedId);
            assertEquals(changedId, detail.getId());
            assertEquals(changedName, detail.getName());
            assertEquals(databaseValue, detail.getValue());
            assertEquals(currentValue, detail.getDisplayValue());
            assertTrue(detail.isRuntimeValueDifferent());
            assertEquals(databaseValue, service.getAutocompleteDetail(user, changedName).getValue());

            ConfDetailsDto created = service.getOne(user, -1);
            assertNull(created.getName());
            assertNull(created.getValue());
            assertNull(service.getOne(user, Long.MAX_VALUE));
        }
    }

    @Test
    void catalogSupportsChangedAllAndHierarchicalModuleViews() {
        Identity user = mock(Identity.class);
        Date defaultDate = new Date(1000L);
        Date databaseDate = new Date(2000L);
        Date customDate = new Date(3000L);

        ConfDetails alphaDefault = conf(
            "configurationCatalogAlphaTest",
            "alpha-default",
            "apps.gallery;security.oauth2",
            "Alpha default description",
            defaultDate
        );
        ConfDetails betaDefault = conf(
            "configurationCatalogBetaTest",
            "beta-default",
            "security.oauth2.github;security.oauth2",
            "Beta default description",
            defaultDate
        );
        ConfDetails alphaDatabase = conf(
            alphaDefault.getName(),
            "alpha-database",
            "ignored.database.module",
            "Ignored database description",
            databaseDate
        );
        ConfDetails customDatabase = conf(
            "configurationCatalogCustomTest",
            "custom-database",
            null,
            null,
            customDate
        );

        List<ConfDetails> defaultData = List.of(alphaDefault, betaDefault);
        List<ConfDetails> databaseData = List.of(alphaDatabase, customDatabase);
        ConfigurationService service = new ConfigurationService(new ConfDetailsMapper());

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class)) {
            constants.when(Constants::getAllValues).thenReturn(defaultData);
            confDb.when(ConfDB::getConfig).thenReturn(databaseData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, defaultData)).thenReturn(defaultData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, databaseData)).thenReturn(databaseData);
            stubDisplayValue(constants, confDb, alphaDefault.getName(), "alpha-database");
            stubDisplayValue(constants, confDb, betaDefault.getName(), "beta-default");
            stubDisplayValue(constants, confDb, customDatabase.getName(), "custom-database");

            assertEquals(
                List.of(alphaDefault.getName(), customDatabase.getName()),
                names(service.getAll(user))
            );
            assertEquals(
                List.of(alphaDefault.getName(), customDatabase.getName()),
                names(service.getAll(user, null, null))
            );
            assertEquals(
                List.of(alphaDefault.getName(), customDatabase.getName()),
                names(service.getAll(user, "unsupported", "security"))
            );

            List<ConfDetailsDto> all = service.getAll(user, ConfigurationService.VIEW_ALL, null);
            assertEquals(
                List.of(alphaDefault.getName(), betaDefault.getName(), customDatabase.getName()),
                names(all)
            );
            Map<String, ConfDetailsDto> allByName = all.stream()
                .collect(Collectors.toMap(ConfDetailsDto::getName, Function.identity()));

            ConfDetailsDto alpha = allByName.get(alphaDefault.getName());
            assertEquals("alpha-database", alpha.getValue());
            assertEquals("alpha-default", alpha.getOldValue());
            assertEquals(alphaDefault.getDescription(), alpha.getDescription());
            assertEquals(alphaDefault.getModules(), alpha.getModules());
            assertEquals(databaseDate, alpha.getDateChanged());
            assertTrue(alpha.isDatabaseValuePresent());

            ConfDetailsDto beta = allByName.get(betaDefault.getName());
            assertEquals("beta-default", beta.getValue());
            assertEquals("beta-default", beta.getOldValue());
            assertEquals(betaDefault.getDescription(), beta.getDescription());
            assertNull(beta.getDateChanged());
            assertFalse(beta.isDatabaseValuePresent());

            ConfDetailsDto custom = allByName.get(customDatabase.getName());
            assertEquals("custom-database", custom.getValue());
            assertNull(custom.getOldValue());
            assertNull(custom.getDescription());
            assertNull(custom.getModules());
            assertEquals(customDate, custom.getDateChanged());
            assertTrue(custom.isDatabaseValuePresent());

            assertEquals(
                List.of(alphaDefault.getName(), betaDefault.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_MODULE, "security"))
            );
            assertEquals(
                List.of(alphaDefault.getName(), betaDefault.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_MODULE, "security.oauth2"))
            );
            assertEquals(
                List.of(betaDefault.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_MODULE, "security.oauth2.github"))
            );
            assertEquals(
                List.of(alphaDefault.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_MODULE, "apps"))
            );
            assertTrue(service.getAll(user, ConfigurationService.VIEW_MODULE, "security.oauth").isEmpty());
            assertTrue(service.getAll(user, ConfigurationService.VIEW_MODULE, "invalid path").isEmpty());
            assertTrue(service.getAll(user, ConfigurationService.VIEW_MODULE, null).isEmpty());
            assertEquals(
                List.of("apps.gallery", "security.oauth2", "security.oauth2.github"),
                service.getVisibleModulePaths(user)
            );

            ConfDetailsDto betaDetail = service.getOne(user, beta.getId());
            assertNotNull(betaDetail);
            assertEquals(betaDefault.getName(), betaDetail.getName());
            assertFalse(betaDetail.isDatabaseValuePresent());

            ConfDetailsDto imported = new ConfDetailsDto();
            imported.setName(betaDefault.getName());
            List<ConfDetailsDto> importMatches = service.findConfDetailsBy("name", imported, user);
            assertEquals(1, importMatches.size());
            assertEquals(betaDefault.getName(), importMatches.get(0).getName());
        }
    }

    @Test
    void customViewCombinesDatabaseOnlyAndInstallPrefixedValuesWithoutDuplicates() {
        Identity user = mock(Identity.class);
        String installName = "customerPrefix";
        ConfDetails prefixedDefault = conf(
            installName + "DefaultTest",
            "default-value",
            "customer.defaults",
            "Prefixed default",
            new Date(1000L)
        );
        ConfDetails prefixedOverrideDefault = conf(
            installName + "OverrideTest",
            "override-default",
            "customer.overrides",
            "Prefixed override",
            new Date(1000L)
        );
        ConfDetails unrelatedDefault = conf(
            "configurationCustomUnrelatedDefaultTest",
            "unrelated-default",
            "system.config",
            "Unrelated default",
            new Date(1000L)
        );
        ConfDetails prefixedOverride = conf(
            prefixedOverrideDefault.getName(),
            "override-database",
            null,
            null,
            new Date(2000L)
        );
        ConfDetails databaseOnly = conf(
            "configurationCustomDatabaseOnlyTest",
            "database-only",
            null,
            null,
            new Date(2000L)
        );
        ConfDetails prefixedDatabaseOnly = conf(
            installName + "DatabaseOnlyTest",
            "prefixed-database-only",
            null,
            null,
            new Date(2000L)
        );
        List<ConfDetails> defaultData = List.of(prefixedDefault, prefixedOverrideDefault, unrelatedDefault);
        List<ConfDetails> databaseData = List.of(prefixedOverride, databaseOnly, prefixedDatabaseOnly);
        ConfigurationService service = new ConfigurationService(new ConfDetailsMapper());

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class)) {
            constants.when(Constants::getAllValues).thenReturn(defaultData);
            constants.when(Constants::getInstallName).thenReturn(installName);
            confDb.when(ConfDB::getConfig).thenReturn(databaseData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, defaultData)).thenReturn(defaultData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, databaseData)).thenReturn(databaseData);
            stubDisplayValue(constants, confDb, prefixedDefault.getName(), "default-value");
            stubDisplayValue(constants, confDb, prefixedOverride.getName(), "override-database");
            stubDisplayValue(constants, confDb, unrelatedDefault.getName(), "unrelated-default");
            stubDisplayValue(constants, confDb, databaseOnly.getName(), "database-only");
            stubDisplayValue(constants, confDb, prefixedDatabaseOnly.getName(), "prefixed-database-only");

            List<ConfDetailsDto> custom = service.getAll(user, ConfigurationService.VIEW_CUSTOM, null);
            assertEquals(
                List.of(
                    prefixedDefault.getName(),
                    prefixedOverride.getName(),
                    databaseOnly.getName(),
                    prefixedDatabaseOnly.getName()
                ),
                names(custom)
            );
            assertEquals(4, custom.size());

            Map<String, ConfDetailsDto> customByName = custom.stream()
                .collect(Collectors.toMap(ConfDetailsDto::getName, Function.identity()));
            assertFalse(customByName.get(prefixedDefault.getName()).isDatabaseValuePresent());
            assertEquals("override-database", customByName.get(prefixedOverride.getName()).getValue());
            assertEquals("override-default", customByName.get(prefixedOverride.getName()).getOldValue());
            assertTrue(customByName.get(prefixedOverride.getName()).isDatabaseValuePresent());
            assertTrue(customByName.get(databaseOnly.getName()).isDatabaseValuePresent());
            assertTrue(customByName.get(prefixedDatabaseOnly.getName()).isDatabaseValuePresent());

            constants.when(Constants::getInstallName).thenReturn("");
            assertEquals(
                List.of(databaseOnly.getName(), prefixedDatabaseOnly.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_CUSTOM, null))
            );
        }
    }

    @Test
    void catalogAppliesPermissionsToEveryViewAndLookup() {
        Identity user = mock(Identity.class);
        ConfDetails allowedDefault = conf(
            "configurationPermissionAllowedTest",
            "allowed-default",
            "visible.branch",
            "Allowed",
            new Date(1000L)
        );
        ConfDetails deniedDefault = conf(
            "configurationPermissionDeniedTest",
            "denied-default",
            "hidden.branch",
            "Denied",
            new Date(1000L)
        );
        ConfDetails allowedDatabase = conf(
            allowedDefault.getName(),
            "allowed-database",
            null,
            null,
            new Date(2000L)
        );
        ConfDetails deniedDatabase = conf(
            deniedDefault.getName(),
            "denied-database",
            null,
            null,
            new Date(2000L)
        );
        ConfDetails allowedDatabaseOnly = conf(
            "allowedStandaloneDatabaseOnlyTest",
            "allowed-database-only",
            null,
            null,
            new Date(2000L)
        );
        ConfDetails deniedDatabaseOnly = conf(
            "deniedStandaloneDatabaseOnlyTest",
            "denied-database-only",
            null,
            null,
            new Date(2000L)
        );
        List<ConfDetails> defaultData = List.of(allowedDefault, deniedDefault);
        List<ConfDetails> visibleDefaults = List.of(allowedDefault);
        List<ConfDetails> databaseData = List.of(
            allowedDatabase,
            deniedDatabase,
            allowedDatabaseOnly,
            deniedDatabaseOnly
        );
        List<ConfDetails> visibleDatabase = List.of(allowedDatabase, allowedDatabaseOnly);
        ConfigurationService service = new ConfigurationService(new ConfDetailsMapper());

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class)) {
            constants.when(Constants::getAllValues).thenReturn(defaultData);
            constants.when(Constants::getInstallName).thenReturn("configurationPermission");
            confDb.when(ConfDB::getConfig).thenReturn(databaseData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, defaultData)).thenReturn(visibleDefaults);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, databaseData)).thenReturn(visibleDatabase);
            stubDisplayValue(constants, confDb, allowedDefault.getName(), "allowed-database");
            stubDisplayValue(constants, confDb, allowedDatabaseOnly.getName(), "allowed-database-only");

            assertEquals(
                List.of(allowedDefault.getName(), allowedDatabaseOnly.getName()),
                names(service.getAll(user))
            );
            assertEquals(
                List.of(allowedDefault.getName(), allowedDatabaseOnly.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_ALL, null))
            );
            assertEquals(
                List.of(allowedDefault.getName(), allowedDatabaseOnly.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_CUSTOM, null))
            );
            assertEquals(
                List.of(allowedDefault.getName()),
                names(service.getAll(user, ConfigurationService.VIEW_MODULE, "visible"))
            );
            assertTrue(service.getAll(user, ConfigurationService.VIEW_MODULE, "hidden").isEmpty());
            assertEquals(List.of("visible.branch"), service.getVisibleModulePaths(user));

            long deniedId = new ConfDetailsMapper().entityListToDtoList(List.of(deniedDefault)).get(0).getId();
            assertNull(service.getOne(user, deniedId));

            ConfDetailsDto deniedImport = new ConfDetailsDto();
            deniedImport.setName(deniedDefault.getName());
            assertTrue(service.findConfDetailsBy("name", deniedImport, user).isEmpty());

            ConfDetailsDto allowedImport = new ConfDetailsDto();
            allowedImport.setName(allowedDefault.getName());
            assertEquals(1, service.findConfDetailsBy("name", allowedImport, user).size());
        }
    }

    @Test
    void deleteOnlyResetsVisibleDatabaseOverrides() {
        Identity user = mock(Identity.class);
        String hiddenName = "configurationDeleteHiddenTest";
        String defaultOnlyName = "configurationDeleteDefaultOnlyTest";
        String storedName = "configurationDeleteStoredTest";
        String localStoredName = "licenseExpiryDate";
        ConfigurationService service = new ConfigurationService(new ConfDetailsMapper());

        try (MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class);
                MockedStatic<ClusterDB> clusterDb = mockStatic(ClusterDB.class)) {
            confDb.when(() -> ConfDB.isKeyVisibleToUser(user, hiddenName)).thenReturn(false);
            confDb.when(() -> ConfDB.isKeyVisibleToUser(user, defaultOnlyName)).thenReturn(true);
            confDb.when(() -> ConfDB.isKeyVisibleToUser(user, storedName)).thenReturn(true);
            confDb.when(() -> ConfDB.isKeyVisibleToUser(user, localStoredName)).thenReturn(true);
            confDb.when(() -> ConfDB.getVariable(defaultOnlyName)).thenReturn(null);
            confDb.when(() -> ConfDB.getVariable(storedName)).thenReturn(new ConfDetails(storedName, "stored"));
            confDb.when(() -> ConfDB.getVariable(localStoredName)).thenReturn(new ConfDetails(localStoredName, "stored"));
            confDb.when(() -> ConfDB.deleteName(storedName)).thenReturn(true);
            confDb.when(() -> ConfDB.deleteName(localStoredName)).thenReturn(true);
            confDb.when(() -> ConfDB.isOnlyLocalConfig(storedName)).thenReturn(false);
            confDb.when(() -> ConfDB.isOnlyLocalConfig(localStoredName)).thenReturn(true);

            assertFalse(service.deleteConfDetails(user, hiddenName));
            assertFalse(service.deleteConfDetails(user, defaultOnlyName));
            assertTrue(service.deleteConfDetails(user, storedName));
            assertTrue(service.deleteConfDetails(user, localStoredName));

            confDb.verify(() -> ConfDB.getVariable(hiddenName), never());
            confDb.verify(() -> ConfDB.deleteName(hiddenName), never());
            confDb.verify(() -> ConfDB.deleteName(defaultOnlyName), never());
            confDb.verify(() -> ConfDB.deleteName(storedName), times(1));
            confDb.verify(() -> ConfDB.deleteName(localStoredName), times(1));
            clusterDb.verify(() -> ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-" + storedName), times(1));
            clusterDb.verify(() -> ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-" + localStoredName), never());
        }
    }

    @Test
    void saveTemporaryUpdatesRuntimeValueWithoutPersistenceOrClusterRefresh() throws Exception {
        String name = "configurationServiceTemporaryTest";
        String value = "temporary-value";
        ConfDetailsMapper mapper = mock(ConfDetailsMapper.class);
        ConfigurationService service = new ConfigurationService(mapper);
        ConfDetailsDto dto = new ConfDetailsDto();
        dto.setId(123L);
        dto.setName(name);
        dto.setValue(value);
        dto.setEncrypt(true);
        dto.setDatePrepared(new Date(123456789L));
        dto.setTemporary(true);

        try (MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class);
                MockedStatic<ClusterDB> clusterDb = mockStatic(ClusterDB.class)) {
            ConfDetailsDto saved = service.save(mock(Identity.class), dto);

            assertSame(dto, saved);
            assertEquals(value, saved.getValue());
            confDb.verify(() -> ConfDB.setRuntimeValue(name, value));
            clusterDb.verifyNoInteractions();
            verifyNoInteractions(mapper);
        }
    }

    private static ConfDetails conf(String name, String value, String modules, String description, Date dateChanged) {
        ConfDetails conf = new ConfDetails(name, value, dateChanged);
        conf.setModules(modules);
        conf.setDescription(description);
        return conf;
    }

    private static List<String> names(List<ConfDetailsDto> configurations) {
        return configurations.stream().map(ConfDetailsDto::getName).collect(Collectors.toList());
    }

    private static void stubDisplayValue(MockedStatic<Constants> constants, MockedStatic<ConfDB> confDb,
            String name, String value) {
        constants.when(() -> Constants.getString(name)).thenReturn(value);
        confDb.when(() -> ConfDB.normalizeRuntimeValue(name, value)).thenReturn(value);
    }
}
