package sk.iway.iwcm;

import java.security.KeyPair;

public class CryptoKeys {
    private KeyPair keyPair;
    private String publicKeyEncoded;
    private String privateKeyEncoded;

    public CryptoKeys(KeyPair keyPair, String publicKeyEncoded, String privateKeyEncoded) {
        this.keyPair = keyPair;
        this.publicKeyEncoded = publicKeyEncoded;
        this.privateKeyEncoded = privateKeyEncoded;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public String getPublicKeyEncoded() {
        return publicKeyEncoded;
    }

    public void setPublicKeyEncoded(String publicKeyEncoded) {
        this.publicKeyEncoded = publicKeyEncoded;
    }

    public String getPrivateKeyEncoded() {
        return privateKeyEncoded;
    }

    public void setPrivateKeyEncoded(String privateKeyEncoded) {
        this.privateKeyEncoded = privateKeyEncoded;
    }
}
