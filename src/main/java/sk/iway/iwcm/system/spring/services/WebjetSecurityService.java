package sk.iway.iwcm.system.spring.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 *
 *  V pripade, ze nejaka metoda ma byt dostupna len pre prihlaseneho pouzivatela, admina, prip. nejaku pouzivatelsku skupinu mozeme pouzit anotacie:
 *  @PreAuthorize("@WebjetSecurityService.isLogged()") - prihalseny pouzivatel
 *  @PreAuthorize("@WebjetSecurityService.isAdmin()") - admin
 *  @PreAuthorize("@WebjetSecurityService.isInUserGroup('nazov-skupiny')") - patri do skupiny
 *  @PreAuthorize("@WebjetSecurityService.hasPermission('editDir|addSubdir')") - ma pravo na modul editDir ALEBO addSubdir
 *  @PreAuthorize("@WebjetSecurityService.hasPermission('editDir&addSubdir')") - musi mat pravo na modul editDir SUCASNE na addSubdir
 *
 * @author mpijak
 */
@Service("WebjetSecurityService") //NOSONAR
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class WebjetSecurityService {

    private final HttpSession session;
    private final HttpServletRequest request;

    @Autowired
    public WebjetSecurityService(HttpSession session, HttpServletRequest request) {
        this.session = session;
        this.request = request;
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    protected boolean hasAuthority(String authority) {
        Authentication auth = getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(g-> g.getAuthority().equalsIgnoreCase(authority));
    }

    private UserDetails getUser() {
        return UsersDB.getCurrentUser(session);
    }

    public boolean isAdmin() {
        if (hasAuthority("ROLE_Group_admin")) {
            return true;
        }

        UserDetails currentUser = getUser();
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isLogged() {
        Authentication auth = getAuthentication();
        if (auth != null && !auth.getClass().isAssignableFrom(AnonymousAuthenticationToken.class)) {
            return true;
        }

        return getUser() != null;
    }

    /**
     * Check user permissions. Perms can be separated by '|' or '&amp;'.
     * If separated by '|' user must have at least one of this permissions.
     * If separated by '&amp;' user must have all of this permissions.
     * @param permission
     * @return
     */
    public boolean hasPermission(String permission) {
        if (!isAdmin()) {
            return false;
        }

        if (permission.startsWith("Constants:")) {
            //get permission from constants
            permission = Constants.getString(permission.substring(10));
        }

        //Cant contain both separators at same time
        if(permission.contains("|") && permission.contains("&")) {
            return false;
        }

        //OR user must have at least one of this permissions
        if(permission.contains("|")) {
            StringTokenizer st = new StringTokenizer(permission, "|");
            while (st.hasMoreTokens()) {
                if (hasAuthority("ROLE_Permission_" + normalizeUserGroupName(st.nextToken().trim()))) return true;
            }

            return false;
        }

        //AND user must have all of this permissions
        if(permission.contains("&")) {
            StringTokenizer st = new StringTokenizer(permission, "&");
            while (st.hasMoreTokens()) {
                if (hasAuthority("ROLE_Permission_" + normalizeUserGroupName(st.nextToken().trim())) == false) return false;
            }

            return true;
        }

        //Just need this one permission
        return hasAuthority("ROLE_Permission_" + normalizeUserGroupName(permission.trim()));
    }

    public boolean isInUserGroup(String group) {

        if (hasAuthority("ROLE_Group_" + normalizeUserGroupName(group))) {
            return true;
        }

        UserDetails currentUser = getUser();

        //Authentication auth = getAuthentication();

        if (currentUser == null) {
            return false;
        }

        String groupName = WebjetSecurityService.normalizeUserGroupName(group);
        String userGroupNames = currentUser.getUserGroupsNames();
        if (Tools.isNotEmpty(userGroupNames)) {
            for (String userGroupName : userGroupNames.split(",")) {
                if (groupName.equalsIgnoreCase(WebjetSecurityService.normalizeUserGroupName(userGroupName))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String normalizeUserGroupName(String userGropName) {
        String userGropNameLocal = DB.internationalToEnglish(userGropName);

        userGropNameLocal = userGropNameLocal.replaceAll("\\s+", "-");

        return userGropNameLocal.toLowerCase();
    }

    public boolean checkAccessAllowedOnController(Object controller) {
        //najskor over prava na celu triedu, lebo toto na DT metodach overwrite anotaciu celej triedy
        if (controller.getClass().isAnnotationPresent(PreAuthorize.class)) {
            String value = controller.getClass().getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
            //value je nieco ako: @WebjetSecurityService.hasPermission('editor_edit_media_group')
            if (value.startsWith("@WebjetSecurityService.isAdmin")) {
                if (isAdmin()==false) return false;
            } else if (value.startsWith("@WebjetSecurityService.isLogged")) {
                if (isLogged()==false) return false;
            } else if (value.startsWith("@WebjetSecurityService.")) {
                int dot = value.indexOf(".");
                int bracketStart = value.indexOf("(");
                int bracketEnd = value.indexOf(")");

                if (bracketEnd > bracketStart && bracketStart > dot) {
                    String methodName = value.substring(dot+1, bracketStart);
                    String perms = value.substring(bracketStart+1, bracketEnd).trim();
                    //odstran apostrofy/uvodzovky
                    perms = Tools.replace(perms, "'", "");
                    perms = Tools.replace(perms, "\"", "");
                    perms = perms.trim();

                    if ("hasPermission".equals(methodName)) {
                        if (hasPermission(perms)==false) return false;
                    } else if ("isInUserGroup".equals(methodName)) {
                        if (isInUserGroup(perms)==false) return false;
                    } else {
                        //neznama metoda, pre istotu vratme false
                        return false;
                    }
                }
                else {
                    //nieco je zle zapisane v anotacii
                    return false;
                }
            }
        }

        try {
            //custom metoda
            Method[] methods = controller.getClass().getMethods();
            Optional<Method> first = Arrays.stream(methods).filter(m->m.getName().equals("checkAccessAllowed")).findFirst();
            if (first.isPresent()) {
                Method method = first.get();
                boolean ret = (boolean) method.invoke(controller, request);
                //Logger.debug(WebjetSecurityService.class, "calling checkAccessAllowed, ret="+ret);
                return ret;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            //sk.iway.iwcm.Logger.error(e);
        }

        //zavolaj defaultnu v DTv2
        //if (DatatableRestControllerV2.class.isAssignableFrom(controller.getClass())) {
        //    return ((DatatableRestControllerV2)controller).checkAccessAllowed(request);
        //}

        return true;
    }
}