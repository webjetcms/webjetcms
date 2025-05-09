package sk.iway.iwcm.tags;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.beanutils.PropertyUtils;

import sk.iway.iwcm.tags.support_logic.CustomResponseUtils;
import sk.iway.iwcm.tags.support_logic.CustomTagUtils;
import sk.iway.iwcm.tags.support_logic.IteratorAdapter;

/**
 * Tag for creating multiple &lt;select&gt; options from a collection.  The
 * associated values displayed to the user may optionally be specified by a
 * second collection, or will be the same as the values themselves.  Each
 * collection may be an array of objects, a Collection, an Enumeration, an
 * Iterator, or a Map. <b>NOTE</b> - This tag requires a Java2 (JDK 1.2 or
 * later) platform.
 */
public class OptionsTag extends TagSupport {

    private static final String GETTER_ACCESS = "getter.access";
    private static final String GETTER_RESULT = "getter.result";
    private static final String GETTER_METHOD = "getter.method";

    /**
     * The name of this package.
     */
    public static final String PACKAGE = "org.apache.struts.taglib.html";

      /**
     * The attribute key for the bean our form is related to.
     */
    public static final String BEAN_KEY = PACKAGE + ".BEAN";

     /**
     * The attribute key for the select tag itself.
     */
    public static final String SELECT_KEY = PACKAGE + ".SELECT";

    /**
     * The name of the collection containing beans that have properties to
     * provide both the values and the labels (identified by the
     * <code>property</code> and <code>labelProperty</code> attributes).
     */
    protected String collection = null;

    /**
     * Should the label values be filtered for HTML sensitive characters?
     */
    protected boolean filter = true;

    /**
     * The name of the bean containing the labels collection.
     */
    protected String labelName = null;

    /**
     * The bean property containing the labels collection.
     */
    protected String labelProperty = null;

    /**
     * The name of the bean containing the values collection.
     */
    protected String name = null;

    /**
     * The name of the property to use to build the values collection.
     */
    protected String property = null;

    /**
     * The style associated with this tag.
     */
    private String style = null;

    /**
     * The named style class associated with this tag.
     */
    private String styleClass = null;

    public String getCollection() {
        return (this.collection);
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public boolean getFilter() {
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public String getLabelProperty() {
        return labelProperty;
    }

    public void setLabelProperty(String labelProperty) {
        this.labelProperty = labelProperty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * Process the start of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    @Override
    public int doStartTag() throws JspException {
        return SKIP_BODY;
    }

    /**
     * Process the end of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    @Override
    public int doEndTag() throws JspException {
        CustomTagUtils customTagUtils = CustomTagUtils.getInstance();

        // Acquire the select tag we are associated with
        SelectTag selectTag =
            (SelectTag) pageContext.getAttribute(SELECT_KEY);

        if (selectTag == null) {
            throw new JspException(customTagUtils.getMessage("optionsTag.select"));
        }

        StringBuffer sb = new StringBuffer();

        // If a collection was specified, use that mode to render options
        if (collection != null) {
            Iterator<?> collIterator = getIterator(collection, null);

            while (collIterator.hasNext()) {
                Object bean = collIterator.next();
                Object value = null;
                Object label = null;

                try {
                    value = PropertyUtils.getProperty(bean, property);

                    if (value == null) {
                        value = "";
                    }
                } catch (IllegalAccessException e) {
                    throw new JspException(customTagUtils.getMessage(
                            GETTER_ACCESS, property, collection), e);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();

                    throw new JspException(customTagUtils.getMessage(
                            GETTER_RESULT, property, t.toString()), e);
                } catch (NoSuchMethodException e) {
                    throw new JspException(customTagUtils.getMessage(
                            GETTER_METHOD, property, collection), e);
                }

                try {
                    if (labelProperty != null) {
                        label = PropertyUtils.getProperty(bean, labelProperty);
                    } else {
                        label = value;
                    }

                    if (label == null) {
                        label = "";
                    }
                } catch (IllegalAccessException e) {
                    throw new JspException(customTagUtils.getMessage(
                            GETTER_ACCESS, labelProperty, collection), e);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();

                    throw new JspException(customTagUtils.getMessage(
                            GETTER_RESULT, labelProperty, t.toString()), e);
                } catch (NoSuchMethodException e) {
                    throw new JspException(customTagUtils.getMessage(
                            GETTER_METHOD, labelProperty, collection), e);
                }

                String stringValue = value.toString();

                addOption(sb, stringValue, label.toString(),
                    selectTag.isMatched(stringValue));
            }
        }
        // Otherwise, use the separate iterators mode to render options
        else {
            // Construct iterators for the values and labels collections
            Iterator<?> valuesIterator = getIterator(name, property);
            Iterator<?> labelsIterator = null;

            if ((labelName != null) || (labelProperty != null)) {
                labelsIterator = getIterator(labelName, labelProperty);
            }

            // Render the options tags for each element of the values coll.
            while (valuesIterator.hasNext()) {
                Object valueObject = valuesIterator.next();

                if (valueObject == null) {
                    valueObject = "";
                }

                String value = valueObject.toString();
                String label = value;

                if ((labelsIterator != null) && labelsIterator.hasNext()) {
                    Object labelObject = labelsIterator.next();

                    if (labelObject == null) {
                        labelObject = "";
                    }

                    label = labelObject.toString();
                }

                addOption(sb, value, label, selectTag.isMatched(value));
            }
        }

        customTagUtils.write(pageContext, sb.toString());

        return EVAL_PAGE;
    }

    /**
     * Release any acquired resources.
     */
    @Override
    public void release() {
        super.release();
        collection = null;
        filter = true;
        labelName = null;
        labelProperty = null;
        name = null;
        property = null;
        style = null;
        styleClass = null;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Add an option element to the specified StringBuffer based on the
     * specified parameters. <p> Note that this tag specifically does not
     * support the <code>styleId</code> tag attribute, which causes the HTML
     * <code>id</code> attribute to be emitted.  This is because the HTML
     * specification states that all "id" attributes in a document have to be
     * unique.  This tag will likely generate more than one
     * <code>option</code> element element, but it cannot use the same
     * <code>id</code> value.  It's conceivable some sort of mechanism to
     * supply an array of <code>id</code> values could be devised, but that
     * doesn't seem to be worth the trouble.
     *
     * @param sb      StringBuffer accumulating our results
     * @param value   Value to be returned to the server for this option
     * @param label   Value to be shown to the user for this option
     * @param matched Should this value be marked as selected?
     */
    protected void addOption(StringBuffer sb, String value, String label,
        boolean matched) {
        sb.append("<option value=\"");

        if (filter) {
            sb.append(CustomResponseUtils.filter(value));
        } else {
            sb.append(value);
        }

        sb.append("\"");

        if (matched) {
            sb.append(" selected=\"selected\"");
        }

        if (style != null) {
            sb.append(" style=\"");
            sb.append(CustomResponseUtils.filter(style));
            sb.append("\"");
        }

        if (styleClass != null) {
            sb.append(" class=\"");
            sb.append(CustomResponseUtils.filter(styleClass));
            sb.append("\"");
        }

        sb.append(">");

        if (filter) {
            sb.append(CustomResponseUtils.filter(label));
        } else {
            sb.append(label);
        }

        sb.append("</option>\r\n");
    }

    /**
     * Return an iterator for the option labels or values, based on our
     * configured properties.
     *
     * @param name     Name of the bean attribute (if any)
     * @param property Name of the bean property (if any)
     * @throws JspException if an error occurs
     */
    protected Iterator getIterator(String name, String property) throws JspException {
        CustomTagUtils customTagUtils = CustomTagUtils.getInstance();

        // Identify the bean containing our collection
        String beanName = name;

        if (beanName == null) {
            beanName = BEAN_KEY;
        }

        Object bean =
            customTagUtils.lookup(pageContext, beanName, null);

        if (bean == null) {
            throw new JspException(customTagUtils.getMessage("getter.bean", beanName));
        }

        // Identify the collection itself
        Object collectionToIterate = bean;

        if (property != null) {
            try {
                collectionToIterate = PropertyUtils.getProperty(bean, property);

                if (collectionToIterate == null) {
                    throw new JspException(customTagUtils.getMessage(
                            "getter.property", property));
                }
            } catch (IllegalAccessException e) {
                throw new JspException(customTagUtils.getMessage(GETTER_ACCESS,
                        property, name), e);
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();

                throw new JspException(customTagUtils.getMessage(GETTER_RESULT,
                        property, t.toString()), e);
            } catch (NoSuchMethodException e) {
                throw new JspException(customTagUtils.getMessage(GETTER_METHOD,
                        property, name), e);
            }
        }

        // Construct and return an appropriate iterator
        if (collectionToIterate.getClass().isArray()) {
            collectionToIterate = Arrays.asList((Object[]) collectionToIterate);
        }

        if (collectionToIterate instanceof Collection<?> collectionCollection) {
            return collectionCollection.iterator();
        } else if (collectionToIterate instanceof Iterator<?> iterator) {
            return iterator;
        } else if (collectionToIterate instanceof Map<?, ?> map) {
            return map.entrySet().iterator();
        } else if (collectionToIterate instanceof Enumeration<?> enumeration) {
            return new IteratorAdapter<>(enumeration);
        } else {
            throw new JspException(customTagUtils.getMessage("optionsTag.iterator", collectionToIterate.toString()));
        }
    }
}