package sk.iway.iwcm.users.crypto;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

public class GenerateNewCredentials extends BaseWebjetTest {

    @Test
    void generatePassword() {
        String password = System.getenv("CODECEPT_DEFAULT_PASSWORD");
        String passwordSha512 = password+".sha512";
        String passwordBcrypt = password+".bcrypt";

        //SHA512
        Sha512 sha512 = new Sha512();
        String salt = sha512.generateSalt();
        String hash = sha512.calculateHash(passwordSha512, salt);
        //System.out.println("SHA512 Password: " + passwordSha512);
        System.out.println("SHA512 Salt: " + salt);
        System.out.println("SHA512 Hash: " + hash);

        //BCRYPT
        Bcrypt bcrypt = new Bcrypt();
        String bcryptSalt = bcrypt.generateSalt();
        bcrypt = new Bcrypt();
        String bcryptHash = bcrypt.calculateHash(passwordBcrypt, bcryptSalt);
        //System.out.println("BCRYPT Password: " + passwordBcrypt);
        System.out.println("BCRYPT Salt: " + bcryptSalt);
        System.out.println("BCRYPT Hash: " + bcryptHash);

        //UPDATE SQL - see DEBUG CONSOLE and switch to Launch Java Tests view
        System.out.println("UPDATE users SET password = '"+hash+"', password_salt = '"+salt+"' WHERE login = 'user_sha512';");
        System.out.println("UPDATE users SET password = '"+bcryptHash+"', password_salt = 'bcrypt:"+bcryptSalt+"' WHERE login = 'user_bcrypt';");
    }

}
