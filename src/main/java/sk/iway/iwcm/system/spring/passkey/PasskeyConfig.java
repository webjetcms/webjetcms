package sk.iway.iwcm.system.spring.passkey;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
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
        if (ds == null) {
            Logger.error(PasskeyConfig.class, "PassKey: DataSource 'iwcm' is NULL! JDBC repositories will not work.");
        } else {
            Logger.info(PasskeyConfig.class, "PassKey: DataSource obtained successfully: " + ds.getClass().getName());
        }
        return new JdbcTemplate(ds);
    }

    @Bean
    public UserCredentialRepository userCredentialRepository(JdbcTemplate passkeyJdbcTemplate) {
        if (Constants.getBoolean("password_passKeyEnabled") == false) {
            Logger.debug(PasskeyConfig.class, "PassKey is disabled, using no-op UserCredentialRepository");
        }
        JdbcUserCredentialRepository delegate = new JdbcUserCredentialRepository(passkeyJdbcTemplate);
        return new LoggingUserCredentialRepository(delegate);
    }

    @Bean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(JdbcTemplate passkeyJdbcTemplate) {
        if (Constants.getBoolean("password_passKeyEnabled") == false) {
            Logger.debug(PasskeyConfig.class, "PassKey is disabled, using no-op PublicKeyCredentialUserEntityRepository");
        }
        JdbcPublicKeyCredentialUserEntityRepository delegate = new JdbcPublicKeyCredentialUserEntityRepository(passkeyJdbcTemplate);
        return new LoggingUserEntityRepository(delegate);
    }

    /**
     * Logging wrapper for UserCredentialRepository to diagnose save/find issues.
     */
    private static class LoggingUserCredentialRepository implements UserCredentialRepository {
        private final JdbcUserCredentialRepository delegate;

        LoggingUserCredentialRepository(JdbcUserCredentialRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public void save(CredentialRecord record) {
            Logger.info(PasskeyConfig.class, "PassKey JDBC: save credential, credentialId=" + record.getCredentialId().toBase64UrlString()
                    + ", userId=" + record.getUserEntityUserId().toBase64UrlString()
                    + ", label=" + record.getLabel());
            try {
                delegate.save(record);
                Logger.info(PasskeyConfig.class, "PassKey JDBC: save credential SUCCESS");
            } catch (Exception e) {
                Logger.error(PasskeyConfig.class, "PassKey JDBC: save credential FAILED: " + e.getMessage(), e);
                throw e;
            }
        }

        @Override
        public CredentialRecord findByCredentialId(Bytes credentialId) {
            Logger.debug(PasskeyConfig.class, "PassKey JDBC: findByCredentialId=" + credentialId.toBase64UrlString());
            CredentialRecord result = delegate.findByCredentialId(credentialId);
            Logger.debug(PasskeyConfig.class, "PassKey JDBC: findByCredentialId result=" + (result != null ? "found" : "null"));
            return result;
        }

        @Override
        public List<CredentialRecord> findByUserId(Bytes userId) {
            Logger.debug(PasskeyConfig.class, "PassKey JDBC: findByUserId=" + userId.toBase64UrlString());
            List<CredentialRecord> result = delegate.findByUserId(userId);
            Logger.info(PasskeyConfig.class, "PassKey JDBC: findByUserId result count=" + result.size());
            return result;
        }

        @Override
        public void delete(Bytes credentialId) {
            Logger.info(PasskeyConfig.class, "PassKey JDBC: delete credentialId=" + credentialId.toBase64UrlString());
            delegate.delete(credentialId);
        }
    }

    /**
     * Logging wrapper for PublicKeyCredentialUserEntityRepository to diagnose save/find issues.
     */
    private static class LoggingUserEntityRepository implements PublicKeyCredentialUserEntityRepository {
        private final JdbcPublicKeyCredentialUserEntityRepository delegate;

        LoggingUserEntityRepository(JdbcPublicKeyCredentialUserEntityRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public void save(PublicKeyCredentialUserEntity userEntity) {
            Logger.info(PasskeyConfig.class, "PassKey JDBC: save userEntity, id=" + userEntity.getId().toBase64UrlString()
                    + ", name=" + userEntity.getName()
                    + ", displayName=" + userEntity.getDisplayName());
            try {
                delegate.save(userEntity);
                Logger.info(PasskeyConfig.class, "PassKey JDBC: save userEntity SUCCESS");
            } catch (Exception e) {
                Logger.error(PasskeyConfig.class, "PassKey JDBC: save userEntity FAILED: " + e.getMessage(), e);
                throw e;
            }
        }

        @Override
        public PublicKeyCredentialUserEntity findByUsername(String username) {
            Logger.debug(PasskeyConfig.class, "PassKey JDBC: findByUsername=" + username);
            PublicKeyCredentialUserEntity result = delegate.findByUsername(username);
            Logger.info(PasskeyConfig.class, "PassKey JDBC: findByUsername result=" + (result != null ? "found (id=" + result.getId().toBase64UrlString() + ")" : "null"));
            return result;
        }

        @Override
        public PublicKeyCredentialUserEntity findById(Bytes id) {
            Logger.debug(PasskeyConfig.class, "PassKey JDBC: findById=" + id.toBase64UrlString());
            PublicKeyCredentialUserEntity result = delegate.findById(id);
            Logger.debug(PasskeyConfig.class, "PassKey JDBC: findById result=" + (result != null ? "found" : "null"));
            return result;
        }

        @Override
        public void delete(Bytes id) {
            Logger.info(PasskeyConfig.class, "PassKey JDBC: delete userEntity id=" + id.toBase64UrlString());
            delegate.delete(id);
        }
    }
}
