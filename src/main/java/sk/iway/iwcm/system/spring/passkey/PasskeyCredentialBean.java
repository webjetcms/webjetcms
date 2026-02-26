package sk.iway.iwcm.system.spring.passkey;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a WebAuthn credential record (passkey_credentials table).
 * Each entry represents one registered passkey (authenticator) for a user.
 *
 * Stores all data needed by Spring Security's CredentialRecord interface:
 * credential ID, public key, signature count, flags, transports, attestation data, etc.
 */
@Entity
@Table(name = "passkey_credentials")
@Getter
@Setter
public class PasskeyCredentialBean {

    @Id
    @Column(name = "passkey_credential_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * WebAuthn credential ID (Base64URL encoded).
     * Used to identify the credential during authentication.
     */
    @Column(name = "credential_id", nullable = false, unique = true)
    @NotBlank
    @Size(max = 255)
    private String credentialId;

    /**
     * Reference to the user entity that owns this credential.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_entity_id", nullable = false)
    @NotNull
    private PasskeyUserEntityBean userEntity;

    /**
     * WebAuthn user handle (Base64URL encoded), denormalized for fast lookup.
     * Must match userEntity.webauthnUserId.
     */
    @Column(name = "webauthn_user_id", nullable = false)
    @NotBlank
    @Size(max = 255)
    private String webauthnUserId;

    /**
     * COSE-encoded public key bytes.
     */
    @Column(name = "public_key", nullable = false)
    @Lob
    private byte[] publicKey;

    /**
     * Signature counter from the authenticator, used to detect cloning.
     */
    @Column(name = "signature_count")
    private Long signatureCount;

    /**
     * Whether user verification was initialized for this credential.
     */
    @Column(name = "uv_initialized", nullable = false)
    private Boolean uvInitialized;

    /**
     * Whether this credential is eligible for backup (BE flag).
     */
    @Column(name = "backup_eligible", nullable = false)
    private Boolean backupEligible;

    /**
     * Whether this credential is currently backed up (BS flag).
     */
    @Column(name = "backup_state", nullable = false)
    private Boolean backupState;

    /**
     * Comma-separated list of authenticator transports (e.g. "internal,hybrid").
     */
    @Column(name = "authenticator_transports")
    @Size(max = 1000)
    private String authenticatorTransports;

    /**
     * Public key credential type (typically "public-key").
     */
    @Column(name = "public_key_credential_type")
    @Size(max = 100)
    private String publicKeyCredentialType;

    /**
     * Raw attestation object bytes from registration.
     */
    @Column(name = "attestation_object")
    @Lob
    private byte[] attestationObject;

    /**
     * Raw attestation client data JSON bytes from registration.
     */
    @Column(name = "attestation_client_data_json")
    @Lob
    private byte[] attestationClientDataJson;

    /**
     * When this credential was created.
     */
    @Column(name = "created")
    private Instant created;

    /**
     * When this credential was last used for authentication.
     */
    @Column(name = "last_used")
    private Instant lastUsed;

    /**
     * Human-readable label assigned by the user.
     */
    @Column(name = "label", nullable = false)
    @NotBlank
    @Size(max = 1000)
    private String label;
}
