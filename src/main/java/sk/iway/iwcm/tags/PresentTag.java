package sk.iway.iwcm.tags;

import java.security.Principal;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import sk.iway.iwcm.tags.support_logic.CustomConditionalTagBase;
import sk.iway.iwcm.tags.support_logic.CustomTagUtils;

public class PresentTag extends CustomConditionalTagBase {
    public static final String ROLE_DELIMITER = ",";

    // ------------------------------------------------------ Protected Methods

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition()
        throws JspException {
        return (condition(true));
    }

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @param desired Desired outcome for a true result
     * @throws JspException if a JSP exception occurs
     */
    protected boolean condition(boolean desired)
        throws JspException {
        // Evaluate the presence of the specified value
        boolean present = false;
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        if (cookie != null) {
            present = this.isCookiePresent(request);
        } else if (header != null) {
            String value = request.getHeader(header);

            present = (value != null);
        } else if (name != null) {
            present = this.isBeanPresent();
        } else if (parameter != null) {
            String value = request.getParameter(parameter);

            present = (value != null);
        } else if (role != null) {
            StringTokenizer st =
                new StringTokenizer(role, ROLE_DELIMITER, false);

            while (!present && st.hasMoreTokens()) {
                present = request.isUserInRole(st.nextToken());
            }
        } else if (user != null) {
            Principal principal = request.getUserPrincipal();

            present = (principal != null) && user.equals(principal.getName());
        } else {
            JspException e =
                new JspException(CustomTagUtils.getInstance().getMessage("logic.selector"));

            CustomTagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        return (present == desired);
    }

    /**
     * Returns true if the bean given in the <code>name</code> attribute is
     * found.
     *
     * @since Struts 1.2
     */
    protected boolean isBeanPresent() {
        Object value = null;

        try {
            if (this.property != null) {
                value =
                    CustomTagUtils.getInstance().lookup(pageContext, name,
                        this.property, scope);
            } else {
                value = CustomTagUtils.getInstance().lookup(pageContext, name, scope);
            }
        } catch (JspException e) {
            value = null;
        }

        return (value != null);
    }

    /**
     * Returns true if the cookie is present in the request.
     *
     * @since Struts 1.2
     */
    protected boolean isCookiePresent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        for (int i = 0; i < cookies.length; i++) {
            if (this.cookie.equals(cookies[i].getName())) {
                return true;
            }
        }

        return false;
    }
}