package sk.iway.iwcm.components.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Date;
import java.util.List;

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
        String databaseValue = "database-value";
        String currentValue = "current-value";
        String sameValue = "same-value";
        String encryptedValue = "encrypted:0123456789012345678901234567890123456789";
        String decryptedValue = "decrypted-value";
        Identity user = mock(Identity.class);

        ConfDetails changed = new ConfDetails(changedName, databaseValue);
        ConfDetails same = new ConfDetails(sameName, sameValue);
        ConfDetails encrypted = new ConfDetails(encryptedName, encryptedValue);
        List<ConfDetails> databaseData = List.of(changed, same, encrypted);

        ConfDetails defaults = new ConfDetails(changedName, "default-value");
        defaults.setDescription("description");

        ConfigurationService service = new ConfigurationService(new ConfDetailsMapper());

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class)) {
            confDb.when(ConfDB::getConfig).thenReturn(databaseData);
            confDb.when(() -> ConfDB.filterConfDetailsByPerms(user, databaseData)).thenReturn(databaseData);
            confDb.when(() -> ConfDB.tryDecrypt(databaseValue)).thenReturn(databaseValue);
            confDb.when(() -> ConfDB.tryDecrypt(sameValue)).thenReturn(sameValue);
            confDb.when(() -> ConfDB.tryDecrypt(encryptedValue)).thenReturn(decryptedValue);
            constants.when(Constants::getAllValues).thenReturn(List.of(defaults));
            constants.when(() -> Constants.getString(changedName)).thenReturn(currentValue);
            constants.when(() -> Constants.getString(sameName)).thenReturn(sameValue);
            constants.when(() -> Constants.getString(encryptedName)).thenReturn(decryptedValue);

            List<ConfDetailsDto> displayed = service.getAll(user);

            assertEquals(databaseValue, displayed.get(0).getValue());
            assertEquals(currentValue, displayed.get(0).getDisplayValue());
            assertTrue(displayed.get(0).isRuntimeValueDifferent());
            assertEquals("default-value", displayed.get(0).getOldValue());
            assertEquals("description", displayed.get(0).getDescription());
            assertEquals(sameValue, displayed.get(1).getDisplayValue());
            assertFalse(displayed.get(1).isRuntimeValueDifferent());
            assertEquals(encryptedValue, displayed.get(2).getDisplayValue());
            assertFalse(displayed.get(2).isRuntimeValueDifferent());
            long changedId = displayed.get(0).getId();
            assertTrue(changedId > 0 && changedId <= 9_007_199_254_740_991L);

            constants.when(() -> Constants.getString(encryptedName)).thenReturn("changed-secret");
            ConfDetailsDto changedEncrypted = service.getAll(user).get(2);
            assertEquals("********", changedEncrypted.getDisplayValue());
            assertTrue(changedEncrypted.isRuntimeValueDifferent());

            ConfDetails inserted = new ConfDetails("configurationServiceAInsertedTest", "inserted-value");
            List<ConfDetails> shiftedDatabaseData = List.of(inserted, changed, same, encrypted);
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

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class);
                MockedStatic<ClusterDB> clusterDb = mockStatic(ClusterDB.class)) {
            ConfDetailsDto saved = service.save(mock(Identity.class), dto);

            assertSame(dto, saved);
            assertEquals(value, saved.getValue());
            constants.verify(() -> Constants.setString(name, value));
            confDb.verifyNoInteractions();
            clusterDb.verifyNoInteractions();
            verifyNoInteractions(mapper);
        }
    }
}
