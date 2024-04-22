package sk.iway.iwcm.dmail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.common.CloudToolsForCore;

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
        String cacheKey = "sk.iway.iwcm.dmail.unsubscribedEmails-"+CloudToolsForCore.getDomainId();
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

    /**
     * Extract email:
     * from "Janko Tester <tester@test.sk>" -> "tester@test.sk"
     * from "janko@test.sk" -> "janko@test.sk"
     * @param email
     * @return
     */
    public static String parseEmailFromNameEmailFormat(String email) {
        String fixed = email;
        //fixed = email.replaceAll(".*<([^<>]+)>.*|^([^<>]+)$", "$1$2");
        if (email.contains("<") && email.contains(">")) {
            fixed = email.replaceAll(".*<([^<>]+)>.*", "$1");
        }
        return fixed;
    }

}
