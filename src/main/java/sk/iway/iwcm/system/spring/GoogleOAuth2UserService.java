package sk.iway.iwcm.system.spring;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.users.UsersDB;

@Service
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            Logger.error(GoogleOAuth2UserService.class, "Google OAuth2 user nemá email");
            throw new OAuth2AuthenticationException("Google OAuth2 user nemá email");
        }
        Identity user = new Identity(UsersDB.getUserByEmail(email, 1));
        if (user == null || user.isAuthorized() == false) {
            Logger.error(GoogleOAuth2UserService.class, "Používateľ s emailom " + email + " neexistuje alebo nie je autorizovaný");
            throw new OAuth2AuthenticationException("Používateľ s emailom " + email + " neexistuje alebo nie je autorizovaný");
        }
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attr.getRequest();
        HttpSession session = request.getSession();
        LogonTools.setUserToSession(session, user);
        Logger.info(GoogleOAuth2UserService.class, "Google OAuth2 login OK pre " + email);
        return oAuth2User;
    }
}
