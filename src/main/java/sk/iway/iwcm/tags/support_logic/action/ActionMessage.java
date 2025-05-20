package sk.iway.iwcm.tags.support_logic.action;

import java.io.Serializable;

public class ActionMessage implements Serializable {
   protected String key;
   protected Object[] values;
   protected boolean resource;

   public ActionMessage(String key) {
      this(key, (Object[])null);
   }

   public ActionMessage(String key, Object value0) {
      this(key, new Object[]{value0});
   }

   public ActionMessage(String key, Object value0, Object value1) {
      this(key, new Object[]{value0, value1});
   }

   public ActionMessage(String key, Object value0, Object value1, Object value2) {
      this(key, new Object[]{value0, value1, value2});
   }

   public ActionMessage(String key, Object value0, Object value1, Object value2, Object value3) {
      this(key, new Object[]{value0, value1, value2, value3});
   }

   public ActionMessage(String key, Object[] values) {
      this.key = null;
      this.values = null;
      this.resource = true;
      this.key = key;
      this.values = values;
      this.resource = true;
   }

   public ActionMessage(String key, boolean resource) {
      this.key = null;
      this.values = null;
      this.resource = true;
      this.key = key;
      this.resource = resource;
   }

   public String getKey() {
      return this.key;
   }

   public Object[] getValues() {
      return this.values;
   }

   public boolean isResource() {
      return this.resource;
   }

   public String toString() {
      StringBuffer buff = new StringBuffer();
      buff.append(this.key);
      buff.append("[");
      if (this.values != null) {
         for(int i = 0; i < this.values.length; ++i) {
            buff.append(this.values[i]);
            if (i < this.values.length - 1) {
               buff.append(", ");
            }
         }
      }

      buff.append("]");
      return buff.toString();
   }
}