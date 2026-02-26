package sk.iway.iwcm.system.spring.passkey;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring DATA repository for PasskeyUserEntityBean (passkey_user_entities table).
 * Provides CRUD operations and custom finders for WebAuthn user entity management.
 */
@Repository
public interface PasskeyUserEntityRepository extends JpaRepository<PasskeyUserEntityBean, Long> {

    /**
     * Find a user entity by WebAuthn user handle (Base64URL encoded).
     * @param webauthnUserId the WebAuthn user handle
     * @return the user entity or empty
     */
    Optional<PasskeyUserEntityBean> findByWebauthnUserId(String webauthnUserId);

    /**
     * Find a user entity by username (login name).
     * @param name the username
     * @return the user entity or empty
     */
    Optional<PasskeyUserEntityBean> findByName(String name);

    /**
     * Delete a user entity by WebAuthn user handle.
     * @param webauthnUserId the WebAuthn user handle
     */
    void deleteByWebauthnUserId(String webauthnUserId);
}
