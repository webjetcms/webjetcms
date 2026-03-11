package sk.iway.iwcm.system.spring.passkey;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialRequestOptionsRequest;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 * Dynamic WebAuthn Relying Party Operations that determines rpId from the current HTTP request.
 * This allows passkeys to work across multiple domains by automatically setting the rpId
 * based on the request's server name.
 *
 * The wrapper creates and caches Webauthn4JRelyingPartyOperations instances per domain,
 * ensuring that passkey registration and authentication use the correct rpId for each domain.
 *
 * Thread-safe: uses ConcurrentHashMap to cache operations per rpId.
 */
public class DynamicWebAuthnRelyingPartyOperations implements WebAuthnRelyingPartyOperations {

    private final PublicKeyCredentialUserEntityRepository userEntityRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final String rpName;
    private final Set<String> configuredAllowedOrigins;

    /**
     * Cache of Webauthn4JRelyingPartyOperations instances per rpId (domain).
     * Key: rpId (domain name), Value: Webauthn4JRelyingPartyOperations instance
     */
    private final ConcurrentHashMap<String, Webauthn4JRelyingPartyOperations> operationsCache = new ConcurrentHashMap<>();

    public DynamicWebAuthnRelyingPartyOperations(
            PublicKeyCredentialUserEntityRepository userEntityRepository,
            UserCredentialRepository userCredentialRepository,
            String rpName,
            Set<String> allowedOrigins) {
        this.userEntityRepository = userEntityRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.rpName = rpName;
        this.configuredAllowedOrigins = allowedOrigins != null ? allowedOrigins : new HashSet<>();
    }

    /**
     * Determine the rpId from the current HTTP request.
     * Falls back to configured default if no request is available.
     * @return the rpId (domain) for the current request
     */
    public static String determineRpId() {
        String defaultRpId = Constants.getString("password_passKeyRpId");

        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                    "No current request, using default rpId: " + defaultRpId);
            return defaultRpId;
        }

        String serverName = request.getServerName();
        Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                "Determined rpId from request: " + serverName);

        return serverName;
    }

    /**
     * Get the current HTTP request from Spring's RequestContextHolder.
     * @return the current request or null
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                    "Could not get current request: " + e.getMessage());
            return null;
        }
    }

    /**
     * Build the allowed origins set for a given rpId.
     * Includes the configured origins plus auto-generated origins for the rpId.
     */
    private Set<String> buildAllowedOrigins(String rpId) {
        Set<String> origins = new HashSet<>(configuredAllowedOrigins);

        // Auto-add common origins for the rpId
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String scheme = Tools.getScheme(request);
            int port = request.getServerPort();

            // Add the current request's origin
            String origin;
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
                origin = scheme + "://" + rpId;
            } else {
                origin = scheme + "://" + rpId + ":" + port;
            }
            origins.add(origin);
        }

        // Always add https origin for the domain
        origins.add("https://" + rpId);

        Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                "Allowed origins for rpId " + rpId + ": " + origins);

        return origins;
    }

    /**
     * Get or create a Webauthn4JRelyingPartyOperations instance for the given rpId.
     * @param rpId the Relying Party ID (domain)
     * @return the cached or newly created operations instance
     */
    private Webauthn4JRelyingPartyOperations getOperations(String rpId) {
        return operationsCache.computeIfAbsent(rpId, id -> {
            Logger.info(DynamicWebAuthnRelyingPartyOperations.class,
                    "Creating WebAuthn operations for rpId: " + id);

            String name = Tools.isNotEmpty(rpName) ? rpName : id;
            PublicKeyCredentialRpEntity rpEntity = PublicKeyCredentialRpEntity.builder()
                    .id(id)
                    .name(name)
                    .build();

            return new Webauthn4JRelyingPartyOperations(
                    userEntityRepository,
                    userCredentialRepository,
                    rpEntity,
                    buildAllowedOrigins(id));
        });
    }

    @Override
    public PublicKeyCredentialCreationOptions createPublicKeyCredentialCreationOptions(
            PublicKeyCredentialCreationOptionsRequest request) {
        String rpId = determineRpId();
        Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                "createPublicKeyCredentialCreationOptions for rpId: " + rpId);
        return getOperations(rpId).createPublicKeyCredentialCreationOptions(request);
    }

    @Override
    public CredentialRecord registerCredential(RelyingPartyRegistrationRequest request) {
        String rpId = determineRpId();
        Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                "registerCredential for rpId: " + rpId);
        return getOperations(rpId).registerCredential(request);
    }

    @Override
    public PublicKeyCredentialRequestOptions createCredentialRequestOptions(
            PublicKeyCredentialRequestOptionsRequest request) {
        String rpId = determineRpId();
        Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                "createCredentialRequestOptions for rpId: " + rpId);
        return getOperations(rpId).createCredentialRequestOptions(request);
    }

    @Override
    public PublicKeyCredentialUserEntity authenticate(RelyingPartyAuthenticationRequest request) {
        String rpId = determineRpId();
        Logger.debug(DynamicWebAuthnRelyingPartyOperations.class,
                "authenticate for rpId: " + rpId);
        return getOperations(rpId).authenticate(request);
    }
}
