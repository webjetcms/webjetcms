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

import javax.servlet.jsp.JspException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomBaseInputTag extends CustomBaseHandlerTag {

    private String autocomplete = null;
    protected String cols = null;
    protected String maxlength = null;
    protected String property = null;
    protected String rows = null;
    protected String value = null;
    protected String name = "org.apache.struts.taglib.html.BEAN";

    @Override
    protected String prepareName() throws JspException {
        if (this.property == null) {
            return null;
        } else if (this.indexed) {
            StringBuffer results = new StringBuffer();
            this.prepareIndex(results, this.name);
            results.append(this.property);
            return results.toString();
        } else {
            return this.property;
        }
    }
}
