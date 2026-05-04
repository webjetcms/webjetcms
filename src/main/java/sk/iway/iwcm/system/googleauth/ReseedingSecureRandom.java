package sk.iway.iwcm.system.googleauth;

/**
 *  ReseedingSecureRandom.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff mminda $
 *@version      $Revision: 1.3 $
 *@created      Date: Jun 5, 2017 9:51:02 AM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 08/04/14
 * Time: 15:21
 *
 * @author Enrico M. Crisostomo
 */
class ReseedingSecureRandom {
    private static final int MAX_OPERATIONS = 1_000_000;
    private final String provider;
    private final String algorithm;
    private final AtomicInteger count = new AtomicInteger(0);
    private SecureRandom secureRandom;

    @SuppressWarnings("UnusedDeclaration")
    ReseedingSecureRandom() {
        this.algorithm = null;
        this.provider = null;

        buildSecureRandom();
    }

    @SuppressWarnings("UnusedDeclaration")
    ReseedingSecureRandom(String algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null.");
        }

        this.algorithm = algorithm;
        this.provider = null;

        buildSecureRandom();
    }

    ReseedingSecureRandom(String algorithm, String provider) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null.");
        }

        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null.");
        }

        this.algorithm = algorithm;
        this.provider = provider;

        buildSecureRandom();
    }

    private void buildSecureRandom() {
        try {
            if (this.algorithm == null && this.provider == null) {
                this.secureRandom = new SecureRandom();
            } else if (this.provider == null) {
                this.secureRandom = SecureRandom.getInstance(this.algorithm);
            } else {
                this.secureRandom = SecureRandom.getInstance(this.algorithm, this.provider);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new GoogleAuthenticatorException(
                    String.format(
                            "Could not initialise SecureRandom with the specified algorithm: %s. " +
                                    "Another provider can be chosen setting the %s system property.",
                            this.algorithm,
                            GoogleAuthenticator.RNG_ALGORITHM
                    ), e
            );
        } catch (NoSuchProviderException e) {
            throw new GoogleAuthenticatorException(
                    String.format(
                            "Could not initialise SecureRandom with the specified provider: %s. " +
                                    "Another provider can be chosen setting the %s system property.",
                            this.provider,
                            GoogleAuthenticator.RNG_ALGORITHM_PROVIDER
                    ), e
            );
        }
    }

    void nextBytes(byte[] bytes) {
        if (count.incrementAndGet() > MAX_OPERATIONS) {
            synchronized (this) {
                if (count.get() > MAX_OPERATIONS) {
                    buildSecureRandom();
                    count.set(0);
                }
            }
        }

        this.secureRandom.nextBytes(bytes);
    }
}