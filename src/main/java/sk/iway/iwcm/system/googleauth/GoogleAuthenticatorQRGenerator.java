package sk.iway.iwcm.system.googleauth;

/**
 *  GoogleAuthenticatorQRGenerator.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff mminda $
 *@version      $Revision: 1.3 $
 *@created      Date: Jun 5, 2017 9:54:19 AM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
import org.apache.http.client.utils.URIBuilder;

/**
 * This class provides helper methods to create a QR code containing the
 * provided credential.  The generated QR code can be fed to the Google
 * Authenticator application so that it can configure itself with the data
 * contained therein.
 */
public final class GoogleAuthenticatorQRGenerator {

    /**
     * The label is used to identify which account a key is associated with.
     * It contains an account name, which is a URI-encoded string, optionally
     * prefixed by an issuer string identifying the provider or service managing
     * that account.  This issuer prefix can be used to prevent collisions
     * between different accounts with different providers that might be
     * identified using the same account name, e.g. the user's email address.
     * The issuer prefix and account name should be separated by a literal or
     * url-encoded colon, and optional spaces may precede the account name.
     * Neither issuer nor account name may themselves contain a colon.
     * Represented in ABNF according to RFC 5234:
     * <p/>
     * label = accountname / issuer (“:” / “%3A”) *”%20” accountname
     *
     * @see <a href="https://code.google.com/p/google-authenticator/wiki/KeyUriFormat">Google Authenticator - KeyUriFormat</a>
     */
    private static String formatLabel(String issuer, String accountName) {
        if (accountName == null || accountName.trim().length() == 0) {
            throw new IllegalArgumentException("Account name must not be empty.");
        }

        StringBuilder sb = new StringBuilder();
        if (issuer != null) {
            if (issuer.contains(":")) {
                throw new IllegalArgumentException("Issuer cannot contain the \':\' character.");
            }

            sb.append(issuer);
            sb.append(":");
        }

        sb.append(accountName);

        return sb.toString();
    }

    /**
     * Returns the URL of a Google Chart API call to generate a QR barcode to
     * be loaded into the Google Authenticator application.  The user scans this
     * bar code with the application on their smart phones or enters the
     * secret manually.
     * <p/>
     * The current implementation supports the following features:
     * <ul>
     * <li>Label, made up of an optional issuer and an account name.</li>
     * <li>Secret parameter.</li>
     * <li>Issuer parameter.</li>
     * </ul>
     *
     * @param issuer      The issuer name. This parameter cannot contain the colon
     *                    (:) character. This parameter can be null.
     * @param accountName The account name. This parameter shall not be null.
     * @param credentials The generated credentials.  This parameter shall not be null.
     * @return the Google Chart API call URL to generate a QR code containing
     * the provided information.
     * @see <a href="https://code.google.com/p/google-authenticator/wiki/KeyUriFormat">Google Authenticator - KeyUriFormat</a>
     */
    public static String getOtpAuthURL(String issuer,
                                       String accountName,
                                       GoogleAuthenticatorKey credentials) {

        return getOtpAuthTotpURL(issuer, accountName, credentials);
    }

    /**
     * Returns the basic otpauth TOTP URI. This URI might be sent to the user via email, QR code or some other method.
     * Use a secure transport since this URI contains the secret.
     * <p/>
     * The current implementation supports the following features:
     * <ul>
     * <li>Label, made up of an optional issuer and an account name.</li>
     * <li>Secret parameter.</li>
     * <li>Issuer parameter.</li>
     * </ul>
     *
     * @param issuer      The issuer name. This parameter cannot contain the colon
     *                    (:) character. This parameter can be null.
     * @param accountName The account name. This parameter shall not be null.
     * @param credentials The generated credentials.  This parameter shall not be null.
     * @return an otpauth scheme URI for loading into a client application.
     * @see <a href="https://code.google.com/p/google-authenticator/wiki/KeyUriFormat">Google Authenticator - KeyUriFormat</a>
     */
    public static String getOtpAuthTotpURL(String issuer,
                                           String accountName,
                                           GoogleAuthenticatorKey credentials) {

        URIBuilder uri = new URIBuilder()
            .setScheme("otpauth")
            .setHost("totp")
            .setPath("/" + formatLabel(issuer, accountName))
            .setParameter("secret", credentials.getKey());


        if (issuer != null) {
            if (issuer.contains(":")) {
                throw new IllegalArgumentException("Issuer cannot contain the \':\' character.");
            }

            uri.setParameter("issuer", issuer);
        }

        /*
            The following parameters aren't needed since they are all defaults.
            We can exclude them to make the URI shorter.
         */
        // uri.setParameter("algorithm", "SHA1");
        // uri.setParameter("digits", "6");
        // uri.setParameter("period", "30");

        return uri.toString();

    }

}
