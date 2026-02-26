package sk.iway.iwcm.system.spring.passkey;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring DATA repository for PasskeyCredentialBean (passkey_credentials table).
 * Provides CRUD operations and custom finders for WebAuthn credential management.
 */
@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredentialBean, Long> {

    /**
     * Find a credential by its WebAuthn credential ID (Base64URL encoded).
     * @param credentialId the credential ID
     * @return the credential or empty
     */
    Optional<PasskeyCredentialBean> findByCredentialId(String credentialId);

    /**
     * Find all credentials belonging to a user by WebAuthn user handle.
     * @param webauthnUserId the WebAuthn user handle
     * @return list of credentials
     */
    List<PasskeyCredentialBean> findByWebauthnUserId(String webauthnUserId);

    /**
     * Find all credentials belonging to a user entity.
     * @param userEntity the user entity
     * @return list of credentials
     */
    List<PasskeyCredentialBean> findByUserEntity(PasskeyUserEntityBean userEntity);

    /**
     * Delete a credential by its WebAuthn credential ID.
     * @param credentialId the credential ID
     */
    void deleteByCredentialId(String credentialId);
}
