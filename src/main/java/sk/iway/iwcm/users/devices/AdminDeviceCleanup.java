package sk.iway.iwcm.users.devices;

import java.time.Duration;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;

/** Daily cron task removing expired device recognition and login event history. */
public class AdminDeviceCleanup {
    private AdminDeviceCleanup() {
    }

    /** Runs retention cleanup even while detection is disabled, without logging account identifiers. */
    public static void main(String[] args) {
        long now = System.currentTimeMillis();
        try {
            new AdminDeviceRepository().cleanup(now, now - Duration.ofDays(retentionDays()).toMillis());
        } catch (RuntimeException exception) {
            Logger.error(AdminDeviceCleanup.class, "Could not clean up expired login device history");
        }
    }

    /** Retains devices for the longest enabled domain policy while login applies the current account's policy. */
    static int retentionDays() {
        int days = AdminDeviceService.maxAgeDays();
        if (Constants.isConstantsAliasSearch()) {
            for (String key : Constants.getAllKeys()) {
                if (key.endsWith("-adminNewDeviceMaxAgeDays")) {
                    days = Math.max(days, AdminDeviceService.maxAgeDays(Constants.getInt(key)));
                }
            }
        }
        return days;
    }
}
