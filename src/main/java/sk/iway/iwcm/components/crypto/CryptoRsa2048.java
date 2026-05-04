package sk.iway.iwcm.components.crypto;

import javax.crypto.Cipher;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Crypto;
import sk.iway.iwcm.CryptoKeys;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * CryptoTools - trieda sluzi na sifrovanie a desifrovanie textovych retazcov algoritmom RSA
 * s dlzkou kluca 2048
 */
public class CryptoRsa2048 implements Crypto {
    public static CryptoRsa2048 INSTANCE = new CryptoRsa2048();
    private String ALGORITHM = "RSA";
    private int KEY_SIZE = 2048;

    public CryptoRsa2048() {
    }

    /**
     * Metoda zasifruje data, vrati vo formate base64-enkodovane
     * @param plainData data ktore maju byt zasifrovane
     * @param publicKey verejny kluc ktorym budu data zasifrovane
     * @return zasifrovane data
     */
    public String encrypt(String plainData, String publicKey) throws Exception {
        //ak nemame publicKey tak nic nesifrujeme, vratime ako je
        if (Tools.isEmpty(publicKey))
            return plainData;

        String cleanPublicKey = publicKey;
        Key key = loadPublicKey(cleanPublicKey);
        byte[] encryptedData = encrypt(key, plainData);
        String encrypted = new String(Base64.getEncoder().encode(encryptedData));
        return encrypted;
    }

    /**
     * Metoda desifruje zasifrovane data algoritmom RSA
     * @param encryptedData zasifrovane data
     * @param privateKey sukromny kluc
     * @return desifrovane data
     */
    public String decrypt(String encryptedData, String privateKey) throws Exception {

        // hodnota zacina na encrypted-rsa2048-admin_2 pouzije sa CryptoTools,
        // hodnota zacina na encrypted-v1- pouzije sa CryptoToolsV2
        
        String cleanPrivateKey = privateKey;
        Key key = loadPrivateKey(cleanPrivateKey);
        byte[] data = Base64.getDecoder().decode(((encryptedData).getBytes()));
        final String decrypted = new String(decrypt(key, data));
        return decrypted;
    }

    @Override
    public String getAlgKey() {
        return "-"+ALGORITHM.toLowerCase()+KEY_SIZE+"-";
    }

    /**
     * Vygeneruje privatny a verejny kluc a vrati ho v objekte aj enkodovane
     * @return CrypTokeys objekt obsahuje oba kluce aj enkodovane
     * @throws NoSuchAlgorithmException
     */
    @Override
    public CryptoKeys generateNewPrivateAndPublicKey(String loginName) throws NoSuchAlgorithmException {
        KeyPair keyPair = this.buildKeyPair();

        //int id = PkeyGenerator.getNextValue("crypto_key");

        String privateKeyEncoded = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKeyEncoded = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        boolean showLog = Constants.getBoolean("cryptoToolsShowGeneratedKeysLog");
        if (showLog) {
            Logger.println(CryptoRsa2048.class, "=== Generating RSA keys ===");
            Logger.println(CryptoRsa2048.class, "Generated Private key => " + privateKeyEncoded);
            Logger.println(CryptoRsa2048.class, "Generated Public key => " + publicKeyEncoded);
            Logger.println(CryptoRsa2048.class, "===========================");
        }
        return new CryptoKeys(keyPair, publicKeyEncoded, privateKeyEncoded);
    }

    /**
     * Metoda vygeneruje sukromny a verejny kluc na sifrovanie a desifrovanie dat.
     * @return KeyPair objekt s klucami
     * @throws NoSuchAlgorithmException
     */
    private KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = KEY_SIZE;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.genKeyPair();
    }

    private byte[] encrypt(Key publicKey, String sensitiveData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(sensitiveData.getBytes());
    }

    private byte[] decrypt(Key privateKey, byte [] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(encrypted);
    }

    private Key loadPublicKey(String publicKey) throws GeneralSecurityException {
        byte[] data = Base64.getDecoder().decode((publicKey.getBytes()));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance(ALGORITHM);
        return fact.generatePublic(spec);
    }

    private Key loadPrivateKey(String key64) throws GeneralSecurityException {
        byte[] clear = Base64.getDecoder().decode(key64.getBytes());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        KeyFactory fact = KeyFactory.getInstance(ALGORITHM);
        PrivateKey priv = fact.generatePrivate(keySpec);
        return priv;
    }

}
