package sk.iway.iwcm.system.spring.passkey;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;
import sk.iway.iwcm.users.UsersDB;

/**
 * Spring Security UserDetailsService implementation for WebAuthn/PassKey.
 * Used by WebAuthnAuthenticationProvider to load user details and authorities
 * after successful passkey verification.
 *
 * This class implements org.springframework.security.core.userdetails.UserDetailsService
 * (different from sk.iway.iwcm.components.users.userdetail.UserDetailsService).
 */
@Service("webauthnUserDetailsService")
public class WebjetWebAuthnUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Logger.debug(WebjetWebAuthnUserDetailsService.class, "loadUserByUsername: " + username);

        // Try to find admin user by login
        sk.iway.iwcm.users.UserDetails userDetails = UsersDB.getUser(username);
        if (userDetails == null || userDetails.isAdmin() == false) {
            throw new UsernameNotFoundException("User not found or is not admin: " + username);
        }

        Identity identity = new Identity(userDetails);
        List<GrantedAuthority> authorities = buildAuthorities(identity, userDetails);

        return new User(username, "N/A", true, true, true, true, authorities);
    }

    /**
     * Builds the list of granted authorities for the user.
     * Follows the same logic as WebjetAuthentificationProvider.authenticate(Identity).
     */
    private List<GrantedAuthority> buildAuthorities(Identity identity, sk.iway.iwcm.users.UserDetails userDetails) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String groupName : Tools.getTokens(userDetails.getUserGroupsNames(), ",")) {
            groupName = WebjetSecurityService.normalizeUserGroupName(groupName);
            authorities.add(new SimpleGrantedAuthority("ROLE_Group_" + groupName));
        }

        if (userDetails.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_Group_admin"));
        }

        for (ModuleInfo mod : Modules.getInstance().getAvailableModules()) {
            if (identity.isDisabledItem(mod.getItemKey()) == false && identity.isDisabledItem(mod.getItemKey()) == false) {
                String itemKey = WebjetSecurityService.normalizeUserGroupName(mod.getItemKey());
                authorities.add(new SimpleGrantedAuthority("ROLE_Permission_" + itemKey));
            }

            if (mod.getSubmenus() != null) {
                for (ModuleInfo submod : mod.getSubmenus()) {
                    if (Tools.isEmpty(submod.getItemKey())) continue;
                    if (identity.isDisabledItem(submod.getItemKey())) continue;
                    if (identity.isDisabledItem(submod.getItemKey())) continue;

                    String itemKey = WebjetSecurityService.normalizeUserGroupName(submod.getItemKey());
                    authorities.add(new SimpleGrantedAuthority("ROLE_Permission_" + itemKey));
                }
            }
        }

        return authorities;
    }
}
