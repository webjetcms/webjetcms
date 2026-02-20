package sk.iway.iwcm;

import java.security.NoSuchAlgorithmException;

/**
 * Crypto.java
 *
 * Class Crypto is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2020
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      29. 4. 2020 12:24
 * modified     29. 4. 2020 12:24
 */

public interface Crypto {

    CryptoKeys generateNewPrivateAndPublicKey(String login) throws NoSuchAlgorithmException;
    String encrypt(String plainData, String publicKeyBASE64) throws Exception;
    String decrypt(String encryptedDataBASE64, String privateKeyBASE64) throws Exception;
    String getAlgKey();
}
