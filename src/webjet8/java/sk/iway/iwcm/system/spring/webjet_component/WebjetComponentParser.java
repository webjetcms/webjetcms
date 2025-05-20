package sk.iway.iwcm.system.spring.webjet_component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentInterface;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;
import sk.iway.iwcm.system.monitoring.MemoryMeasurement;
import sk.iway.iwcm.system.spring.WebjetComponentParserInterface;
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.tags.support_logic.ResponseUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * trieda pre parsovanie komponent z html kodu (doc_data) a nahradzanie za vygenerovany content
 */
@Component
@RequestScope
public class WebjetComponentParser implements WebjetComponentParserInterface {
    @Autowired
    ApplicationContext context;

    // vykonava komponentu cez prisluchajucu triedu
    @Autowired
    WebjetComponentResolver componentResolver;

    private static final String INCLUDE_START = "!INCLUDE(";
	private static final String INCLUDE_END = ")!";

    /**
     * Metód parse slúži na parsovanie a nahradzovanie !INCLUDE()! v Stringu za vygenerovaný kód
     * @param request
     * @param html
     * @return
     */
    public String parse(HttpServletRequest request, HttpServletResponse response, String html) {
        Map<String, WebjetComponentInterface> components = new LinkedHashMap<>();

        if (Tools.isEmpty(html)) return html;

        if (html.contains("!INCLUDE")==false) return html;

        try {
            // parsuje komponenty a ich page params z html kodu
            parseComponentsAndPageParams(html, components);

            DebugTimer dt = new DebugTimer("WriteTag");
            // renderuje html kod z tried pre dane komponenty a nahradza v html kode include za html
            html = renderComponents(request, response, html, components, dt);
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

        String[] attributes = {
                "doc_data",
                "doc_header",
                "doc_footer",
                "doc_menu",
                "doc_right_menu",
                "template_object_a",
                "template_object_b",
                "template_object_c",
                "template_object_d"
        };

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
    private void parseComponentsAndPageParams(String html, Map<String, WebjetComponentInterface> components) throws BeansException {
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
    private String renderComponents(HttpServletRequest request, HttpServletResponse response, String html, Map<String, WebjetComponentInterface> components, DebugTimer dt) throws Exception {
        if (components.isEmpty()) {
            return html;
        }

        boolean writePerfStat = "true".equals(request.getParameter("_writePerfStat"));

        for (Map.Entry<String, WebjetComponentInterface> entry : components.entrySet()) {
            String key = entry.getKey();
            WebjetComponentInterface v = entry.getValue();

            PageParams pageParams;
            int i = key.indexOf(",");
            if (i>0) pageParams = new PageParams(key.substring(i));
            else pageParams = new PageParams("");

            /** Check if this app can be rendered in current device type (onlz if we checking device) **/
            boolean render = canRenderForDevice(pageParams, request);

            if(render) {
                /*** CACHE logic ***/
                String rendered = null;

                Cache cache = Cache.getInstance();
                //prepare cache key
                String cacheKey = getCacheKey(key, request);
                int cacheMinutes = 0;
                boolean servedFromCache = false;

                StopWatch executionTimeStopWatch = new StopWatch();
                executionTimeStopWatch.start();
                MemoryMeasurement memoryConsumed = new MemoryMeasurement();

                //Is cache permitted ?
                if (isCacheEnabled(request, key)) {
                    //Get cache time param
                    cacheMinutes = pageParams.getIntValue("cacheMinutes", -1);

                    if(cacheMinutes > 0) {
                        String cachedHtml = (String) cache.getObject(cacheKey);

                        //If html code is in cache, use it
                        if(Tools.isNotEmpty(cachedHtml)) {
                            rendered = cachedHtml;
                            servedFromCache = true;
                        }
                    }
                }

                //If cache logic wasn't executed, render html code
                if(rendered == null) {
                    // render html kodu z triedy
                    rendered = componentResolver.render(request, response, v, pageParams);

                    // ak navratova hodnota obsahuje redirect
                    if (isRedirected(response)) {
                        // nastavime nazov triedy pre vypis
                        redirectComponent = v.getClass().getSimpleName();
                        return "";
                    }

                    if (cacheMinutes > 0 && cacheKey != null) {
                        //Save new rendered html code to cache
                        cache.setObjectSeconds(cacheKey, rendered, cacheMinutes * 60, true);
                    }
                }

                if (rendered != null && html.contains(key)) {
                    if (writePerfStat && key.length()>1) {
                        long diff = dt.getDiff();
                        long lastDiff = dt.getLastDiff();
                        String logText = "\nPerfStat: " + diff + " ms (+"+lastDiff+") " + key.substring(1) +"\n";
                        rendered = rendered + logText;
                        Logger.debug(WriteTag.class, logText);
                    }
                    executionTimeStopWatch.stop();
                    if (servedFromCache) ExecutionTimeMonitor.recordComponentExecutionFromCache(cacheKey, executionTimeStopWatch.getTime());
					else ExecutionTimeMonitor.recordComponentExecution(cacheKey, executionTimeStopWatch.getTime(), memoryConsumed.diff());
                    html = Tools.replace(html, key, rendered);
                }
            } else {
                //do not render component for current device type, remove it from html
                html = Tools.replace(html, key, "");
            }
        }

        return html;
    }

    /**
     * Chech if this app can be rendered in current device type
     * @param pageParams
     * @param request
     * @return
     */
    private boolean canRenderForDevice(PageParams pageParams, HttpServletRequest request) {

        //We are checking device type only if we are not in preview mode (preview mode showing apps for all devices types)
        if (request.getAttribute("inPreviewMode") != null) return true;

        String devices = pageParams.getValue("device", "");
        if (Tools.isEmpty(devices)) return true;

        String[] devicesArr = Tools.getTokens(devices, "+", true);

        BrowserDetector browser = BrowserDetector.getInstance(request);
        for (String device : devicesArr) {
            if("pc".equalsIgnoreCase(device) && browser.isDesktop()) {
                return true;
            }
            else if("tablet".equalsIgnoreCase(device) && browser.isTablet()) {
                return true;
            }
            else if("phone".equalsIgnoreCase(device) && browser.isPhone()) {
                return true;
            } else if (device.equalsIgnoreCase(browser.getBrowserDeviceType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if result of component can be cached:
     * - if user is admin, cache is disabled
     * - if parameter _disableCache=true then cache is disabled
     * - if component has parameter page other than 1 then cache is disabled
     * @param request
     * @param includeText
     * @return
     */
    private boolean isCacheEnabled(HttpServletRequest request, String includeText) {
        if ("true".equals(request.getParameter("_disableCache"))) return false;

        Identity user = getUser(request);
        if (user!=null && user.isAdmin() && Constants.getBoolean("cacheStaticContentForAdmin")==false) return false;

        //news komponenta nemoze cachovat ak ma parameter page
        if (request.getParameter("page")!=null && "1".equals(request.getParameter("page"))==false)
        {
            return false;
        }

        return true;
    }

    /**
     * Prepare chache key for html code (rendered code)
     * @param key
     * @param request
     * @return
     */
    private String getCacheKey(String key, HttpServletRequest request) {
        //Prepare cache key
        StringBuilder cacheKeySB = new StringBuilder( key );
        String cacheKey = "";
        int startIndex = cacheKeySB.indexOf(INCLUDE_START);
        int includeEndIndex;
        int failsafe = 0;

        while (startIndex != -1 && failsafe < 100) {
            failsafe++;
            includeEndIndex = cacheKeySB.indexOf(INCLUDE_END, startIndex);

            if (includeEndIndex < 0) {
                //nenasiel sa koniec
                cacheKeySB.delete(0,startIndex+INCLUDE_START.length());
                startIndex = cacheKeySB.indexOf(INCLUDE_START);
                continue;
            }

            cacheKey = "writeTag_" + cacheKeySB.substring(startIndex, includeEndIndex);
			cacheKey = Tools.replace(cacheKey, "!DOC_ID!", request.getParameter("docid")) + " ;" + PageLng.getUserLng(request);
        }

        return cacheKey;
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

        UserDetails user = getUser(request);
        if (user != null && user.isAdmin() && request.getAttribute("writeTagDontShowError") == null)
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
    private Identity getUser(HttpServletRequest request) {
        return UsersDB.getCurrentUser(request);
    }
}