package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

            long earliestCutoff = System.currentTimeMillis() - Duration.ofDays(180).toMillis();
            DeviceCleanup.main(new String[0]);
            long latestCutoff = System.currentTimeMillis() - Duration.ofDays(180).toMillis();

            ArgumentCaptor<Long> cutoff = ArgumentCaptor.forClass(Long.class);
            verify(devices).cleanup(cutoff.capture());
            assertTrue(cutoff.getValue() >= earliestCutoff && cutoff.getValue() <= latestCutoff);
        }
    }

}
