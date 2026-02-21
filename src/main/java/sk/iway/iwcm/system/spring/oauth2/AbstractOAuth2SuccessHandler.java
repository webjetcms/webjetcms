package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for OAuth2 Success Handlers
 * Contains shared functionality for admin and user login
 */
public abstract class AbstractOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    protected static final String ROLES_ATTRIBUTE = "roles";
    protected static final String GROUPS_ATTRIBUTE = "groups";
    protected static final String GROUP_MEMBERSHIP_ATTRIBUTE = "group_membership";
    protected static final String RESOURCE_ACCESS_ATTRIBUTE = "resource_access";
    protected static final String REALM_ACCESS_ATTRIBUTE = "realm_access";
    protected static final String EMAIL_ATTRIBUTE = "email";
    protected static final String GIVEN_NAME_ATTRIBUTE = "given_name";
    protected static final String FAMILY_NAME_ATTRIBUTE = "family_name";
    protected static final String USERNAME_ATTRIBUTE_DEFAULT = "preferred_username";
    protected static final String ROLE_PREFIX = "ROLE_";

    /**
     * Returns the username attribute name from configuration, or default "preferred_username"
     */
    protected String getUsernameAttribute() {
        String configured = Constants.getString("oauth2_usernameAttribute");
        if (Tools.isNotEmpty(configured)) {
            return configured;
        }
        return USERNAME_ATTRIBUTE_DEFAULT;
    }

    @Override
    public abstract void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException;

    /**
     * Creates a new user from OAuth2 data
     */
    protected UserDetails createNewUserFromOAuth2(OAuth2User oauth2User, String email) {
        UserDetails userDetails = new UserDetails();
        userDetails.setEmail(email);

        String givenName = oauth2User.getAttribute(GIVEN_NAME_ATTRIBUTE);
        String familyName = oauth2User.getAttribute(FAMILY_NAME_ATTRIBUTE);

        if (givenName != null) userDetails.setFirstName(givenName);
        if (familyName != null) userDetails.setLastName(familyName);

        // Set login - preferably from username attribute, otherwise use email before @ sign
        String usernameAttr = getUsernameAttribute();
        String username = oauth2User.getAttribute(usernameAttr);
        String login;
        if (username != null && !username.trim().isEmpty()) {
            login = username;
            Logger.debug(this.getClass(), "Using username from OAuth2 provider (attribute '" + usernameAttr + "'): " + login);
        } else {
            login = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
            Logger.debug(this.getClass(), "Username not provided by OAuth2 provider (attribute '" + usernameAttr + "'), using email prefix: " + login);
        }
        userDetails.setLogin(login);
        userDetails.setAuthorized(true);

        boolean isUserSaved = UsersDB.saveUser(userDetails);
        if (!isUserSaved) {
            return null;
        }

        return userDetails;
    }

    /**
     * Updates existing user with new data from OAuth2
     */
    protected void updateExistingUserFromOAuth2(OAuth2User oauth2User, UserDetails userDetails) {
        String givenName = oauth2User.getAttribute(GIVEN_NAME_ATTRIBUTE);
        String familyName = oauth2User.getAttribute(FAMILY_NAME_ATTRIBUTE);
        String username = oauth2User.getAttribute(getUsernameAttribute());

        boolean needsUpdate = false;
        if (givenName != null && !givenName.equals(userDetails.getFirstName())) {
            userDetails.setFirstName(givenName);
            needsUpdate = true;
        }
        if (familyName != null && !familyName.equals(userDetails.getLastName())) {
            userDetails.setLastName(familyName);
            needsUpdate = true;
        }

        // Update login if changed in OAuth2 provider
        if (username != null && !username.trim().isEmpty() && !username.equals(userDetails.getLogin())) {
            Logger.info(this.getClass(), "Updating login from '" + userDetails.getLogin() + "' to '" + username + "' for user: " + userDetails.getEmail());
            userDetails.setLogin(username);
            needsUpdate = true;
        }

        if (needsUpdate) {
            boolean isUserUpdated = UsersDB.saveUser(userDetails);
            if (isUserUpdated) {
                Logger.info(this.getClass(), "Updated user data for email: " + userDetails.getEmail());
            } else {
                Logger.error(this.getClass(), "Failed to update user for email: " + userDetails.getEmail());
            }
        }
    }

    /**
     * Gets OAuth2 provider ID from authentication
     */
    protected String getProviderId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            String providerId = oauth2Token.getAuthorizedClientRegistrationId();
            Logger.debug(this.getClass(), "Detected OAuth2 provider ID: " + providerId);
            return providerId;
        }
        Logger.debug(this.getClass(), "Could not detect OAuth2 provider ID from authentication");
        return null;
    }

    /**
     * Determines if the given provider is configured to synchronize permissions
     */
    protected boolean shouldSyncPermissions(String providerId) {
        if (providerId == null) {
            return false;
        }

        String configuredProviders = Constants.getString("oauth2_clientsWithPermissions");
        if (Tools.isEmpty(configuredProviders)) {
            Logger.debug(this.getClass(), "No providers configured for permission synchronization (oauth2_clientsWithPermissions is empty)");
            return false;
        }

        List<String> providers = List.of(Tools.getTokens(configuredProviders, ","));
        boolean shouldSync = providers.contains(providerId);

        Logger.debug(this.getClass(), "Provider '" + providerId + "' shouldSyncPermissions: " + shouldSync + " (configured: " + configuredProviders + ")");
        return shouldSync;
    }

    /**
     * Extracts list of groups from OAuth2 attributes
     * Supports various formats: groups, roles, resource_access.client.roles, etc.
     */
    protected List<String> extractGroupsFromOAuth2(OAuth2User oauth2User) {
        List<String> groups = new ArrayList<>();

        // Try to get from all possible attributes
        groups.addAll(extractFromAttribute(oauth2User, GROUPS_ATTRIBUTE));
        groups.addAll(extractFromAttribute(oauth2User, ROLES_ATTRIBUTE));
        groups.addAll(extractFromAttribute(oauth2User, GROUP_MEMBERSHIP_ATTRIBUTE));

        // Try to get from Keycloak format (resource_access)
        groups.addAll(extractFromResourceAccess(oauth2User));

        // Try to get from realm_access (Keycloak realm roles)
        groups.addAll(extractFromRealmAccess(oauth2User));

        // Try to get from authorities (Spring Security)
        if (oauth2User.getAuthorities() != null) {
            for (var authority : oauth2User.getAuthorities()) {
                String authName = authority.getAuthority();
                if (authName.startsWith(ROLE_PREFIX)) {
                    authName = authName.substring(ROLE_PREFIX.length()); // Remove ROLE_ prefix
                }
                if (!groups.contains(authName)) {
                    groups.add(authName);
                    Logger.debug(this.getClass(), "Added authority: " + authName);
                }
            }
        }

        Logger.info(this.getClass(), "Final extracted groups: " + groups);
        return groups;
    }

    /**
     * Extracts list from simple attribute
     */
    protected List<String> extractFromAttribute(OAuth2User oauth2User, String attributeName) {
        List<String> result = new ArrayList<>();
        Object attr = oauth2User.getAttribute(attributeName);
        Logger.debug(this.getClass(), attributeName + " attribute: " + attr);

        if (attr instanceof Collection) {
            for (Object item : (Collection<?>) attr) {
                if (item instanceof String) {
                    result.add((String) item);
                    Logger.debug(this.getClass(), "Added from '" + attributeName + "': " + item);
                }
            }
        } else if (attr instanceof String) {
            // If it's a string, try to split by commas or spaces
            String[] parts = ((String) attr).split("[,\\s]+");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    result.add(part.trim());
                    Logger.debug(this.getClass(), "Added from '" + attributeName + "': " + part.trim());
                }
            }
        }

        return result;
    }

    /**
     * Extracts roles from resource_access
     */
    @SuppressWarnings("unchecked")
    protected List<String> extractFromResourceAccess(OAuth2User oauth2User) {
        List<String> result = new ArrayList<>();
        Object resourceAccess = oauth2User.getAttribute(RESOURCE_ACCESS_ATTRIBUTE);
        Logger.debug(this.getClass(), "Resource access attribute: " + resourceAccess);

        if (resourceAccess instanceof Map) {
            Map<String, Object> resourceMap = (Map<String, Object>) resourceAccess;
            for (Map.Entry<String, Object> entry : resourceMap.entrySet()) {
                result.addAll(extractRolesFromClientResource(entry));
            }
        }

        return result;
    }

    /**
     * Extracts roles from one client resource
     */
    @SuppressWarnings("unchecked")
    protected List<String> extractRolesFromClientResource(Map.Entry<String, Object> clientEntry) {
        List<String> result = new ArrayList<>();
        String clientName = clientEntry.getKey();
        Object clientData = clientEntry.getValue();

        Logger.debug(this.getClass(), "Resource access client '" + clientName + "': " + clientData);

        if (clientData instanceof Map) {
            Map<String, Object> clientMap = (Map<String, Object>) clientData;
            Object clientRoles = clientMap.get(ROLES_ATTRIBUTE);
            result.addAll(extractRolesFromRolesObject(clientRoles, "resource_access." + clientName));
        }

        return result;
    }

    /**
     * Extracts roles from realm_access
     */
    @SuppressWarnings("unchecked")
    protected List<String> extractFromRealmAccess(OAuth2User oauth2User) {
        List<String> result = new ArrayList<>();
        Object realmAccess = oauth2User.getAttribute(REALM_ACCESS_ATTRIBUTE);
        Logger.debug(this.getClass(), "Realm access attribute: " + realmAccess);

        if (realmAccess instanceof Map) {
            Map<String, Object> realmMap = (Map<String, Object>) realmAccess;
            result.addAll(extractRolesFromRolesObject(realmMap.get(ROLES_ATTRIBUTE), REALM_ACCESS_ATTRIBUTE));
        }

        return result;
    }

    /**
     * Helper method for extracting roles from roles object
     */
    protected List<String> extractRolesFromRolesObject(Object rolesObject, String source) {
        List<String> result = new ArrayList<>();
        Logger.debug(this.getClass(), source + " roles: " + rolesObject);

        if (rolesObject instanceof Collection) {
            for (Object role : (Collection<?>) rolesObject) {
                if (role instanceof String) {
                    result.add((String) role);
                    Logger.debug(this.getClass(), "Added role from " + source + ": " + role);
                }
            }
        }

        return result;
    }

    /**
     * Logs all attributes from OAuth2 token as JSON for diagnostics
     */
    protected void logOAuth2Attributes(OAuth2User oauth2User) {
        if (oauth2User == null) return;
        try {
            Map<String, Object> attributes = oauth2User.getAttributes();
            StringBuilder sb = new StringBuilder();
            sb.append("OAuth2 token attributes: {\n");
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                sb.append("  \"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }
                sb.append(",\n");
            }
            sb.append("}");
            Logger.debug(this.getClass(), sb.toString());
        } catch (Exception ex) {
            Logger.error(this.getClass(), "Error logging OAuth2 attributes", ex);
        }
    }

    /**
     * Helper method for error handling - sets error to session and performs redirect
     */
    protected void handleError(HttpServletRequest request, HttpServletResponse response, String errorCode, String redirectUrl) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute("oauth2_logon_error", errorCode);
        response.sendRedirect(redirectUrl);
    }
}
