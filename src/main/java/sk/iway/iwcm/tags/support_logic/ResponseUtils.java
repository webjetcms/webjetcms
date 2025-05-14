package sk.iway.iwcm.tags.support_logic;

import sk.iway.iwcm.Tools;

public class ResponseUtils {

    private ResponseUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Filter the specified string for characters that are senstive to HTML
     * interpreters, returning the string with these characters replaced by
     * the corresponding character entities.
     *
     * @param value The string to be filtered and returned
     */
    public static String filter(String value) {
        if (Tools.isEmpty(value)) {
            return value;
        }

        StringBuffer result = null;
        String filtered = null;

        for (int i = 0; i < value.length(); i++) {
            filtered = null;

            switch (value.charAt(i)) {
                case '<':
                    filtered = "&lt;";

                    break;

                case '>':
                    filtered = "&gt;";

                    break;

                case '&':
                    filtered = "&amp;";

                    break;

                case '"':
                    filtered = "&quot;";

                    break;

                case '\'':
                    filtered = "&#39;";

                    break;
                default: break;
            }

            if (result == null) {
                if (filtered != null) {
                    result = new StringBuffer(value.length() + 50);

                    if (i > 0) {
                        result.append(value.substring(0, i));
                    }

                    result.append(filtered);
                }
            } else {
                if (filtered == null) {
                    result.append(value.charAt(i));
                } else {
                    result.append(filtered);
                }
            }
        }

        return (result == null) ? value : result.toString();
    }
}
