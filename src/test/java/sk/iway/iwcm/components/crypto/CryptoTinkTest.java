package sk.iway.iwcm.components.crypto;

import com.google.crypto.tink.KeysetHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.CryptoKeys;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoTinkTest extends BaseWebjetTest {

    private CryptoTink cryptoTink;

    String encryptedData = "encrypted-tink-tester_42:AVGs6QkErAjYk0mYen0s35bp2u1xK9ZxDjNtGmw9Pkw9AGEOr4iB4jFO5VlB2GLtUvlToJAUmOHe5ZlMh/7anKDYxwNAdbh7U2oZfsiinOAE8nNRvAhP9fkgSQxIndpmxtccqHj1GJNLuJeEyXNrO9Vl+z4BsB+ec1PkdLjgC9wkLwR34Ck=";
    String originalText = "Form-autotest-25-02-05-195650-61";
    String privateKey = "decrypt_key-tink-tester_42:eyJwcmltYXJ5S2V5SWQiOjEzNzAyODYzNDUsImtleSI6W3sia2V5RGF0YSI6eyJ0eXBlVXJsIjoidHlwZS5nb29nbGVhcGlzLmNvbS9nb29nbGUuY3J5cHRvLnRpbmsuRWNpZXNBZWFkSGtkZlByaXZhdGVLZXkiLCJ2YWx1ZSI6IkVxTUJFbHdLQkFnQ0VBTVNVaEpRQ2poMGVYQmxMbWR2YjJkc1pXRndhWE11WTI5dEwyZHZiMmRzWlM1amNubHdkRzh1ZEdsdWF5NUJaWE5EZEhKSWJXRmpRV1ZoWkV0bGVSSVNDZ1lLQWdnUUVCQVNDQW9FQ0FNUUVCQWdHQUVZQVJvZ1RKMWJQYm83MW5ZTk56V1lVZmFITnJsZEJyUkp2ZXF3NHI1My9DMVhtTFFpSVFESU12TWtyUlhQdlNNN2hVaklHblcybWdob1BCd21HaDJKaEFhMEg0MlhWaG9nQ29oTno4bWR5NktBbWFmaCt2T2JUb2pTRkl6Q2VIWE50WS91SnFmNWx6RT0iLCJrZXlNYXRlcmlhbFR5cGUiOiJBU1lNTUVUUklDX1BSSVZBVEUifSwic3RhdHVzIjoiRU5BQkxFRCIsImtleUlkIjoxMzcwMjg2MzQ1LCJvdXRwdXRQcmVmaXhUeXBlIjoiVElOSyJ9XX0K";
    String publicKey = "encrypt_key-tink-tester_42:eyJwcmltYXJ5S2V5SWQiOjEzNzAyODYzNDUsImtleSI6W3sia2V5RGF0YSI6eyJ0eXBlVXJsIjoidHlwZS5nb29nbGVhcGlzLmNvbS9nb29nbGUuY3J5cHRvLnRpbmsuRWNpZXNBZWFkSGtkZlB1YmxpY0tleSIsInZhbHVlIjoiRWx3S0JBZ0NFQU1TVWhKUUNqaDBlWEJsTG1kdmIyZHNaV0Z3YVhNdVkyOXRMMmR2YjJkc1pTNWpjbmx3ZEc4dWRHbHVheTVCWlhORGRISkliV0ZqUVdWaFpFdGxlUklTQ2dZS0FnZ1FFQkFTQ0FvRUNBTVFFQkFnR0FFWUFSb2dUSjFiUGJvNzFuWU5OeldZVWZhSE5ybGRCclJKdmVxdzRyNTMvQzFYbUxRaUlRRElNdk1rclJYUHZTTTdoVWpJR25XMm1naG9QQndtR2gySmhBYTBINDJYVmc9PSIsImtleU1hdGVyaWFsVHlwZSI6IkFTWU1NRVRSSUNfUFVCTElDIn0sInN0YXR1cyI6IkVOQUJMRUQiLCJrZXlJZCI6MTM3MDI4NjM0NSwib3V0cHV0UHJlZml4VHlwZSI6IlRJTksifV19Cg==";

    @BeforeEach
    public void setUp() {
        Constants.setString("cryptoContextInfo", "BJSPBMUHGTSQUNEP");
        cryptoTink = CryptoTink.getInstance();
    }

    @Test
    public void testGenerateNewPrivateAndPublicKey() {
        CryptoKeys keys = cryptoTink.generateNewPrivateAndPublicKey("testLogin");
        assertNotNull(keys);
        assertNotNull(keys.getPrivateKeyEncoded());
        assertNotNull(keys.getPublicKeyEncoded());
    }

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        String plainText = "Hello, World!";
        CryptoKeys keys = cryptoTink.generateNewPrivateAndPublicKey("testLogin");

        String encryptedText = cryptoTink.encrypt(plainText, keys.getPublicKeyEncoded());
        assertNotNull(encryptedText);

        String decryptedText = cryptoTink.decrypt(encryptedText, keys.getPrivateKeyEncoded());
        assertEquals(plainText, decryptedText);
    }

    @Test
    public void testGetAlgKey() {
        assertEquals(CryptoTink.ALG_KEY, cryptoTink.getAlgKey());
    }

    @Test
    public void testGenerateNewPrivateAndPublicKey2() {
        KeysetHandle keysetHandle = cryptoTink.generateNewPrivateAndPublicKey2();
        assertNotNull(keysetHandle);
    }

    @Test
    public void testLoadPublicKeyBase64() {
        CryptoKeys keys = cryptoTink.generateNewPrivateAndPublicKey("testLogin");
        KeysetHandle publicKeyHandle = cryptoTink.loadPublicKeyBase64(keys.getPublicKeyEncoded());
        assertNotNull(publicKeyHandle);
    }

    @Test
    public void testLoadPrivateKeyBase64() {
        CryptoKeys keys = cryptoTink.generateNewPrivateAndPublicKey("testLogin");
        KeysetHandle privateKeyHandle = cryptoTink.loadPrivateKeyBase64(keys.getPrivateKeyEncoded());
        assertNotNull(privateKeyHandle);
    }

    @Test
    public void testGetKeyBase64() {
        KeysetHandle keysetHandle = cryptoTink.generateNewPrivateAndPublicKey2();
        String keyBase64 = cryptoTink.getKeyBase64(keysetHandle);
        assertNotNull(keyBase64);
    }

    @Test
    public void testGetPublicKeyFromPrivateKeyBase64() {
        KeysetHandle keysetHandle = cryptoTink.generateNewPrivateAndPublicKey2();
        String publicKeyBase64 = cryptoTink.getPublicKeyFromPrivateKeyBase64(keysetHandle);
        assertNotNull(publicKeyBase64);
    }

    @Test
    public void testGetPrivateKeyBase64() {
        KeysetHandle keysetHandle = cryptoTink.generateNewPrivateAndPublicKey2();
        String privateKeyBase64 = cryptoTink.getPrivateKeyBase64(keysetHandle);
        assertNotNull(privateKeyBase64);
    }

    @Test
    public void testGetContextInfo() {
        String contextInfo = cryptoTink.getContextInfo();
        assertNotNull(contextInfo);
    }

    @Test
    void testDecryption() {

        String decrypted = CryptoFactory.decrypt(encryptedData, privateKey);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testEncrypt() {
        CryptoFactory cryptoFactory = new CryptoFactory();

        String encrypted = cryptoFactory.encrypt(originalText, publicKey);
        String decrypted = CryptoFactory.decrypt(encrypted, privateKey);

        assertEquals(originalText, decrypted);
    }
}