package sk.iway.iwcm.tags.support_logic;

import javax.servlet.jsp.JspException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectTag extends CustomBaseHandlerTag {

    protected String[] match = null;
    protected String multiple = null;
    protected String name = "org.apache.struts.taglib.html.BEAN";
    protected String property = null;
    protected String saveBody = null;
    protected String size = null;
    protected String value = null;

    protected String renderSelectStartElement() throws JspException {
        StringBuffer results = new StringBuffer("<select");
        this.prepareAttribute(results, "name", this.prepareName());
        this.prepareAttribute(results, "accesskey", this.getAccesskey());
        if (this.multiple != null) {
            results.append(" multiple=\"multiple\"");
        }

        this.prepareAttribute(results, "size", this.getSize());
        this.prepareAttribute(results, "tabindex", this.getTabindex());
        results.append(this.prepareEventHandlers());
        results.append(this.prepareStyles());
        this.prepareOtherAttributes(results);
        results.append(">");
        return results.toString();
    }
}
