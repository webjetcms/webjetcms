package sk.iway.iwcm.components.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.system.ConfDB;

class ConfigurationControllerTest {

    @Test
    void checksConfigurationKeyVisibilityForItemOperations() {
        Identity user = mock(Identity.class);
        ConfigurationController controller = createController(user);
        ConfDetailsDto visible = createConfDetails("visibleConfigurationKey");
        ConfDetailsDto hidden = createConfDetails("hiddenConfigurationKey");

        try (MockedStatic<ConfDB> confDb = mockStatic(ConfDB.class)) {
            confDb.when(() -> ConfDB.isKeyVisibleToUser(user, visible.getName())).thenReturn(true);
            confDb.when(() -> ConfDB.isKeyVisibleToUser(user, hidden.getName())).thenReturn(false);

            assertTrue(controller.checkItemPerms(visible, -1L));
            assertFalse(controller.checkItemPerms(hidden, 1L));
        }
    }

    @Test
    void rejectsItemOperationsWithoutUserOrConfiguration() {
        ConfDetailsDto confDetailsDto = createConfDetails("configurationKey");

        assertFalse(createController(null).checkItemPerms(confDetailsDto, -1L));
        assertFalse(createController(mock(Identity.class)).checkItemPerms(null, -1L));
    }

    private static ConfDetailsDto createConfDetails(String name) {
        ConfDetailsDto confDetailsDto = new ConfDetailsDto();
        confDetailsDto.setName(name);
        return confDetailsDto;
    }

    private static ConfigurationController createController(Identity user) {
        return new ConfigurationController(mock(ConfigurationService.class)) {
            @Override
            public Identity getUser() {
                return user;
            }
        };
    }
}
