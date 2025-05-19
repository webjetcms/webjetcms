package sk.iway.iwcm.common;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts.util.ResponseUtils;
import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.XslFilter;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import sk.iway.iwcm.*;
import sk.iway.iwcm.components.dictionary.DictionaryDB;
import sk.iway.iwcm.components.dictionary.model.DictionaryBean;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;


public class DocTools {

    protected DocTools() {
        //utility class
    }


    /**
     * Otestuje, ci zadana cesta je nastavena ako vynimka pre Strict XSS filter
     * @param path - URL adresa
     * @param constantName - meno konstanty, bud xssProtectionStrictGetUrlException, alebo xssProtectionStrictPostUrlException
     * @return
     */
    public static boolean isXssStrictUrlException(String path, String constantName)
    {
        return isUrlAllowed(path, constantName, true);
    }

    /**
     * Otestuje, ci zadana cesta je v zozname ciest definovanych v zadanej konstante. Konstanta je definovana ako ciarkou
     * oddeleny zoznam URL adries,
     * ak URL zacina na % pouzije sa substring,
     * ak konci na ! pouzije sa equal,
     * ak URL zacina na % a konci na ! pouzije sa endsWith,
     * inak sa pouzije startsWith
     * @param path - URL adresa
     * @param constantName - meno konstanty so zoznamom povolenych URL adries
     * @param appendSystemValue - ak je nastavene na true, tak sa hlada v constantName aj v constantNameSystem (zoznam povoleni sa spoji)
     * @return
     */
    public static boolean isUrlAllowed(String path, String constantName, boolean appendSystemValue)
    {
        if (path == null) path = "";

        path = path.toLowerCase();

        String xssProtectionStrictUrlException = Constants.getString(constantName+"System");
        if (Tools.isNotEmpty(Constants.getString(constantName))) xssProtectionStrictUrlException += "," + Constants.getString(constantName);
        if (xssProtectionStrictUrlException != null && xssProtectionStrictUrlException.length()>2)
        {
            xssProtectionStrictUrlException = xssProtectionStrictUrlException.toLowerCase();

            StringTokenizer st = new StringTokenizer(xssProtectionStrictUrlException, ",+;\n");
            while (st.hasMoreTokens())
            {
                String token = st.nextToken();

                //Logger.debug(ShowDocAction.class, "isXssStrictUrlException path="+path+" token="+token);

                if (token.startsWith("%") && token.endsWith("!") && token.length()>4 && path.endsWith(token.substring(1, token.length()-1))) return true;
                else if (token.startsWith("%") && token.length()>2 && path.indexOf(token.substring(1))!=-1) return true;
                else if (token.endsWith("!") && token.length()>=2 && path.equals(token.substring(0, token.length()-1))) return true;
                else if (Tools.isNotEmpty(path) && path.startsWith(token))
                {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     *  vyhodi nepovolene znaky z nazvu suboru (zrusi aj znak /)
     *  POZOR: DA to na LOWER CASE!!!
     *
     *@param  ret   Description of the Parameter
     *@return       Description of the Return Value
     */
    public static String removeChars(String ret)
    {
        return removeChars(ret, true);
    }

    /**
     * Vyhodi nepovolene znaky z nazvu suboru
     * @param ret
     * @param lowerCase - ak je nastavene na true, zmeni aj velkost na male pismena
     * @return
     */
    public static String removeChars(String ret, boolean lowerCase)
    {
        //odstran HTML kod
        if (ret.indexOf("<")!=-1 && ret.indexOf(">")!=-1 && (ret.indexOf("/")!=-1 || ret.indexOf("-")!=-1))
        {
            ret = SearchTools.htmlToPlain(ret);
        }

        //v nazve suboru nesmie byt znak /
        ret = ret.trim();
        ret = ret.replace('/', '-');
        //premenovanie .jpeg suborov na .jpg
        ret = Tools.replace(ret, ".jpeg", ".jpg");
        ret = Tools.replace(ret, ".JPEG", ".JPG");

        if (lowerCase) return (removeCharsDir(ret, false)).toLowerCase();
        return (removeCharsDir(ret, false));
    }


    /**
     * Otestuje, ci zadana hodnota obsahuje znaky XSS
     * @param value
     * @return
     */
    public static boolean testXss(String value)
    {
        if (Constants.getBoolean("xssProtection")==false) return false;

        if (value == null) return false;

        //null byte test (IE bug)
        if (value.indexOf('\u0000')!=-1)
        {
            Adminlog.add(Adminlog.TYPE_XSS, "XSS null byte, value="+value, -1, -1);
            return true;
        }

        //odstran zakladne nepotrebne znaky (tab, medzera...)
        value = Tools.replace(value, "\t", "");
        value = Tools.replace(value, "\r", "");
        value = Tools.replace(value, "\n", "");
        if (Constants.getBoolean("xssDeleteSpaces")) value = Tools.replace(value, " ", "");
        if (Constants.getBoolean("xssDeleteSpaces")) value = Tools.replace(value, Constants.NON_BREAKING_SPACE, "");

        String valueLC = value.toLowerCase();
        //nahrad ascii entity typu JaVaS&#99;RiPt:alert(142277,334213762);
        String value2LC = StringEscapeUtils.unescapeHtml(value).toLowerCase();
        value2LC = Tools.replace(value2LC, "\t", "");
        value2LC = Tools.replace(value2LC, "\r", "");
        value2LC = Tools.replace(value2LC, "\n", "");
        if (Constants.getBoolean("xssDeleteSpaces")) value2LC = Tools.replace(value2LC, " ", "");
        if (Constants.getBoolean("xssDeleteSpaces")) value2LC = Tools.replace(value2LC, Constants.NON_BREAKING_SPACE, "");

        //nahrad &amp;#39; za povodny apostrof, aby do formularov bolo mozne zadat znak apostrofu
        valueLC = Tools.replace(valueLC, "&amp;#39;", "'");
        valueLC = Tools.replace(valueLC, "&#39;", "'");
        value2LC = Tools.replace(value2LC, "&amp;#39;", "'");
        value2LC = Tools.replace(value2LC, "&#39;", "'");

        String xssTestValues = Constants.getString("xssTestValues");
        StringTokenizer st = new StringTokenizer(xssTestValues, "|");
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            if (valueLC.indexOf(token)!=-1 || value2LC.indexOf(token)!=-1)
            {
                Adminlog.add(Adminlog.TYPE_XSS, "XSS token="+token+", value="+value, -1, -1);
                return true;
            }
        }

        // <IMG STYLE="xss:expr/*XSS*/ession(alert('XSS'))">
        if ((valueLC.indexOf("/*")!=-1 && valueLC.indexOf("*/")!=-1) ||
                (value2LC.indexOf("/*")!=-1 && value2LC.indexOf("*/")!=-1))
        {
            Adminlog.add(Adminlog.TYPE_XSS, "XSS comment, value="+value, -1, -1);
            return true;
        }

        return false;
    }


    /**
     *  Vyhodi nepovolene znaky z nazvu adresara (ponecha znak /)
     *  POZOR: neda to na LOWER CASE!!!
     *
     *@param  ret   Description of the Parameter
     *@return
     */
    public static String removeCharsDir(String ret)
    {
        return removeCharsDir(ret, true);
    }


    public static String removeCharsDir(String ret, boolean removeSpojky)
    {
        //palarikovske_luky-pr .pdf
        ret = Tools.replace(ret, " .", ".");

        ret = Tools.replace(ret, Constants.NON_BREAKING_SPACE, " ");

        ret = DB.internationalToEnglish(ret); //.toLowerCase();

        //odstran HTML kod
        if (ret.indexOf("<")!=-1 && ret.indexOf(">")!=-1 && (ret.indexOf("/")!=-1 || ret.indexOf("-")!=-1))
        {
            //removeChars uz nahradi </strong> za <-strong>
            ret = SearchTools.htmlToPlain(ret);
        }

        ret = Tools.replace(ret, "&#47;", "-");

        ret = ret.trim();
        ret=ret.replaceAll(" +", "-");
        ret=ret.replaceAll("\\.+", ".");
        ret=ret.replaceAll("\\-+", "-");
        ret=ret.replaceAll(Constants.getString("DocTools.removeCharsDir"),"");

        if (removeSpojky && Constants.getBoolean("urlRemoveSpojky"))
        {
            String[] spojky = Tools.getTokens(Constants.getString("urlRemoveSpojkyList"), ",");
            for (String spojka : spojky)
            {
                ret = Tools.replace(ret, "-"+spojka+"-", "-");
                ret = Tools.replace(ret, "-"+spojka+"/", "/");
            }

            try
            {
                //ponechame len poslednu .
                int lastBodka = ret.lastIndexOf('.');
                if (lastBodka > 0)
                {
                    String ret2 = Tools.replace(ret, ".", "-");
                    ret = ret2.substring(0, lastBodka) + "." + ret2.substring(lastBodka+1);
                }
            } catch (Exception ex)
            {

            }
        }

        ret = Tools.replace(ret, "---", "-");
        ret = Tools.replace(ret, "--", "-");
        ret = Tools.replace(ret, "____", "_");
        ret = Tools.replace(ret, "___", "_");
        ret = Tools.replace(ret, "__", "_");
        ret = Tools.replace(ret, "__", "_");
        ret = Tools.replace(ret, "_-_", "-");
        ret = Tools.replace(ret, "-_", "-");
        ret = Tools.replace(ret, "_-", "-");
        ret = Tools.replace(ret, "-.-", "-");
        ret = Tools.replace(ret, "-.html", ".html");
        ret = Tools.replace(ret, "-.jpg", ".jpg");
        ret = Tools.replace(ret, "-.doc", ".doc");
        ret = Tools.replace(ret, "-.xls", ".xls");
        ret = Tools.replace(ret, "-.pdf", ".pdf");
        ret = Tools.replace(ret, "-.", "-");
        ret = Tools.replace(ret, ".-", "-");
        ret = Tools.replace(ret, "./", "/");
        ret = Tools.replace(ret, "-/", "/");

        if (ret.length()>1 && ret.endsWith("_")) ret = ret.substring(0, ret.length()-1);
        if (ret.length()>1 && ret.endsWith(".")) ret = ret.substring(0, ret.length()-1);

        return (ret);
    }

    /**
     * Vrati docId stranky pre request hodnotu doc_data, doc_menu, doc_header atd
     * @param name
     * @param request
     * @return
     */
    public static int getRequestNameDocId(String name, HttpServletRequest request)
    {
        try
         {
            Integer docId = (Integer)request.getAttribute(name+"-docId=");
           if (docId != null) return docId.intValue();
         }
         catch (Exception e)
         {
             sk.iway.iwcm.Logger.error(e);
         }

         if ("doc_data".equals(name)) return Tools.getDocId(request);

        return -1;
    }

    /**
     *  vrati string z requestu pod menom name, ak neexistuje vrati prazdny
     *  string
     *
     *@param  request
     *@param  name
     *@return
     */
    private static String getRequestString(HttpServletRequest request, String name)
    {
        String nameOrig = name;
        String ret = (String) request.getAttribute(name);

        boolean filter = true;
        //default false for request objects, we expect it to be inserted as HTML code
        if (ret != null) filter = false;
        if (name.startsWith("unfilter.")) {
            filter = false;
            name = name.substring("unfilter.".length());
        } else if (name.startsWith("filter.")) {
            filter = true;
            name = name.substring("filter.".length());
        }

        //try again request attribute without prefix
        if (ret == null && name.equals(nameOrig)==false) ret = (String) request.getAttribute(name);

        if (ret == null && name.startsWith("header.") && name.length()>8 && request.getHeader(name.substring(7))!=null)
        {
            ret = request.getHeader(name.substring(7));
        }
        if (ret == null && "remoteIP".equals(name)) ret = Tools.getRemoteIP(request);
        if (ret == null && "remoteHost".equals(name)) ret = Tools.getRemoteHost(request);

        if (ret == null && "baseHref".equals(name)) ret = Tools.getBaseHref(request);

        if (ret == null && "domainName".equals(name)) ret = DocDB.getDomain(request);

        if (ret == null && "url".equals(name)) ret = PathFilter.getOrigPathDocId(request);

        if (ret == null && "urlQuery".equals(name))
        {
            ret = PathFilter.getOrigPath(request);
            String qs = (String)request.getAttribute("path_filter_query_string");
            if (Tools.isNotEmpty(qs))
            {
                ret = Tools.addParametersToUrl(ret, qs);
            }
        }

        if (ret == null && name.startsWith("wjtooltip:"))
        {
            String lng = PageLng.getUserLng(request);
            String domainName = DocDB.getDomain(request);

            String tooltipName = name.substring(name.lastIndexOf("wjtooltip:")+10);
            tooltipName = Tools.replace(tooltipName, "&nbsp;", " ");
            tooltipName = Tools.replace(tooltipName, Constants.NON_BREAKING_SPACE, " ");
            List<DictionaryBean> tooltipsAll = DictionaryDB.getDictionariesByName(tooltipName, "tooltip");

            List<DictionaryBean> tooltipsFilterd = new ArrayList<>();
            for (DictionaryBean db : tooltipsAll)
            {
                if (lng.equalsIgnoreCase(db.getLanguage()))
                {
                    tooltipsFilterd.add(db);
                }
            }
            tooltipsAll = tooltipsFilterd;
            tooltipsFilterd = new ArrayList<>();
            for (DictionaryBean db : tooltipsAll)
            {
                if (Tools.isEmpty(db.getDomain()) || domainName.equalsIgnoreCase(db.getDomain()))
                {
                    tooltipsFilterd.add(db);
                }
            }


            if(tooltipsFilterd.size()>0)
            {
                String suffix = "\" "+Constants.getString("tooltipReplacerAttributes")+" data-tooltip-name=\""+tooltipName;

                DictionaryBean tooltip = tooltipsFilterd.get(0);

                if (tooltipsFilterd.size()>1)
                {
                    //skus najst best match podla podla domeny
                    for (DictionaryBean db : tooltipsFilterd)
                    {
                        if (Tools.isNotEmpty(domainName) && Tools.isNotEmpty(db.getDomain()) && domainName.equals(db.getDomain()))
                        {
                            tooltip = db;
                        }
                    }
                }

                ret = Tools.replace(tooltip.getValue(), "\"", "&quot;")+suffix;
                return (ret);
            }
            else {
                ret = tooltipName;
            }
            //System.out.println("_______________________________________"+ ret +" >>> "+ name);
        }

        if (ret == null) ret = "";

        if (filter) {
            ret = ResponseUtils.filter(ret);
            ret = Tools.replace(ret, "\n", " ");
            ret = Tools.replace(ret, "\r", " ");
            ret = Tools.replace(ret, "&amp;", "&");
        }

        return (ret);
    }

    /**
     *  vrati string z parametru pod menom name, ak neexistuje vrati prazdny
     *  string
     *
     *@param  request
     *@param  name
     *@return
     */
    private static String getParameterString(HttpServletRequest request, String name)
    {
        String ret = request.getParameter(name);
        if (ret == null)
        {
            ret = "";
        }
        //ochrana pred XSS
        ret = ResponseUtils.filter(ret);
        return (ret);
    }

    /**
     *  aktualizuje kody v texte
     *
     *@param  user
     *@param  text
     *@param  currentDocId
     *@param  request
     *@param  servletContext
     *@return
     */
    public static StringBuilder updateCodes(Identity user, StringBuilder text, int currentDocId, HttpServletRequest request, ServletContext servletContext)
    {
        try
        {
            String docTitle = (String) request.getAttribute("doc_title");
            if (docTitle == null)
            {
                docTitle = "";
            }

            java.util.Date d_now = new java.util.Date();
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(Constants.getString("dateFormat"));
            String actualDate = formatter.format(d_now);
            String actualDateCZ = actualDate;

            text = Tools.replace(text, "!DATUM!", actualDate);

            java.util.GregorianCalendar cal = new java.util.GregorianCalendar();
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
            {
                actualDate = "Pondelok " + actualDate;
                actualDateCZ = "Pondělí " + actualDateCZ;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY)
            {
                actualDate = "Utorok " + actualDate;
                actualDateCZ = "Úterý " + actualDateCZ;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
            {
                actualDate = "Streda " + actualDate;
                actualDateCZ = "Středa " + actualDateCZ;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY)
            {
                actualDate = "Štvrtok " + actualDate;
                actualDateCZ = "Čtvrtek " + actualDateCZ;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
            {
                actualDate = "Piatok " + actualDate;
                actualDateCZ = "Pátek " + actualDateCZ;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
            {
                actualDate = "Sobota " + actualDate;
                actualDateCZ = "Sobota " + actualDateCZ;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            {
                actualDate = "Nedeľa " + actualDate;
                actualDateCZ = "Neděle " + actualDateCZ;
            }

            text = Tools.replace(text, "!DEN_DATUM!", actualDate);
            text = Tools.replace(text, "!DEN_DATUM_CZ!", actualDateCZ);

            formatter = new java.text.SimpleDateFormat(Constants.getString("timeFormat"));
            text = Tools.replace(text, "!TIME!", formatter.format(d_now));

            //english
            formatter = new java.text.SimpleDateFormat(Constants.getString("dateFormatEn"));
            String actualDateEN = formatter.format(d_now);

            text = Tools.replace(text, "!DATE!", actualDateEN);

            cal = new java.util.GregorianCalendar();
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
            {
                actualDateEN = "Monday " + actualDateEN;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY)
            {
                actualDateEN = "Tuesday " + actualDateEN;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
            {
                actualDateEN = "Wednesday " + actualDateEN;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY)
            {
                actualDateEN = "Thursday " + actualDateEN;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
            {
                actualDateEN = "Friday " + actualDateEN;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
            {
                actualDateEN = "Saturday " + actualDateEN;
            }
            else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            {
                actualDateEN = "Sunday " + actualDateEN;
            }

            String year = String.valueOf(cal.get(Calendar.YEAR));

            text = Tools.replace(text, "!YEAR!", year);
            text = Tools.replace(text, "!DAY_DATE!", actualDateEN);
            text = Tools.replace(text, "!DOC_TITLE!", docTitle);
            //text = Tools.replace(text, "TITLE", docTitle);

            //poreplacuj texty s !request(
            try
            {
                int counter = 0;
                String REPLACE_START = "!REQUEST(";
                String REPLACE_END = ")!";
                int start = text.indexOf(REPLACE_START);
                int end;
                String name;
                while (start != -1)
                {
                    end = text.indexOf(REPLACE_END, start);
                    if (end > start)
                    {
                        try
                        {
                            name = text.substring(start + REPLACE_START.length(), end);
                            //Logger.println(this,"name="+name);
                            text = Tools.replace(text, REPLACE_START + name + REPLACE_END, getRequestString(request, name));
                        }
                        catch (Exception ex)
                        {
                            sk.iway.iwcm.Logger.error(ex);
                        }
                    }

                    //failsafe
                    counter++;
                    if (counter > 30)
                    {
                        break;
                    }
                    start = text.indexOf(REPLACE_START, start + 1);
                }
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }

            //poreplacuj texty s !parameter(
            try
            {
                int counter = 0;
                String REPLACE_START = "!PARAMETER(";
                String REPLACE_END = ")!";
                int start = text.indexOf(REPLACE_START);
                int end;
                String name;
                while (start != -1)
                {
                    end = text.indexOf(REPLACE_END, start);
                    if (end > start)
                    {
                        try
                        {
                            name = text.substring(start + REPLACE_START.length(), end);
                            //Logger.println(this,"name="+name);
                            text = Tools.replace(text, REPLACE_START + name + REPLACE_END, getParameterString(request, name));
                        }
                        catch (Exception ex)
                        {
                            sk.iway.iwcm.Logger.error(ex);
                        }
                    }

                    //failsafe
                    counter++;
                    if (counter > 30)
                    {
                        break;
                    }
                    start = text.indexOf(REPLACE_START, start + 5);
                }
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }

            //poreplacuj texty s !text(
            try
            {
                Prop prop = Prop.getInstance(request);
                int counter = 0;
                String REPLACE_START = "!TEXT(";
                String REPLACE_END = ")!";
                int start = text.indexOf(REPLACE_START);
                int end;
                String name;
                while (start != -1)
                {
                    end = text.indexOf(REPLACE_END, start);
                    if (end > start)
                    {
                        try
                        {
                            name = text.substring(start + REPLACE_START.length(), end);
                            //Logger.println(this,"name="+name);
                            text = Tools.replace(text, REPLACE_START + name + REPLACE_END, prop.getText(name));
                        }
                        catch (Exception ex)
                        {
                            sk.iway.iwcm.Logger.error(ex);
                        }
                    }

                    //failsafe
                    counter++;
                    if (counter > 60)
                    {
                        break;
                    }
                    start = text.indexOf(REPLACE_START, start + 5);
                }
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }

            //update CSRF token
            if (text.indexOf("!CSRF_TOKEN!")!=-1)
            {
                text = Tools.replace(text, "!CSRF_TOKEN!", CSRF.getCsrfToken(request.getSession(), true));
            }
            if (text.indexOf("!CSRF_INPUT!")!=-1)
            {
                text = Tools.replace(text, "!CSRF_INPUT!", CSRF.getCsrfTokenInputFiled(request.getSession(), true));
            }

            text = updateUserCodes(user, text);
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        return (text);
    }

    /**
     *  Aktualizuje kody pouzivatela !LOGGED_USER_XXX! v texte
     *
     *@param  user  Description of the Parameter
     *@param  text  Description of the Parameter
     *@return       Description of the Return Value
     */
    public static StringBuilder updateUserCodes(Identity user, StringBuilder text)
    {
        if (user == null)
        {
            text = Tools.replace(text, "!LOGGED_USER_NAME!", "");
            text = Tools.replace(text, "!name!", "");
            text = Tools.replace(text, "!LOGGED_USER_FIRSTNAME!", "");
            text = Tools.replace(text, "!LOGGED_USER_LASTNAME!", "");
            text = Tools.replace(text, "!LOGGED_USER_TITLE!", "");
            text = Tools.replace(text, "!LOGGED_USER_LOGIN!", "");
            text = Tools.replace(text, "!LOGGED_USER_PASSWORD!", "");
            text = Tools.replace(text, "!LOGGED_USER_EMAIL!", "");
            text = Tools.replace(text, "!LOGGED_USER_COMPANY!", "");
            text = Tools.replace(text, "!LOGGED_USER_CITY!", "");
            text = Tools.replace(text, "!LOGGED_USER_ADDRESS!", "");
            text = Tools.replace(text, "!LOGGED_USER_COUNTRY!", "");
            text = Tools.replace(text, "!LOGGED_USER_PHONE!", "");
            text = Tools.replace(text, "!LOGGED_USER_ZIP!", "");
            text = Tools.replace(text, "!LOGGED_USER_ID!", "");
            text = Tools.replace(text, "!LOGGED_USER_BIRTH_DATE!", "");

            text = Tools.replace(text, "!LOGGED_USER_FIELDA!", "");
            text = Tools.replace(text, "!LOGGED_USER_FIELDB!", "");
            text = Tools.replace(text, "!LOGGED_USER_FIELDC!", "");
            text = Tools.replace(text, "!LOGGED_USER_FIELDD!", "");
            text = Tools.replace(text, "!LOGGED_USER_FIELDE!", "");

            text = Tools.replace(text, "!LOGGED_USER_GROUPS!", "");
        }
        else
        {
            text = Tools.replace(text, "!LOGGED_USER_NAME!", user.getFullName());
            text = Tools.replace(text, "!name!", user.getFullName());
            text = Tools.replace(text, "!LOGGED_USER_FIRSTNAME!", user.getFirstName());
            text = Tools.replace(text, "!LOGGED_USER_LASTNAME!", user.getLastName());
            text = Tools.replace(text, "!LOGGED_USER_TITLE!", user.getTitle());
            text = Tools.replace(text, "!LOGGED_USER_LOGIN!", user.getLoginName());
            text = Tools.replace(text, "!LOGGED_USER_PASSWORD!", user.getPassword());
            text = Tools.replace(text, "!LOGGED_USER_EMAIL!", user.getEmail());
            text = Tools.replace(text, "!LOGGED_USER_COMPANY!", user.getCompany());
            text = Tools.replace(text, "!LOGGED_USER_CITY!", user.getCity());
            text = Tools.replace(text, "!LOGGED_USER_ADDRESS!", user.getAdress());
            text = Tools.replace(text, "!LOGGED_USER_COUNTRY!", user.getCountry());
            text = Tools.replace(text, "!LOGGED_USER_PHONE!", user.getPhone());
            text = Tools.replace(text, "!LOGGED_USER_ZIP!", user.getPSC());
            text = Tools.replace(text, "!LOGGED_USER_ID!", Integer.toString(user.getUserId()));
            text = Tools.replace(text, "!LOGGED_USER_BIRTH_DATE!", user.getDateOfBirth());

            text = Tools.replace(text, "!LOGGED_USER_FIELDA!", user.getFieldA());
            text = Tools.replace(text, "!LOGGED_USER_FIELDB!", user.getFieldB());
            text = Tools.replace(text, "!LOGGED_USER_FIELDC!", user.getFieldC());
            text = Tools.replace(text, "!LOGGED_USER_FIELDD!", user.getFieldD());
            text = Tools.replace(text, "!LOGGED_USER_FIELDE!", user.getFieldE());

            UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
            text = Tools.replace(text, "!LOGGED_USER_GROUPS!", userGroupsDB.convertIdsToNames(user.getUserGroupsIds()));
        }
        return (text);
    }

    /**
     * Strict XSS filter, pouziva sa na KOMPLET VSETKY GET poziadavky (ak je zapnuty), volane z PathFilter
     * @param request
     * @param path
     * @param qs
     * @return
     */
    public static String getXssStrictUrlRedirect(HttpServletRequest request, String path, String qs)
    {
        //STRICT XSS FILTER
        boolean xssProtectionStrictGet = Constants.getBoolean("xssProtectionStrictGet");
        if (xssProtectionStrictGet==false || qs==null || qs.length()<2) return null;

        //ak sme vynimka, skonci
        if (isXssStrictUrlException(path, "xssProtectionStrictGetUrlException")) return null;

        //xssProtectionStrictGet pouzijeme len pre GET metody
        if ("get".equalsIgnoreCase(request.getMethod())==false)
        {
            //pre /components/ adresar a JSP/Stripes subory budem testovat aj pre POST requesty
            if (path.indexOf("/components/")!=-1 && (path.endsWith(".jsp") || path.endsWith(".action") || path.endsWith(".do")))
            {
                //vynimky, pre ktore netreba testovat

                //ak je prihlaseny admin, tak POST ignorujeme
                Identity user = UsersDB.getCurrentUser(request);
                if (user != null && user.isAdmin()) return null;

                //ak nie je admin, prebehni POST vynimky
                if (isXssStrictUrlException(path, "xssProtectionStrictPostUrlException")) return null;
            }
            else
            {
                //xssProtectionStrictGet pouzijeme len pre GET metody
                return null;
            }
        }

        boolean hasAtLeastOneXss = false;
        StringBuilder redirectUrl = new StringBuilder();

        Enumeration<String> parameters = request.getParameterNames();
        while (parameters.hasMoreElements())
        {
            String name = parameters.nextElement();

            //prenos include parametra z editor_component.jsp
            if ("include".equals(name) && path.startsWith("/apps/") && path.contains("/admin/")) continue;

           if (testXssStrictGet(name))
           {
               Logger.debug(DocTools.class, "XSS HAS: "+name);
               hasAtLeastOneXss = true;
               continue;
           }
           String[] values = request.getParameterValues(name);
           for (int i=0; i<values.length; i++)
           {
               String value = values[i];

               //vynimky pre Stripes formulare
               if ("_sourcePage".equals(name) || "__fp".equals(name) || "__token".equals(name)) value = Tools.replace(value, "=", " ");

               //test na strict hodnoty
               boolean hasXss = false;
               if (testXssStrictGet(value))
               {
                   Logger.debug(DocTools.class, "XSS HAS: "+value);
                   hasXss = true;
                   hasAtLeastOneXss = true;
               }

               //docid nam do URL netreba
               if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML && "docid".equals(name)) continue;

               //pridaj parameter do URL
               if (redirectUrl.length() != 0) redirectUrl.append('&');
               if (hasXss)
               {
                   //hodnotu parametra upravime
                   redirectUrl.append(name+"="+Tools.URLEncode(fixXssStrictGet(value)));
               }
               else
               {
                   //pridame celu hodnotu parametra
                   redirectUrl.append(name+"="+Tools.URLEncode(value));
               }
           }
        }

        if (hasAtLeastOneXss)
        {
            String redirUrl = PathFilter.getOrigPath(request)+"?"+redirectUrl.toString();
            redirUrl = Tools.sanitizeHttpHeaderParam(redirUrl);

            Logger.println(DocTools.class, "XSS STRICT GET:"+redirUrl);

            return redirUrl;
        }
        return null;
    }


    /**
     * Striktny test na XSS hodnotu, pouziva sa len v pripade GET poziadaviek
     * @param value
     * @return
     */
    public static boolean testXssStrictGet(String value)
    {
        if (value == null) return false;

        for (int i=0; i<value.length(); i++)
        {
            char ch = value.charAt(i);

            //pozor, rovnaky test je aj v nasledovnej metode
            if (ch=='\'' || ch=='"' || ch=='<' || ch=='>' || ch=='=') return true;
        }

        String valueLC = value.toLowerCase();
        if (valueLC.indexOf("://")!=-1) return true;
        if (valueLC.indexOf("<script")!=-1) return true;

        return false;
    }

    private static String fixXssStrictGet(String value)
    {
        if (value == null) return "";

        StringBuilder sb = new StringBuilder(value.length());
        for (int i=0; i<value.length(); i++)
        {
            char ch = value.charAt(i);

            //pozor, rovnaky test je aj v predchadzajucej metode
            if (ch=='\'' || ch=='"' || ch=='<' || ch=='>' || ch=='=') sb.append(' ');
            else sb.append(ch);
        }

        value = sb.toString();

        try
        {
            int start = value.indexOf("://");
            if (start!=-1)
            {
                value = Tools.replace(value, "://", "");
            }

            String valueLC = value.toLowerCase();
            start = valueLC.indexOf("script");
            if (start != -1)
            {
                value = Tools.replaceIgnoreCase(value, "script", "");
            }
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
            return "";
        }

        return value;
    }

    /**
     * @deprecated - use AdminTools.createWebPage
     */
    @Deprecated
    public static int createWebPage(GroupDetails group, Identity user, HttpServletRequest request, String title)
    {
        return AdminTools.createWebPage(group,user,request, title);
    }

    /**
     * Pripravi zoznam editovatelnych stranok pre daneho pouzivatela, maxSize sa pouziva pre specialny pripad obmedzujuci vrateny zoznam
     * @param group_id
     * @param user
     * @param maxSize
     * @return
     */
    public static List<DocDetails> getEditableDocs(int group_id, Identity user, int maxSize)
    {
        List<DocDetails> docs = new ArrayList<>();
        GroupsDB groupsDB = GroupsDB.getInstance();

        Connection db_conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            db_conn = DBPool.getConnectionReadUncommited();

            boolean linkTypeHtml = false;
            if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML)
            {
                linkTypeHtml = true;
            }
            String sql = "SELECT d.*, u.title AS u_title, u.first_name, u.last_name, u.email, u.photo FROM documents d LEFT JOIN  users u ON d.author_id=u.user_id WHERE d.group_id=? ORDER BY d.sort_priority, d.title";
            if (Constants.DB_TYPE == Constants.DB_ORACLE)
            {
                sql = "SELECT d.*, u.title AS u_title, u.first_name, u.last_name, u.email, u.photo FROM documents d,  users u WHERE d.author_id=u.user_id(+) AND d.group_id=? ORDER BY d.sort_priority, d.title";
            }

            ps = db_conn.prepareStatement(sql);
            ps.setInt(1, group_id);

            rs = ps.executeQuery();

            int counter = 0;
            while (rs.next())
            {
                counter++;

                DocDetails doc = DocDB.getDocDetails(rs, false);

                if (linkTypeHtml)
                {
                    doc.setDocLink(DocDB.getURL(doc, groupsDB));
                }
                if (maxSize > 0)
                {
                    doc.setNavbar(groupsDB.getNavbarNoHref(doc.getGroupId()));
                }

                if (Constants.getBoolean("adminCheckUserGroups") && DocDB.canAccess(doc, user, true)==false)
                {
                    //skontroluj, ci ma user pravo na zobrazenie stranky
                    continue;
                }

                docs.add(doc);

                if (maxSize > 0 && counter > maxSize)
                {
                    break;
                }

            }
            rs.close();
            ps.close();
            db_conn.close();
            rs = null;
            ps = null;
            db_conn = null;
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        finally
        {
            try
            {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            }
            catch (Exception ex2)
            {
            }
        }

        return docs;
    }

    private static TextNodeComparator getTextNodeComparator(String htmlCode, Locale locale) throws IOException, SAXException
    {
        HtmlCleaner cleaner = new HtmlCleaner();
        DomTreeBuilder oldHandler = new DomTreeBuilder();
        InputSource oldSource = new InputSource(new StringReader(htmlCode));
        cleaner.cleanAndParse(oldSource, oldHandler);

        TextNodeComparator comparator = new TextNodeComparator(oldHandler, locale);

        return comparator;
    }


    /**
	 * Performs HTML diffing on two HTML strings. Notice that the input strings
	 * are "cleaned-up" first (e.g. all html tags are converted to lowercase).
	 *
	 * @param htmlCodeNew - current HTML code
	 * @param htmlCodeOld - old HTML code to compare with
	 * @return the result
	 * @throws Exception - something went wrong.
	 */
	public static String getHtmlDiff(String htmlCodeNew, String htmlCodeOld) throws Exception {
		StringWriter outStreamWriter = new StringWriter();

        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();

        TransformerHandler result = tf.newTransformerHandler();
        result.setResult(new StreamResult(outStreamWriter));

        XslFilter filter = new XslFilter();

        ContentHandler postProcess = filter.xsl(result, "xslfilter/htmlheader.xsl");

        String prefix = "diff";

        //todo: nejako rozumnejsie
        Locale locale = Locale.getDefault();

        TextNodeComparator leftComparator = getTextNodeComparator(htmlCodeNew, locale);
        TextNodeComparator rightComparator = getTextNodeComparator(htmlCodeOld, locale);

        postProcess.startDocument();
        postProcess.startElement("", "diffreport", "diffreport", new AttributesImpl());

        postProcess.startElement("", "diff", "diff", new AttributesImpl());
        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess, prefix);

        HTMLDiffer differ = new HTMLDiffer(output);
        differ.diff(rightComparator, leftComparator);
        postProcess.endElement("", "diff", "diff");
        postProcess.endElement("", "diffreport", "diffreport");
        postProcess.endDocument();

        String headHtml = htmlCodeNew;
        headHtml = headHtml.replaceAll("<head[\\s\\S]*?>([\\s\\S]*?)</head>[\\s]*?<body([\\s\\S]*?)>[\\s\\S]*?</body>","<head>$1<link href=\"/components/_common/css/daisy-diff.css\" type=\"text/css\" rel=\"stylesheet\"/><script type=\"text/javascript\" src=\"/components/_common/javascript/jquery.tools.tooltip.js\" ></script></head><body$2>");
        headHtml = headHtml.replace("$","\\$");
        if (headHtml.indexOf("<head") !=-1) headHtml = headHtml.substring(headHtml.indexOf("<head"));

        String outputHtml = outStreamWriter.toString();
        outputHtml = outputHtml.replaceAll("<head[\\s\\S]*?>[\\s\\S]*?<body[\\s\\S]*?>", headHtml);

        outputHtml = Tools.replace(outputHtml, "\" changes=\"", "\" title=\"");

        return outputHtml;
	}

}
