package sk.iway.iwcm.admin.layout;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.users.SettingsAdminBean;
import sk.iway.iwcm.users.UserDetails;

/**
 * DTO objekt prihlaseneho pouzivatela, je v JS kode dostupny ako
 * window.currentUser
 */
@Getter
@Setter
public class UserDto {
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String photo;
    private int userId;
    private String login;
    private Map<String, String> adminSettings;

    public UserDto(UserDetails user) {
        firstName = user.getFirstName();
        lastName = user.getLastName();
        fullName = user.getFullName();
        email = user.getEmail();
        photo = user.getPhoto();
        userId = user.getUserId();
        login = user.getLogin();

        adminSettings = new HashMap<>();

        Map<String, SettingsAdminBean> settingsMap = user.getAdminSettings();
        for (Map.Entry<String, SettingsAdminBean> entry : settingsMap.entrySet()) {
            SettingsAdminBean s = entry.getValue();
            adminSettings.put(s.getSkey(), s.getValue());
        }
    }
}
