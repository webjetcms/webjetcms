package org.apache.struts.util;

/**
 * Wrapper for backward compatibility with Struts 1.x.
 */
public class ResponseUtils {

    /**
     * Filter the specified string for characters that are sensitive to HTML
     * @param value
     * @return
     * @deprecated Use {@link sk.iway.iwcm.tags.support.ResponseUtils#filter(String)} instead.
     */
    @Deprecated
    public static String filter(String value) {
        return sk.iway.iwcm.tags.support.ResponseUtils.filter(value);
    }

}
