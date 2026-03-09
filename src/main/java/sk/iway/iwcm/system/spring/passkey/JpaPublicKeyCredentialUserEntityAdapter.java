package sk.iway.iwcm.system.spring.passkey;

import java.util.Optional;

import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.users.UsersDB;

/**
 * JPA-backed adapter implementing Spring Security's PublicKeyCredentialUserEntityRepository.
 * Bridges between Spring Security's WebAuthn API (which works with Bytes IDs and
 * PublicKeyCredentialUserEntity interface) and the users table via UserDetailsRepository.
 */
@Component
public class JpaPublicKeyCredentialUserEntityAdapter implements PublicKeyCredentialUserEntityRepository {

    private final UserDetailsRepository userDetailsRepository;

    public JpaPublicKeyCredentialUserEntityAdapter(UserDetailsRepository userDetailsRepository) {
        this.userDetailsRepository = userDetailsRepository;
    }

    @Override
    @Transactional
    public void save(PublicKeyCredentialUserEntity userEntity) {
        String webauthnId = userEntity.getId().toBase64UrlString();
        String username = userEntity.getName();

        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: save userEntity, webauthnId=" + webauthnId + ", name=" + username);

        Optional<UserDetailsEntity> existing = userDetailsRepository.findByWebauthnUserIdAndDomainId(webauthnId, UsersDB.getDomainId());
        if (existing.isEmpty()) {
            existing = userDetailsRepository.findFirstByLoginAndDomainId(username, UsersDB.getDomainId());
        }

        if (existing.isPresent()) {
            UserDetailsEntity user = existing.get();
            user.setWebauthnUserId(webauthnId);
            userDetailsRepository.save(user);
            Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                    "PassKey JPA: save userEntity SUCCESS, userId=" + user.getId());
        } else {
            Logger.error(JpaPublicKeyCredentialUserEntityAdapter.class,
                    "PassKey JPA: Cannot save userEntity - user not found for username=" + username);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: findByUsername=" + username);

        Optional<UserDetailsEntity> user = userDetailsRepository.findFirstByLoginAndDomainId(username, UsersDB.getDomainId());
        if (user.isEmpty() || Tools.isEmpty(user.get().getWebauthnUserId())) {
            Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                    "PassKey JPA: findByUsername result=null");
            return null;
        }

        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: findByUsername result=found");
        return toSpringSecurityEntity(user.get());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        String webauthnId = id.toBase64UrlString();
        Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: findById=" + webauthnId);

        Optional<UserDetailsEntity> user = userDetailsRepository.findByWebauthnUserIdAndDomainId(webauthnId, UsersDB.getDomainId());
        if (user.isEmpty()) {
            Logger.debug(JpaPublicKeyCredentialUserEntityAdapter.class,
                    "PassKey JPA: findById result=null");
            return null;
        }

        return toSpringSecurityEntity(user.get());
    }

    @Override
    @Transactional
    public void delete(Bytes id) {
        String webauthnId = id.toBase64UrlString();
        Logger.info(JpaPublicKeyCredentialUserEntityAdapter.class,
                "PassKey JPA: delete userEntity webauthnId=" + webauthnId);

        Optional<UserDetailsEntity> user = userDetailsRepository.findByWebauthnUserIdAndDomainId(webauthnId, UsersDB.getDomainId());
        if (user.isPresent()) {
            user.get().setWebauthnUserId(null);
            userDetailsRepository.save(user.get());
        }
    }

    private PublicKeyCredentialUserEntity toSpringSecurityEntity(UserDetailsEntity user) {
        StringBuilder displayName = new StringBuilder();
        if (Tools.isNotEmpty(user.getFirstName())) displayName.append(user.getFirstName()).append(" ");
        if (Tools.isNotEmpty(user.getLastName())) displayName.append(user.getLastName());
        String display = displayName.toString().trim();
        if (Tools.isEmpty(display)) display = user.getLogin();

        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.fromBase64(user.getWebauthnUserId()))
                .name(user.getLogin())
                .displayName(display)
                .build();
    }
}
