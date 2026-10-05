package sk.iway.iwcm.users.devices;

/** The device's current notice, exposed with its device ID and without the browser token or its hash. */
public record LoginEvent(String id, long createdAt, long expiresAt, String browserName,
    String browserVersion, String operatingSystem, String ipAddress, Long confirmedAt, Long reportedAt) {
}
