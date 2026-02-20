package sk.iway.iwcm.system.googleauth;

/**
 *  GoogleAuthenticatorConfig.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff mminda $
 *@version      $Revision: 1.3 $
 *@created      Date: Jun 5, 2017 9:49:56 AM
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
import java.util.concurrent.TimeUnit;

public class GoogleAuthenticatorConfig {
    private long timeStepSizeInMillis = TimeUnit.SECONDS.toMillis(30);
    private int windowSize = 3;
    private int codeDigits = 6;
    private int keyModulus = (int) Math.pow(10, codeDigits);
    private KeyRepresentation keyRepresentation = KeyRepresentation.BASE32;

    /**
     * Returns the key module.
     *
     * @return the key module.
     */
    public int getKeyModulus() {
        return keyModulus;
    }

    /**
     * Returns the key representation.
     *
     * @return the key representation.
     */
    public KeyRepresentation getKeyRepresentation() {
        return keyRepresentation;
    }

    /**
     * Returns the number of digits in the generated code.
     *
     * @return the number of digits in the generated code.
     */
    @SuppressWarnings("UnusedDeclaration")
    public int getCodeDigits() {
        return codeDigits;
    }

    /**
     * Returns the time step size, in milliseconds, as specified by RFC 6238.
     * The default value is 30.000.
     *
     * @return the time step size in milliseconds.
     */
    public long getTimeStepSizeInMillis() {
        return timeStepSizeInMillis;
    }

    /**
     * Returns an integer value representing the number of windows of size
     * timeStepSizeInMillis that are checked during the validation process,
     * to account for differences between the server and the client clocks.
     * The bigger the window, the more tolerant the library code is about
     * clock skews.
     * <p/>
     * We are using Google's default behaviour of using a window size equal
     * to 3.  The limit on the maximum window size, present in older
     * versions of this library, has been removed.
     *
     * @return the window size.
     * @see #timeStepSizeInMillis
     */
    public int getWindowSize() {
        return windowSize;
    }

    public static class GoogleAuthenticatorConfigBuilder {
        private GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig();

        public GoogleAuthenticatorConfig build() {
            return config;
        }

        public GoogleAuthenticatorConfigBuilder setCodeDigits(int codeDigits) {
            if (codeDigits <= 0) {
                throw new IllegalArgumentException("Code digits must be positive.");
            }

            if (codeDigits < 6) {
                throw new IllegalArgumentException("The minimum number of digits is 6.");
            }

            if (codeDigits > 8) {
                throw new IllegalArgumentException("The maximum number of digits is 8.");
            }

            config.codeDigits = codeDigits;
            config.keyModulus = (int) Math.pow(10, codeDigits);
            return this;
        }

        public GoogleAuthenticatorConfigBuilder setTimeStepSizeInMillis(long timeStepSizeInMillis) {
            if (timeStepSizeInMillis <= 0) {
                throw new IllegalArgumentException("Time step size must be positive.");
            }

            config.timeStepSizeInMillis = timeStepSizeInMillis;
            return this;
        }

        public GoogleAuthenticatorConfigBuilder setWindowSize(int windowSize) {
            if (windowSize <= 0) {
                throw new IllegalArgumentException("Window number must be positive.");
            }

            config.windowSize = windowSize;
            return this;
        }

        public GoogleAuthenticatorConfigBuilder setKeyRepresentation(KeyRepresentation keyRepresentation) {
            if (keyRepresentation == null) {
                throw new IllegalArgumentException("Key representation cannot be null.");
            }

            config.keyRepresentation = keyRepresentation;
            return this;
        }
    }
}
