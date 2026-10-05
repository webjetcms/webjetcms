package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/** Verifies that background cleanup uses the configured device recognition lifetime. */
class DeviceCleanupTest {
    /** The cron cutoff follows the configured lifetime instead of a hardcoded default. */
    @Test
    void cronUsesConfiguredLifetime() {
        DeviceService devices = mock(DeviceService.class);
        try (var constants = mockStatic(Constants.class); var tools = mockStatic(Tools.class)) {
            tools.when(() -> Tools.getSpringBean("deviceService", DeviceService.class)).thenReturn(devices);
            constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(180);

            DeviceCleanup.main(new String[0]);

            ArgumentCaptor<Long> now = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> cutoff = ArgumentCaptor.forClass(Long.class);
            verify(devices).cleanup(now.capture(), cutoff.capture());
            assertEquals(Duration.ofDays(180).toMillis(), now.getValue() - cutoff.getValue());
        }
    }

}
