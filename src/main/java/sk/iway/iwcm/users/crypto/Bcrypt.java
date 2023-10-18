package sk.iway.iwcm.users.crypto;

import org.springframework.security.crypto.bcrypt.BCrypt;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.users.PasswordSecurityAlgorithm;

/**
 * Hashovanie hesiel pomocou bcrypt algoritmu
 */
public class Bcrypt implements PasswordSecurityAlgorithm {

    public String generateSalt() {
        return "bcrypt:" + BCrypt.gensalt(Constants.getInt("bcryptSaltRounds"));
    }

    public String calculateHash(String password, String salt) {
        //Check if salt does have prefix "bcrypt"
        if(salt.startsWith("bcrypt:"))
            return "bcrypt:" + BCrypt.hashpw(password, salt.replaceFirst("^bcrypt:", ""));
        else
            return "bcrypt:" + BCrypt.hashpw(password, salt);
    }

    public boolean isPasswordCorrect (String password, String salt, String hash) {
        //Check if hash does have prefix "bcrypt"
        if(hash.startsWith("bcrypt:"))
            return BCrypt.checkpw(password, hash.replaceFirst("^bcrypt:", ""));
        else
            return BCrypt.checkpw(password, hash);
    }
}
