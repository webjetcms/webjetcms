package sk.iway.iwcm.components.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.google.crypto.tink.subtle.Hex;

import sk.iway.Password;

/**
 * Implementation for Rijnadel algorithm from cryptix package with standard java crypto packages
 */
public class Rijndael {

    private Rijndael() {
        //utility class
    }

    /**
     * Encrypt text by rijndaels algorithm
     * @param text
     * @param key
     * @return
     * @throws Exception
     */
    public static String encrypt(String text, String key) throws Exception {
        //dlzka encryptovaneho stringu musi byt nasobok 16
        if (text.length() % 16 > 0) {
            int len = ((text.length() / 16) + 1) * 16;
            text = text + "                                 ";
            if (text.length() > len) {
                text = text.substring(0, len);
            }
        }

        String to_crypt = Password.fromByteArray(text.getBytes());

        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding"); //NOSONAR
		//You can use ENCRYPT_MODE or DECRYPT_MODE
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Password.toByteArray(key), "AES"));
		byte[] ciphertext = cipher.doFinal(Password.toByteArray(to_crypt));

		String encoded = Hex.encode(ciphertext);
        return encoded;
    }

    /**
     * Decrypt data by rijndaels algorithm
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    public static String decrypt(String data, String key) throws Exception {
        if (data == null)
		{
			return ("");
		}
		if (data.length() < 10)
		{
			return (data);
		}

        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding"); //NOSONAR
		//You can use ENCRYPT_MODE or DECRYPT_MODE
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Password.toByteArray(key), "AES"));
		byte[] ciphertext = cipher.doFinal(Password.toByteArray(data));

		String decoded = new String(ciphertext).trim();
        return decoded;
    }
}
