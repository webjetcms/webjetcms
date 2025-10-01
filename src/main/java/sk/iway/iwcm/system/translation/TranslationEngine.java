package sk.iway.iwcm.system.translation;

import java.util.Random;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;

public abstract class TranslationEngine {

    protected static final int MAX_RETRIES = 5;
    protected static final int BASE_DELAY_MS = 1000;
    protected static final Random random = new Random();

    public abstract String engineName();
    public abstract Long numberOfFreeCharacters();
    public abstract String translate(String text, String fromLanguage, String toLanguage);
    public abstract boolean isConfigured();

    /**
     * Compute exponential delay based on attempt number. Use delay ms to fall asleep thread.
     * @param attempt - number of attemp
     * @return TRUE if sleep was successfull and we have another try, else FALSE
     */
    protected boolean applyDelay(int attempt) {
        try {
            //Too many requests, slow down
            int exponentialDelay = BASE_DELAY_MS * (1 << attempt); // 2^attempt * base
            // Add jitter: +/- 0-1000 ms
            exponentialDelay = exponentialDelay + random.nextInt(1000);

            Logger.debug(TranslationEngine.class, "Too many requests error. Attempt number : " + (attempt + 1) + ", waiting for for : " + exponentialDelay + " ms");
            Thread.sleep(exponentialDelay);
        } catch (Exception ex2) {
            sk.iway.iwcm.Logger.error(ex2);
            return false;
        }

        // Waiting done
        Logger.debug(TranslationEngine.class, "Waiting done");
        // Increment attempt count
        attempt++;

        if(attempt >= MAX_RETRIES) {
            Logger.error(TranslationEngine.class, "Unable to call API again, reached maximum number of attempts");
            return false;
        }

        // We can try again
        return true;
    }

    protected void auditBilledCharacters(long billedCharacters) {
        Adminlog.add(Adminlog.TYPE_TRANSLATION, "AUTO_TRANSLATION " + engineName() + " used: " + billedCharacters, (int)billedCharacters, -1);
    }

    protected void auditRemainingCharacters(long characterLimit, long characterCount) {
        long remainingCharacters = characterLimit - characterCount;

        //calculate percentage of used characters
        long usedPercentage = 0;
        if (characterLimit == 0) {
            usedPercentage = 100;
        } else if (characterCount != 0) {
            usedPercentage = Math.round((double) characterCount / characterLimit * 100);
        }

        Adminlog.add(Adminlog.TYPE_TRANSLATION, "AUTO_TRANSLATION " + engineName() + " remaining: " + remainingCharacters + " usage: " + usedPercentage + "% (" + characterCount+"/"+characterLimit +")", (int)remainingCharacters, -1);
    }
}