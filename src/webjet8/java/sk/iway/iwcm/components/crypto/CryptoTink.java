package sk.iway.iwcm.components.crypto;

import com.google.crypto.tink.*;
import com.google.crypto.tink.hybrid.HybridConfig;

import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Crypto;
import sk.iway.iwcm.CryptoKeys;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConfDB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * CryptoTink je verzia sifrovania s vyuzitim kniznice google tink - https://github.com/google/tink
 * Pouziva sa hybridne sifrovanie pre podporu dlhych dat
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2019
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      11. 10. 2019 15:30
 * modified     11. 10. 2019 15:30
 */

public class CryptoTink implements Crypto {

    public static String ALG_KEY = "-tink-";

    private static CryptoTink INSTANCE = new CryptoTink();

    public static CryptoTink getInstance() {
        return INSTANCE;
    }

    private CryptoTink() {
        try {
            HybridConfig.register();
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @Override
    public CryptoKeys generateNewPrivateAndPublicKey(String login) {
        KeysetHandle keysetHandle = this.generateNewPrivateAndPublicKey2();
        if (keysetHandle==null) return null;
        try {
            String privateKeyEncoded = this.getKeyBase64(keysetHandle);
            String publicKeyEncoded = this.getKeyBase64(keysetHandle.getPublicKeysetHandle()); // public key viem dostat z privatneho kluca
            return new CryptoKeys(null, publicKeyEncoded, privateKeyEncoded);
        } catch (GeneralSecurityException e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return null;
    }

    @Override
    public String encrypt(String plainData, String publicKey) throws Exception {
        KeysetHandle publicKeyHandle = this.loadPublicKeyBase64(publicKey);
        if (publicKeyHandle == null) return "";
        HybridEncrypt hybridEncrypt = publicKeyHandle.getPrimitive(RegistryConfiguration.get(), HybridEncrypt.class);
        byte[] encryptedData = hybridEncrypt.encrypt(plainData.getBytes(), this.getContextInfo().getBytes());

        return new String(Base64.getEncoder().encode(encryptedData));
    }

    @Override
    public String decrypt(String encryptedData, String privateKey) throws Exception {
        KeysetHandle privateKeyHandle = this.loadPrivateKeyBase64(privateKey);
        if (privateKeyHandle == null) return "";
        HybridDecrypt hybridDecrypt = privateKeyHandle.getPrimitive(RegistryConfiguration.get(), HybridDecrypt.class);

        return new String(hybridDecrypt.decrypt(Base64.getDecoder().decode(encryptedData.getBytes()), this.getContextInfo().getBytes()));
    }

    @Override
    public String getAlgKey() {
        return ALG_KEY;
    }

    protected KeysetHandle generateNewPrivateAndPublicKey2() {
        try {
            KeysetHandle privateKey = KeysetHandle.generateNew(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256"));
            KeysetHandle publiceKey = privateKey.getPublicKeysetHandle();

            boolean showLog = Constants.getBoolean("cryptoToolsShowGeneratedKeysLog");
            if (showLog) {
                Logger.println(this.getClass(), "=== Generating keys ===");
                Logger.println(this.getClass(), "Generated Private key => " + this.getPrivateKeyBase64(privateKey));
                Logger.println(this.getClass(), "Generated Public key => " + this.getKeyBase64(publiceKey));
                Logger.println(this.getClass(), "===========================");
            }
            return privateKey;
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return null;
    }

    protected KeysetHandle loadPublicKeyBase64(String publicKey) {
        String string = new String(Base64.getDecoder().decode(publicKey));
        try {
            return loadKey(string);
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return null;
    }

    protected KeysetHandle loadPrivateKeyBase64(String privateKey) {
        String string = new String(Base64.getDecoder().decode(privateKey));
        try {
           return loadKey(string);
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return null;
    }

    private KeysetHandle loadKey(String key) throws GeneralSecurityException, IOException {
        return CleartextKeysetHandle.read(JsonKeysetReader.withString(key));
    }

    protected String getKeyBase64(KeysetHandle keysetHandle) {
        try {
            String serializedKeyset = TinkJsonProtoKeysetFormat.serializeKeyset(keysetHandle, InsecureSecretKeyAccess.get());
            return Base64.getEncoder().encodeToString(serializedKeyset.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return null;
    }

    public String getPublicKeyFromPrivateKeyBase64(KeysetHandle keysetHandle) {
        try {
            return getKeyBase64(keysetHandle.getPublicKeysetHandle());
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return null;
    }

    protected String getPrivateKeyBase64(KeysetHandle keysetHandle) {
        return getKeyBase64(keysetHandle);
    }

    protected String getContextInfo() {
        String cryptoContextInfo = Constants.getString("cryptoContextInfo", "");
        if (Tools.isEmpty(cryptoContextInfo)) {
            cryptoContextInfo = Password.generateStringHash(16);
            Logger.println(this, "=== Generating cryptoContextInfo ===");
            ConfDB.setName("cryptoContextInfo", cryptoContextInfo);
        }
        return Constants.getString("cryptoContextInfo", "Tink");
    }
}
