package sk.iway.iwcm.system.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.PermissionGroupDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String GROUPS_ATTRIBUTE = "groups";
    private static final String GROUP_MEMBERSHIP_ATTRIBUTE = "group_membership";
    private static final String RESOURCE_ACCESS_ATTRIBUTE = "resource_access";
    private static final String REALM_ACCESS_ATTRIBUTE = "realm_access";
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String GIVEN_NAME_ATTRIBUTE = "given_name";
    private static final String FAMILY_NAME_ATTRIBUTE = "family_name";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            Object principal = authentication.getPrincipal();
            String email = null;
            OAuth2User oauth2User = null;

            if (principal instanceof OAuth2User) {
                oauth2User = (OAuth2User) principal;
                email = oauth2User.getAttribute(EMAIL_ATTRIBUTE);
            }

            if (email == null) {
                Logger.error(OAuth2SuccessHandler.class, "OAuth2 email not found");
                response.sendRedirect("/admin/logon.jsp?error=oauth2_email_not_found");
                return;
            }

            UserDetails userDetails = UsersDB.getUserByEmail(email, 1);
            if (userDetails == null) {
                // Vytvor nového používateľa
                userDetails = createNewUserFromOAuth2(oauth2User, email);
                if (userDetails == null) {
                    Logger.error(OAuth2SuccessHandler.class, "Failed to create user for email: " + email);
                    response.sendRedirect("/admin/logon.jsp?error=user_create_failed");
                    return;
                }
                Logger.info(OAuth2SuccessHandler.class, "Created new user for email: " + email);
            } else {
                // Aktualizuj existujúceho používateľa s novými údajmi z OAuth2
                updateExistingUserFromOAuth2(oauth2User, userDetails);
            }

            // Aplikuj práva z OAuth2 iba pre nakonfigurovaných providerov
            String providerId = getProviderId(authentication);
            if (shouldSyncPermissions(providerId)) {
                Logger.info(OAuth2SuccessHandler.class, "Applying OAuth2 permissions for provider: " + providerId);
                applyOAuth2Permissions(oauth2User, userDetails);
            } else {
                Logger.info(OAuth2SuccessHandler.class, "Skipping group synchronization for provider '" + providerId + "' (not configured in oauth2_clientsWithPermissions)");
            }

            Identity identity = new Identity(userDetails);
            identity.setValid(true);
            HttpSession session = request.getSession();
            LogonTools.setUserToSession(session, identity);
            Authentication springAuth = WebjetAuthentificationProvider.authenticate(identity);
            SecurityContextHolder.getContext().setAuthentication(springAuth);
            response.sendRedirect("/admin/");
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, ex);
            response.sendRedirect("/admin/logon.jsp?error=oauth2_exception");
        }
    }

    /**
     * Vytvorí nového používateľa z OAuth2 údajov
     */
    private UserDetails createNewUserFromOAuth2(OAuth2User oauth2User, String email) {
        UserDetails userDetails = new UserDetails();
        userDetails.setEmail(email);

        String givenName = oauth2User.getAttribute(GIVEN_NAME_ATTRIBUTE);
        String familyName = oauth2User.getAttribute(FAMILY_NAME_ATTRIBUTE);

        if (givenName != null) userDetails.setFirstName(givenName);
        if (familyName != null) userDetails.setLastName(familyName);

        // Nastav login ako email pred zavináčom
        String login = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        userDetails.setLogin(login);
        userDetails.setAuthorized(true);

        boolean isUserSaved = UsersDB.saveUser(userDetails);
        if (!isUserSaved) {
            return null;
        }

        return userDetails;
    }

    /**
     * Aktualizuje existujúceho používateľa s novými údajmi z OAuth2
     */
    private void updateExistingUserFromOAuth2(OAuth2User oauth2User, UserDetails userDetails) {
        String givenName = oauth2User.getAttribute(GIVEN_NAME_ATTRIBUTE);
        String familyName = oauth2User.getAttribute(FAMILY_NAME_ATTRIBUTE);

        boolean needsUpdate = false;
        if (givenName != null && !givenName.equals(userDetails.getFirstName())) {
            userDetails.setFirstName(givenName);
            needsUpdate = true;
        }
        if (familyName != null && !familyName.equals(userDetails.getLastName())) {
            userDetails.setLastName(familyName);
            needsUpdate = true;
        }

        if (needsUpdate) {
            boolean isUserUpdated = UsersDB.saveUser(userDetails);
            if (isUserUpdated) {
                Logger.info(OAuth2SuccessHandler.class, "Updated user data for email: " + userDetails.getEmail());
            } else {
                Logger.error(OAuth2SuccessHandler.class, "Failed to update user for email: " + userDetails.getEmail());
            }
        }
    }

    /**
     * Získa ID OAuth2 providera z autentifikácie
     */
    private String getProviderId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            String providerId = oauth2Token.getAuthorizedClientRegistrationId();
            Logger.debug(OAuth2SuccessHandler.class, "Detected OAuth2 provider ID: " + providerId);
            return providerId;
        }
        Logger.debug(OAuth2SuccessHandler.class, "Could not detect OAuth2 provider ID from authentication");
        return null;
    }

    /**
     * Zisťuje či má daný provider nakonfigurované synchronizovať práva
     */
    private boolean shouldSyncPermissions(String providerId) {
        if (providerId == null) {
            return false;
        }

        String configuredProviders = Constants.getString("oauth2_clientsWithPermissions");
        if (Tools.isEmpty(configuredProviders)) {
            Logger.debug(OAuth2SuccessHandler.class, "No providers configured for permission synchronization (oauth2_clientsWithPermissions is empty)");
            return false;
        }

        List<String> providers = List.of(Tools.getTokens(configuredProviders, ","));
        boolean shouldSync = providers.contains(providerId);

        Logger.debug(OAuth2SuccessHandler.class, "Provider '" + providerId + "' shouldSyncPermissions: " + shouldSync + " (configured: " + configuredProviders + ")");
        return shouldSync;
    }

    /**
     * Aplikuje práva z OAuth2 atribútov na používateľa (iba pre Keycloak)
     */
    private void applyOAuth2Permissions(OAuth2User oauth2User, UserDetails userDetails) {
        try {
            List<String> oauth2Groups = extractGroupsFromOAuth2(oauth2User);
            Logger.info(OAuth2SuccessHandler.class, "Found OAuth2 groups for user " + userDetails.getEmail() + ": " + oauth2Groups);

            if (oauth2Groups.isEmpty()) {
                Logger.info(OAuth2SuccessHandler.class, "No OAuth2 groups found for user " + userDetails.getEmail());
                return;
            }

            // Nájdi existujúce skupiny používateľov v WebJET
            List<UserGroupDetails> matchingUserGroups = new ArrayList<>();
            List<PermissionGroupBean> matchingPermissionGroups = new ArrayList<>();

            try {
                // Načítaj všetky user groups a filtruj podľa názvu
                List<UserGroupDetails> allUserGroups = UserGroupsDB.getInstance().getUserGroups();
                for (UserGroupDetails userGroup : allUserGroups) {
                    if (oauth2Groups.contains(userGroup.getUserGroupName())) {
                        matchingUserGroups.add(userGroup);
                        Logger.debug(OAuth2SuccessHandler.class, "Found matching user group: " + userGroup.getUserGroupName());
                    }
                }

                // Načítaj všetky permission groups a filtruj podľa názvu
                List<PermissionGroupBean> allPermissionGroups = PermissionGroupDB.getPermissionGroups(null);
                for (PermissionGroupBean permissionGroup : allPermissionGroups) {
                    if (oauth2Groups.contains(permissionGroup.getTitle())) {
                        matchingPermissionGroups.add(permissionGroup);
                        Logger.debug(OAuth2SuccessHandler.class, "Found matching permission group: " + permissionGroup.getTitle());
                    }
                }

            } catch (Exception ex) {
                Logger.error(OAuth2SuccessHandler.class, "Error loading groups for user: " + userDetails.getEmail(), ex);
                return;
            }

            // Synchronizuj skupiny - odstráň staré a pridaj nové
            synchronizeUserGroups(userDetails, matchingUserGroups, matchingPermissionGroups);

            if (matchingUserGroups.isEmpty() && matchingPermissionGroups.isEmpty()) {
                Logger.info(OAuth2SuccessHandler.class, "No matching user groups or permission groups found for user " + userDetails.getEmail());
            }

            // Nastav admin práva na základe skupín z Keycloak
            try {
                String adminGroupName = Constants.getString("NTLMAdminGroupName");
                boolean isAdmin = false;
                if (adminGroupName != null && !adminGroupName.isEmpty() && oauth2Groups.contains(adminGroupName)) {
                    isAdmin = true;
                }
                if (userDetails.isAdmin() != isAdmin) {
                    userDetails.setAdmin(isAdmin);
                    UsersDB.saveUser(userDetails);
                    Logger.info(OAuth2SuccessHandler.class, "Set admin=" + isAdmin + " for user: " + userDetails.getEmail() + " based on Keycloak groups");
                }
            } catch (Exception e) {
                Logger.error(OAuth2SuccessHandler.class, "Error setting admin flag for user: " + userDetails.getEmail(), e);
            }
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, "Error applying OAuth2 permissions for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Odstráni všetky skupinové priradenia používateľa
     */
    private void removeAllGroupAssignments(UserDetails userDetails) {
        try {
            // Odstráň zo všetkých skupín používateľov
            userDetails.setUserGroupsIds(null);

            // Odstráň zo všetkých skupín práv
            List<PermissionGroupBean> userPermGroups = UserGroupsDB.getPermissionGroupsFor(userDetails.getUserId());
            for (PermissionGroupBean permGroup : userPermGroups) {
                UsersDB.deleteUserFromPermissionGroup(userDetails.getUserId(), permGroup.getUserPermGroupId());
            }
            Logger.info(OAuth2SuccessHandler.class, "Removed user from all permission groups: " + userDetails.getEmail());
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, "Error removing all group assignments for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Synchronizuje skupiny používateľa - odstráni staré a pridá nové
     */
    private void synchronizeUserGroups(UserDetails userDetails, List<UserGroupDetails> newUserGroups, List<PermissionGroupBean> newPermissionGroups) {
        String userEmail = userDetails != null ? userDetails.getEmail() : "unknown";
        try {
            if (userDetails == null) {
                Logger.error(OAuth2SuccessHandler.class, "UserDetails is null, cannot synchronize groups");
                return;
            }

            // Najprv odstráň zo všetkých existujúcich skupín
            removeAllGroupAssignments(userDetails);

            // Pridaj nové user groups
            if (!newUserGroups.isEmpty()) {
                for (UserGroupDetails group : newUserGroups) {
                    userDetails.addToGroup(group.getUserGroupId());
                    Logger.debug(OAuth2SuccessHandler.class, "Added user to group: " + group.getUserGroupName());
                }
            }

            // Ulož všetky zmeny
            boolean saved = UsersDB.saveUser(userDetails);
            if (saved) {
                Logger.info(OAuth2SuccessHandler.class, "Successfully synchronized user groups: " + userDetails.getUserGroupsIds());
                if (!newUserGroups.isEmpty()) {
                    Logger.info(OAuth2SuccessHandler.class, "Synchronized user " + userDetails.getEmail() + " to " + newUserGroups.size() + " user groups");
                }
            } else {
                Logger.error(OAuth2SuccessHandler.class, "Failed to save user groups synchronization for user: " + userDetails.getEmail());
            }

            // Pridaj permission groups
            if (!newPermissionGroups.isEmpty()) {
                for (PermissionGroupBean group : newPermissionGroups) {
                    UsersDB.addUserToPermissionGroup(userDetails.getUserId(), group.getUserPermGroupId());
                    Logger.debug(OAuth2SuccessHandler.class, "Added user to permission group: " + group.getTitle());
                }
                Logger.info(OAuth2SuccessHandler.class, "Synchronized user " + userDetails.getEmail() + " to " + newPermissionGroups.size() + " permission groups");
            }
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, "Error synchronizing user groups for user: " + userEmail, ex);
        }
    }

    /**
     * Extrahuje zoznam skupín z OAuth2 atribútov
     * Podporuje rôzne formáty: groups, roles, resource_access.client.roles, atď.
     */
    private List<String> extractGroupsFromOAuth2(OAuth2User oauth2User) {
        List<String> groups = new ArrayList<>();

        // Skús získať zo všetkých možných atribútov (nielen ak sú groups prázdne)
        groups.addAll(extractFromAttribute(oauth2User, GROUPS_ATTRIBUTE));
        groups.addAll(extractFromAttribute(oauth2User, ROLES_ATTRIBUTE));
        groups.addAll(extractFromAttribute(oauth2User, GROUP_MEMBERSHIP_ATTRIBUTE));

        // Skús získať z Keycloak formátu (resource_access)
        groups.addAll(extractFromResourceAccess(oauth2User));

        // Skús získať z realm_access (Keycloak realm roles)
        groups.addAll(extractFromRealmAccess(oauth2User));

        // Skús získať z authorities (Spring Security)
        if (oauth2User.getAuthorities() != null) {
            for (var authority : oauth2User.getAuthorities()) {
                String authName = authority.getAuthority();
                if (authName.startsWith(ROLE_PREFIX)) {
                    authName = authName.substring(ROLE_PREFIX.length()); // Odstráň prefix ROLE_
                }
                if (!groups.contains(authName)) {
                    groups.add(authName);
                    Logger.debug(OAuth2SuccessHandler.class, "Added authority: " + authName);
                }
            }
        }

        Logger.info(OAuth2SuccessHandler.class, "Final extracted groups: " + groups);
        return groups;
    }

    /**
     * Extrahuje zoznam z jednoduchého atribútu
     */
    private List<String> extractFromAttribute(OAuth2User oauth2User, String attributeName) {
        List<String> result = new ArrayList<>();
        Object attr = oauth2User.getAttribute(attributeName);
        Logger.debug(OAuth2SuccessHandler.class, attributeName + " attribute: " + attr);

        if (attr instanceof Collection) {
            for (Object item : (Collection<?>) attr) {
                if (item instanceof String) {
                    result.add((String) item);
                    Logger.debug(OAuth2SuccessHandler.class, "Added from '" + attributeName + "': " + item);
                }
            }
        } else if (attr instanceof String) {
            // Ak je to string, skús ho rozdeliť podľa čiarok alebo medzier
            String[] parts = ((String) attr).split("[,\\s]+");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    result.add(part.trim());
                    Logger.debug(OAuth2SuccessHandler.class, "Added from '" + attributeName + "': " + part.trim());
                }
            }
        }

        return result;
    }

    /**
     * Extrahuje role z resource_access
     */
    private List<String> extractFromResourceAccess(OAuth2User oauth2User) {
        List<String> result = new ArrayList<>();
        Object resourceAccess = oauth2User.getAttribute(RESOURCE_ACCESS_ATTRIBUTE);
        Logger.debug(OAuth2SuccessHandler.class, "Resource access attribute: " + resourceAccess);

        if (resourceAccess instanceof Map) {
            Map<String, Object> resourceMap = (Map<String, Object>) resourceAccess;
            for (Map.Entry<String, Object> entry : resourceMap.entrySet()) {
                result.addAll(extractRolesFromClientResource(entry));
            }
        }

        return result;
    }

    /**
     * Extrahuje role z jedného klientského resource
     */
    private List<String> extractRolesFromClientResource(Map.Entry<String, Object> clientEntry) {
        List<String> result = new ArrayList<>();
        String clientName = clientEntry.getKey();
        Object clientData = clientEntry.getValue();

        Logger.debug(OAuth2SuccessHandler.class, "Resource access client '" + clientName + "': " + clientData);

        if (clientData instanceof Map) {
            Map<String, Object> clientMap = (Map<String, Object>) clientData;
            Object clientRoles = clientMap.get(ROLES_ATTRIBUTE);
            result.addAll(extractRolesFromRolesObject(clientRoles, "resource_access." + clientName));
        }

        return result;
    }

    /**
     * Extrahuje role z realm_access
     */
    private List<String> extractFromRealmAccess(OAuth2User oauth2User) {
        List<String> result = new ArrayList<>();
        Object realmAccess = oauth2User.getAttribute(REALM_ACCESS_ATTRIBUTE);
        Logger.debug(OAuth2SuccessHandler.class, "Realm access attribute: " + realmAccess);

        if (realmAccess instanceof Map) {
            Map<String, Object> realmMap = (Map<String, Object>) realmAccess;
            result.addAll(extractRolesFromRolesObject(realmMap.get(ROLES_ATTRIBUTE), REALM_ACCESS_ATTRIBUTE));
        }

        return result;
    }

    /**
     * Pomocná metóda na extrahovanie rolí z roles objektu
     */
    private List<String> extractRolesFromRolesObject(Object rolesObject, String source) {
        List<String> result = new ArrayList<>();
        Logger.debug(OAuth2SuccessHandler.class, source + " roles: " + rolesObject);

        if (rolesObject instanceof Collection) {
            for (Object role : (Collection<?>) rolesObject) {
                if (role instanceof String) {
                    result.add((String) role);
                    Logger.debug(OAuth2SuccessHandler.class, "Added role from " + source + ": " + role);
                }
            }
        }

        return result;
    }
}
