package sk.iway.iwcm.tags;

import javax.servlet.jsp.JspException;

public class NotPresentTag extends PresentTag {

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @throws JspException if a JSP exception occurs
     */
    @Override
    protected boolean condition()
        throws JspException {
        return (condition(false));
    }
}