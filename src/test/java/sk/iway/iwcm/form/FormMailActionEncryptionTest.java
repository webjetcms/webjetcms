package sk.iway.iwcm.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.CryptoKeys;
import sk.iway.iwcm.components.crypto.CryptoTink;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies encryption key selection by saving and reloading classic submissions in the configured database. */
class FormMailActionEncryptionTest extends BaseWebjetTest {

    private static final String MESSAGE = "Form encryption autotest message";
    private static CryptoKeys ownerKeys;
    private static CryptoKeys foreignKeys;
    private String formName;
    private String previousContext;
    private boolean previousSpamProtection;
    private boolean previousExistingFormsOnly;

    /** Creates disposable keys without registering them in the database. */
    @BeforeAll
    static void generateKeys() {
        ownerKeys = CryptoTink.getInstance().generateNewPrivateAndPublicKey("autotest-owner");
        foreignKeys = CryptoTink.getInstance().generateNewPrivateAndPublicKey("autotest-foreign");
    }

    /** Isolates submissions and avoids spam checks unrelated to encryption policy. */
    @BeforeEach
    void prepareSubmission() {
        formName = "autotest-encryption-" + UUID.randomUUID();
        previousContext = Constants.getString("cryptoContextInfo");
        previousSpamProtection = Constants.getBoolean("spamProtection");
        previousExistingFormsOnly = Constants.getBoolean("formAllowOnlyExistingFormsOnPublicNode");
        Constants.setString("cryptoContextInfo", "autotest-form-encryption");
        Constants.setBoolean("spamProtection", false);
        Constants.setBoolean("formAllowOnlyExistingFormsOnPublicNode", false);
    }

    /** Removes only this test's submission and header and restores in-process configuration. */
    @AfterEach
    void cleanupSubmission() {
        try {
            query.execute("DELETE FROM forms WHERE form_name=?", formName);
        } finally {
            Constants.setString("cryptoContextInfo", previousContext);
            Constants.setBoolean("spamProtection", previousSpamProtection);
            Constants.setBoolean("formAllowOnlyExistingFormsOnPublicNode", previousExistingFormsOnly);
        }
    }

    /** Uses the configured key for both stored values and HTML regardless of the submitted key. */
    @ParameterizedTest
    @ValueSource(strings = {"absent", "empty", "invalid", "foreign"})
    void requestCannotOverrideConfiguredKey(String requestKeyType) {
        FormSettingsEntity settings = new FormSettingsEntity();
        settings.setEncryptKey("encrypt_key-tink-autotest-owner:" + ownerKeys.getPublicKeyEncoded());
        String requestKey = switch (requestKeyType) {
            case "empty" -> "";
            case "invalid" -> "encrypt_key-tink-autotest-invalid:not-a-key";
            case "foreign" -> "encrypt_key-tink-autotest-foreign:" + foreignKeys.getPublicKeyEncoded();
            default -> null;
        };

        String[] stored = saveAndReload(settings, requestKey);
        assertTrue(stored[0].startsWith("message~encrypted-tink-autotest-owner:"), stored[0]);
        assertTrue(stored[1].startsWith("encrypted-tink-autotest-owner:"), stored[1]);
        assertFalse(stored[0].contains(MESSAGE));
        assertFalse(stored[1].contains(MESSAGE));

        String privateKey = "decrypt_key-tink-autotest-owner:" + ownerKeys.getPrivateKeyEncoded();
        assertEquals(MESSAGE, CryptoFactory.decrypt(stored[0].substring("message~".length()), privateKey));
        assertTrue(CryptoFactory.decrypt(stored[1], privateKey).contains(MESSAGE));
    }

    /** Ignores a request key when server settings are absent or do not enable encryption. */
    @ParameterizedTest
    @ValueSource(strings = {"absent", "null-key", "empty-key"})
    void requestCannotEnableEncryption(String settingsType) {
        FormSettingsEntity settings = null;
        if (!"absent".equals(settingsType)) {
            settings = new FormSettingsEntity();
            if ("empty-key".equals(settingsType)) settings.setEncryptKey("");
        }

        String[] stored = saveAndReload(settings, "encrypt_key-tink-autotest-foreign:" + foreignKeys.getPublicKeyEncoded());
        assertEquals("message~" + MESSAGE, stored[0]);
        assertTrue(stored[1].contains(MESSAGE));
        assertFalse(stored[1].startsWith("encrypted-"));
    }

    /** Stubs configuration and page caches while exercising real encryption and database writes. */
    private String[] saveAndReload(FormSettingsEntity settings, String requestKey) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setParameter("language", "en");
        request.setParameter("savedb", formName);
        request.setParameter("fields", "message");
        request.setParameter("message", MESSAGE);
        request.setParameter("recipients", "nobody@nowhere.com");
        if (requestKey != null) request.setParameter("encryptKey", requestKey);

        try (MockedStatic<FormSettingsService> formSettings = mockStatic(FormSettingsService.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class);
                MockedStatic<TemplatesDB> templates = mockStatic(TemplatesDB.class);
                MockedStatic<Adminlog> audit = mockStatic(Adminlog.class)) {
            formSettings.when(() -> FormSettingsService.loadEntity(formName)).thenReturn(settings);
            docs.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
            templates.when(TemplatesDB::getInstance).thenReturn(mock(TemplatesDB.class));

            String forward = FormMailAction.saveForm(request, Map.of(), List.of(), null);
            assertTrue(forward.contains("formsend=true"), forward);
            Object id = request.getAttribute("formMailFormFormId");
            assertNotNull(id, "The submission must have been inserted into the database");
            return new String[] {
                query.forString("SELECT data FROM forms WHERE id=? AND form_name=?", id, formName),
                query.forString("SELECT html FROM forms WHERE id=? AND form_name=?", id, formName)
            };
        }
    }
}
