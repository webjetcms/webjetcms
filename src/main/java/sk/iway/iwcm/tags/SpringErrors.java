package sk.iway.iwcm.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.spring.webjet_component.WebjetError;
import sk.iway.iwcm.system.spring.webjet_component.WebjetErrors;

public class SpringErrors extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

    private String wrapper = "div";
    private String listTag = "ul";
    private String itemTag = "li";

    private Map<String, String> wrapperOptionsMap;
    private Map<String, String> listTagOptionsMap;
    private Map<String, String> itemTagOptionsMap;

    private HttpServletRequest request;

    public int doEndTag() throws JspException {

        request = (HttpServletRequest) pageContext.getRequest();
        List<WebjetError> errors = getErrors();

        if (errors.isEmpty()) {
            return super.doEndTag();
        }

        StringBuilder sb = new StringBuilder();

        String wrapper = getWrapper();
        String listTag = getListTag();
        String itemTag = getItemTag();

        if (Tools.isNotEmpty(wrapper)) {
            sb.append("<")
                .append(getWrapper())
                .append(getOptions(getWrapperOptionsMap()))
                .append(">");
        }

        //this.writeDefaultAttributes(tagWriter);

        if (Tools.isNotEmpty(listTag)) {
            sb.append("<")
                .append(getListTag())
                .append(getOptions(getListTagOptionsMap()))
                .append(">");
        }

        for (WebjetError error : errors) {
            String field = error.getField();
            String message = error.getMessage();

            if (Tools.isNotEmpty(itemTag)) {
                sb.append("<")
                    .append(getItemTag())
                    .append(getOptions(getItemTagOptionsMap()))
                    .append(">");
            }

            if (Tools.isNotEmpty(field)) {
                sb.append(error.getField()).append(" - ");
            }

            sb.append(message);

            if (Tools.isNotEmpty(itemTag)) {
                sb.append("</")
                    .append(getItemTag())
                    .append(">");
            }
        }

        if (Tools.isNotEmpty(listTag)) {
            sb.append("</")
                .append(getListTag())
                .append(">");
        }

        if (Tools.isNotEmpty(wrapper)) {
            sb.append("</")
                .append(getWrapper())
                .append(">");
        }

        try {
            pageContext.getOut().write(sb.toString());
        } catch (IOException e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return super.doEndTag();
    }

    private String getOptions(Map<String, String> options) {
        StringBuilder sb = new StringBuilder();

        if (options != null && !options.isEmpty()) {
            for (Map.Entry<String, String> entry : options.entrySet()) {
                sb.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            }
        }

        return sb.toString();
    }

    private List<WebjetError> getErrors() throws JspException {
        List<WebjetError> result = new ArrayList<>();

        if (request.getAttribute("errors") != null && request.getAttribute("errors").getClass().isAssignableFrom(WebjetErrors.class)) {
            WebjetErrors errors = (WebjetErrors) request.getAttribute("errors");
            List<WebjetError> errorsList = errors.getErrors();
            result.addAll(errorsList);
        }

        return result;
    }

    public String getWrapper() {
        return wrapper;
    }

    public void setWrapper(String wrapper) {
        this.wrapper = wrapper;
    }

    public String getListTag() {
        return listTag;
    }

    public void setListTag(String listTag) {
        this.listTag = listTag;
    }

    public String getItemTag() {
        return itemTag;
    }

    public void setItemTag(String itemTag) {
        this.itemTag = itemTag;
    }

    public void setWrapperOptions(String wrapperOptions) {
        if (wrapperOptionsMap == null) {
            wrapperOptionsMap = new HashMap<>();
        }
        addToMap(wrapperOptionsMap, wrapperOptions);
    }

    public void setListTagOptions(String listTagOptions) {
        if (listTagOptionsMap == null) {
            listTagOptionsMap = new HashMap<>();
        }
        addToMap(listTagOptionsMap, listTagOptions);
    }

    public void setItemTagOptions(String itemTagOptions) {
        if (itemTagOptionsMap == null) {
            itemTagOptionsMap = new HashMap<>();
        }
        addToMap(itemTagOptionsMap, itemTagOptions);
    }

    public Map<String, String> getWrapperOptionsMap() {
        return wrapperOptionsMap;
    }

    public Map<String, String> getListTagOptionsMap() {
        return listTagOptionsMap;
    }

    public Map<String, String> getItemTagOptionsMap() {
        return itemTagOptionsMap;
    }

    private void addToMap(Map<String, String> map, String str) {
        if (!str.contains("=")) {
            return;
        }

        String[] arr = Tools.getTokens(str, ",");
        for (String val : arr) {
            String[] values = Tools.getTokens(val, "=");

            if (values.length == 2) {
                map.put(values[0], values[1]);
            }
        }
    }
}
