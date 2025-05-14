package sk.iway.iwcm.tags.support_logic;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.IteratorAdapter;
import org.apache.struts.util.MessageResources;

public class CustomIterateTag extends BodyTagSupport {
   protected static MessageResources messages = MessageResources.getMessageResources("org.apache.struts.taglib.logic.LocalStrings");
   protected Iterator iterator = null;
   protected int lengthCount = 0;
   protected int lengthValue = 0;
   protected int offsetValue = 0;
   protected boolean started = false;
   protected Object collection = null;
   protected String id = null;
   protected String indexId = null;
   protected String length = null;
   protected String name = null;
   protected String offset = null;
   protected String property = null;
   protected String scope = null;
   protected String type = null;

   public Object getCollection() {
      return this.collection;
   }

   public void setCollection(Object collection) {
      this.collection = collection;
   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public int getIndex() {
      return this.started ? this.offsetValue + this.lengthCount - 1 : 0;
   }

   public String getIndexId() {
      return this.indexId;
   }

   public void setIndexId(String indexId) {
      this.indexId = indexId;
   }

   public String getLength() {
      return this.length;
   }

   public void setLength(String length) {
      this.length = length;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getOffset() {
      return this.offset;
   }

   public void setOffset(String offset) {
      this.offset = offset;
   }

   public String getProperty() {
      return this.property;
   }

   public void setProperty(String property) {
      this.property = property;
   }

   public String getScope() {
      return this.scope;
   }

   public void setScope(String scope) {
      this.scope = scope;
   }

   public String getType() {
      return this.type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public int doStartTag() throws JspException {
      Object collection = this.collection;
      if (collection == null) {
         collection = TagUtils.getInstance().lookup(this.pageContext, this.name, this.property, this.scope);
      }

      JspException e;
      if (collection == null) {
         e = new JspException(messages.getMessage("iterate.collection"));
         TagUtils.getInstance().saveException(this.pageContext, e);
         throw e;
      } else {
         if (collection.getClass().isArray()) {
            try {
               this.iterator = Arrays.asList((Object[])((Object[])collection)).iterator();
            } catch (ClassCastException var8) {
               int length = Array.getLength(collection);
               ArrayList c = new ArrayList(length);

               for(int i = 0; i < length; ++i) {
                  c.add(Array.get(collection, i));
               }

               this.iterator = c.iterator();
            }
         } else if (collection instanceof Collection) {
            this.iterator = ((Collection)collection).iterator();
         } else if (collection instanceof Iterator) {
            this.iterator = (Iterator)collection;
         } else if (collection instanceof Map) {
            this.iterator = ((Map)collection).entrySet().iterator();
         } else {
            if (!(collection instanceof Enumeration)) {
               e = new JspException(messages.getMessage("iterate.iterator"));
               TagUtils.getInstance().saveException(this.pageContext, e);
               throw e;
            }

            this.iterator = new IteratorAdapter((Enumeration)collection);
         }

         Integer lengthObject;
         if (this.offset == null) {
            this.offsetValue = 0;
         } else {
            try {
               this.offsetValue = Integer.parseInt(this.offset);
            } catch (NumberFormatException var7) {
               lengthObject = (Integer)TagUtils.getInstance().lookup(this.pageContext, this.offset, (String)null);
               if (lengthObject == null) {
                  this.offsetValue = 0;
               } else {
                  this.offsetValue = lengthObject;
               }
            }
         }

         if (this.offsetValue < 0) {
            this.offsetValue = 0;
         }

         if (this.length == null) {
            this.lengthValue = 0;
         } else {
            try {
               this.lengthValue = Integer.parseInt(this.length);
            } catch (NumberFormatException var6) {
               lengthObject = (Integer)TagUtils.getInstance().lookup(this.pageContext, this.length, (String)null);
               if (lengthObject == null) {
                  this.lengthValue = 0;
               } else {
                  this.lengthValue = lengthObject;
               }
            }
         }

         if (this.lengthValue < 0) {
            this.lengthValue = 0;
         }

         this.lengthCount = 0;

         for(int i = 0; i < this.offsetValue; ++i) {
            if (this.iterator.hasNext()) {
               this.iterator.next();
            }
         }

         if (this.iterator.hasNext()) {
            Object element = this.iterator.next();
            if (element == null) {
               this.pageContext.removeAttribute(this.id);
            } else {
               this.pageContext.setAttribute(this.id, element);
            }

            ++this.lengthCount;
            this.started = true;
            if (this.indexId != null) {
               this.pageContext.setAttribute(this.indexId, new Integer(this.getIndex()));
            }

            return 2;
         } else {
            return 0;
         }
      }
   }

   public int doAfterBody() throws JspException {
      if (this.bodyContent != null) {
         TagUtils.getInstance().writePrevious(this.pageContext, this.bodyContent.getString());
         this.bodyContent.clearBody();
      }

      if (this.lengthValue > 0 && this.lengthCount >= this.lengthValue) {
         return 0;
      } else if (this.iterator.hasNext()) {
         Object element = this.iterator.next();
         if (element == null) {
            this.pageContext.removeAttribute(this.id);
         } else {
            this.pageContext.setAttribute(this.id, element);
         }

         ++this.lengthCount;
         if (this.indexId != null) {
            this.pageContext.setAttribute(this.indexId, new Integer(this.getIndex()));
         }

         return 2;
      } else {
         return 0;
      }
   }

   public int doEndTag() throws JspException {
      this.started = false;
      this.iterator = null;
      return 6;
   }

   public void release() {
      super.release();
      this.iterator = null;
      this.lengthCount = 0;
      this.lengthValue = 0;
      this.offsetValue = 0;
      this.id = null;
      this.collection = null;
      this.length = null;
      this.name = null;
      this.offset = null;
      this.property = null;
      this.scope = null;
      this.started = false;
   }
}