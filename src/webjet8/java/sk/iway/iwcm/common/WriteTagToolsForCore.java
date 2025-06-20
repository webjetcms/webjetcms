package sk.iway.iwcm.common;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.stripes.CSRF;

public class WriteTagToolsForCore {
    private static final String CACHE_KEY_FILE_EXISTS = "sk.iway.iwcm.tags.WriteTag.fileExists";

    protected WriteTagToolsForCore() {
        //utility class
    }

    public static StringBuilder fixXhtml(StringBuilder text, HttpServletRequest request)
    {
        StringBuilder replacedText = text;
        //nahrad target="_blank"
        replacedText = Tools.replace(replacedText, "target='_blank'", "onclick=\"return openTargetBlank(this, event)\"");
        replacedText = Tools.replace(replacedText, "target=\"_blank\"", "onclick=\"return openTargetBlank(this, event)\"");
        replacedText = Tools.replace(replacedText, "target=\"__blank\"", "target=\"_blank\"");
        replacedText = Tools.replace(replacedText, "target='__blank'", "target='_blank'");

        //uprav multi domain linky
        if (Constants.getBoolean("multiDomainEnabled")==true)
        {
            replacedText = MultiDomainFilter.fixDomainPaths(replacedText, request);
        }

        return(replacedText);
    }

    /**
     * Pre domeny nastavene v konfiguracnej premennej formmailHttpsDomains zmeni odoslanie /formmail.do na https://domena/formmail.do
     * @param text
     * @param request
     * @return
     */
    public static StringBuilder secureFormmail(StringBuilder text, HttpServletRequest request)
    {
        if (Tools.isEmpty(text)) return text;
        if (text.indexOf("/formmail.do")==-1) return text;

        String httpsDomains = Constants.getString("formmailHttpsDomains");
        StringBuilder replacedText = text;
        //POZOR: takyto kod je aj v JS ochrane formularov
        if (Tools.isNotEmpty(httpsDomains))
        {
            String actualDomain = DocDB.getDomain(request);

            if ( (","+httpsDomains+",").indexOf(","+actualDomain+",")!=-1)
            {
                replacedText = Tools.replace(replacedText, "=\"/formmail.do", "=\"https://"+actualDomain+"/formmail.do");
                replacedText = Tools.replace(replacedText, "='/formmail.do", "='https://"+actualDomain+"/formmail.do");
            }
        }

        return replacedText;
    }

    public static StringBuilder preventSpam(StringBuilder text, HttpServletRequest request)
    {
        if (Constants.getBoolean("spamProtection") == false)
            return (text);

        if (request.getAttribute("spamProtectionDisable")!=null)
            return (text);

        String lng = PageLng.getUserLng(request);

        //vypnutie spam ochrany na urovni sablony
        TemplateDetails temp = (TemplateDetails)request.getAttribute("templateDetails");
        if (temp != null && temp.isDisableSpamProtection()) return text;

        String dmail = request.getHeader("dmail");
        if (dmail!=null && dmail.equals(EmailToolsForCore.ACTUAL_USER_HASH))
        {
            //pre direct mail sa nepozije ochrana
            return text;
        }

        //#11325: pre offline export do HTML sa nepouzije ochrana
        String userAgent = request.getHeader("User-Agent");
        if(userAgent != null && userAgent.indexOf("WebJET Offline") != -1 && "true".equals(Constants.getString("disableSpamProtectionForOffline")))
            return text;

        if (PathFilter.getOrigPath(request).endsWith("spamprotectiondisable.jsp")) return text;

        StringBuilder replacedText = text;
        try
        {
            int failsafe = 0;
            int start = replacedText.indexOf("action=\"/formmail.do?");
            while (failsafe++ <= 5 && start != -1)
            {
                start = start + "action=\"/formmail.do".length();
                int end = replacedText.indexOf("\"", start);
                if (end > start)
                {
                    String url = replacedText.substring(start, end);
                    Logger.debug(WriteTagToolsForCore.class, "preventSpam: url=" + url);
                    if (replacedText.indexOf("useFormDocId") == -1)
                    {
                        if (url.indexOf('?') != -1)
                            url += "&amp;useFormDocId=" + request.getParameter("docid");
                        else
                            url += "?useFormDocId=" + request.getParameter("docid");
                    }
                    // uprav URL
                    int recStart = url.indexOf("recipients=");
                    if (recStart != -1)
                    {
                        int recEnd = url.indexOf("&", recStart+1);
                        String recipients;
                        if (recEnd == -1)
                        {
                            recipients = url.substring(recStart + "recipients=".length());
                        }
                        else
                        {
                            recipients = url.substring(recStart + "recipients=".length(), recEnd);
                        }
                        String newRecipients = encodeEmailAddress(recipients);
                        url = url.replace(recipients, newRecipients);
                        Logger.debug(WriteTagToolsForCore.class, "recipients=" + recipients);
                    }
                    replacedText =new StringBuilder().append(replacedText.substring(0, start)).append(url).append(replacedText.substring(end));
                }
                start = replacedText.indexOf("action=\"/formmail.do?", start + 1);
            }
        }
        catch (RuntimeException e)
        {
            sk.iway.iwcm.Logger.error(e);
        }

        //uprav linky formmail na odoslanie do zabezpecenej zony
        replacedText = secureFormmail(replacedText, request);

        //vypnutie SPAM ochrany pre danu session - nastavovane v /components/form/spamprotectiondisable.jsp
        if ("true".equals(request.getSession().getAttribute("WriteTag.disableSpamProtectionJavascript")))
        {
            try
            {
                //vypis este informaciu o chybe odoslania formularu (ak nejaku mame)
                String qs = (String)request.getAttribute("path_filter_query_string");
                if (qs == null) qs = "";
                Prop prop = Prop.getInstance(lng);
                String writeText = null;
                if (qs.indexOf("formsend=true")!=-1)
                {
                    writeText = prop.getText("checkform.sent");
                }
                if (qs.indexOf("formfail=formIsAllreadySubmitted")!=-1)
                {
                    writeText = prop.getText("checkform.formIsAllreadySubmitted");
                }
                else if (qs.indexOf("formfail=javascript")!=-1)
                {
                    writeText = prop.getText("checkform.fail_javascript");
                }
                else if (qs.indexOf("formfail=probablySpamBot")!=-1)
                {
                    writeText = prop.getText("checkform.fail_probablySpamBot");
                }
                else if (qs.indexOf("formfail=requiredFields")!=-1)
                {
                    writeText = prop.getText("stripes.ajax.errors.header");
                    String errors = (String)request.getSession().getAttribute("formMailValidationErrors");
                    if (Tools.isNotEmpty(errors)) writeText = writeText + errors;
                }
                else if (qs.indexOf("formfail=bad_file")!=-1)
                {
                    writeText = prop.getText("checkform.bad_file");
                }
                else if (qs.indexOf("formfail=captcha")!=-1)
                {
                    writeText = prop.getText("captcha.textNotCorrect");
                }
                else if (qs.indexOf("formfail=")!=-1)
                {
                    writeText = prop.getText("checkform.sendfail");
                }

                if (Tools.isNotEmpty(writeText))
                {
                    int start = replacedText.indexOf("/formmail.do");
                    if (start > 0)
                    {
                        //uprav CSS styly label elementom
                        @SuppressWarnings("unchecked")
                        Map<String, String> formMailValidationErrorsElementNameTable = (Map<String, String>)request.getSession().getAttribute("formMailValidationErrorsElementNameTable");

                        if (formMailValidationErrorsElementNameTable != null)
                        {
                            Iterator<String> keys = formMailValidationErrorsElementNameTable.keySet().iterator();
                            while (keys.hasNext())
                            {
                                String id = keys.next();
                                StringBuffer labelText = new StringBuffer();
                                replacedText = updateLabelClassInvalid(replacedText, id, labelText);

                                if (labelText.length()>0)
                                {
                                    writeText = Tools.replace(writeText, "<li>"+id, "<li>"+labelText.toString());
                                }
                            }
                        }

                        start = replacedText.indexOf("/formmail.do");
                        if (start > 0)
                        {
                            int formStart = replacedText.lastIndexOf("<form", start);
                            if (formStart > 0)
                            {
                                replacedText = new StringBuilder().append(replacedText.substring(0, formStart)).append("<link type='text/css' media='screen' rel='stylesheet' href='/components/form/check_form.css'></link>" + prop.getText("spamprotectiondisable.formmailErrorNotify", writeText)).append(replacedText.substring(formStart));
                            }
                        }

                        request.getSession().removeAttribute("formMailValidationErrors");
                        request.getSession().removeAttribute("formMailValidationErrorsElementNameTable");
                    }
                }
            }
            catch (Exception e)
            {
                sk.iway.iwcm.Logger.error(e);
            }
            return replacedText;
        }

        String spamProtectionJavascript = Constants.getString("spamProtectionJavascript");
        if (spamProtectionJavascript.indexOf("all")!=-1 || spamProtectionJavascript.indexOf("formmail")!=-1)
        {
            Prop prop = Prop.getInstance(lng);
            try
            {
                int failsafe = 0;
                int start = replacedText.indexOf("<form");
                while (failsafe++ <= 5 && start != -1)
                {
                    int end = replacedText.indexOf(">", start);
                    if (end > start)
                    {
                        String formTag = replacedText.substring(start, end + 1);
                        boolean isFormMail = formTag.contains("formmail.do");
                        if (formTag.contains("donotobfuscate")==false)
                        {
                            Logger.debug(WriteTagToolsForCore.class, "preventSpam: formTag=" + formTag);
                            if (spamProtectionJavascript.indexOf("all")!=-1 || formTag.indexOf("formmail.do") != -1)
                            {
                                // uprav tag
                                String name = "wjFrmJSTag";

                                String backUrl = PathFilter.getOrigPath(request);
                                if (ContextFilter.isRunning(request))
                                {
                                    backUrl = ContextFilter.addContextPath(request.getContextPath(), backUrl);
                                    formTag = ContextFilter.addContextPath(request.getContextPath(), formTag);
                                }

                                String qs = (String)request.getAttribute("path_filter_query_string");
                                if (Tools.isNotEmpty(qs)) backUrl += "?" + qs;

                                String newTag = prop.getText("checkform.fail_javascript_formtag", Tools.URLEncode(backUrl));
                                //FAKE csrf ochrana
                                if (spamProtectionJavascript.contains("formtagCsrf")) newTag += CSRF.getCsrfTokenInputFiled(request.getSession(), false);

                                newTag += "<script type=\"text/javascript\">/* <![CDATA[ */";
                                newTag += "var " + name + "=\"\";";
                                formTag = Tools.replace(formTag, "&", "&amp;");
                                formTag = Tools.replace(formTag, "&amp;amp;", "&amp;");
                                // vyiteruj to do premennej
                                String newTag2 = "";
                                int failsafe2 = 0;
                                while (formTag.length() > 4 && failsafe2 < 100)
                                {
                                    failsafe2++;
                                    String substr = formTag.substring(0, 3);
                                    formTag = formTag.substring(3);
                                    substr = Tools.replace(substr, "\"", "\\\"");
                                    newTag2 = name + "=\"" + substr + "\"+" + name + ";" + newTag2; //NOSONAR
                                }
                                if (Tools.isNotEmpty(formTag))
                                {
                                    formTag = Tools.replace(formTag, "\"", "\\\"");
                                    newTag2 = name + "=\"" + formTag + "\"+" + name + ";" + newTag2;
                                }

                                newTag += newTag2;
                                newTag += name + "=\"m>\"+" + name + ";";
                                newTag += name + "=\"<\\/for\"+" + name + ";";
                                newTag += "document.write(" + name + ");/* ]]> */</script><noscript><div class='noprint'>" + prop.getText("checkform.fail_javascript", Tools.URLEncode(backUrl)) + "</div></noscript>";

                                //REALNA csrf ochrana
                                if (spamProtectionJavascript.contains("formmailCsrf") && isFormMail)
                                {
                                    newTag += CSRF.getCsrfTokenInputFiled(request.getSession(), true);
                                    newTag += "<input type=\"hidden\" name=\"__lng\" value=\""+lng+"\"/>";
                                }

                                replacedText = new StringBuilder(replacedText.substring(0, start)).append(newTag).append(replacedText.substring(end + 1));
                                start = start + newTag.length();
                            }
                        }
                    }
                    start = replacedText.indexOf("<form", start + 1);
                }
            }
            catch (RuntimeException e)
            {
                sk.iway.iwcm.Logger.error(e);
            }
        }

        if (spamProtectionJavascript.indexOf("all")!=-1 || spamProtectionJavascript.indexOf("mailto")!=-1)
        {
            int startMailto = replacedText.indexOf("mailto:");
            String inlineJs = "<script src=\"/components/_common/javascript/mail_protection.js\" type=\"text/javascript\"></script>";
            if(startMailto != -1 && request.getAttribute("mailProtection") == null && request.getAttribute("commonPageFunctionsInserted")==null)
            {
                if (request.getAttribute("mailProtection")==null)
                {
                    replacedText = new StringBuilder().append(inlineJs).append(replacedText);
                }
                request.setAttribute("mailProtection", true);
            }
            try
            {
                int failsafe = 0;
                int start;

                //nahrada mailto:email@domena.sk na javascript:decodeEmail("kodovany@email.sk")
                start = replacedText.indexOf("mailto:");
                failsafe = 0;
                while(start != -1 && failsafe++ < 500)
                {
                    while(replacedText.charAt(start) != '='){
                        start--;
                    }
                    start++;

                    int end = start+5;
                    while(replacedText.length()>end && replacedText.charAt(end) != '"' && replacedText.charAt(end) != '\'' && replacedText.charAt(end) != ' ' && replacedText.charAt(end) != '\t'){
                        end++;
                    }
                    end++; //replacedText bereme vratane koncovej uvodzovky

                    String oldFormTag = replacedText.substring(start, end);
                    String newOldFormTag = oldFormTag;
                    if(oldFormTag.indexOf('"') != -1)
                        newOldFormTag = oldFormTag.replace("\"", "");
                    else if(oldFormTag.indexOf('\'') != -1)
                        newOldFormTag = oldFormTag.replace("\'", "");
                    int formTagInd = newOldFormTag.indexOf(':');
                    if(formTagInd != -1 && formTagInd < newOldFormTag.length()-1)
                    {
                        String emailAddress = newOldFormTag.substring(formTagInd+1);
                        String formTag1 = "\"javascript:decodeEmail('";
                        String formTag2 = encodeEmailAddress(emailAddress)+"')\" ";
                        String newFormTag = formTag1+formTag2;

                        replacedText = Tools.replace(replacedText, oldFormTag, newFormTag);
                    }
                    start = replacedText.indexOf("mailto:", end+1);
                }

                //nahrad aj email v samotnom texte, email musi byt obaleny v A elemente (odkaze)
                if (spamProtectionJavascript.contains("emailAuto") && replacedText.indexOf("@")!=-1)
                {
                    String regex = ">(\\s*[a-z0-9]+[a-z0-9\\._+=#$%&-]*[a-z0-9]+@[a-z0-9]+[a-z0-9\\._-]*[a-z0-9]+\\.[a-z]{2,4}\\s*)</a>";
                    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(replacedText);
                    while (matcher.find())
                    {
                        try
                        {
                            if (matcher.groupCount()>=1)
                            {
                                if (request.getAttribute("mailProtection") == null)
                                {
                                    replacedText = new StringBuilder().append(inlineJs).append(replacedText);
                                    request.setAttribute("mailProtection", true);
                                }

                                String emailAddress = matcher.group(1);
                                Logger.debug(WriteTagToolsForCore.class, "replacing email:"+emailAddress);
                                String oldHTML = ">"+emailAddress+"</a>";
                                String newHTML = "><script type='text/javascript'>writeEmailToPage(\""+encodeEmailAddress(emailAddress.trim())+"\");</script></a>";
                                replacedText = Tools.replace(replacedText, oldHTML, newHTML);
                            }
                        }
                        catch (Exception ex)
                        {
                            sk.iway.iwcm.Logger.error(ex);
                        }
                    }
                }

            }
            catch (RuntimeException e)
            {
                sk.iway.iwcm.Logger.error(e);
            }
        }

        return (replacedText);
    }

    /**
     * V HTML kode nastavi elementu LABEL s danym for=id CSS triedu invalidLabel
     * @param html
     * @param id
     * @param labelText - pole pre nastavenie navratovej hodnoty textu labelu (jeho obsahu)
     * @return
     */
    private static StringBuilder updateLabelClassInvalid(StringBuilder html, String id, StringBuffer labelText)
    {
        try
        {
            int start = html.indexOf("for=\""+id+"\"");
            if (start == -1) return html;

            int end = html.indexOf(">", start);
            if (end == -1) return html;

            start = html.lastIndexOf("<", end);

            String labelTag = html.substring(start, end);

            String newLabel = null;
            if (labelTag.indexOf("class")==-1) newLabel = labelTag + " class=\"invalidLabel\"";
            else
            {
                newLabel = Tools.replace(labelTag, "class=\"", "class=\"invalidLabel ");
                newLabel = Tools.replace(newLabel, "class='", "class='invalidLabel ");
            }

            //ziskaj text labelu
            int labelEnd = html.indexOf("</label>", end);
            if (labelEnd > end)
            {
                String labelContentHTML = html.substring(end+1, labelEnd);
                labelText.append(SearchTools.htmlToPlain(labelContentHTML));
            }

            StringBuilder newHtml = new StringBuilder(html.length());
            newHtml.append(html.substring(0, start));
            newHtml.append(newLabel);
            newHtml.append(html.substring(end));

            return newHtml;
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }

        return html;
    }

    public static String encodeEmailAddress(String email)
    {
        //adresa uz bola raz zakodovana
        if (email.indexOf('~')!=-1 && email.indexOf('!')!=-1) return email;

        String returnEmail = email;
        //prekoduj znaky
        returnEmail = returnEmail.replace('@', '~');
        returnEmail = returnEmail.replace('.', '!');

        //cele to pre istotu otoc
        char[] newEmail = new char[returnEmail.length()];
        for (int i=0; i<returnEmail.length(); i++)
        {
            newEmail[i] = returnEmail.charAt(returnEmail.length()-i-1);
        }
        returnEmail = new String(newEmail);

        return(returnEmail);
    }

    /**
     * Vykona nahradenie znaciek !WRITE a !CALL v texte ktory sa vypisuje (napr. az po vykonani nejakeho include)
     * @param text
     * @param request
     * @return
     */
    public static StringBuilder replaceWriteText(StringBuilder text, HttpServletRequest request)
    {
        //	poreplacuj texty s !WRITE(
        StringBuilder replacedText = text;
        try
        {
            int counter = 0;
            String REPLACE_START = "!WRITE(";
            String REPLACE_END = ")!";
            int start = replacedText.indexOf(REPLACE_START);
            int end;
            String name;
            while (start != -1)
            {
                end = replacedText.indexOf(REPLACE_END, start);
                if (end > start)
                {
                    try
                    {
                        name = replacedText.substring(start + REPLACE_START.length(), end);
                        //Logger.println(this,"name="+name);
                        replacedText = Tools.replace(replacedText, REPLACE_START + name + REPLACE_END, getRequestString(request, name));
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
                start = replacedText.indexOf(REPLACE_START, start + 1);
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        try
        {
            int counter = 0;
            String REPLACE_START = "!CALL(";
            String REPLACE_END = ")!";
            int start = replacedText.indexOf(REPLACE_START);
            int end;
            while (start != -1)
            {
                end = replacedText.indexOf(REPLACE_END, start);
                if (end > start)
                {
                    try
                    {
                        String name = replacedText.substring(start + REPLACE_START.length(), end);
                        String callMethod = name;
                        //Logger.println(this,"name="+name);

                        //zavolaj cez reflection pozadovanu metodu
                        int i = callMethod.lastIndexOf('.');
                        String callClass = callMethod.substring(0, i);
                        callMethod = callMethod.substring(i+1);
                        i = callMethod.indexOf('(');
                        String callParameters = null;
                        if (i > 0)
                        {
                            callParameters = callMethod.substring(i+1, callMethod.lastIndexOf(')'));
                            callMethod = callMethod.substring(0, i).trim();
                        }
                        String returnText = null;
                        try
                        {
                            Class<?> c = Class.forName(callClass);
                            Object o = c.getDeclaredConstructor().newInstance();
                            Method m;
                            Class<?>[] parameterTypes = new Class[0]; // {String.class, String.class, HttpServletRequest.class};
                            Object[] arguments = new Object[0]; // {username, password, request};

                            if (Tools.isNotEmpty(callParameters))
                            {
                                StringTokenizer st = new StringTokenizer(callParameters, ",");
                                int pocet = st.countTokens();
                                if (pocet > 0)
                                {
                                    parameterTypes = new Class[pocet];
                                    arguments = new Object[pocet];
                                    String param;
                                    i = 0;
                                    while (st.hasMoreTokens())
                                    {
                                        param = st.nextToken().trim();
                                        Logger.debug(WriteTagToolsForCore.class, "CALL param: " + param);

                                        if ("request".equals(param))
                                        {
                                            parameterTypes[i] = HttpServletRequest.class;
                                            arguments[i] = request;
                                        }
                                        if ("session".equals(param))
                                        {
                                            parameterTypes[i] = HttpSession.class;
                                            arguments[i] = request.getSession();
                                        }
                                        else if (param.startsWith("\"") || param.startsWith("'"))
                                        {
                                            parameterTypes[i] = String.class;
                                            arguments[i] = param.substring(1, param.length()-1);
                                        }
                                        else if (param.indexOf('.')!=-1)
                                        {
                                            parameterTypes[i] = Double.class;
                                            arguments[i] = Double.valueOf(Tools.getDoubleValue(param, 0));
                                        }
                                        else
                                        {
                                            parameterTypes[i] = Integer.class;
                                            arguments[i] = Integer.valueOf(Tools.getIntValue(param, 0));
                                        }

                                        i++;
                                    }
                                }
                            }

                            m = c.getMethod(callMethod, parameterTypes);
                            Object result = m.invoke(o, arguments);
                            returnText = result != null ? result.toString() : "";
                        }
                        catch (Exception ex)
                        {
                            sk.iway.iwcm.Logger.error(ex);
                        }

                        replacedText = Tools.replace(replacedText, REPLACE_START + name + REPLACE_END, returnText);
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
                start = replacedText.indexOf(REPLACE_START, start + 1);
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        //fixni XHTML zalezitosti


        return(replacedText);
    }

    private static String getRequestString(HttpServletRequest request, String name)
    {
        String ret = (String) request.getAttribute(name);
        if (ret == null)
        {
            ret = "";
        }
        return (ret);
    }

    @SuppressWarnings("unchecked")
    public static boolean isFileExists(String fileUrl)
    {
        //cache pre zvysenie vykonu v pluska GlusterFS
        Boolean fileExists;
        Map<String, Boolean> fileExistsTable = null;
        int writeTagFileExistsCacheMinutes = Constants.getInt("writeTagFileExistsCacheMinutes");
        if (writeTagFileExistsCacheMinutes > 0)
        {
            Cache c = Cache.getInstance();
            fileExistsTable = (Map<String, Boolean>)c.getObject(CACHE_KEY_FILE_EXISTS);
            if (fileExistsTable == null)
            {
                fileExistsTable = new Hashtable<>();
                c.setObjectSeconds(CACHE_KEY_FILE_EXISTS, fileExistsTable, writeTagFileExistsCacheMinutes*60, true);
            }
            fileExists = fileExistsTable.get(fileUrl);
            if (fileExists != null) return fileExists.booleanValue();
        }

        //Logger.println(this,"Testing IFN: " + fileUrl);
        if (FileTools.exists(fileUrl)) fileExists = Boolean.TRUE;
        else fileExists = Boolean.FALSE;

        if (fileExistsTable!=null) fileExistsTable.put(fileUrl, fileExists);

        return fileExists.booleanValue();
    }

    private static String checkSetFile(String includeFileName, String newName)
    {
        if (isFileExists(newName))
        {
            return newName;
        }
        return(includeFileName);
    }

    /**
     * Vrati custom cestu ku komponente, alebo null, ak nie je custom cesta
     * @param includeFileName
     * @param request
     * @return
     */
    public static String getCustomPageNull(String includeFileName, HttpServletRequest request)
    {
        String newIncludeFileName = getCustomPage(includeFileName, request);
        if (includeFileName.equals(newIncludeFileName))
        {
            return null;
        }
        return newIncludeFileName;
    }


    /**
     * Pre zadane URL JSP suboru najde jeho custom alternativu
     * @param includeFileName
     * @param request
     * @return
     */
    public static String getCustomPage(String includeFileName, HttpServletRequest request)
    {
        if (includeFileName.lastIndexOf('.')==-1) return includeFileName;
        return getCustomPath(includeFileName, request);
    }

    /**
     * Pre zadanu URL najde jeho custom cestu v installName alebo logInstallName adresari
     */
    @SuppressWarnings("java:S1075")
    public static String getCustomPath(String url, HttpServletRequest request)
    {
        //riesime len /components/ alebo /templates/ adresare
        if (url.startsWith("/components/")==false && url.startsWith("/templates/")==false) return url;

        if ("/components/form/check_form_impl.jsp".equals(url))
        {
            try
            {
                String referer = request.getHeader("Referer");
                if (Tools.isNotEmpty(referer))
                {
                    referer = referer.toLowerCase();
                    //odstran domenu
                    if (referer.startsWith("http")) referer = referer.substring(referer.indexOf("/", 10));

                    Logger.debug(WriteTagToolsForCore.class, "url=" + url + " referer=" + referer);
                    //pre admin cast chcem davat standardny checkform, nie custom verziu
                    if (referer.startsWith("/admin") || referer.startsWith("/components/")) return url;
                }
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }
        }

        String lng = PageLng.getUserLng(request);
        String origUrl = url;

        String ext = "";
        if (url.contains(".")) ext = url.substring(url.lastIndexOf('.'));
        String newUrl = url;

        //prefix bude /components/ alebo /templates/
        String prefix = url.substring(0, url.indexOf("/", 3)+1);

        //ak nezacina na istall name skus najst v installName adresari, pre /templates/ prehladaj vzdy, kedze mame /templates/intranet/intranet/ (meno installName sa zhoduje s menom sablony)
        if ("/templates/".equals(prefix) || origUrl.startsWith(prefix+Constants.getInstallName()+"/")==false)
        {
            //skus prehladavat /components/INSTALL-NAME/news/news.jsp
            String fileName = Tools.replace(origUrl, prefix, prefix+Constants.getInstallName()+"/");

            newUrl = checkSetFile(newUrl, fileName);
            newUrl = checkSetFile(newUrl, Tools.replace(fileName, ext, "-"+lng+ext));
        }

        String logInstallName = Constants.getString("logInstallName");
        //ak nezacina na logInstallName skus najst v logInstallName
        if (Tools.isNotEmpty(logInstallName) && ("/templates/".equals(prefix) || origUrl.startsWith(prefix + logInstallName+"/")==false))
        {
            //toto sa pouziva a intranetoch, kde mame installName intranet ale je customizovany pre nejakeho klienta, takto vieme dohladat specialnu stranku
            String fileName = Tools.replace(origUrl, prefix, prefix+logInstallName+"/");
            newUrl = checkSetFile(newUrl, fileName);

            if ("/templates/".equals(prefix) && fileName.equals(newUrl)==false)
            {
                //nenasla sa zhoda, skusme este zo zaciatku odstranit installName, ked napr. mame /templates/intranet/intranet -> /templates/siov/intranet
                fileName = Tools.replace(origUrl, prefix+Constants.getInstallName()+"/", prefix+logInstallName+"/");
                newUrl = checkSetFile(newUrl, fileName);
            }
        }

        if (request != null)
        {
            String templateInstallName = null;
            TemplateDetails temp = (TemplateDetails)request.getAttribute("templateDetails");
            if (temp != null)
            {
                templateInstallName = temp.getTemplateInstallName();
            } else {
                //set by CombineTag
                templateInstallName = (String)request.getSession().getAttribute("templateInstallName");
            }
            if (Tools.isNotEmpty(templateInstallName) && ("/templates/".equals(prefix) || origUrl.startsWith(prefix+templateInstallName+"/")==false))
            {
                //skus prehladavat /components/ing/news/news.jsp (orig. /components/ing/menu/news.jsp)
                String fileName;
                if (origUrl.startsWith(prefix+templateInstallName+"/")==false) fileName = Tools.replace(origUrl, prefix, prefix+templateInstallName+"/");
                else fileName = origUrl;

                newUrl = checkSetFile(newUrl, fileName);
                newUrl = checkSetFile(newUrl, Tools.replace(fileName, ext, "-"+lng+ext));
            }
        }

        //Logger.println(this,"IFN RESULT: " + newIncludeFileName);
        return(newUrl);
    }

    /**
     * Vrati cestu k custom page pre Admin cast, alebo null, ak neexistuje ziadna
     * @param pageURL
     * @param request
     * @return
     */
    public static String getCustomPageAdmin(String pageURL, HttpServletRequest request)
    {
        //custom page
        String forward = Tools.replace(pageURL, "/admin/", "/admin/spec/"+Constants.getInstallName()+"/");
        if (FileTools.exists(forward))
        {
            return forward;
        }
        forward = Tools.replace(pageURL, "/admin/", "/components/"+Constants.getInstallName()+"/admin/");
        if (FileTools.exists(forward))
        {
            return forward;
        }

        String defaultSkin = Constants.getString("defaultSkin");

        //WebJET 9 skin page
        if ("webjet9".equals(defaultSkin))
        {
            forward = Tools.replace(pageURL, "/admin/", "/admin/skins/"+defaultSkin+"/");
            if (FileTools.exists(forward))
            {
                return forward;
            }
            //ak neexistuje wj9, sprav fallback na wj8
            defaultSkin = "webjet8";
        }

        //WebJET 8 skin page
        forward = Tools.replace(pageURL, "/admin/", "/admin/skins/"+defaultSkin+"/");
        if (FileTools.exists(forward))
        {
            return forward;
        }

        //WebJET 6 skin page fallback
        if ("webjet6".equals(defaultSkin)==false)
        {
            forward = Tools.replace(pageURL, "/admin/", "/admin/skins/webjet6/");
            if (FileTools.exists(forward))
            {
                return forward;
            }
        }

        return null;
    }

}
