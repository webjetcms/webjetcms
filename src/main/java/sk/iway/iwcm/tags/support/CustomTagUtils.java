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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.commons.beanutils.PropertyUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.i18n.Prop;

/**
 * Provides helper methods for JSP tags.
 *
 * @version $Rev$
 * @since Struts 1.2
 */
public class CustomTagUtils {


   private static final Map<String, Integer> scopes = new HashMap<>();
   private static final String CUSTOM_TAG_UTILS = "CustomTagUtils";
   public static final String XHTML_KEY = "org.apache.struts.globals.XHTML";

   static {
        scopes.put("page", Integer.valueOf(PageContext.PAGE_SCOPE));
        scopes.put("request", Integer.valueOf(PageContext.REQUEST_SCOPE));
        scopes.put("session", Integer.valueOf(PageContext.SESSION_SCOPE));
        scopes.put("application", Integer.valueOf(PageContext.APPLICATION_SCOPE));
   }

   /* Keys gonna be only in EN version.
   *  Original struts had keys only in EN and ESP version, so there is no meaning to create CS and SK version.
   */
   private static final Prop prop = Prop.getInstance("en");
   private static final String STRUTS_KEYS_PREFIX = "struts.";

   public static CustomTagUtils getInstance() {
      javax.servlet.ServletContext servletContext = Constants.getServletContext();

      synchronized (CustomTagUtils.class) {
         CustomTagUtils customTagUtils = (CustomTagUtils) servletContext.getAttribute(CUSTOM_TAG_UTILS);
         if(customTagUtils == null) {
            customTagUtils = new CustomTagUtils();
            //	remove
				servletContext.removeAttribute(CUSTOM_TAG_UTILS);
				//save us to server space
				servletContext.setAttribute(CUSTOM_TAG_UTILS, customTagUtils);
         }
         return customTagUtils;
      }
   }

   public int getScope(String scopeName) throws JspException {
      Integer scope = scopes.get(scopeName.toLowerCase());
      if (scope == null) {
         throw new JspException(getMessage("lookup.scope", scopeName));
      } else {
         return scope;
      }
   }

   public Object lookup(PageContext pageContext, String name, String scopeName) throws JspException {
      if (scopeName == null) {
         return pageContext.findAttribute(name);
      } else {
         try {
            return pageContext.getAttribute(name, this.getScope(scopeName));
         } catch (JspException e) {
            this.saveException(pageContext, e);
            throw e;
         }
      }
   }

   public Object lookup(PageContext pageContext, String name, String property, String scope) throws JspException {
      Object bean = this.lookup(pageContext, name, scope);
      if (bean == null) {
         JspException e = (scope == null)
               ? new JspException(getMessage("lookup.bean.any", name))
               : new JspException(getMessage("lookup.bean", name, scope));
         this.saveException(pageContext, e);
         throw e;
      } else if (property == null) {
         return bean;
      } else {
         try {
            return PropertyUtils.getProperty(bean, property);
         } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException e) {
            this.saveException(pageContext, e);
            throw new JspException(getMessage("lookup.error", property, name), e);
         } catch (InvocationTargetException e) {
            Throwable t = (e.getTargetException() != null) ? e.getTargetException() : e;
            this.saveException(pageContext, t);
            throw new JspException(getMessage("lookup.target", property, name), t);
         }
      }
   }

   public void saveException(PageContext pageContext, Throwable exception) {
      pageContext.setAttribute("org.apache.struts.action.EXCEPTION", exception, PageContext.REQUEST_SCOPE);
   }

   public String getMessage(String key) {
      if(key.startsWith(STRUTS_KEYS_PREFIX) == false) key = STRUTS_KEYS_PREFIX + key;
      return prop.getText(key);
   }
   public String getMessage(String key, Object arg0) {
      if(key.startsWith(STRUTS_KEYS_PREFIX) == false) key = STRUTS_KEYS_PREFIX + key;
      return prop.getText(key, String.valueOf(arg0));
   }

   public String getMessage(String key, Object arg0, Object arg1) {
      if(key.startsWith(STRUTS_KEYS_PREFIX) == false) key = STRUTS_KEYS_PREFIX + key;
      return prop.getText(key, String.valueOf(arg0), String.valueOf(arg1));
   }

   public String getMessage(String key, Object arg0, Object arg1, Object arg2) {
      if(key.startsWith(STRUTS_KEYS_PREFIX) == false) key = STRUTS_KEYS_PREFIX + key;
      return prop.getText(key, String.valueOf(arg0), String.valueOf(arg1), String.valueOf(arg2));
   }

   public String getMessage(String key, Object arg0, Object arg1, Object arg2, Object arg3) {
      if(key.startsWith(STRUTS_KEYS_PREFIX) == false) key = STRUTS_KEYS_PREFIX + key;
      return prop.getText(key, String.valueOf(arg0), String.valueOf(arg1), String.valueOf(arg2), String.valueOf(arg3));
   }

   /**
    * Write the specified text as the response to the writer associated with
    * this page.  <strong>WARNING</strong> - If you are writing body content
    * from the <code>doAfterBody()</code> method of a custom tag class that
    * implements <code>BodyTag</code>, you should be calling
    * <code>writePrevious()</code> instead.
    *
    * @param pageContext The PageContext object for this page
    * @param text The text to be written
    *
    * @exception JspException if an input/output error occurs (already saved)
    */
   public void write(PageContext pageContext, String text) throws JspException {
       JspWriter writer = pageContext.getOut();

       try {
           writer.print(text);

       } catch (IOException e) {
           this.saveException(pageContext, e);
           throw new JspException
                   (getMessage("write.io", e.toString()));
       }
   }

    /**
    * Write the specified text as the response to the writer associated with
    * the body content for the tag within which we are currently nested.
    *
    * @param pageContext The PageContext object for this page
    * @param text The text to be written
    *
    * @exception JspException if an input/output error occurs (already saved)
    */
   public void writePrevious(PageContext pageContext, String text) throws JspException {
      JspWriter writer = pageContext.getOut();
      if (writer instanceof BodyContent writerB) {
            writer = writerB.getEnclosingWriter();
      }

      try {
            writer.print(text);
      } catch (IOException e) {
            this.saveException(pageContext, e);
            throw new JspException(this.getMessage("write.io", e.toString()));
      }
   }

   /**
    * Returns true if the custom tags are in XHTML mode.
    */
   public boolean isXhtml(PageContext pageContext) {
      //WebJET CMS is always in XHTML mode
      return true;
   }
}