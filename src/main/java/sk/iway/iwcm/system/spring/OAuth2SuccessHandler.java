package sk.iway.iwcm.system.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
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
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            Object principal = authentication.getPrincipal();
            String email = null;
            OAuth2User oauth2User = null;

            if (principal instanceof OAuth2User) {
                oauth2User = (OAuth2User) principal;
                email = oauth2User.getAttribute("email");
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

            // Aplikuj práva z OAuth2
            applyOAuth2Permissions(oauth2User, userDetails);

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

        String givenName = oauth2User.getAttribute("given_name");
        String familyName = oauth2User.getAttribute("family_name");

        if (givenName != null) userDetails.setFirstName(givenName);
        if (familyName != null) userDetails.setLastName(familyName);

        // Nastav login ako email pred zavináčom
        String login = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        userDetails.setLogin(login);
        userDetails.setAuthorized(true);
        userDetails.setAdmin(true);

        boolean isUserSaved = UsersDB.saveUser(userDetails);
        if (!isUserSaved) {
            return null;
        }

        return UsersDB.getUserByEmail(email, 1);
    }

    /**
     * Aktualizuje existujúceho používateľa s novými údajmi z OAuth2
     */
    private void updateExistingUserFromOAuth2(OAuth2User oauth2User, UserDetails userDetails) {
        String givenName = oauth2User.getAttribute("given_name");
        String familyName = oauth2User.getAttribute("family_name");

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
     * Aplikuje práva z OAuth2 atribútov na používateľa
     */
    private void applyOAuth2Permissions(OAuth2User oauth2User, UserDetails userDetails) {
        try {
            List<String> oauth2Groups = extractGroupsFromOAuth2(oauth2User);
            if (oauth2Groups.isEmpty()) {
                Logger.debug(OAuth2SuccessHandler.class, "No groups found in OAuth2 for user: " + userDetails.getEmail());
                return;
            }

            Logger.info(OAuth2SuccessHandler.class, "Found OAuth2 groups for user " + userDetails.getEmail() + ": " + oauth2Groups);

            // Nájdi existujúce skupiny používateľov v WebJET
            List<UserGroupDetails> matchingUserGroups = new ArrayList<>();
            List<PermissionGroupBean> matchingPermissionGroups = new ArrayList<>();

            for (String groupName : oauth2Groups) {
                try {
                    // Skús nájsť UserGroup
                    UserGroupDetails userGroup = UserGroupsDB.getInstance().getUserGroup(groupName);
                    if (userGroup != null) {
                        matchingUserGroups.add(userGroup);
                        Logger.debug(OAuth2SuccessHandler.class, "Found matching user group: " + groupName);
                    } else {
                        Logger.debug(OAuth2SuccessHandler.class, "User group not found: " + groupName);
                    }

                    // Skús nájsť PermissionGroup
                    try {
                        PermissionGroupBean permissionGroup = PermissionGroupDB.getPermissionGroup(groupName);
                        if (permissionGroup != null) {
                            matchingPermissionGroups.add(permissionGroup);
                            Logger.debug(OAuth2SuccessHandler.class, "Found matching permission group: " + groupName);
                        } else {
                            Logger.debug(OAuth2SuccessHandler.class, "Permission group not found: " + groupName);
                        }
                    } catch (Exception ex) {
                        Logger.debug(OAuth2SuccessHandler.class, "Error finding permission group '" + groupName + "': " + ex.getMessage());
                    }

                } catch (Exception ex) {
                    Logger.debug(OAuth2SuccessHandler.class, "Error finding groups for '" + groupName + "': " + ex.getMessage());
                }
            }

            // Priradí používateľa do nájdených skupín používateľov
            if (!matchingUserGroups.isEmpty()) {
                assignUserToUserGroups(userDetails, matchingUserGroups);
                Logger.info(OAuth2SuccessHandler.class, "Assigned user " + userDetails.getEmail() + " to " + matchingUserGroups.size() + " user groups");
            }

            // Priradí používateľa do nájdených skupín práv
            if (!matchingPermissionGroups.isEmpty()) {
                assignUserToPermissionGroups(userDetails, matchingPermissionGroups);
                Logger.info(OAuth2SuccessHandler.class, "Assigned user " + userDetails.getEmail() + " to " + matchingPermissionGroups.size() + " permission groups");
            }

            if (matchingUserGroups.isEmpty() && matchingPermissionGroups.isEmpty()) {
                Logger.info(OAuth2SuccessHandler.class, "No matching user groups or permission groups found for user " + userDetails.getEmail());
            }
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, "Error applying OAuth2 permissions for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Extrahuje zoznam skupín z OAuth2 atribútov
     * Podporuje rôzne formáty: groups, roles, resource_access.client.roles, atď.
     */
    private List<String> extractGroupsFromOAuth2(OAuth2User oauth2User) {
        List<String> groups = new ArrayList<>();

        // Skús získať zo všetkých možných atribútov (nielen ak sú groups prázdne)
        groups.addAll(extractFromAttribute(oauth2User, "groups"));
        groups.addAll(extractFromAttribute(oauth2User, "roles"));
        groups.addAll(extractFromAttribute(oauth2User, "group_membership"));

        // Skús získať z Keycloak formátu (resource_access)
        groups.addAll(extractFromResourceAccess(oauth2User));

        // Skús získať z realm_access (Keycloak realm roles)
        groups.addAll(extractFromRealmAccess(oauth2User));

        // Skús získať z authorities (Spring Security)
        if (oauth2User.getAuthorities() != null) {
            for (var authority : oauth2User.getAuthorities()) {
                String authName = authority.getAuthority();
                if (authName.startsWith("ROLE_")) {
                    authName = authName.substring(5); // Odstráň prefix ROLE_
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
        Object resourceAccess = oauth2User.getAttribute("resource_access");
        Logger.debug(OAuth2SuccessHandler.class, "Resource access attribute: " + resourceAccess);

        if (resourceAccess instanceof Map) {
            Map<String, Object> resourceMap = (Map<String, Object>) resourceAccess;
            for (Map.Entry<String, Object> entry : resourceMap.entrySet()) {
                Logger.debug(OAuth2SuccessHandler.class, "Resource access client '" + entry.getKey() + "': " + entry.getValue());
                Object clientData = entry.getValue();
                if (clientData instanceof Map) {
                    Map<String, Object> clientMap = (Map<String, Object>) clientData;
                    Object clientRoles = clientMap.get("roles");
                    Logger.debug(OAuth2SuccessHandler.class, "Client roles for '" + entry.getKey() + "': " + clientRoles);
                    if (clientRoles instanceof Collection) {
                        for (Object role : (Collection<?>) clientRoles) {
                            if (role instanceof String) {
                                result.add((String) role);
                                Logger.debug(OAuth2SuccessHandler.class, "Added role from resource_access: " + role);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Extrahuje role z realm_access
     */
    private List<String> extractFromRealmAccess(OAuth2User oauth2User) {
        List<String> result = new ArrayList<>();
        Object realmAccess = oauth2User.getAttribute("realm_access");
        Logger.debug(OAuth2SuccessHandler.class, "Realm access attribute: " + realmAccess);

        if (realmAccess instanceof Map) {
            Map<String, Object> realmMap = (Map<String, Object>) realmAccess;
            Object realmRoles = realmMap.get("roles");
            Logger.debug(OAuth2SuccessHandler.class, "Realm roles: " + realmRoles);
            if (realmRoles instanceof Collection) {
                for (Object role : (Collection<?>) realmRoles) {
                    if (role instanceof String) {
                        result.add((String) role);
                        Logger.debug(OAuth2SuccessHandler.class, "Added role from realm_access: " + role);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Priradí používateľa do zadaných skupín používateľov
     */
    private void assignUserToUserGroups(UserDetails userDetails, List<UserGroupDetails> userGroups) {
        try {
            // Priradí používateľa do skupín používateľov
            for (UserGroupDetails group : userGroups) {
                userDetails.addToGroup(group.getUserGroupId());
                Logger.debug(OAuth2SuccessHandler.class, "Added user to group: " + group.getUserGroupName());
            }

            // Ulož používateľa do databázy s novými skupinami
            boolean saved = UsersDB.saveUser(userDetails);
            if (saved) {
                Logger.info(OAuth2SuccessHandler.class, "Successfully saved user with new groups: " + userDetails.getUserGroupsIds());
            } else {
                Logger.error(OAuth2SuccessHandler.class, "Failed to save user with new groups for user: " + userDetails.getEmail());
            }
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, "Error assigning user to user groups", ex);
        }
    }

    /**
     * Priradí používateľa do zadaných skupín práv
     */
    private void assignUserToPermissionGroups(UserDetails userDetails, List<PermissionGroupBean> permissionGroups) {
        try {
            // Priradí používateľa do skupín práv
            for (PermissionGroupBean group : permissionGroups) {
                UsersDB.addUserToPermissionGroup(userDetails.getUserId(), group.getUserPermGroupId());
                Logger.debug(OAuth2SuccessHandler.class, "Added user to permission group: " + group.getTitle());
            }
            Logger.info(OAuth2SuccessHandler.class, "Successfully assigned user to permission groups");
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, "Error assigning user to permission groups", ex);
        }
    }
}
