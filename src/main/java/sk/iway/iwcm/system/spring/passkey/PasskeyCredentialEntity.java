package sk.iway.iwcm.system.spring.passkey;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

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
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(Adminlog.TYPE_USER_EDIT)
public class PasskeyCredentialEntity {

    @Id
    @Column(name = "passkey_credential_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    /**
     * Human-readable label assigned by the user.
     */
    @Column(name = "label", nullable = false)
    @NotBlank
    @Size(max = 1000)
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "passkey.manage.colLabel", editor = @DataTableColumnEditor(attr = {
        @DataTableColumnEditorAttr(key = "placeholder", value = "passkey.manage.labelPlaceholder")
    }))
    private String label;

    /**
     * WebAuthn credential ID (Base64URL encoded).
     * Used to identify the credential during authentication.
     */
    @Column(name = "credential_id", nullable = false, unique = true)
    @NotBlank
    @Size(max = 255)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String credentialId;

    /**
     * FK to users.user_id - the WebJET user who owns this credential.
     */
    @Column(name = "user_id", nullable = false)
    @NotNull
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Long userId;

    /**
     * COSE-encoded public key, stored as Base64URL string.
     */
    @Lob
    @Column(name = "public_key", nullable = false)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String publicKey;

    /**
     * Signature counter from the authenticator, used to detect cloning.
     */
    @Column(name = "signature_count")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Long signatureCount;

    /**
     * Whether user verification was initialized for this credential.
     */
    @Column(name = "uv_initialized", nullable = false)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Boolean uvInitialized;

    /**
     * Whether this credential is eligible for backup (BE flag).
     */
    @Column(name = "backup_eligible", nullable = false)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Boolean backupEligible;

    /**
     * Whether this credential is currently backed up (BS flag).
     */
    @Column(name = "backup_state", nullable = false)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Boolean backupState;

    /**
     * Comma-separated list of authenticator transports (e.g. "internal,hybrid").
     */
    @Column(name = "authenticator_transports")
    @Size(max = 1000)
    @DataTableColumn(inputType = DataTableColumnType.DISABLED, className = "hideOnCreate", title = "passkey.manage.colTransports",
        editor = { @DataTableColumnEditor(attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
    private String authenticatorTransports;

    /**
     * Public key credential type (typically "public-key").
     */
    @Column(name = "public_key_credential_type")
    @Size(max = 100)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String publicKeyCredentialType;

    /**
     * Raw attestation object from registration, stored as Base64URL string.
     */
    @Lob
    @Column(name = "attestation_object")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String attestationObject;

    /**
     * Attestation client data JSON from registration.
     */
    @Lob
    @Column(name = "attestation_client_data_json")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String attestationClientDataJson;

    /**
     * When this credential was created.
     */
    @Column(name = "created")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, className = "hideOnCreate", title = "passkey.manage.colCreated",
        renderFormat = "dt-format-date-time",
        editor = { @DataTableColumnEditor(attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
    private Instant created;

    /**
     * When this credential was last used for authentication.
     */
    @Column(name = "last_used")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, className = "hideOnCreate",title = "passkey.manage.colLastUsed",
        renderFormat = "dt-format-date-time",
        editor = { @DataTableColumnEditor(attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
    private Instant lastUsed;

    /**
     * The Relying Party ID (domain) for which this credential was registered.
     * WebAuthn credentials are domain-bound - a credential registered for one domain
     * cannot be used on a different domain.
     */
    @Column(name = "rp_id")
    @Size(max = 255)
    @DataTableColumn(inputType = DataTableColumnType.DISABLED, className = "hideOnCreate", title = "passkey.manage.colDomain",
        editor = { @DataTableColumnEditor(attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
    private String rpId;
}
