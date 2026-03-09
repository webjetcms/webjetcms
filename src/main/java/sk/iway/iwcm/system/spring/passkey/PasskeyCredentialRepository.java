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
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredentialEntity, Long> {

    /**
     * Find a credential by its WebAuthn credential ID (Base64URL encoded).
     * @param credentialId the credential ID
     * @return the credential or empty
     */
    Optional<PasskeyCredentialEntity> findByCredentialId(String credentialId);

    /**
     * Find all credentials belonging to a user.
     * @param userId the user_id FK from the users table
     * @return list of credentials
     */
    List<PasskeyCredentialEntity> findByUserId(Long userId);

    /**
     * Find all credentials belonging to a user for a specific Relying Party.
     * @param userId the user_id FK from the users table
     * @param rpId the Relying Party ID (domain)
     * @return list of credentials for that domain
     */
    List<PasskeyCredentialEntity> findByUserIdAndRpId(Long userId, String rpId);

    /**
     * Delete a credential by its WebAuthn credential ID.
     * @param credentialId the credential ID
     */
    void deleteByCredentialId(String credentialId);
}
