package sk.iway.iwcm.users.devices;

import java.time.Duration;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/** Daily cron task removing expired device recognition and login event history. */
public class DeviceCleanup {
    private DeviceCleanup() {
    }

    /** Runs retention cleanup even while detection is disabled, without logging account identifiers. */
    public static void main(String[] args) {
        long now = System.currentTimeMillis();
        try {
            Tools.getSpringBean("deviceService", DeviceService.class).cleanup(now, now - Duration.ofDays(AdminDeviceService.maxAgeDays()).toMillis());
        } catch (RuntimeException exception) {
            Logger.error(DeviceCleanup.class, "Could not clean up expired login device history");
        }
    }

}
