/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package sk.iway.iwcm.tags;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.tags.support.CustomTagUtils;
import sk.iway.iwcm.tags.support.ResponseUtils;

/**
 * Tag that retrieves the specified property of the specified bean, converts
 * it to a String representation (if necessary), and writes it to the current
 * output stream, optionally filtering characters that are sensitive in HTML.
 *
 * @version $Rev$ $Date$
 */
public class BeanWriteTag extends TagSupport {

    //Default formats ...
    public static final String INT_FORMAT = null;
    public static final String FLOAT_FORMAT = null;

    /**
     * Filter the rendered output for characters that are sensitive in HTML?
     */
    protected boolean filter = true;

    public boolean getFilter() {
        return (this.filter);
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    /**
     * Should we ignore missing beans and simply output nothing?
     */
    protected boolean ignore = false;

    public boolean getIgnore() {
        return (this.ignore);
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    /**
     * Name of the bean that contains the data we will be rendering.
     */
    protected String name = null;

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Name of the property to be accessed on the specified bean.
     */
    protected String property = null;

    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * The scope to be searched to retrieve the specified bean.
     */
    protected String scope = null;

    public String getScope() {
        return (this.scope);
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * The format string to be used as format to convert
     * value to String.
     */
    protected String formatStr = null;

    public String getFormat() {
        return (this.formatStr);
    }

    public void setFormat(String formatStr) {
        this.formatStr = formatStr;
    }

    /**
     * The key to search format string in applciation resources
     */
    protected String formatKey = null;

    public String getFormatKey() {
        return (this.formatKey);
    }

    public void setFormatKey(String formatKey) {
        this.formatKey = formatKey;
    }

    /**
     * The session scope key under which our Locale is stored.
     */
    protected String localeKey = null;

    public String getLocale() {
        return (this.localeKey);
    }

    public void setLocale(String localeKey) {
        this.localeKey = localeKey;
    }

    /**
     * The servlet context attribute key for our resources.
     */
    protected String bundle = null;

    public String getBundle() {
        return (this.bundle);
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Process the start tag.
     *
     * @exception JspException if a JSP exception has occurred
     */
    @Override
    public int doStartTag() throws JspException {

        // Look up the requested bean (if necessary)
        if (ignore) {
            if (CustomTagUtils.getInstance().lookup(pageContext, name, scope) == null) {
                return (SKIP_BODY); // Nothing to output
            }
        }

        // Look up the requested property value
        Object value = CustomTagUtils.getInstance().lookup(pageContext, name, property, scope);

        if (value == null) {
            return (SKIP_BODY); // Nothing to output
        }

        // Convert value to the String with some formatting
        String output = formatValue(value);

        // Print this property value to our output writer, suitably filtered
        if (filter) {
            CustomTagUtils.getInstance().write(pageContext, ResponseUtils.filter(output));
        } else {
            CustomTagUtils.getInstance().write(pageContext, output);
        }

        // Continue processing this page
        return (SKIP_BODY);

    }

    /**
     * Format value according to specified format string (as tag attribute or
     * as string from message resources) or to current user locale.
     *
     * When a format string is retrieved from the message resources,
     * <code>applyLocalizedPattern</code> is used. For more about localized
     * patterns, see
     * <http://www.dei.unipd.it/corsi/fi2ae-docs/source/jdk1.1.7/src/java/text/resources/>.
     * (To obtain the correct value for some characters, you may need to view
     * the file in a hex editor and then use the Unicode escape form in the
     * property resources file.)
     *
     * @param valueToFormat value to process and convert to String
     * @exception JspException if a JSP exception has occurred
     */
    protected String formatValue(Object valueToFormat) throws JspException {
        Format format = null;
        Object value = valueToFormat;
        boolean formatStrFromResources = false;
        String formatString = formatStr;

        //Iway logic to get locale
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        Locale locale = LayoutService.getUserLocale(request);

        // Return String object as is.
        if (value instanceof String strValue) {
            return strValue;
        } else {

            // Try to retrieve format string from resources by the key from formatKey.
            if ((formatString == null) && (formatKey != null)) {
                formatString = CustomTagUtils.getInstance().getMessage(this.formatKey);
                if (formatString != null) {
                    formatStrFromResources = true;
                }
            }

            // Prepare format object for numeric values.
            if (value instanceof Number) {

                if (formatString == null) {
                    if ((value instanceof Byte)
                        || (value instanceof Short)
                        || (value instanceof Integer)
                        || (value instanceof Long)
                        || (value instanceof BigInteger)) {

                        formatString = INT_FORMAT;

                    } else if (
                        (value instanceof Float)
                            || (value instanceof Double)
                            || (value instanceof BigDecimal)) {

                        formatString = FLOAT_FORMAT;
                    }

                    if (formatString != null) {
                        formatStrFromResources = true;
                    }
                }

                if (formatString != null) {
                    try {
                        format = NumberFormat.getNumberInstance(locale);
                        if (formatStrFromResources) {
                            ((DecimalFormat) format).applyLocalizedPattern(
                                formatString);
                        } else {
                            ((DecimalFormat) format).applyPattern(formatString);
                        }

                    } catch (IllegalArgumentException e) {
                        CustomTagUtils customTagUtils = CustomTagUtils.getInstance();
                        JspException ex =
                            new JspException(
                                customTagUtils.getMessage("write.format", formatString));
                        customTagUtils.saveException(pageContext, ex);
                        throw ex;
                    }
                }

            } else if (value instanceof java.util.Date) {

                if (formatString == null) {

                    if (value instanceof java.sql.Timestamp) {
                        formatString = Constants.getString("dateTimeFormat");
                    } else if (value instanceof java.sql.Date) {
                        formatString = Constants.getString("dateFormat");
                    } else if (value instanceof java.sql.Time) {
                        formatString = Constants.getString("timeFormat");
                    } else if (value instanceof java.util.Date) {
                        formatString = Constants.getString("dateFormat");
                    }

                }

                if (formatString != null) {
                        format = new SimpleDateFormat(formatString, locale);
                }
            }
        }

        if (format != null) {
            return format.format(value);
        } else {
            return value.toString();
        }

    }

    /**
     * Release all allocated resources.
     */
    @Override
    public void release() {
        super.release();
        filter = true;
        ignore = false;
        name = null;
        property = null;
        scope = null;
        formatStr = null;
        formatKey = null;
        localeKey = null;
        bundle = null;
    }

}
