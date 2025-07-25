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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import javax.servlet.jsp.JspException;

import sk.iway.iwcm.tags.support.CustomConditionalTagBase;
import sk.iway.iwcm.tags.support.CustomTagUtils;

/**
 * Evalute the nested body content of this tag if the specified value is empty
 * for this request.
 *
 * @version $Rev$ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public class EmptyTag extends CustomConditionalTagBase {
    // ------------------------------------------------------ Protected Methods

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition()
        throws JspException {
        return (condition(true));
    }

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @param desired Desired outcome for a true result
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition(boolean desired)
        throws JspException {
        if (this.name == null) {
            JspException e =
                new JspException(CustomTagUtils.getInstance().getMessage("empty.noNameAttribute"));

            CustomTagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        Object value = null;

        if (this.property == null) {
            value = CustomTagUtils.getInstance().lookup(pageContext, name, scope);
        } else {
            value =
                CustomTagUtils.getInstance().lookup(pageContext, name, property, scope);
        }

        boolean empty = true;

        if (value == null) {
            empty = true;
        } else if (value instanceof String strValue) {
            empty = strValue.isEmpty();
        } else if (value instanceof Collection<?> collValue) {
            empty = collValue.isEmpty();
        } else if (value instanceof Map<?, ?> mapValue) {
            empty = mapValue.isEmpty();
        } else if (value.getClass().isArray()) {
            empty = Array.getLength(value) == 0;
        } else {
            empty = false;
        }

        return (empty == desired);
    }
}