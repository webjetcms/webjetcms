package sk.iway.iwcm.system.spring.passkey;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Logger;
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

        List<GrantedAuthority> authorities = new ArrayList<>(); //they will be populated in LogonTools.logonUserWithAllChecks later

        return new User(username, "N/A", true, true, true, true, authorities);
    }

}
