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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.tags.IterateTag;

/**
 * Base class for tags that render form elements capable of including
 * JavaScript event handlers and/or CSS Style attributes. This class does not
 * implement the doStartTag() or doEndTag() methods. Subclasses should provide
 * appropriate implementations of these.
 *
 * @version $Rev$ $Date$
 */
@Getter
@Setter
public class CustomBaseHandlerTag extends BodyTagSupport {

    protected String accesskey = null;
    protected String tabindex = null;
    protected boolean indexed = false;
    private String onclick = null;
    private String ondblclick = null;
    private String onmouseover = null;
    private String onmouseout = null;
    private String onmousemove = null;
    private String onmousedown = null;
    private String onmouseup = null;
    private String onkeydown = null;
    private String onkeyup = null;
    private String onkeypress = null;
    private String onselect = null;
    private String onchange = null;
    private String onblur = null;
    private String onfocus = null;
    private boolean disabled = false;
    protected boolean doDisabled = true;
    private boolean readonly = false;
    protected boolean doReadonly = false;
    private String style = null;
    private String styleClass = null;
    private String styleId = null;
    private String errorKey = "org.apache.struts.action.ERROR";
    private String errorStyle = null;
    private String errorStyleClass = null;
    private String errorStyleId = null;
    private String alt = null;
    private String altKey = null;
    private String bundle = null;
    private String locale = "org.apache.struts.action.LOCALE";
    private String title = null;
    private String lang = null;
    private String dir = null;
    private String titleKey = null;
    private Class<?> loopTagClass = null;
    private transient Method loopTagGetStatus = null;
    private Class<?> loopTagStatusClass = null;
    private transient Method loopTagStatusGetIndex = null;
    private boolean triedJstlInit = false;
    private boolean triedJstlSuccess = false;

    protected void prepareAttribute(StringBuffer handlers, String name, Object value) {
        if (value != null) {
            handlers.append(" ");
            handlers.append(name);
            handlers.append("=\"");
            handlers.append(value);
            handlers.append("\"");
        }
    }

    protected void prepareIndex(StringBuffer handlers, String name) throws JspException {
        if (name != null) {
            handlers.append(name);
        }

        handlers.append("[");
        handlers.append(this.getIndexValue());
        handlers.append("]");
        if (name != null) {
            handlers.append(".");
        }
    }

    protected int getIndexValue() throws JspException {
        IterateTag iterateTag = (IterateTag)findAncestorWithClass(this, IterateTag.class);
        if (iterateTag != null) {
            return iterateTag.getIndex();
        } else {
            Integer i = this.getJstlLoopIndex();
            if (i != null) {
                return i;
            } else {
                JspException e = new JspException( CustomTagUtils.getInstance().getMessage("indexed.noEnclosingIterate") );
                CustomTagUtils.getInstance().saveException(this.pageContext, e);
                throw e;
            }
        }
    }

    private Integer getJstlLoopIndex() {
        if (!this.triedJstlInit) {
            this.triedJstlInit = true;

            try {
                this.loopTagClass = RequestUtils.applicationClass("jakarta.servlet.jsp.jstl.core.LoopTag");
                this.loopTagGetStatus = ((Class<?>)this.loopTagClass).getDeclaredMethod("getLoopStatus", (Class<?>[])null);
                this.loopTagStatusClass = RequestUtils.applicationClass("jakarta.servlet.jsp.jstl.core.LoopTagStatus");
                this.loopTagStatusGetIndex = ((Class<?>)this.loopTagStatusClass).getDeclaredMethod("getIndex", (Class<?>[])null);
                this.triedJstlSuccess = true;
            } catch (ClassNotFoundException | NoSuchMethodException ex) {}
        }

        if (this.triedJstlSuccess) {
            try {
                Object loopTag = findAncestorWithClass(this, this.loopTagClass);
                if (loopTag == null) {
                    return null;
                }

                Object status = this.loopTagGetStatus.invoke(loopTag, (Object[])null);
                return (Integer)this.loopTagStatusGetIndex.invoke(status, (Object[])null);
            } catch (IllegalAccessException var3) {
                Logger.error(CustomBaseHandlerTag.class, var3);
            } catch (IllegalArgumentException var4) {
                Logger.error(CustomBaseHandlerTag.class, var4);
            } catch (InvocationTargetException var5) {
                Logger.error(CustomBaseHandlerTag.class, var5);
            } catch (NullPointerException var6) {
                Logger.error(CustomBaseHandlerTag.class, var6);
            } catch (ExceptionInInitializerError var7) {
                Logger.error(CustomBaseHandlerTag.class, var7.getMessage());
            }
        }

        return null;
    }

    protected String prepareEventHandlers() {
        StringBuffer handlers = new StringBuffer();
        this.prepareMouseEvents(handlers);
        this.prepareKeyEvents(handlers);
        this.prepareTextEvents(handlers);
        this.prepareFocusEvents(handlers);
        return handlers.toString();
    }

    protected void prepareMouseEvents(StringBuffer handlers) {
        this.prepareAttribute(handlers, "onclick", this.getOnclick());
        this.prepareAttribute(handlers, "ondblclick", this.getOndblclick());
        this.prepareAttribute(handlers, "onmouseover", this.getOnmouseover());
        this.prepareAttribute(handlers, "onmouseout", this.getOnmouseout());
        this.prepareAttribute(handlers, "onmousemove", this.getOnmousemove());
        this.prepareAttribute(handlers, "onmousedown", this.getOnmousedown());
        this.prepareAttribute(handlers, "onmouseup", this.getOnmouseup());
    }

    protected void prepareKeyEvents(StringBuffer handlers) {
        this.prepareAttribute(handlers, "onkeydown", this.getOnkeydown());
        this.prepareAttribute(handlers, "onkeyup", this.getOnkeyup());
        this.prepareAttribute(handlers, "onkeypress", this.getOnkeypress());
    }

    protected void prepareTextEvents(StringBuffer handlers) {
        this.prepareAttribute(handlers, "onselect", this.getOnselect());
        this.prepareAttribute(handlers, "onchange", this.getOnchange());
    }

    protected void prepareFocusEvents(StringBuffer handlers) {
        this.prepareAttribute(handlers, "onblur", this.getOnblur());
        this.prepareAttribute(handlers, "onfocus", this.getOnfocus());
    }

    protected String prepareStyles() throws JspException {
        StringBuffer styles = new StringBuffer();
        boolean errorsExist = this.doErrorsExist();
        if (errorsExist && this.getErrorStyleId() != null) {
            this.prepareAttribute(styles, "id", this.getErrorStyleId());
        } else {
            this.prepareAttribute(styles, "id", this.getStyleId());
        }

        if (errorsExist && this.getErrorStyle() != null) {
            this.prepareAttribute(styles, "style", this.getErrorStyle());
        } else {
            this.prepareAttribute(styles, "style", this.getStyle());
        }

        if (errorsExist && this.getErrorStyleClass() != null) {
            this.prepareAttribute(styles, "class", this.getErrorStyleClass());
        } else {
            this.prepareAttribute(styles, "class", this.getStyleClass());
        }

        this.prepareAttribute(styles, "title", this.message(this.getTitle(), this.getTitleKey()));
        this.prepareAttribute(styles, "alt", this.message(this.getAlt(), this.getAltKey()));
        this.prepareInternationalization(styles);
        return styles.toString();
    }

    protected boolean doErrorsExist() throws JspException {
        boolean errorsExist = false;
        if (this.getErrorStyleId() != null || this.getErrorStyle() != null || this.getErrorStyleClass() != null) {
            String actualName = this.prepareName();
            if (actualName != null) {
                // ActionMessages errors = TagUtils.getInstance().getActionMessages(this.pageContext, this.errorKey);
                // errorsExist = errors != null && errors.size(actualName) > 0;
            }
        }

        return errorsExist;
    }

    protected String prepareName() throws JspException {
        return null;
    }

    protected String message(String literal, String key) throws JspException {
        if (literal != null) {
            if (key != null) {
                JspException e = new JspException( CustomTagUtils.getInstance().getMessage("common.both") );
                CustomTagUtils.getInstance().saveException(this.pageContext, e);
                throw e;
            } else {
                return literal;
            }
        } else {
            return key != null ? CustomTagUtils.getInstance().getMessage(key) : null;
        }
    }

    protected void prepareInternationalization(StringBuffer handlers) {
        this.prepareAttribute(handlers, "lang", this.getLang());
        this.prepareAttribute(handlers, "dir", this.getDir());
    }

    protected boolean isXhtml() {
        return CustomTagUtils.getInstance().isXhtml(this.pageContext);
    }

    protected String getElementClose() {
        return this.isXhtml() ? " />" : ">";
    }

    protected void prepareOtherAttributes(StringBuffer handlers) {
        //So tag can override this after extending
    }
}