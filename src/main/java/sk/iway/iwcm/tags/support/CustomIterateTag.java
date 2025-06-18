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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomIterateTag extends BodyTagSupport {

   @SuppressWarnings("rawtypes")
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

   public int getIndex() {
      return this.started ? this.offsetValue + this.lengthCount - 1 : 0;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public int doStartTag() throws JspException {
      Object localCollection = this.collection;
      if (localCollection == null) {
         localCollection = CustomTagUtils.getInstance().lookup(this.pageContext, this.name, this.property, this.scope);
      }

      JspException e;
      if (localCollection == null) {
         e = new JspException( CustomTagUtils.getInstance().getMessage("iterate.collection") );
         CustomTagUtils.getInstance().saveException(this.pageContext, e);
         throw e;
      } else {
         if (localCollection.getClass().isArray()) {
            try {
               this.iterator = Arrays.asList((Object[])localCollection).iterator();
            } catch (ClassCastException var8) {
               int localCollectionLength = Array.getLength(localCollection);
               ArrayList<Object> c = new ArrayList<>(localCollectionLength);
               for(int i = 0; i < localCollectionLength; ++i) {
                  c.add(Array.get(localCollection, i));
               }

               this.iterator = c.iterator();
            }
         } else if (localCollection instanceof Collection<?> collectionCollection) {
            this.iterator = collectionCollection.iterator();
         } else if (localCollection instanceof Iterator<?> collectionIterator) {
            this.iterator = collectionIterator;
         } else if (localCollection instanceof Map<?, ?> collectionMap) {
            this.iterator = collectionMap.entrySet().iterator();
         } else {
            if (!(localCollection instanceof Enumeration)) {
               e = new JspException( CustomTagUtils.getInstance().getMessage("iterate.iterator") );
               CustomTagUtils.getInstance().saveException(this.pageContext, e);
               throw e;
            }


            this.iterator = new IteratorAdapter((Enumeration)localCollection);
         }

         Integer lengthObject;
         if (this.offset == null) {
            this.offsetValue = 0;
         } else {
            try {
               this.offsetValue = Integer.parseInt(this.offset);
            } catch (NumberFormatException var7) {
               lengthObject = (Integer)CustomTagUtils.getInstance().lookup(this.pageContext, this.offset, (String)null);
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
               lengthObject = (Integer)CustomTagUtils.getInstance().lookup(this.pageContext, this.length, (String)null);
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
               this.pageContext.setAttribute(this.indexId, Integer.valueOf(this.getIndex()));
            }

            return 2;
         } else {
            return 0;
         }
      }
   }

   @Override
   public int doAfterBody() throws JspException {
      if (this.bodyContent != null) {
         CustomTagUtils.getInstance().writePrevious(this.pageContext, this.bodyContent.getString());
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
            this.pageContext.setAttribute(this.indexId, this.getIndex());
         }

         return 2;
      } else {
         return 0;
      }
   }

   @Override
   public int doEndTag() throws JspException {
      this.started = false;
      this.iterator = null;
      return 6;
   }

   @Override
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