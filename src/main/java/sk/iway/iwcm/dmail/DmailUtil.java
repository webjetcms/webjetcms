package sk.iway.iwcm.dmail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.Cache;

/**
 * Various util methods for emails/newsletters
 */
public class DmailUtil {

    private DmailUtil() {
        //this is Util class with only static methods
    }

    /**
     * Returns unsubscribed emails, cached for 2 minutes (for use during excel import)
     * @return
     */
    public static Set<String> getUnsubscribedEmails() {
        Cache cache = Cache.getInstance();
        String cacheKey = "sk.iway.iwcm.dmail.unsubscribedEmails";
        @SuppressWarnings("unchecked")
        Set<String> unsubscribedEmailsSet = cache.getObject(cacheKey, Set.class);
        if (unsubscribedEmailsSet == null) {
            Map<String, Integer> unsubscribedEmailsTable = EmailDB.getHashtableFromUnsubscribedEmails();
            unsubscribedEmailsSet = new HashSet<>();
            unsubscribedEmailsSet.addAll(unsubscribedEmailsTable.keySet());
            cache.setObjectSeconds(cacheKey, unsubscribedEmailsSet, 120, true);
        }
        return unsubscribedEmailsSet;
    }

}
