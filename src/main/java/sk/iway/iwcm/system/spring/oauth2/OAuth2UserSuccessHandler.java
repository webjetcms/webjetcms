package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UserGroupDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 Success Handler for customer zone (user logon)
 * Unlike OAuth2AdminSuccessHandler for admin zone:
 * - Does not set isAdmin flag
 * - Does not set permission groups
 * - Maps only user groups
 */
public class OAuth2UserSuccessHandler extends AbstractOAuth2SuccessHandler {

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
                Logger.error(OAuth2UserSuccessHandler.class, "OAuth2 email not found");
                handleError(request, response, "oauth2_email_not_found", "/");
                return;
            }

            UserDetails userDetails = UsersDB.getUserByEmail(email, 1);
            if (userDetails == null) {
                // Create new user
                userDetails = createNewUserFromOAuth2(oauth2User, email);
                if (userDetails == null) {
                    Logger.error(OAuth2UserSuccessHandler.class, "Failed to create user for email: " + email);
                    handleError(request, response, "oauth2_user_create_failed", "/");
                    return;
                }
                Logger.info(OAuth2UserSuccessHandler.class, "Created new user for email: " + email);
            } else {
                // Update existing user with new data from OAuth2
                updateExistingUserFromOAuth2(oauth2User, userDetails);
            }

            // Apply user groups from OAuth2 only for configured providers
            String providerId = getProviderId(authentication);
            if (shouldSyncPermissions(providerId)) {
                Logger.info(OAuth2UserSuccessHandler.class, "Applying OAuth2 user groups for provider: " + providerId);
                applyOAuth2UserGroups(oauth2User, userDetails);
            } else {
                Logger.info(OAuth2UserSuccessHandler.class, "Skipping group synchronization for provider '" + providerId + "' (not configured in oauth2_clientsWithPermissions)");
            }

            Identity identity = new Identity(userDetails);
            identity.setValid(true);
            HttpSession session = request.getSession();
            LogonTools.setUserToSession(session, identity);

            Authentication springAuth = WebjetAuthentificationProvider.authenticate(identity);
            SecurityContextHolder.getContext().setAuthentication(springAuth);

            // Redirect after login
            String afterLogonRedirect = (String)session.getAttribute("afterLogonRedirect");
            if (afterLogonRedirect != null) {
                response.sendRedirect(afterLogonRedirect);
            } else {
                response.sendRedirect("/");
            }
        } catch (Exception ex) {
            Logger.error(OAuth2UserSuccessHandler.class, ex);
            handleError(request, response, "oauth2_exception", "/");
        }
    }

    /**
     * Applies user groups from OAuth2 attributes to user (only for configured providers)
     * DOES NOT apply permission groups or admin rights - this is user zone!
     */
    private void applyOAuth2UserGroups(OAuth2User oauth2User, UserDetails userDetails) {
        try {
            List<String> oauth2Groups = extractGroupsFromOAuth2(oauth2User);
            Logger.info(OAuth2UserSuccessHandler.class, "Found OAuth2 groups for user " + userDetails.getEmail() + ": " + oauth2Groups);

            if (oauth2Groups.isEmpty()) {
                Logger.info(OAuth2UserSuccessHandler.class, "No OAuth2 groups found for user " + userDetails.getEmail());
                return;
            }

            // Find existing user groups in WebJET
            List<UserGroupDetails> matchingUserGroups = new ArrayList<>();

            try {
                // Load all user groups and filter by name
                List<UserGroupDetails> allUserGroups = UserGroupsDB.getInstance().getUserGroups();
                for (UserGroupDetails userGroup : allUserGroups) {
                    if (oauth2Groups.contains(userGroup.getUserGroupName())) {
                        matchingUserGroups.add(userGroup);
                        Logger.debug(OAuth2UserSuccessHandler.class, "Found matching user group: " + userGroup.getUserGroupName());
                    }
                }

            } catch (Exception ex) {
                Logger.error(OAuth2UserSuccessHandler.class, "Error loading groups for user: " + userDetails.getEmail(), ex);
                return;
            }

            // Synchronize groups - remove old and add new
            synchronizeUserGroups(userDetails, matchingUserGroups);

            if (matchingUserGroups.isEmpty()) {
                Logger.info(OAuth2UserSuccessHandler.class, "No matching user groups found for user " + userDetails.getEmail());
            }

        } catch (Exception ex) {
            Logger.error(OAuth2UserSuccessHandler.class, "Error applying OAuth2 user groups for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Removes all group assignments from user
     */
    private void removeAllGroupAssignments(UserDetails userDetails) {
        try {
            // Remove from all user groups
            userDetails.setUserGroupsIds(null);
            Logger.info(OAuth2UserSuccessHandler.class, "Removed user from all user groups: " + userDetails.getEmail());
        } catch (Exception ex) {
            Logger.error(OAuth2UserSuccessHandler.class, "Error removing all group assignments for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Synchronizes user groups - removes old and adds new
     */
    private void synchronizeUserGroups(UserDetails userDetails, List<UserGroupDetails> newUserGroups) {
        String userEmail = userDetails != null ? userDetails.getEmail() : "unknown";
        try {
            if (userDetails == null) {
                Logger.error(OAuth2UserSuccessHandler.class, "UserDetails is null, cannot synchronize groups");
                return;
            }

            // First remove from all existing groups
            removeAllGroupAssignments(userDetails);

            // Add new user groups
            if (!newUserGroups.isEmpty()) {
                for (UserGroupDetails group : newUserGroups) {
                    userDetails.addToGroup(group.getUserGroupId());
                    Logger.debug(OAuth2UserSuccessHandler.class, "Added user to group: " + group.getUserGroupName());
                }
            }

            // Save all changes
            boolean saved = UsersDB.saveUser(userDetails);
            if (saved) {
                Logger.info(OAuth2UserSuccessHandler.class, "Successfully synchronized user groups: " + userDetails.getUserGroupsIds());
                if (!newUserGroups.isEmpty()) {
                    Logger.info(OAuth2UserSuccessHandler.class, "Synchronized user " + userDetails.getEmail() + " to " + newUserGroups.size() + " user groups");
                }
            } else {
                Logger.error(OAuth2UserSuccessHandler.class, "Failed to save user groups synchronization for user: " + userDetails.getEmail());
            }

        } catch (Exception ex) {
            Logger.error(OAuth2UserSuccessHandler.class, "Error synchronizing user groups for user: " + userEmail, ex);
        }
    }
}
