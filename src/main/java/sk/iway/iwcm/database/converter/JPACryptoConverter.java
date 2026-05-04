package sk.iway.iwcm.database.converter;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;
import sk.iway.iwcm.*;

/**
 * Date: 15.08.2019
 * Time: 09:00
 * Project: webjet8
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2019
 *
 * Implementacia pre EclipseLink CryptoConverter (sifrovanie a desifrovanie dat algoritmom RSA s dlzkou kluca 2048 do a z DB)
 * Converter predpoklada ze je v konstante zadany verejny kluc.
 * Verejny kluc musi byt zadany ako konstanta v tvare CRYPTO_PUBLIC_KEY-nazovEntity napr.
 * pre entitu CreditCardBean.java bude nazov kluca vyzerat takto: CRYPTO_PUBLIC_KEY-CreditCardBean.
 * Sukromny kluc sa cita zo session. Preto je potrebne kluc pridat do session pomocou metody
 * boolean CryptoTools.addPrivateKeyToSession(HttpSession session, String privateKey).
 *
 * Pre spring DATA treba pouzit springovy converter: https://sunitkatkar.blogspot.com/2018/04/spring-boot-2-generic-jpa-converter-to.html
 *
 * @author mpijak
 */
public class JPACryptoConverter implements Converter {

    private static final long serialVersionUID = 1L;

    private String entityName;

    @Override
    public String convertObjectValueToDataValue(Object value, Session arg1) {
        String sensitiveData = "";
        if (value instanceof String) {
            sensitiveData = (String) value;
        }
        return this.encryptSensitiveData(sensitiveData);
    }

    @Override
    public String convertDataValueToObjectValue(Object value, Session arg1) {
        String sensitiveData = "";
        if (value instanceof String) {
            sensitiveData = (String) value;
        }
        return decryptSensitiveData(sensitiveData);
    }

    /**
     * Zasifruje data a pripoji prefix ak najde verejny kluc
     * @param sensitiveData data na sifrovanie
     * @return zasifrovane data
     */
    private String encryptSensitiveData(String sensitiveData) {
        String publicKey = this.getPublicKey();
        if (publicKey == null) {
            Logger.println(JPACryptoConverter.class, "Chyba: verejny kluc neexistuje. Data nebudu zasifrovane");
            return sensitiveData;
        }
        CryptoFactory crypto = new CryptoFactory();
        return crypto.encrypt(sensitiveData, publicKey);
    }

    /**
     * Metoda najskor skontroluje ci je textovy retazec uz zasifrovany (sufix encrypted) a az potom ho desifruje.
     * Ak retazec nie je zasifrovany alebo je privatny kluc prazdny, vrati povodny retazec
     * @param sensitiveData pravdepodobne zasifrovane data
     * @return desifrovane data
     */
    private String decryptSensitiveData(String sensitiveData) {
        return CryptoFactory.decrypt(sensitiveData);
    }

    /**
     * Metoda vrati verejny kluc pre databazovu entitu.
     * Verejny kluc musi byt zadany ako konstanta v tvare CRYPTO_PUBLIC_KEY-nazovEntity napr.
     * pre entitu CraditCardBean.java bude nazov kluca vyzerat takto CRYPTO_PUBLIC_KEY-CreditCardBean
     * @return enkodovany verejny kluc
     */
    private String getPublicKey() {
        String publicKey = Constants.getString("CRYPTO_PUBLIC_KEY-" + this.getEntityName());
        if (Tools.isEmpty(publicKey)) {
            return null;
        }
//        return CryptoTools.INSTANCE.cleanKey(publicKey);
        return CryptoFactory.removeRedundantPrefix(publicKey);
    }

    /**
     * Metoda vrati sukromny kluc pre databazovu entitu. Tento kluc by mal byt ulozeny
     * v session pod nazvom JPACryptoConverter.privateKey
     * @return enkodovany privatny kluc
     */
    /*private String getPrivateKey() {
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean == null) {
            return null;
        }
        String privateKey = requestBean.getCryptoPrivateKey();
        if (Tools.isEmpty(privateKey)) {
            return null;
        }
        return CryptoFactory.removeRedundantPrefix(privateKey);
    }*/

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public void initialize(DatabaseMapping databaseMapping, Session session) {
        this.entityName = databaseMapping.getDescriptor().getAlias();
    }

    /**
     * Vrati nazov JPA entity. Napr. pre entitu CreditCardBean.java vrati CreditCardBean
     * @return nazov beany bez sufixu
     */
    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
}