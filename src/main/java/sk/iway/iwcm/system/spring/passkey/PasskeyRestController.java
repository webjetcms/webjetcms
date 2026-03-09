package sk.iway.iwcm.system.spring.passkey;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.users.UsersDB;

/**
 * REST controller for PassKey/WebAuthn credential management.
 * Provides endpoints for listing and deleting passkeys in the admin profile dialog.
 *
 * Endpoints:
 * - GET  /admin/rest/passkey/list        - list all passkeys for the current user
 * - DELETE /admin/rest/passkey/{credentialId} - delete a specific passkey
 */
@RestController
@RequestMapping("/admin/rest/passkey")
public class PasskeyRestController {

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasskeyCredentialRepository credentialRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    /**
     * List all passkeys for the currently logged-in user.
     */
    @GetMapping("/list")
    public ResponseEntity<List<PasskeyInfoDto>> listPasskeys(HttpServletRequest request) {
        if (Constants.getBoolean("password_passKeyEnabled") == false) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || user.isAdmin() == false) {
            return ResponseEntity.status(403).build();
        }

        Optional<UserDetailsEntity> userEntityOpt = userDetailsRepository.findFirstByLoginAndDomainId(user.getLogin(), UsersDB.getDomainId());
        if (userEntityOpt.isEmpty() || Tools.isEmpty(userEntityOpt.get().getWebauthnUserId())) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<PasskeyCredentialBean> credentials = credentialRepository.findByUserId(userEntityOpt.get().getId());
        List<PasskeyInfoDto> result = credentials.stream()
                .map(PasskeyInfoDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Delete a specific passkey by credential ID (Base64URL encoded).
     */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<Void> deletePasskey(@PathVariable String credentialId, HttpServletRequest request) {
        if (Constants.getBoolean("password_passKeyEnabled") == false) {
            return ResponseEntity.notFound().build();
        }

        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || user.isAdmin() == false) {
            return ResponseEntity.status(403).build();
        }

        // Verify the credential belongs to the current user
        Optional<PasskeyCredentialBean> credentialOpt = credentialRepository.findByCredentialId(credentialId);
        if (credentialOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PasskeyCredentialBean credential = credentialOpt.get();
        Optional<UserDetailsEntity> userEntityOpt = userDetailsRepository.findFirstByLoginAndDomainId(user.getLogin(), UsersDB.getDomainId());
        if (userEntityOpt.isEmpty() || credential.getUserId().equals(userEntityOpt.get().getId()) == false) {
            return ResponseEntity.status(403).build();
        }

        userCredentialRepository.delete(Bytes.fromBase64(credentialId));
        Logger.info(PasskeyRestController.class, "PassKey deleted for user " + user.getLogin() + ", credentialId=" + credentialId);

        return ResponseEntity.ok().build();
    }

    /**
     * DTO for returning passkey information to the frontend.
     */
    public static class PasskeyInfoDto {
        private String credentialId;
        private String label;
        private String created;
        private String lastUsed;
        private String transports;
        private String rpId;

        public static PasskeyInfoDto fromEntity(PasskeyCredentialBean entity) {
            PasskeyInfoDto dto = new PasskeyInfoDto();
            dto.credentialId = entity.getCredentialId();
            dto.label = entity.getLabel();
            dto.created = entity.getCreated() != null ? entity.getCreated().toString() : null;
            dto.lastUsed = entity.getLastUsed() != null ? entity.getLastUsed().toString() : null;
            dto.transports = entity.getAuthenticatorTransports();
            dto.rpId = entity.getRpId();
            return dto;
        }

        public String getCredentialId() { return credentialId; }
        public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        public String getLastUsed() { return lastUsed; }
        public void setLastUsed(String lastUsed) { this.lastUsed = lastUsed; }
        public String getTransports() { return transports; }
        public void setTransports(String transports) { this.transports = transports; }
        public String getRpId() { return rpId; }
        public void setRpId(String rpId) { this.rpId = rpId; }
    }
}
