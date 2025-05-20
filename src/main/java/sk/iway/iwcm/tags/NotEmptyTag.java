package sk.iway.iwcm.tags;

import javax.servlet.jsp.JspException;

/**
 * Evalute the nested body content of this tag if the specified value is not
 * empty for this request.
 *
 * @version $Rev$ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public class NotEmptyTag extends EmptyTag {
    // ------------------------------------------------------ Protected Methods

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