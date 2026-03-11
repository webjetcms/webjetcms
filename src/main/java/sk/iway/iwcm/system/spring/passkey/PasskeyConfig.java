package sk.iway.iwcm.system.spring.passkey;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

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
 * - users (extended with webauthn_user_id column to store the WebAuthn user identifier)
 * - passkey_credentials (passkey_credential_id, credential_id, user_id, webauthn_user_id, ..., rp_id)
 */
@Configuration
public class PasskeyConfig {

    /**
     * Provides a dynamic WebAuthnRelyingPartyOperations that determines rpId from the current HTTP request.
     * This allows passkeys to work across multiple domains automatically.
     */
    @Bean
    public WebAuthnRelyingPartyOperations dynamicWebAuthnRelyingPartyOperations(
            PublicKeyCredentialUserEntityRepository userEntityRepository,
            UserCredentialRepository userCredentialRepository) {

        String rpName = Constants.getString("password_passKeyRpName");
        String allowedOriginsStr = Constants.getString("password_passKeyAllowedOrigins");

        Set<String> allowedOrigins = new HashSet<>();
        if (Tools.isNotEmpty(allowedOriginsStr)) {
            for (String origin : Tools.getTokens(allowedOriginsStr, ",")) {
                allowedOrigins.add(origin.trim());
            }
        }

        return new DynamicWebAuthnRelyingPartyOperations(
                userEntityRepository,
                userCredentialRepository,
                rpName,
                allowedOrigins);
    }
}

