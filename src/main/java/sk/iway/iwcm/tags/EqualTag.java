package sk.iway.iwcm.tags;

import javax.servlet.jsp.JspException;

import sk.iway.iwcm.tags.support_logic.CustomCompareTagBase;

/**
 * Evaluate the nested body content of this tag if the specified variable and
 * value are equal.
 *
 * @version $Rev$ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class EqualTag extends CustomCompareTagBase {
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
        return (condition(0, 0));
    }
}