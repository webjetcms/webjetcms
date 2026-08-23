package sk.iway.iwcm.components.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.cluster.ClusterDB;

class ConfigurationServiceTest {

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
