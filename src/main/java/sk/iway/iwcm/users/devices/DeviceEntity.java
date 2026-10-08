package sk.iway.iwcm.users.devices;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

/** Stores a user's browser recognition and its latest new-device notice. */
@Entity
@Table(name = "user_login_devices")
@Cacheable(false)
@Getter
@Setter
public class DeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @JsonIgnore
    private int userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    @JsonIgnore
    private String tokenHash;

    /** Creation time of the latest notice, refreshed when the browser is detected again. */
    @Column(name = "create_date", nullable = false)
    private Instant createDate;

    /** Latest successful login, independently of the retained notice's browser and IP snapshot. */
    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "browser_name", length = 128)
    private String browserName;

    @Column(name = "browser_version", length = 64)
    private String browserVersion;

    @Column(name = "operating_system", length = 128)
    private String operatingSystem;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Column(name = "confirmation_hash", length = 64)
    @JsonIgnore
    private String confirmationHash;

    @Column(name = "confirmation_expires")
    @JsonIgnore
    private Instant confirmationExpires;

    @Column(name = "code_hash", length = 64)
    @JsonIgnore
    private String codeHash;

    @Column(name = "code_expires")
    @JsonIgnore
    private Instant codeExpires;

    @Column(name = "code_attempts", nullable = false)
    @JsonIgnore
    private int codeAttempts;

    /** Returns the notice deadline without storing a separate expiration column. */
    @Transient
    public long getExpiresAt() {
        return createDate.toEpochMilli() + DeviceService.NOTICE_AGE;
    }
}
