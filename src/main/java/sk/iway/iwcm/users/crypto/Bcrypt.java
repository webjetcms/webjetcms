package sk.iway.iwcm.users.crypto;

import java.nio.charset.StandardCharsets;

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
        // CVE-2025-22228: bcrypt only uses the first 72 UTF-8 bytes, not characters.
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > 72) return false;

        //Check if hash does have prefix "bcrypt"
        if(hash.startsWith("bcrypt:"))
            return BCrypt.checkpw(password, hash.replaceFirst("^bcrypt:", ""));
        else
            return BCrypt.checkpw(password, hash);
    }
}
