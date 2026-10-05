package sk.iway.iwcm.users.devices;

/** A login snapshot safe to expose to its owner, without the device token or its hash. */
public record AdminLoginEvent(String id, long createdAt, long expiresAt, String browserName,
    String browserVersion, String operatingSystem, String ipAddress, Long confirmedAt, Long reportedAt) {
}
