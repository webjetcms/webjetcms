package sk.iway.iwcm.system.spring.webjet_component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts.util.ResponseUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.WebjetComponentParserInterface;
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.users.UserDetails;

/**
 * trieda pre parsovanie komponent z html kodu (doc_data) a nahradzanie za vygenerovany content
 */
@Component
public class WebjetComponentParser implements WebjetComponentParserInterface {
    @Autowired
    ApplicationContext context;

    // vykonava komponentu cez prisluchajucu triedu
    @Autowired
    WebjetComponentResolver componentResolver;

    // mapa najdenych komponent (kluc: cely include string, hodnota: trieda)
    Map<String, WebjetComponentInterface> components;

    /*
    HttpServletRequest request;

    HttpServletResponse response;
    */

    List<String> attributes;

    /**
     * Metód parse slúži na parsovanie a nahradzovanie !INCLUDE()! v Stringu za vygenerovaný kód
     * @param request
     * @param html
     * @return
     */
    public String parse(HttpServletRequest request, HttpServletResponse response, String html) {
        //this.request = request;
        components = new HashMap<>();

        if (Tools.isEmpty(html)) return html;

        try {
            // parsuje komponenty a ich page params z html kodu
            parseComponentsAndPageParams(html);

            // renderuje html kod z tried pre dane komponenty a nahradza v html kode include za html
            html = renderComponents(request, response, html);
        }
        catch (Exception ex) {
            html = getErrorMessage(request, ex, html);
            sk.iway.iwcm.Logger.error(ex);
        }

        return html;
    }

    /**
     * Metóda run slúži na parsovanie a nahradzovanie !INCLUDE()! v pevne definovaných request atributoch
     * "doc_data", "doc_header","doc_footer", "doc_menu", "doc_right_menu", "template_object_a", "template_object_b", "template_object_c", "template_object_d"
     * @param request
     * @param response
     */
    public void run(HttpServletRequest request, HttpServletResponse response) {
        /*
        this.request = request;
        this.response = response;
        */

        components = new HashMap<>();

        attributes = new ArrayList<>(Arrays.asList(
                "doc_data",
                "doc_header",
                "doc_footer",
                "doc_menu",
                "doc_right_menu",
                "template_object_a",
                "template_object_b",
                "template_object_c",
                "template_object_d")
        );

        // nastavenie locale pre message source, aby sa pri validaciach pouzili spravne texty zo sablony
        LocaleContextHolder.setLocale(request.getLocale());

        for (String attribute : attributes) {

            // vybratie html kodu z requestu
            String html = getHtmlFromRequestAttribute(request, attribute);

            html = parse(request, response, html);

            // nastavenie html kodu spat do requestu
            setHtmlToRequestAttribute(request, attribute, html);
        }
    }

    /**
     * Metóda parseComponentsAndPageParams slúži na parsovanie komponent tried a pageParams z !INCLUDE()!
     * @param html
     * @throws BeansException
     */
    private void parseComponentsAndPageParams(String html) throws BeansException {
        // pattern od !INCLUDE do )!
        Pattern pattern = Pattern.compile("!INCLUDE\\((.*?)\\)!");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String group = matcher.group();

            if (group.startsWith("!INCLUDE(/")) {
                //it's JSP component
                continue;
            }

            // unescape pretoze html kod moze obsahovat &quote;
            String unescapedGroup = StringEscapeUtils.unescapeHtml(group);

            if (components.containsKey(group)) {
                continue;
            }

            WebjetComponentInterface component = parseComponentClass(unescapedGroup);
            if (component != null) {
                components.put(group, component);
            }
        }
    }

    /**
     * Metóda parseComponentClass slúži na parsovanie triedy komponenty z !INCLUDE()!
     * @param include
     * @return
     * @throws BeansException
     */
    private WebjetComponentInterface parseComponentClass(String include) throws BeansException {
        WebjetComponentInterface result = null;
        String componentClass = getComponentClass(include);
        if (componentClass != null) {
            // vyhladanie beanu s originalnym nazvom
            if (context.containsBean(componentClass)) {
                result = context.getBean(componentClass, WebjetComponentInterface.class);
            }

            // vyhladanie beanu s nazvom s prvym pismenom malym
            if (result == null && context.containsBean(firstToLower(componentClass))) {
                result = context.getBean(firstToLower(componentClass), WebjetComponentInterface.class);
            }
        }

        return result;
    }

    /**
     * Parse className from !INCLUDE()! tag
     * @param include
     * @return
     */
    private String getComponentClass(String include) {
        int start = include.indexOf("(");
        int end = include.indexOf(",");
        if (end == -1) end = include.indexOf(")");

        if (end > start && end>0) {
            // nazov triedy komponenty
            String className = include.substring(start+1, end).trim();
            return className;
        }
        return null;
    }

    private String redirectComponent = null;

    /**
     * Metóda renderComponents slúži na renderovanie komponent
     * @param html
     * @return
     * @throws Exception
     */
    private String renderComponents(HttpServletRequest request, HttpServletResponse response, String html) throws Exception {
        if (components.isEmpty()) {
            return html;
        }

        for (Map.Entry<String, WebjetComponentInterface> entry : components.entrySet()) {
            String key = entry.getKey();
            WebjetComponentInterface v = entry.getValue();

            PageParams pageParams;
            int i = key.indexOf(",");
            if (i>0) pageParams = new PageParams(key.substring(i));
            else pageParams = new PageParams("");

            // render html kodu z triedy
            String rendered = componentResolver.render(request, response, v, pageParams);

            // ak navratova hodnota obsahuje redirect
            if (isRedirected(response)) {
                // nastavime nazov triedy pre vypis
                redirectComponent = v.getClass().getSimpleName();
                return "";
            }
            html = Tools.replace(html, key, rendered);
        }

        return html;
    }

    /**
     * Helper pre upravu stringu, prve pismenu na lowercase
     * @param str
     * @return
     */
    private String firstToLower(String str) {
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private String getHtmlFromRequestAttribute(HttpServletRequest request, String attribute) {
        if (attribute == null || request == null) return null;
        return (String) request.getAttribute(attribute);
    }

    private void setHtmlToRequestAttribute(HttpServletRequest request, String attribute, String html) {
        request.setAttribute(attribute, html);
    }

    /**
     * @return boolean či response obsahuje presmerovanie
     */
    public boolean isRedirected(HttpServletResponse response) {
        return getRedirectLocation(response) != null;
    }

    /**
     * @return String hodnotu presmerovania alebo null
     */
    public String getRedirectLocation(HttpServletResponse response) {
        return response != null ? response.getHeader("Location") : null;
    }

    /**
     * @return komponenta, ktorej hodnota vracia presmerovanie
     */
    public String getRedirectComponent() {
        return redirectComponent;
    }

    private String getErrorMessage(HttpServletRequest request, Exception ex, String include) {
        Logger.debug(WebjetComponentParser.class,"INCLUDE ERROR: " + ex.getMessage());

        StringBuilder content = new StringBuilder();
        Prop prop = Prop.getInstance();
        content.append(WriteTag.getErrorMessage(prop, "writetag.error", getComponentClass(include)));

        if (getUser(request).filter(u -> u.isAdmin()).isPresent() && request.getAttribute("writeTagDontShowError") == null)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();

            content.append("<div class=\"component-error\" style='border:2px solid red; background-color: white; color: black; margin: 5px; white-space: pre;'>" + ResponseUtils.filter(ex.getMessage()) + "<br>");
            String stackTrace = ResponseUtils.filter(stack);
            content.append(stackTrace + "</div>");
        }

        Adminlog.add(Adminlog.TYPE_JSPERROR, "ERROR: " + include + "\n\n" + ex.getMessage(), -1, -1);

        return content.toString();
    }

    /**
     * @return Optional prihlásený používateľ
     */
    private Optional<UserDetails> getUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
        return Optional.ofNullable(user);
    }
}