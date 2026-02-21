package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.PermissionGroupDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OAuth2AdminSuccessHandler extends AbstractOAuth2SuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            Object principal = authentication.getPrincipal();
            String email = null;
            OAuth2User oauth2User = null;

            if (principal instanceof OAuth2User) {
                oauth2User = (OAuth2User) principal;
                logOAuth2Attributes(oauth2User);
                email = oauth2User.getAttribute(EMAIL_ATTRIBUTE);
            }

            if (email == null) {
                Logger.error(OAuth2AdminSuccessHandler.class, "OAuth2 email not found");
                handleError(request, response, "oauth2_email_not_found", "/admin/logon/");
                return;
            }

            UserDetails userDetails = UsersDB.getUserByEmail(email, 1);
            if (userDetails == null) {
                // Create new user
                userDetails = createNewUserFromOAuth2(oauth2User, email);
                if (userDetails == null) {
                    Logger.error(OAuth2AdminSuccessHandler.class, "Failed to create user for email: " + email);
                    handleError(request, response, "oauth2_user_create_failed", "/admin/logon/");
                    return;
                }
                Logger.info(OAuth2AdminSuccessHandler.class, "Created new user for email: " + email);
            } else {
                // Update existing user with new data from OAuth2
                updateExistingUserFromOAuth2(oauth2User, userDetails);
            }

            // Apply permissions from OAuth2 only for configured providers
            String providerId = getProviderId(authentication);
            if (shouldSyncPermissions(providerId)) {
                Logger.info(OAuth2AdminSuccessHandler.class, "Applying OAuth2 permissions for provider: " + providerId);
                applyOAuth2Permissions(oauth2User, userDetails);
            } else {
                Logger.info(OAuth2AdminSuccessHandler.class, "Skipping group synchronization for provider '" + providerId + "' (not configured in oauth2_clientsWithPermissions)");
            }

            // Check if user has admin rights
            if (!userDetails.isAdmin()) {
                Logger.warn(OAuth2AdminSuccessHandler.class, "User " + userDetails.getEmail() + " does not have admin rights after OAuth2 synchronization");
                HttpSession session = request.getSession();
                session.setAttribute("oauth2_logon_error", "accessDenied");
                response.sendRedirect("/admin/logon/");
                return;
            }

            Identity identity = new Identity(userDetails);
            identity.setValid(true);
            HttpSession session = request.getSession();
            LogonTools.setUserToSession(session, identity);
            Authentication springAuth = WebjetAuthentificationProvider.authenticate(identity);
            SecurityContextHolder.getContext().setAuthentication(springAuth);
            response.sendRedirect("/admin/");
            } catch (Exception ex) {
            Logger.error(OAuth2AdminSuccessHandler.class, ex);
            handleError(request, response, "oauth2_exception", "/admin/logon/");
        }
    }

    /**
     * Applies permissions from OAuth2 attributes to user
     */
    private void applyOAuth2Permissions(OAuth2User oauth2User, UserDetails userDetails) {
        try {
            List<String> oauth2Groups = extractGroupsFromOAuth2(oauth2User);
            Logger.info(OAuth2AdminSuccessHandler.class, "Found OAuth2 groups for user " + userDetails.getEmail() + ": " + oauth2Groups);

            if (oauth2Groups.isEmpty()) {
                Logger.info(OAuth2AdminSuccessHandler.class, "No OAuth2 groups found for user " + userDetails.getEmail());
                return;
            }

            // Find existing user groups in WebJET
            List<UserGroupDetails> matchingUserGroups = new ArrayList<>();
            List<PermissionGroupBean> matchingPermissionGroups = new ArrayList<>();

            try {
                // Load all user groups and filter by name
                List<UserGroupDetails> allUserGroups = UserGroupsDB.getInstance().getUserGroups();
                for (UserGroupDetails userGroup : allUserGroups) {
                    if (oauth2Groups.contains(userGroup.getUserGroupName())) {
                        matchingUserGroups.add(userGroup);
                        Logger.debug(OAuth2AdminSuccessHandler.class, "Found matching user group: " + userGroup.getUserGroupName());
                    }
                }

                // Load all permission groups and filter by name
                List<PermissionGroupBean> allPermissionGroups = PermissionGroupDB.getPermissionGroups(null);
                for (PermissionGroupBean permissionGroup : allPermissionGroups) {
                    if (oauth2Groups.contains(permissionGroup.getTitle())) {
                        matchingPermissionGroups.add(permissionGroup);
                        Logger.debug(OAuth2AdminSuccessHandler.class, "Found matching permission group: " + permissionGroup.getTitle());
                    }
                }

            } catch (Exception ex) {
                Logger.error(OAuth2AdminSuccessHandler.class, "Error loading groups for user: " + userDetails.getEmail(), ex);
                return;
            }

            // Synchronize groups - remove old and add new
            synchronizeUserGroups(userDetails, matchingUserGroups, matchingPermissionGroups);

            if (matchingUserGroups.isEmpty() && matchingPermissionGroups.isEmpty()) {
                Logger.info(OAuth2AdminSuccessHandler.class, "No matching user groups or permission groups found for user " + userDetails.getEmail());
            }

            // Set admin rights based on groups from Keycloak
            try {
                String adminGroupName = Constants.getString("NTLMAdminGroupName");
                if (Tools.isNotEmpty(adminGroupName)) {
                    boolean isAdmin = false;
                    if (oauth2Groups.contains(adminGroupName)) {
                        isAdmin = true;
                    }
                    if (userDetails.isAdmin() != isAdmin) {
                        userDetails.setAdmin(isAdmin);
                        UsersDB.saveUser(userDetails);
                        Logger.info(OAuth2AdminSuccessHandler.class, "Set admin=" + isAdmin + " for user: " + userDetails.getEmail() + " based on Keycloak groups");
                    }
                }
            } catch (Exception e) {
                Logger.error(OAuth2AdminSuccessHandler.class, "Error setting admin flag for user: " + userDetails.getEmail(), e);
            }
        } catch (Exception ex) {
            Logger.error(OAuth2AdminSuccessHandler.class, "Error applying OAuth2 permissions for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Removes all group assignments from user
     */
    private void removeAllGroupAssignments(UserDetails userDetails) {
        try {
            // Remove from all user groups
            userDetails.setUserGroupsIds(null);

            // Remove from all permission groups
            List<PermissionGroupBean> userPermGroups = UserGroupsDB.getPermissionGroupsFor(userDetails.getUserId());
            for (PermissionGroupBean permGroup : userPermGroups) {
                UsersDB.deleteUserFromPermissionGroup(userDetails.getUserId(), permGroup.getUserPermGroupId());
            }
            Logger.info(OAuth2AdminSuccessHandler.class, "Removed user from all permission groups: " + userDetails.getEmail());
        } catch (Exception ex) {
            Logger.error(OAuth2AdminSuccessHandler.class, "Error removing all group assignments for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Synchronizes user groups - removes old and adds new
     */
    private void synchronizeUserGroups(UserDetails userDetails, List<UserGroupDetails> newUserGroups, List<PermissionGroupBean> newPermissionGroups) {
        String userEmail = userDetails != null ? userDetails.getEmail() : "unknown";
        try {
            if (userDetails == null) {
                Logger.error(OAuth2AdminSuccessHandler.class, "UserDetails is null, cannot synchronize groups");
                return;
            }

            // First remove from all existing groups
            removeAllGroupAssignments(userDetails);

            // Add new user groups
            if (!newUserGroups.isEmpty()) {
                for (UserGroupDetails group : newUserGroups) {
                    userDetails.addToGroup(group.getUserGroupId());
                    Logger.debug(OAuth2AdminSuccessHandler.class, "Added user to group: " + group.getUserGroupName());
                }
            }

            // Save all changes
            boolean saved = UsersDB.saveUser(userDetails);
            if (saved) {
                Logger.info(OAuth2AdminSuccessHandler.class, "Successfully synchronized user groups: " + userDetails.getUserGroupsIds());
                if (!newUserGroups.isEmpty()) {
                    Logger.info(OAuth2AdminSuccessHandler.class, "Synchronized user " + userDetails.getEmail() + " to " + newUserGroups.size() + " user groups");
                }
            } else {
                Logger.error(OAuth2AdminSuccessHandler.class, "Failed to save user groups synchronization for user: " + userDetails.getEmail());
            }

            // Add permission groups
            if (!newPermissionGroups.isEmpty()) {
                for (PermissionGroupBean group : newPermissionGroups) {
                    UsersDB.addUserToPermissionGroup(userDetails.getUserId(), group.getUserPermGroupId());
                    Logger.debug(OAuth2AdminSuccessHandler.class, "Added user to permission group: " + group.getTitle());
                }
                Logger.info(OAuth2AdminSuccessHandler.class, "Synchronized user " + userDetails.getEmail() + " to " + newPermissionGroups.size() + " permission groups");
            }
        } catch (Exception ex) {
            Logger.error(OAuth2AdminSuccessHandler.class, "Error synchronizing user groups for user: " + userEmail, ex);
        }
    }

}
