package sk.iway.iwcm.system.spring.passkey;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/**
 * Spring configuration for PassKey/WebAuthn support.
 * Provides JDBC-based repositories for credential and user entity storage.
 *
 * Enabled when Constants.getBoolean("password_passKeyEnabled") is true.
 * Uses Spring Security's built-in JDBC implementations backed by tables:
 * - user_entities (id, name, display_name)
 * - user_credentials (credential_id, user_entity_user_id, public_key, ...)
 */
@Configuration
public class PasskeyConfig {

    @Bean
    public JdbcTemplate passkeyJdbcTemplate() {
        DataSource ds = DBPool.getInstance().getDataSource("iwcm");
        return new JdbcTemplate(ds);
    }

    @Bean
    public UserCredentialRepository userCredentialRepository(JdbcTemplate passkeyJdbcTemplate) {
        if (Constants.getBoolean("password_passKeyEnabled") == false) {
            Logger.debug(PasskeyConfig.class, "PassKey is disabled, using no-op UserCredentialRepository");
        }
        return new JdbcUserCredentialRepository(passkeyJdbcTemplate);
    }

    @Bean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(JdbcTemplate passkeyJdbcTemplate) {
        if (Constants.getBoolean("password_passKeyEnabled") == false) {
            Logger.debug(PasskeyConfig.class, "PassKey is disabled, using no-op PublicKeyCredentialUserEntityRepository");
        }
        return new JdbcPublicKeyCredentialUserEntityRepository(passkeyJdbcTemplate);
    }
}
