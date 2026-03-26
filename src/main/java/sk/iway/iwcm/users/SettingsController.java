package sk.iway.iwcm.users;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;

/**
 * PredmetController.java
 *
 * #31429/8 Ucebne texty - pridanie noveho predmetu
 * #31429/25 Ucebne texty - editacia uvodneho textu predmetu
 *
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 *
 * @author tmarcinkova $
 * @created 2019/05/23 10:31
 */
@Controller
@RequestMapping("/components/users")
public class SettingsController
{
    @PutMapping(path = "/add/{settingsName}/{value}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@WebjetSecurityService.isLogged()")
    public ResponseEntity<Object> add(@PathVariable("settingsName") String settingsName, @PathVariable("value") String value, HttpSession session)
    {
        String[] allowedSettings = Tools.getTokens(Constants.getString("allowedUserSettingsNames"), ",");

        boolean allowed = false;
        for (int i = 0 ; i < allowedSettings.length ; i++) {
            if (allowedSettings[i].equals(settingsName)) {
                allowed = true;
                break;
            }
        }
        if (!allowed)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Identity user = UsersDB.getCurrentUser(session);


        Map<String, SettingsBean> settings = UsersDB.getUser(user.getUserId()).getSettings();
        SettingsBean userSettings = new SettingsBean();
        userSettings.setUserId(user.getUserId());
        userSettings.setSkey(settingsName);
        userSettings.setSvalue1(value);

        settings.put(settingsName, userSettings);
        if (UsersDB.setSettings(user.getUserId(), settings)) {
            user.setSettings(settings);
            LogonTools.setUserToSession(session, user);
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("result", true);
        } catch (JSONException e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return ResponseEntity.ok(jsonObject.toString());
    }
}