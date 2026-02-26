package sk.iway.iwcm.system.spring.passkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a WebAuthn user entity (passkey_user_entities table).
 * Maps between WJA admin users and WebAuthn user handles.
 *
 * Each entry represents one admin user who has registered at least one passkey.
 * The webauthnUserId is the opaque user handle used by the WebAuthn protocol.
 */
@Entity
@Table(name = "passkey_user_entities")
@Getter
@Setter
public class PasskeyUserEntityBean {

    @Id
    @Column(name = "passkey_user_entity_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * WebAuthn user handle (Base64URL encoded random bytes).
     * This is the opaque ID used in the WebAuthn protocol (PublicKeyCredentialUserEntity.id).
     */
    @Column(name = "webauthn_user_id", nullable = false, unique = true)
    @NotBlank
    @Size(max = 255)
    private String webauthnUserId;

    /**
     * Username (login name) of the admin user.
     * Maps to PublicKeyCredentialUserEntity.name.
     */
    @Column(name = "name", nullable = false, unique = true)
    @NotBlank
    @Size(max = 255)
    private String name;

    /**
     * Display name of the admin user.
     * Maps to PublicKeyCredentialUserEntity.displayName.
     */
    @Column(name = "display_name")
    @Size(max = 255)
    private String displayName;
}
