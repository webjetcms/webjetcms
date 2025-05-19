package sk.iway.iwcm.tags.support_logic;

import java.lang.reflect.InvocationTargetException;

import sk.iway.iwcm.Tools;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.beanutils.PropertyUtils;

public abstract class CustomCompareTagBase extends CustomConditionalTagBase{

    protected static final int DOUBLE_COMPARE = 0;

    protected static final int LONG_COMPARE = 1;

    protected static final int STRING_COMPARE = 2;

    private String value = null;

    public String getValue() {
        return (this.value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    protected abstract boolean condition() throws JspException;

    protected boolean condition(int desired1, int desired2) throws JspException {
        // Acquire the value and determine the test type
        int type = -1;
        double doubleValue = 0.0;
        long longValue = 0;

        CustomTagUtils customTagUtils = CustomTagUtils.getInstance();

        if ((type < 0) && Tools.isNotEmpty(value) ) {
            try {
                doubleValue = Double.parseDouble(value);
                type = DOUBLE_COMPARE;
            } catch (NumberFormatException e) {}
        }

        if ((type < 0) && Tools.isNotEmpty(value) ) {
            try {
                longValue = Long.parseLong(value);
                type = LONG_COMPARE;
            } catch (NumberFormatException e) {}
        }

        if (type < 0) {
            type = STRING_COMPARE;
        }

        // Acquire the unconverted variable value
        Object variable = null;

        if (cookie != null) {
            Cookie[] cookies =
                ((HttpServletRequest) pageContext.getRequest()).getCookies();

            if (cookies == null) {
                cookies = new Cookie[0];
            }

            for (int i = 0; i < cookies.length; i++) {
                if (cookie.equals(cookies[i].getName())) {
                    variable = cookies[i].getValue();

                    break;
                }
            }
        } else if (header != null) {
            variable =
                ((HttpServletRequest) pageContext.getRequest()).getHeader(header);
        } else if (name != null) {
            Object bean =
                customTagUtils.lookup(pageContext, name, scope);

            if (property != null) {
                if (bean == null) {
                    JspException e =
                        new JspException(customTagUtils.getMessage("logic.bean", name));

                    customTagUtils.saveException(pageContext, e);
                    throw e;
                }

                try {
                    variable = PropertyUtils.getProperty(bean, property);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();

                    if (t == null) {
                        t = e;
                    }

                    customTagUtils.saveException(pageContext, t);
                    throw new JspException(customTagUtils.getMessage(
                            "logic.property", name, property, t.toString()), t);
                } catch (Throwable t) {
                    customTagUtils.saveException(pageContext, t);
                    throw new JspException(customTagUtils.getMessage(
                            "logic.property", name, property, t.toString()), t);
                }
            } else {
                variable = bean;
            }
        } else if (parameter != null) {
            variable = pageContext.getRequest().getParameter(parameter);
        } else {
            JspException e =
                new JspException(customTagUtils.getMessage("logic.selector"));

            customTagUtils.saveException(pageContext, e);
            throw e;
        }

        if (variable == null) {
            variable = ""; // Coerce null to a zero-length String
        }

        // Perform the appropriate comparison
        int result = 0;

        if (type == DOUBLE_COMPARE) {
            try {
                double doubleVariable = Double.parseDouble(variable.toString());

                if (doubleVariable < doubleValue) {
                    result = -1;
                } else if (doubleVariable > doubleValue) {
                    result = +1;
                }
            } catch (NumberFormatException e) {
                result = variable.toString().compareTo(value);
            }
        } else if (type == LONG_COMPARE) {
            try {
                long longVariable = Long.parseLong(variable.toString());

                if (longVariable < longValue) {
                    result = -1;
                } else if (longVariable > longValue) {
                    result = +1;
                }
            } catch (NumberFormatException e) {
                result = variable.toString().compareTo(value);
            }
        } else {
            result = variable.toString().compareTo(value);
        }

        // Normalize the result
        if (result < 0) {
            result = -1;
        } else if (result > 0) {
            result = +1;
        }

        // Return true if the result matches either desired value
        return ((result == desired1) || (result == desired2));
    }
}