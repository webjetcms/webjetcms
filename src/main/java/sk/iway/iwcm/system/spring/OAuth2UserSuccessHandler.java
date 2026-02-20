package sk.iway.iwcm.system.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UserGroupDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 Success Handler pre zákaznícku zónu (user logon)
 * Na rozdiel od OAuth2AdminSuccessHandler pre admin zónu:
 * - Nenastavuje isAdmin flag
 * - Nenastavuje permission groups (skupiny práv)
 * - Mapuje iba user groups (skupiny používateľov)
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
                // Vytvor nového používateľa
                userDetails = createNewUserFromOAuth2(oauth2User, email);
                if (userDetails == null) {
                    Logger.error(OAuth2UserSuccessHandler.class, "Failed to create user for email: " + email);
                    handleError(request, response, "oauth2_user_create_failed", "/");
                    return;
                }
                Logger.info(OAuth2UserSuccessHandler.class, "Created new user for email: " + email);
            } else {
                // Aktualizuj existujúceho používateľa s novými údajmi z OAuth2
                updateExistingUserFromOAuth2(oauth2User, userDetails);
            }

            // Aplikuj user groups z OAuth2 iba pre nakonfigurovaných providerov
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

            // Redirect po prihlásení
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
     * Aplikuje user groups z OAuth2 atribútov na používateľa (iba pre nakonfigurovaných providerov)
     * NEAPLIKUJE permission groups ani admin práva - toto je user zóna!
     */
    private void applyOAuth2UserGroups(OAuth2User oauth2User, UserDetails userDetails) {
        try {
            List<String> oauth2Groups = extractGroupsFromOAuth2(oauth2User);
            Logger.info(OAuth2UserSuccessHandler.class, "Found OAuth2 groups for user " + userDetails.getEmail() + ": " + oauth2Groups);

            if (oauth2Groups.isEmpty()) {
                Logger.info(OAuth2UserSuccessHandler.class, "No OAuth2 groups found for user " + userDetails.getEmail());
                return;
            }

            // Nájdi existujúce skupiny používateľov v WebJET
            List<UserGroupDetails> matchingUserGroups = new ArrayList<>();

            try {
                // Načítaj všetky user groups a filtruj podľa názvu
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

            // Synchronizuj skupiny - odstráň staré a pridaj nové
            synchronizeUserGroups(userDetails, matchingUserGroups);

            if (matchingUserGroups.isEmpty()) {
                Logger.info(OAuth2UserSuccessHandler.class, "No matching user groups found for user " + userDetails.getEmail());
            }

        } catch (Exception ex) {
            Logger.error(OAuth2UserSuccessHandler.class, "Error applying OAuth2 user groups for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Odstráni všetky skupinové priradenia používateľa
     */
    private void removeAllGroupAssignments(UserDetails userDetails) {
        try {
            // Odstráň zo všetkých skupín používateľov
            userDetails.setUserGroupsIds(null);
            Logger.info(OAuth2UserSuccessHandler.class, "Removed user from all user groups: " + userDetails.getEmail());
        } catch (Exception ex) {
            Logger.error(OAuth2UserSuccessHandler.class, "Error removing all group assignments for user: " + userDetails.getEmail(), ex);
        }
    }

    /**
     * Synchronizuje skupiny používateľa - odstráni staré a pridá nové
     */
    private void synchronizeUserGroups(UserDetails userDetails, List<UserGroupDetails> newUserGroups) {
        String userEmail = userDetails != null ? userDetails.getEmail() : "unknown";
        try {
            if (userDetails == null) {
                Logger.error(OAuth2UserSuccessHandler.class, "UserDetails is null, cannot synchronize groups");
                return;
            }

            // Najprv odstráň zo všetkých existujúcich skupín
            removeAllGroupAssignments(userDetails);

            // Pridaj nové user groups
            if (!newUserGroups.isEmpty()) {
                for (UserGroupDetails group : newUserGroups) {
                    userDetails.addToGroup(group.getUserGroupId());
                    Logger.debug(OAuth2UserSuccessHandler.class, "Added user to group: " + group.getUserGroupName());
                }
            }

            // Ulož všetky zmeny
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
