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

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.tagext.TagSupport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormTag extends TagSupport {
   private static final long serialVersionUID = 1L;
   public static final String TRANSACTION_TOKEN_KEY = "org.apache.struts.action.TOKEN";
   public static final String TOKEN_KEY = "org.apache.struts.taglib.html.TOKEN";

   protected static String lineEnd = System.getProperty("line.separator");
   protected String action = null;
   private String autocomplete = null;
   private String postbackAction = null;
   protected String enctype = null;
   protected String focus = null;
   protected String focusIndex = null;
   protected String method = null;
   protected String onreset = null;
   protected String onsubmit = null;
   protected boolean scriptLanguage = true;
   protected String style = null;
   protected String styleClass = null;
   protected String styleId = null;
   protected String target = null;
   protected String beanName = null;
   protected String beanScope = null;
   protected String beanType = null;
   protected String acceptCharset = null;
   private boolean disabled = false;
   protected boolean readonly = false;
   private String lang = null;
   private String dir = null;
   protected String name = null;
   protected String type = null;
   protected String scope = null;

    protected String renderToken() {

        HttpSession session = pageContext.getSession();

        return renderToken(session);
    }

    public static String renderToken(HttpSession session)
    {
        StringBuffer results = new StringBuffer();
        if (session != null) {
            String token =
            (String) session.getAttribute(TRANSACTION_TOKEN_KEY);

            if (token != null) {
                results.append("<div style=\"display: none;\"><input type=\"hidden\" name=\"");
                results.append(TOKEN_KEY);
                results.append("\" value=\"");
                results.append(token);

                results.append("\" />");

                results.append("</div>");
            }
        }

        return results.toString();
    }
}