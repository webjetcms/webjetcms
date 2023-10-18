package sk.iway.iwcm.users;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.googleauth.GoogleAuthenticator;
import sk.iway.iwcm.system.googleauth.GoogleAuthenticatorKey;
import sk.iway.iwcm.system.googleauth.GoogleAuthenticatorQRGenerator;

@RestController
public class UsersRestServiceController {

    /* Vrati Key voci ktoremu sa overi token zadany uzivatelom*/
    @GetMapping(path={"/admin/users/2factorauthNew"})
    public String getGauthNew( HttpServletRequest request )
    {
        //System.out.println("___________________________________");
        UserDetails user = UsersDB.getCurrentUser(request.getSession());
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        int scratchCode = key.getScratchCodes().get(0);
        String jsonString = null;
        try {
            jsonString = new JSONObject()
                    .put("secret", key.getKey())
                    .put("url", GoogleAuthenticatorQRGenerator.getOtpAuthURL("WebJET "+ Constants.getInstallName() + " (" + Tools.getServerName(request)+")", user.getLogin(),key))
                    .put("scratch", scratchCode).toString();
        } catch (JSONException e) {
            sk.iway.iwcm.Logger.error(e);
        }
        //System.out.println(jsonString);
        return jsonString;
    }

    /* Vrati Key voci ktoremu sa overi token zadany uzivatelom*/
    @GetMapping(path={"/admin/users/2factorauth"})
    public String getGauth( HttpServletRequest request )
    {
        //TODO: nie som si isty, ci je dobre nejako takto verejne ukazovat 2FA token, predpokladam, ze ked niekto ziska token vie k nemu generovat kody
        UserDetails user = UsersDB.getCurrentUser(request.getSession());
        return user.getMobileDevice();
    }

    /* Nastavi / vynyluje key MobileDevice usera*/
    @PostMapping(path={"/admin/users/2factorauth"})
    public void setGauth( @RequestParam(value="secret", defaultValue="") String secret, HttpServletRequest request )
    {
        //TODO: toto je cele zle, lebo ak ma user zapnutu 2FA tak zavolanim tohto URL ju moze utocnik vymazat/nastavit ako potrebuje
        UserDetails user = UsersDB.getCurrentUser(request.getSession());

        if (Tools.isEmpty(secret) )
            user.setMobileDevice(null);
        else
            user.setMobileDevice(secret);

        UsersDB.saveUser(user);
    }
}
