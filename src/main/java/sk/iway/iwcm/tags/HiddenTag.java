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

import javax.servlet.jsp.JspException;

import sk.iway.iwcm.tags.support.CustomBaseInputTag;
import sk.iway.iwcm.tags.support.CustomTagUtils;
import sk.iway.iwcm.tags.support.ResponseUtils;

/**
 * Custom tag for input fields of type "hidden".
 *
 * @version $Rev$ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class HiddenTag extends CustomBaseInputTag {

    /**
     * The name of this package.
     */
    public static final String PACKAGE = "org.apache.struts.taglib.html";

    /**
     * The attribute key for the bean our form is related to.
     */
    public static final String BEAN_KEY = PACKAGE + ".BEAN";

    /**
     * Comma-delimited list of content types that a server processing this
     * form will handle correctly.  This property is defined only for the
     * <code>file</code> tag, but is implemented here because it affects the
     * rendered HTML of the corresponding &lt;input&gt; tag.
     */
    protected String accept = null;

    /**
     * The "redisplay contents" flag (used only on <code>password</code>).
     */
    protected boolean redisplay = true;

    /**
     * The type of input field represented by this tag (text, password, or
     * hidden).
     */
    protected String type = null;

    /**
     * Should the value of this field also be rendered to the response?
     */
    protected boolean write = false;

    /**
     * Construct a new instance of this tag.
     */
    public HiddenTag() {
        super();
        this.type = "hidden";
    }

    public String getAccept() {
        return (this.accept);
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

    public boolean getRedisplay() {
        return (this.redisplay);
    }

    public void setRedisplay(boolean redisplay) {
        this.redisplay = redisplay;
    }

    public boolean getWrite() {
        return (this.write);
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    /**
     * Generate the required input tag, followed by the optional rendered
     * text. Support for <code>write</code> property since Struts 1.1.
     *
     * @throws JspException if a JSP exception has occurred
     */
    @Override
    public int doStartTag() throws JspException {
        // Render the <html:input type="hidden"> tag as before
       CustomTagUtils.getInstance().write(this.pageContext, this.renderInputElement());

        // Is rendering the value separately requested?
        if (!write) {
            return EVAL_BODY_INCLUDE;
        }

        // Calculate the value to be rendered separately
        // * @since Struts 1.1
        String results = null;

        if (value != null) {
            results = ResponseUtils.filter(value);
        } else {
            Object value =
                CustomTagUtils.getInstance().lookup(pageContext, name, property, null);

            if (value == null) {
                results = "";
            } else {
                results = ResponseUtils.filter(value.toString());
            }
        }

        CustomTagUtils.getInstance().write(pageContext, results);

        return EVAL_BODY_INCLUDE;
    }

    /**
     * Release any acquired resources.
     */
    @Override
    public void release() {
        super.release();
        accept = null;
        name = BEAN_KEY;
        redisplay = true;
        write = false;
    }

    /**
    * Renders a fully formed &lt;input&gt; element.
    *
    * @throws JspException
    * @since Struts 1.2
    */
    protected String renderInputElement()
        throws JspException {
        StringBuffer results = new StringBuffer("<input");

        prepareAttribute(results, "type", this.type);
        prepareAttribute(results, "name", prepareName());
        prepareAttribute(results, "accesskey", getAccesskey());
        prepareAttribute(results, "accept", getAccept());
        prepareAttribute(results, "maxlength", getMaxlength());
        prepareAttribute(results, "size", getCols());
        prepareAttribute(results, "tabindex", getTabindex());
        prepareValue(results);
        results.append(this.prepareEventHandlers());
        results.append(this.prepareStyles());
        if (!isXhtml()) {
            prepareAttribute(results, "autocomplete", getAutocomplete());
        }
        prepareOtherAttributes(results);
        results.append(this.getElementClose());

        return results.toString();
    }

    /**
     * Render the value element
     *
     * @param results The StringBuffer that output will be appended to.
     */
    protected void prepareValue(StringBuffer results)
        throws JspException {
        results.append(" value=\"");

        if (value != null) {
            results.append(this.formatValue(value));
        } else if (redisplay || !"password".equals(type)) {
            Object value =
                CustomTagUtils.getInstance().lookup(pageContext, name, property, null);

            results.append(this.formatValue(value));
        }

        results.append('"');
    }

    /**
     * Return the given value as a formatted <code>String</code>.  This
     * implementation escapes potentially harmful HTML characters.
     *
     * @param value The value to be formatted. <code>null</code> values will
     *              be returned as the empty String "".
     * @throws JspException if a JSP exception has occurred
     * @since Struts 1.2
     */
    protected String formatValue(Object value)
        throws JspException {
        if (value == null) {
            return "";
        }

        return ResponseUtils.filter(value.toString());
    }
}