package sk.iway.iwcm.doc.ninja;

import net.sourceforge.stripes.mock.MockHttpServletResponse;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.components.abtesting.ABTesting;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.WJResponseWrapper;
import sk.iway.iwcm.users.UserDetails;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ninja {

    private HttpServletRequest request;
    private Prop prop;
    private boolean allowNinjaDebug;
    private Page page;
    private UserAgent userAgent;
    private Webjet webjet;
    private Temp temp;
    private Properties config;

    //patter a matcher pre nahradu nbsp za spojkou, su staticke, aby sa znovapouzili
    private static Pattern nbspPattern = null;
    private static String nbspReplacement = null;

    public Ninja(HttpServletRequest request) {
        this.request = request;
        loadConfigProperties();

        this.page = new Page(this);
        this.temp = new Temp(this);
        this.webjet = new Webjet(this);
        this.userAgent = new UserAgent(this);

        setDefaultValues();
    }

    private void setDefaultValues() {
        String lng = PageLng.getUserLng(request);
        this.prop = Prop.getInstance(lng);
        this.allowNinjaDebug = getAllowNinjaDebug();
    }

    public static void includeNinja(HttpServletRequest req) {
        req.setAttribute("ninja", new Ninja(req));
    }

    /*private String getBasePath() {
        return temp.getBasePath();
    }*/

    public boolean getDebug() {
        return allowNinjaDebug;
    }

    private boolean getAllowNinjaDebug() {
        if (request.getParameter("ninjaDebug") != null) {
            return Tools.getBooleanValue(request.getParameter("ninjaDebug"), false);
        }

        if (request.getAttribute("ninjaDebug") != null) {
            return Tools.getBooleanValue((String) request.getAttribute("ninjaDebug"), false);
        }

        return Tools.getBooleanValue(getConfig("ninjaDebug", "false"), false);
    }

    public String getConfig(String label) {
        return getConfig(label, null);
    }

    public String getConfig(String label, String defaultValue) {
        Object value = getOption(label);

        if (value != null && value.getClass().isAssignableFrom(String.class)) {
            return (String) value;
        }

        return defaultValue;
    }

    private Object getOption(String label) {
        Object value = null;
        if (request.getParameter(label) != null) {
            value = request.getParameter(label);
        } else if (request.getAttribute(label) != null) {
            value = request.getAttribute(label);
        } else if (config != null && config.containsKey(label)) {
            value = config.get(label);
        }

        return value;
    }

    private Optional<UserDetails> getUser() {
        HttpSession session = request.getSession();

        return Optional.ofNullable((Identity) session.getAttribute(Constants.USER_KEY));
    }


    public Page getPage() {
        return page;
    }

    public Webjet getWebjet() {
        return webjet;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public Temp getTemp() {
        return temp;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public Prop getProp() {
        return prop;
    }

    public Properties getConfig() {
        return config;
    }

    private void loadConfigProperties() {
        Cache c = Cache.getInstance();
        TemplatesGroupBean tempGroup = (TemplatesGroupBean)request.getAttribute("templatesGroupDetails");
        String path = "";
        if (tempGroup != null) {
            String templateFolderName = tempGroup.getDirectory();
            if (Tools.isNotEmpty(templateFolderName) && !("/".equals(templateFolderName))) {
                path = "/templates/"+templateFolderName+"/"; //NOSONAR
            }
        }

        config = c.getObject("configProperties." + path, Properties.class);

        Optional<UserDetails> userOptional = getUser();
        if (userOptional.filter(u -> !u.isAdmin()).isPresent() && config != null) {
            Logger.debug(Ninja.class, "Vratiam config properties z cache");
            return;
        }

        config = new Properties();

        File file = new File(Tools.getRealPath(WriteTagToolsForCore.getCustomPage(path+"config.properties", getRequest())));
        if (!file.exists()) {
            return;
        }

        try (InputStream input = new FileInputStream(file)) {
            config.load(input);
            c.setObject("configProperties." + path, config, 60 * 24);
        } catch (IOException ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
    }

    public DocDetails getDoc() {
        return (DocDetails) getRequest().getAttribute("docDetails");
    }

    /* =========== Nahrada medzery za nbsp za spojkou ============= */
    private static void initNbspReplacement() {
        String[] ninjaNbspReplaceRegex = Tools.getTokens(Constants.getString("ninjaNbspReplaceRegex"), "\n");
        if (ninjaNbspReplaceRegex!=null && ninjaNbspReplaceRegex.length==2) {
            nbspPattern = Pattern.compile(ninjaNbspReplaceRegex[0], Pattern.CASE_INSENSITIVE);
            nbspReplacement = ninjaNbspReplaceRegex[1];
        }
    }

    private static Pattern getNbsPattern() {
        if (nbspPattern == null) {
            initNbspReplacement();
        }
        return nbspPattern;
    }

    private static String getNbspReplacement() {
        if (nbspReplacement == null) {
            initNbspReplacement();
        }
        return nbspReplacement;
    }

    /**
     * Vycisti static objekty po zmene konf. premennej
     */
    public static void resetNbspReplaceRegex() {
        nbspPattern = null;
        nbspReplacement = null;
    }

    /**
     * Nahradi v texte medzeru za spojkou za entitu &nbsp; (napr fero a marek -> fero a&nbsp;marek )
     * @param text
     * @return
     */
    public String replaceNbspSingleChar(String text) {
        try {
            Matcher matcher = getNbsPattern().matcher(text);
            String replaced = matcher.replaceAll(getNbspReplacement());
            return replaced;
        }
        catch (Exception ex) {
            Logger.error(Ninja.class, ex);
        }

        return text;
    }

    /**
     * Metoda pre vykonanie include komponenty
     * @param component String
     * @return String
     */
    public String write(String component) {

        /*if (!component.startsWith("!INCLUDE(")) {
            component = (String) request.getAttribute(component);
        }*/

        if (component == null) {
            return "";
        }

        String includeFileName = "/components/_common/thymeleaf/write.jsp"; //NOSONAR
        request.setAttribute("thymeleaf_write_name", component);

        return executeJsp(includeFileName);
    }

    /**
     * Vykona zadany JSP subor a vrati vysledok ako String
     * @param includeFileName
     * @return
     */
    public String executeJsp(String includeFileName) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        WJResponseWrapper respWrapper = new WJResponseWrapper(response, request);

        try {
            request.getRequestDispatcher(includeFileName).include(request, respWrapper);
        } catch (ServletException | IOException e) {
            Logger.error(Ninja.class, e);
        }

        if (Tools.isEmpty(respWrapper.redirectURL))
        {
            return respWrapper.strWriter.getBuffer().toString();
        }

        return "";
    }

    /**
     * Get AB variant from request attribute or from URL
     * @return - a or b depending on the ABTesing variant
     */
    public String getAbVariant() {

        if(request == null) return "a";

        String variant = (String)request.getAttribute("ABTestingVariant");
        if (variant == null) {
            if (page != null && page.getDoc() != null) {
                return ABTesting.getVariantFromUrl(page.getDoc().getVirtualPath());
            }
        }

        return variant == null ? "a" : variant;
    }
}