package sk.iway.iwcm.system.spring.passkey;

import java.util.Optional;

import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.Logger;

/**
 * JPA-backed adapter implementing Spring Security's PublicKeyCredentialUserEntityRepository.
 * Bridges between Spring Security's WebAuthn API (which works with Bytes IDs and
 * PublicKeyCredentialUserEntity interface) and our JPA PasskeyUserEntityBean entities.
 *
 * This replaces the previous JdbcPublicKeyCredentialUserEntityRepository that had
 * persistence issues with the project's DataSource/connection pool.
 */
@Component
public class JpaPublicKeyCredentialUserEntityAdapter implements PublicKeyCredentialUserEntityRepository {

    private final PasskeyUserEntityRepository repository;

    public JpaPublicKeyCredentialUserEntityAdapter(PasskeyUserEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(PublicKeyCredentialUserEntity userEntity) {
        String webauthnId = userEntity.getId().toBase64UrlString();
        String username = userEntity.getName();
        String displayName = userEntity.getDisplayName();

        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: save userEntity, webauthnId=" + webauthnId + ", name=" + username);

        // Check if entity already exists by WebAuthn ID or username
        Optional<PasskeyUserEntityBean> existing = repository.findByWebauthnUserId(webauthnId);
        if (existing.isEmpty()) {
            existing = repository.findByName(username);
        }

        PasskeyUserEntityBean entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setWebauthnUserId(webauthnId);
            entity.setName(username);
            entity.setDisplayName(displayName);
        } else {
            entity = new PasskeyUserEntityBean();
            entity.setWebauthnUserId(webauthnId);
            entity.setName(username);
            entity.setDisplayName(displayName);
        }

        repository.save(entity);
        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: save userEntity SUCCESS, id=" + entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: findByUsername=" + username);

        Optional<PasskeyUserEntityBean> entity = repository.findByName(username);
        if (entity.isEmpty()) {
            Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                    "PassKey JPA: findByUsername result=null");
            return null;
        }

        PublicKeyCredentialUserEntity result = toSpringSecurityEntity(entity.get());
        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: findByUsername result=found");
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        String webauthnId = id.toBase64UrlString();
        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: findById=" + webauthnId);

        Optional<PasskeyUserEntityBean> entity = repository.findByWebauthnUserId(webauthnId);
        if (entity.isEmpty()) {
            Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                    "PassKey JPA: findById result=null");
            return null;
        }

        return toSpringSecurityEntity(entity.get());
    }

    @Override
    @Transactional
    public void delete(Bytes id) {
        String webauthnId = id.toBase64UrlString();
        Logger.info(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: delete userEntity webauthnId=" + webauthnId);

        repository.deleteByWebauthnUserId(webauthnId);
    }

    /**
     * Convert a JPA entity to Spring Security's PublicKeyCredentialUserEntity.
     */
    private PublicKeyCredentialUserEntity toSpringSecurityEntity(PasskeyUserEntityBean entity) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.fromBase64(entity.getWebauthnUserId()))
                .name(entity.getName())
                .displayName(entity.getDisplayName())
                .build();
    }
}
