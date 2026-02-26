package sk.iway.iwcm.system.spring.passkey;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 * JPA-backed adapter implementing Spring Security's UserCredentialRepository.
 * Bridges between Spring Security's WebAuthn API (which works with CredentialRecord
 * interface and Bytes IDs) and our JPA PasskeyCredentialBean entities.
 *
 * This replaces the previous JdbcUserCredentialRepository that had persistence
 * issues with the project's DataSource/connection pool.
 */
@Component
public class JpaUserCredentialRepositoryAdapter implements UserCredentialRepository {

    private final PasskeyCredentialRepository credentialRepository;
    private final PasskeyUserEntityRepository userEntityRepository;

    public JpaUserCredentialRepositoryAdapter(
            PasskeyCredentialRepository credentialRepository,
            PasskeyUserEntityRepository userEntityRepository) {
        this.credentialRepository = credentialRepository;
        this.userEntityRepository = userEntityRepository;
    }

    @Override
    @Transactional
    public void save(CredentialRecord record) {
        String credId = record.getCredentialId().toBase64UrlString();
        String webauthnUserId = record.getUserEntityUserId().toBase64UrlString();

        Logger.debug(JpaUserCredentialRepositoryAdapter.class,
                "PassKey JPA: save credential, credentialId=" + credId
                + ", userId=" + webauthnUserId + ", label=" + record.getLabel());

        // Find the user entity by WebAuthn user ID
        Optional<PasskeyUserEntityBean> userEntityOpt = userEntityRepository.findByWebauthnUserId(webauthnUserId);
        if (userEntityOpt.isEmpty()) {
            Logger.error(JpaUserCredentialRepositoryAdapter.class,
                    "PassKey JPA: Cannot save credential - user entity not found for webauthnUserId=" + webauthnUserId);
            throw new IllegalStateException("User entity not found for webauthnUserId=" + webauthnUserId);
        }

        PasskeyUserEntityBean userEntity = userEntityOpt.get();

        // Check if credential already exists (update case)
        Optional<PasskeyCredentialBean> existingOpt = credentialRepository.findByCredentialId(credId);
        PasskeyCredentialBean entity;

        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
        } else {
            entity = new PasskeyCredentialBean();
            entity.setCredentialId(credId);
        }

        entity.setUserEntity(userEntity);
        entity.setWebauthnUserId(webauthnUserId);
        entity.setPublicKey(record.getPublicKey().getBytes());
        entity.setSignatureCount(record.getSignatureCount());
        entity.setUvInitialized(record.isUvInitialized());
        entity.setBackupEligible(record.isBackupEligible());
        entity.setBackupState(record.isBackupState());
        entity.setAuthenticatorTransports(serializeTransports(record.getTransports()));
        entity.setPublicKeyCredentialType(record.getCredentialType() != null
                ? record.getCredentialType().getValue() : null);
        entity.setAttestationObject(record.getAttestationObject() != null
                ? record.getAttestationObject().getBytes() : null);
        entity.setAttestationClientDataJson(record.getAttestationClientDataJSON() != null
                ? record.getAttestationClientDataJSON().getBytes() : null);
        entity.setCreated(record.getCreated());
        entity.setLastUsed(record.getLastUsed());
        entity.setLabel(record.getLabel());

        credentialRepository.save(entity);
        Logger.debug(JpaUserCredentialRepositoryAdapter.class,
                "PassKey JPA: save credential SUCCESS, id=" + entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        String credId = credentialId.toBase64UrlString();
        Logger.debug(JpaUserCredentialRepositoryAdapter.class,
                "PassKey JPA: findByCredentialId=" + credId);

        Optional<PasskeyCredentialBean> entity = credentialRepository.findByCredentialId(credId);
        if (entity.isEmpty()) {
            Logger.debug(JpaUserCredentialRepositoryAdapter.class,
                    "PassKey JPA: findByCredentialId result=null");
            return null;
        }
        return toCredentialRecord(entity.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialRecord> findByUserId(Bytes userId) {
        String webauthnUserId = userId.toBase64UrlString();
        Logger.debug(JpaUserCredentialRepositoryAdapter.class,
                "PassKey JPA: findByUserId=" + webauthnUserId);

        List<PasskeyCredentialBean> entities = credentialRepository.findByWebauthnUserId(webauthnUserId);
        Logger.debug(JpaUserCredentialRepositoryAdapter.class,
                "PassKey JPA: findByUserId result count=" + entities.size());

        return entities.stream()
                .map(this::toCredentialRecord)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Bytes credentialId) {
        String credId = credentialId.toBase64UrlString();
        Logger.info(JpaUserCredentialRepositoryAdapter.class,
                "PassKey JPA: delete credentialId=" + credId);

        credentialRepository.deleteByCredentialId(credId);
    }

    /**
     * Convert a JPA entity to Spring Security's CredentialRecord.
     */
    private CredentialRecord toCredentialRecord(PasskeyCredentialBean entity) {
        ImmutableCredentialRecord.ImmutableCredentialRecordBuilder builder = ImmutableCredentialRecord.builder();

        builder.credentialId(Bytes.fromBase64(entity.getCredentialId()));
        builder.userEntityUserId(Bytes.fromBase64(entity.getWebauthnUserId()));
        builder.publicKey(new ImmutablePublicKeyCose(entity.getPublicKey()));
        builder.signatureCount(entity.getSignatureCount() != null ? entity.getSignatureCount() : 0);
        builder.uvInitialized(entity.getUvInitialized() != null && entity.getUvInitialized());
        builder.backupEligible(entity.getBackupEligible() != null && entity.getBackupEligible());
        builder.backupState(entity.getBackupState() != null && entity.getBackupState());
        builder.transports(deserializeTransports(entity.getAuthenticatorTransports()));
        builder.label(entity.getLabel());

        if (Tools.isNotEmpty(entity.getPublicKeyCredentialType())) {
            builder.credentialType(PublicKeyCredentialType.valueOf(entity.getPublicKeyCredentialType()));
        }
        if (entity.getAttestationObject() != null) {
            builder.attestationObject(new Bytes(entity.getAttestationObject()));
        }
        if (entity.getAttestationClientDataJson() != null) {
            builder.attestationClientDataJSON(new Bytes(entity.getAttestationClientDataJson()));
        }
        if (entity.getCreated() != null) {
            builder.created(entity.getCreated());
        }
        if (entity.getLastUsed() != null) {
            builder.lastUsed(entity.getLastUsed());
        }

        return builder.build();
    }

    /**
     * Serialize a set of AuthenticatorTransport values to a comma-separated string.
     */
    private String serializeTransports(Set<AuthenticatorTransport> transports) {
        if (transports == null || transports.isEmpty()) {
            return null;
        }
        return transports.stream()
                .map(AuthenticatorTransport::getValue)
                .collect(Collectors.joining(","));
    }

    /**
     * Deserialize a comma-separated string to a set of AuthenticatorTransport values.
     */
    private Set<AuthenticatorTransport> deserializeTransports(String transports) {
        if (Tools.isEmpty(transports)) {
            return Collections.emptySet();
        }
        return Arrays.stream(transports.split(","))
                .map(String::trim)
                .filter(s -> s.isEmpty() == false)
                .map(AuthenticatorTransport::valueOf)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
