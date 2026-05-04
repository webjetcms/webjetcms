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
package sk.iway.iwcm.tags.support;

import jakarta.servlet.jsp.JspException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectTag extends CustomBaseHandlerTag {

    protected String[] match = null;
    protected String multiple = null;
    protected String name = ".taglib.html.BEAN";
    protected String property = null;
    protected String saveBody = null;
    protected String size = null;
    protected String value = null;

    /**
    * Does the specified value match one of those we are looking for?
    *
    * @param value Value to be compared.
    */
    public boolean isMatched(String value) {
        if ((this.match == null) || (value == null)) {
            return false;
        }

        for (int i = 0; i < this.match.length; i++) {
            if (value.equals(this.match[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create an appropriate select start element based on our parameters.
     */
    protected String renderSelectStartElement() throws JspException {
        StringBuffer results = new StringBuffer("<select");
        this.prepareAttribute(results, "name", this.prepareName());
        this.prepareAttribute(results, "accesskey", this.getAccesskey());
        if (this.multiple != null) {
            results.append(" multiple=\"multiple\"");
        }

        this.prepareAttribute(results, "size", this.getSize());
        this.prepareAttribute(results, "tabindex", this.getTabindex());
        results.append(this.prepareEventHandlers());
        results.append(this.prepareStyles());
        this.prepareOtherAttributes(results);
        results.append(">");
        return results.toString();
    }
}
