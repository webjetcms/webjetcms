package sk.iway.iwcm.tags.support_logic;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.commons.beanutils.PropertyUtils;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

public class CustomTagUtils {

   //private static CustomTagUtils instance = new CustomTagUtils();
   private static final Map<String, Integer> scopes = new HashMap<>();

   private static final String CUSTOM_TAG_UTILS = "CustomTagUtils";

   private static final String CACHE_KEY = "CustomTagUtils.LocalStrings";
   private static final Map<String, String> keyValueMap;

   static {
      Cache c = Cache.getInstance();

      @SuppressWarnings("unchecked")
      Map<String, String> keyValueMapTmp = (Map<String, String>) c.getObject(CACHE_KEY);

      if(keyValueMapTmp == null) {
         //Keys not loaded yet
         keyValueMap = MessageResource.getAllKeyMap();
         c.setObjectSeconds(CACHE_KEY, keyValueMap, 28800); // 28800 = 8 hodin
      } else {
         keyValueMap = keyValueMapTmp;
      }
   }

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
      return this.getMessage(key, new Object[]{});
   }
   public String getMessage(String key, Object arg0) {
      return this.getMessage(key, new Object[]{arg0});
   }

   public String getMessage(String key, Object arg0, Object arg1) {
      return this.getMessage(key, new Object[]{arg0, arg1});
   }

   public String getMessage(String key, Object arg0, Object arg1, Object arg2) {
      return this.getMessage(key, new Object[]{arg0, arg1, arg2});
   }

   public String getMessage(String key, Object arg0, Object arg1, Object arg2, Object arg3) {
      return this.getMessage(key, new Object[]{arg0, arg1, arg2, arg3});
   }

   public String getMessage(String key, Object[] args) {
      String value = keyValueMap.get(key);
      if (Tools.isEmpty(value)) value = "";
      return MessageFormat.format(value, args);
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
}