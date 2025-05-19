package sk.iway.iwcm.helpers;

/**
 * Replaces sensitive informations in logs and other outputs
 */
public class DataSanitizer {

    // List of keywords that are considered sensitive - they are hard coded here to protect it from modifications by configuration
    private static final String[] sensitiveKeywords = new String[]{
            "password",
            "pass",
            "passwd",
            "pwd",
            "pswd",
            "psswd",
            "heslo",
            "secret",
            "key",
            "token",
            "cert",
            "certificate"
    };

    /**
     * Test if name is sensitive
     * @param fieldName - name of the field, eg. user_password, userPassword, pwd, logonToken etc.
     * @return
     */
    public static boolean isSensitive(String fieldName) {
        String lower = fieldName.toLowerCase();
        for (String keyword : sensitiveKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replace sensitive information in value if the name is on the sensitive keywords list
     * @param fieldName - name of the field/configuration key, eg. user_password, userPassword, pwd, logonToken etc.
     * @param value - value to replace
     * @return - asterisk if name is sensitive otherwise the value
     */
    public static String sanitizeIfNameIsSensitive(String fieldName, String value) {
        if (isSensitive(fieldName)) {
            return "********";
        }
        return value;
    }
}
