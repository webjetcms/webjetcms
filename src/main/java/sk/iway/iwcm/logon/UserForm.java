package sk.iway.iwcm.logon;
/**
 * UserForm.java
 *
 * Class UserForm is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      14.9.2018 14:00
 * modified     14.9.2018 14:00
 */

public class UserForm {

    // LOGIN
    private String username;
    private String password;
    private String language;

    // EDIT PASSWORD
    private String newPassword;
    private String retypeNewPassword;

    // CHANGE PASSWORD
    private String login;
    private String auth;
    private String selectedLogin;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getRetypeNewPassword() {
        return retypeNewPassword;
    }

    public void setRetypeNewPassword(String retypeNewPassword) {
        this.retypeNewPassword = retypeNewPassword;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getSelectedLogin() {
        return selectedLogin;
    }

    public void setSelectedLogin(String selectedLogin) {
        this.selectedLogin = selectedLogin;
    }
}
