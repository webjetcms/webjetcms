package sk.iway.iwcm.system.spring.passkey;

import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for PassKey/WebAuthn support.
 *
 * The main beans are provided by @Component annotated classes:
 * - JpaPublicKeyCredentialUserEntityAdapter - implements PublicKeyCredentialUserEntityRepository
 * - JpaUserCredentialRepositoryAdapter - implements UserCredentialRepository
 * - WebjetWebAuthnUserDetailsService - UserDetailsService for WebAuthn authentication
 *
 * These adapters use JPA/Spring DATA repositories (PasskeyUserEntityRepository,
 * PasskeyCredentialRepository) to persist WebAuthn data, consistent with the
 * rest of the project's data access patterns.
 *
 * Enabled when Constants.getBoolean("password_passKeyEnabled") is true.
 *
 * Database tables:
 * - passkey_user_entities (passkey_user_entity_id, webauthn_user_id, name, display_name)
 * - passkey_credentials (passkey_credential_id, credential_id, user_entity_id, webauthn_user_id, ...)
 */
@Configuration
public class PasskeyConfig {
    // All beans are now provided via @Component annotations on adapter classes.
    // This class is kept for documentation and as a @Configuration marker for the package.
}

