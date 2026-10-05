package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import sk.iway.iwcm.Constants;

/** Verifies that background retention cannot prematurely forget devices with a longer domain policy. */
class AdminDeviceCleanupTest {
    /** The cron cutoff must preserve a 180-day domain policy even when the global policy is 90 days. */
    @Test
    void cronUsesLongestEnabledDomainLifetime() {
        try (var constants = mockStatic(Constants.class); var repositories = mockConstruction(AdminDeviceRepository.class)) {
            constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(90);
            constants.when(Constants::isConstantsAliasSearch).thenReturn(true);
            constants.when(Constants::getAllKeys).thenReturn(List.of("adminNewDeviceMaxAgeDays", "tenant-adminNewDeviceMaxAgeDays"));
            constants.when(() -> Constants.getInt("tenant-adminNewDeviceMaxAgeDays")).thenReturn(180);

            AdminDeviceCleanup.main(new String[0]);

            assertEquals(1, repositories.constructed().size());
            ArgumentCaptor<Long> now = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> cutoff = ArgumentCaptor.forClass(Long.class);
            verify(repositories.constructed().get(0)).cleanup(now.capture(), cutoff.capture());
            assertEquals(Duration.ofDays(180).toMillis(), now.getValue() - cutoff.getValue());
        }
    }

    /** Disabled domain aliases cannot extend the global retention duration. */
    @Test
    void ignoresAliasesWhenAliasSearchIsDisabled() {
        try (var constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(90);
            constants.when(Constants::isConstantsAliasSearch).thenReturn(false);
            constants.when(() -> Constants.getInt("tenant-adminNewDeviceMaxAgeDays")).thenReturn(180);

            assertEquals(90, AdminDeviceCleanup.retentionDays());
            constants.verify(Constants::getAllKeys, never());
            constants.verify(() -> Constants.getInt("tenant-adminNewDeviceMaxAgeDays"), never());
        }
    }

    /** Invalid domain values inherit the same 90-day fallback used during login. */
    @Test
    void invalidAliasFallsBackWithoutIncludingUnrelatedConfigurationKeys() {
        try (var constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(30);
            constants.when(Constants::isConstantsAliasSearch).thenReturn(true);
            constants.when(Constants::getAllKeys).thenReturn(List.of("short-adminNewDeviceMaxAgeDays", "invalid-adminNewDeviceMaxAgeDays", "tenant-unrelated"));
            constants.when(() -> Constants.getInt("short-adminNewDeviceMaxAgeDays")).thenReturn(10);
            constants.when(() -> Constants.getInt("invalid-adminNewDeviceMaxAgeDays")).thenReturn(-1);
            constants.when(() -> Constants.getInt("tenant-unrelated")).thenReturn(365);

            assertEquals(90, AdminDeviceCleanup.retentionDays());
            constants.verify(() -> Constants.getInt("tenant-unrelated"), never());
        }
    }

    /** Excessive alias values use the same maximum lifetime that fits the cookie's integer seconds. */
    @Test
    void clampsAliasLifetimeToSupportedCookieRange() {
        try (var constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(90);
            constants.when(Constants::isConstantsAliasSearch).thenReturn(true);
            constants.when(Constants::getAllKeys).thenReturn(List.of("tenant-adminNewDeviceMaxAgeDays"));
            constants.when(() -> Constants.getInt("tenant-adminNewDeviceMaxAgeDays")).thenReturn(Integer.MAX_VALUE);

            assertEquals(Integer.MAX_VALUE / 86_400, AdminDeviceCleanup.retentionDays());
        }
    }
}
