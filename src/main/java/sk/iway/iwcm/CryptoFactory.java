package sk.iway.iwcm;

import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.crypto.CryptoRsa2048;
import sk.iway.iwcm.components.crypto.CryptoTink;

import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;

/**
 * CryptoFactory.java
 * <p>
 * Class CryptoFactory is used for
 * <p>
 * <p>
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2020
 *
 * @author $Author: mhruby $
 * @version $Revision: 1.0 $
 * created      29. 4. 2020 13:19
 * modified     29. 4. 2020 13:19
 */

public class CryptoFactory {

    // handluje I/O plaintext a encryptovanych dat
    // tu handlujem vsetko na vyse (vsetky hovadiny ktore musim pridat k zasifrovanym dat)

    /**
     * Staticka metoda na jednoduche desifrovanie dat, sama si zisti, ci su sifrovane, ziska kluc zo session a skusi desifrovat
     * @param maybeEncryptedText
     * @return
     */
    public static String decrypt(String maybeEncryptedText) {
        if (Tools.isEmpty(maybeEncryptedText) || maybeEncryptedText.startsWith("encrypted-")==false) return maybeEncryptedText;

        String privateKey = CryptoFactory.getCurrentPrivateKey();
        if (Tools.isEmpty(privateKey)) return maybeEncryptedText;

        return CryptoFactory.decrypt(maybeEncryptedText, privateKey); //NOSONAR
    }

    public CryptoKeys generateKeys(String login) {
        Crypto crypto;
        int id = PkeyGenerator.getNextValue("crypto_key");
        String loginIdprefix = DocTools.removeChars(login, true)+"_"+id;
        if ("CryptoTink".equalsIgnoreCase(Constants.getString("cryptoAlg"))) {
            Logger.debug(getClass(), "Using CryptoTink");
            crypto = CryptoTink.getInstance();
        } else {
            Logger.debug(getClass(), "Using CryptoRsa2048");
            crypto = CryptoRsa2048.INSTANCE;
        }
        CryptoKeys cryptoKeys = null;
        try {
            cryptoKeys = crypto.generateNewPrivateAndPublicKey(login);
            cryptoKeys.setPrivateKeyEncoded("decrypt_key" + crypto.getAlgKey() + loginIdprefix + ":" + cryptoKeys.getPrivateKeyEncoded());
            cryptoKeys.setPublicKeyEncoded("encrypt_key" + crypto.getAlgKey() + loginIdprefix + ":" + cryptoKeys.getPublicKeyEncoded());
        } catch (NoSuchAlgorithmException e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return cryptoKeys;
    }

    public String encrypt(String plainText, String publicKeyBASE64) {

        if (Tools.isEmpty(publicKeyBASE64)) return plainText;

        // podla kluca rozhodnem ktory sifrovaciu implementaciu pouzijem nezaujima ma ako sifrovanie funguje
        Crypto crypto;
        if (publicKeyBASE64.contains(CryptoTink.ALG_KEY))
            crypto = CryptoTink.getInstance();
        else
            crypto = CryptoRsa2048.INSTANCE;
        // pridam prefix z kluca k datam aby pouzivatel vedel kto a ktorym klucom zasifroval dane data
        try {
            RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (rb != null) {
                //ak mame zapnute sifrovanie musime vypnut auditovanie request parametrov
                LinkedHashMap<String, String[]> map = new LinkedHashMap<>();
                rb.setParameters(map);
            }

            return Tools.replace(CryptoFactory.getPrefix(publicKeyBASE64), "encrypt_key", "encrypted") + ":" + crypto.encrypt(plainText, removeRedundantPrefix(publicKeyBASE64));
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
        //ked padne sifrovanie radsej vratim povodne data, aby sme o ne neprisli
        return plainText;
    }

    public static String decrypt(String encryptedText, String privateKey) {
        if (Tools.isEmpty(privateKey) || encryptedText==null || encryptedText.startsWith("encrypted")==false)
            return encryptedText;

        Crypto crypto;
        if (privateKey.contains(CryptoTink.ALG_KEY))
            crypto = CryptoTink.getInstance();
        else
            crypto = CryptoRsa2048.INSTANCE;

        // odstranujem nepotrebne data z encryptovanych dat a kluca
        try {
            String decrypted = crypto.decrypt(removeRedundantPrefix(encryptedText), removeRedundantPrefix(privateKey));
            return decrypted;
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
        //desifrovanie sa nepodarilo, vratim povodny zasifrovany text
        return encryptedText;
    }

    /**
     * Metoda odstrani redundantne (nepotrebne) znaky z kluca
     *
     * @param text
     * @return
     */
    public static String removeRedundantPrefix(String text) {
        String cleanKey = text;
        int i = cleanKey.indexOf(":");
        if (i > 0) {
            cleanKey = cleanKey.substring(i + 1);
        }

        cleanKey = cleanKey.replace("\n", "").replace("\r", "");

        return cleanKey;
    }

    /**
     * Ziska prefix kluca alebo zasifrovanych dat.
     * Priklad: key = encrypted-v2-admin_32:ewogICAgI => encrypted-v2-admin_32
     *
     * @param string
     * @return
     */
    public static String getPrefix(String string) {
        int i = string.indexOf(":");
        if (i > 0) {
            return string.substring(0, i);
        }
        return "";
    }

    public static String getCurrentPrivateKey() {
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean != null) {
            return requestBean.getCryptoPrivateKey();
        }
        return null;
    }

    /**
     * Ulozi privatny kluc do session. Tento kluc sa potom pouzije v JPACryptoConverteri na desifrovanie dat.
     * Vrati true/false podla toho ci sa kluc podarilo vlozit do session a requestBeanu
     * @param session
     * @param privateKey privatny kluc ktory sa pouzije na desifrovanie dat
     * @return podarilo sa ulozit kluc
     */
    public boolean setPrivateKeyToSession(String privateKey, HttpSession session) {
        session.setAttribute("JPACryptoConverter.privateKey", privateKey);
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean != null) {
            requestBean.setCryptoPrivateKey(privateKey);
            return true;
        }
        return false;
    }
}
