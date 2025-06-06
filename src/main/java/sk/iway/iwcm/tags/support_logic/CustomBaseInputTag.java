package sk.iway.iwcm.tags.support_logic;

import javax.servlet.jsp.JspException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomBaseInputTag extends CustomBaseHandlerTag {

    private String autocomplete = null;
    protected String cols = null;
    protected String maxlength = null;
    protected String property = null;
    protected String rows = null;
    protected String value = null;
    protected String name = "org.apache.struts.taglib.html.BEAN";

    @Override
    protected String prepareName() throws JspException {
        if (this.property == null) {
            return null;
        } else if (this.indexed) {
            StringBuffer results = new StringBuffer();
            this.prepareIndex(results, this.name);
            results.append(this.property);
            return results.toString();
        } else {
            return this.property;
        }
    }
}
