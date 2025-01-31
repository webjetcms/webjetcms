package sk.iway.iwcm.doc;

import static sk.iway.iwcm.common.DocTools.testXss;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import net.sourceforge.stripes.controller.StripesConstants;
import net.sourceforge.stripes.util.CryptoUtil;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BlogTools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.components.response_header.rest.ResponseHeaderService;
import sk.iway.iwcm.doc.ninja.Amp;
import sk.iway.iwcm.doc.ninja.Ninja;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;
import sk.iway.iwcm.system.spring.WebjetComponentParserInterface;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.tags.CombineTag;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@WebServlet(name = "ShowDoc2",
            urlPatterns = {"/showdoc.do"}
            )
public class ShowDoc extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String REMAP_STRING_START = "!REMAP_PAGE(";
    public static final String REMAP_STRING_END = ")!";

    /**
     * sem sa uklada hash o aktualnom userovi, kontroluje to ShowDocAction
     * (ak sa v url nachadza id pouzivatela pre jeho prihlasenie)
     */
    public static String ACTUAL_USER_HASH = null;

    /**
     * Skontroluje, ci parametre neobsahuju naznaky XSS, ak ano,
     * vrati redirect na URL bez skodlivych parametrov
     * inak vrati null
     * @param request HttpServletRequest
     * @return String
     */
    public static String getXssRedirectUrl(HttpServletRequest request)
    {
        if (!Constants.getBoolean("xssProtection")) {
            return null;
        }

        //umozni vypnut ochranu ak sa ide cez PreviewAction
        if ("true".equals(request.getAttribute("xssTestDisabled"))) return null;

        //skontroluj vynimky
        String constantName = "xssProtectionStrictGetUrlException";
        if ("post".equalsIgnoreCase(request.getMethod())) constantName = "xssProtectionStrictPostUrlException";
        String path = PathFilter.getOrigPath(request);
        if (DocTools.isXssStrictUrlException(path, constantName)) return null;

        boolean hasXss = false;
        StringBuilder redirectUrl = new StringBuilder();

        Enumeration<String> parameters = request.getParameterNames();
        while (parameters.hasMoreElements())
        {
           String name = parameters.nextElement();
           if (testXss(name) || name.indexOf('"')!=-1 || name.indexOf('\'')!=-1)
           {
               hasXss = true;
               continue;
           }
           String[] values = request.getParameterValues(name);
            for (String s : values) {
                String value = s;

                //robime este tu, aby sa nedalo nastavit do docid, ktore preskakujem
                boolean valueHasXss = testXss(value);
                //podvrhnuty parameter _sourcePage=http://www.google.sk
                if (!valueHasXss && StripesConstants.URL_KEY_SOURCE_PAGE.equals(name) && (Tools.isNotEmpty(value) && CryptoUtil.decrypt(value) != null && !CryptoUtil.decrypt(value).startsWith("/components/maybe")))
                    valueHasXss = true;
                if (valueHasXss) hasXss = true;

                //docid nam do URL netreba
                if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML && "docid".equals(name)) continue;

                //pridaj parameter do URL
                if (redirectUrl.length() != 0) redirectUrl.append('&');
                if (valueHasXss) {
                    //hodnotu parametra vyhodime
                    redirectUrl.append(name).append('=');
                } else {
                    //vyhadzujeme niektore value, napr. password a podobne, musi to byt contains, lebo v regforme to moze byt usr.password a podobne
                    //nechceme, aby to bolo potom vidno v access logoch a podobne
                    if (name.contains("password") || name.contains("email") || name.contains("phone") || name.contains("login")) {
                        value = "";
                    }

                    //pridame celu hodnotu parametra
                    redirectUrl.append(name).append('=').append(Tools.URLEncode(value));
                }
            }
        }
        if (hasXss)
        {
            String redirUrl = PathFilter.getOrigPath(request) +"?"+ redirectUrl;
            return Tools.sanitizeHttpHeaderParam(redirUrl);
        }

        return null;
    }

    /**
     *  aktualizuje kody v texte
     *
     *@param  user - identita pouzivatela
     *@param  text - text
     *@param  currentDocId - aktualne doc id
     *@param  request - aktualny request
     *@param  servletContext - ServletContext
     *@return String
     */
    public static String updateCodes(Identity user, String text, int currentDocId, HttpServletRequest request, ServletContext servletContext)
    {
        StringBuilder sb = new StringBuilder(text);
        return DocTools.updateCodes(user, sb, currentDocId, request, servletContext).toString();
    }

    /**
     * Nastavi do requestu rozne hodnoty z docDetails objektu (okrem data)
     * @param doc DocDetails
     * @param group GroupDetails
     * @param docDB DocDB
     * @param groupsDB GroupsDB
     * @param request HttpServletRequest
     */
    public static void setRequestData(DocDetails doc, GroupDetails group, DocDB docDB, GroupsDB groupsDB, HttpServletRequest request)
    {
        request.setAttribute("docDetails", doc);

        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if(rb != null) {
            rb.setDocId(doc.getDocId());
            rb.setGroupId(doc.getGroupId());
        }

        request.setAttribute("group_id", Integer.toString(doc.getGroupId()));

        request.setAttribute("author_id", Integer.toString(doc.getAuthorId()));
        if (!Constants.getBoolean("docAuthorLazyLoad"))
        {
            request.setAttribute("author_name", doc.getAuthorName());
            request.setAttribute("author_email", doc.getAuthorEmail());
            request.setAttribute("doc_author_name", doc.getAuthorName());
        }

        request.setAttribute("doc_id", doc.getDocId());
        request.setAttribute("doc_title", doc.getTitle());
        request.setAttribute("doc_title_original", doc.getTitle());
        if (Constants.getBoolean("docTitleIncludePath"))
        {
            request.setAttribute("doc_title", DocDB.getTitleWithPath(doc));
        }
        request.setAttribute("doc_navbar", doc.getNavbar());
        request.setAttribute("html_head", doc.getHtmlHead());
        request.setAttribute("html_data", doc.getHtmlData());
        request.setAttribute("perex_data", doc.getPerex());
        request.setAttribute("perex_pre", doc.getPerexPre());
        request.setAttribute("perex_place", doc.getPerexPlace());
        request.setAttribute("perex_image", doc.getPerexImage());

        if (doc.getPublishStartString() != null && doc.getPublishStartString().length() > 1)
        {
            request.setAttribute("doc_publish_start", doc.getPublishStartString());
        }
        if (doc.getPublishStartTimeString() != null && doc.getPublishStartTimeString().length() > 1)
        {
            request.setAttribute("doc_publish_start_time", doc.getPublishStartTimeString());
        }
        if (doc.getPublishEndString() != null && doc.getPublishEndString().length() > 1)
        {
            request.setAttribute("doc_publish_end", doc.getPublishEndString());
        }
        if (doc.getPublishEndTimeString() != null && doc.getPublishEndTimeString().length() > 1)
        {
            request.setAttribute("doc_publish_end_time", doc.getPublishEndTimeString());
        }
        if (doc.getDateCreatedString() != null && doc.getDateCreatedString().length() > 1)
        {
            request.setAttribute("doc_date_created", doc.getDateCreatedString());
        }
        if (doc.getTimeCreatedString() != null && doc.getTimeCreatedString().length() > 1)
        {
            request.setAttribute("doc_time_created", doc.getTimeCreatedString());
        }
        if (doc.getEventDateString() != null && doc.getEventDateString().length() > 1)
        {
            request.setAttribute("doc_event_date", doc.getEventDateString());
        }
        if (doc.getEventTimeString() != null && doc.getEventTimeString().length() > 1)
        {
            request.setAttribute("doc_event_time", doc.getEventTimeString());
        }

        request.setAttribute("doc_temp_id", doc.getTempId());

        request.setAttribute("field_a", doc.getFieldA());
        request.setAttribute("field_b", doc.getFieldB());
        request.setAttribute("field_c", doc.getFieldC());
        request.setAttribute("field_d", doc.getFieldD());
        request.setAttribute("field_e", doc.getFieldE());
        request.setAttribute("field_f", doc.getFieldF());
        request.setAttribute("field_g", doc.getFieldG());
        request.setAttribute("field_h", doc.getFieldH());
        request.setAttribute("field_i", doc.getFieldI());
        request.setAttribute("field_j", doc.getFieldJ());
        request.setAttribute("field_k", doc.getFieldK());
        request.setAttribute("field_l", doc.getFieldL());
        request.setAttribute("forum_count", doc.getForumCount());

        request.setAttribute("field_m", doc.getFieldM());
        request.setAttribute("field_n", doc.getFieldN());
        request.setAttribute("field_o", doc.getFieldO());
        request.setAttribute("field_p", doc.getFieldP());
        request.setAttribute("field_q", doc.getFieldQ());
        request.setAttribute("field_r", doc.getFieldR());
        request.setAttribute("field_s", doc.getFieldS());
        request.setAttribute("field_t", doc.getFieldT());

        //set navbar
        String navbar = doc.getNavbar();
        String navbar2;
        if ("rdf".equalsIgnoreCase(Constants.getString("navbarDefaultType")))
        {
            navbar2 = groupsDB.getNavbarRDF(doc.getGroupId(), doc.getDocId(), request.getSession());
        }
        else if ("schema.org".equalsIgnoreCase(Constants.getString("navbarDefaultType")))
        {
            navbar2 = groupsDB.getNavbarSchema(doc.getGroupId(), doc.getDocId(), request.getSession());
        }
        else
        {
            navbar2 = groupsDB.getNavbar(doc.getGroupId(), doc.getDocId(), request.getSession());
        }
        if (navbar2.length() > 2)
        {
            navbar = navbar2;
            //ak to nie je default doc pre grupu tak sprav linku
            if (doc.getDocId() != group.getDefaultDocId())
            {
                if (doc.getNavbar().length() > 2)
                {
                    if ("rdf".equalsIgnoreCase(Constants.getString("navbarDefaultType")) && navbar.contains("</div>"))
                    {
                        navbar = navbar.substring(0, navbar.length()-6) + " " + Constants.getString("navbarSeparator")+" <span>"+doc.getNavbar()+"</span></div>";
                    }
                    else if ("schema.org".equalsIgnoreCase(Constants.getString("navbarDefaultType")))
                    {
                        int counter = StringUtils.countMatches(navbar, "<li") + 1;
                        String link = docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request);
                        navbar = navbar.substring(0, navbar.length() - 5);
                        navbar = navbar + " <li class=\"is-item\" itemprop=\"itemListElement\" itemscope=\"\" itemtype=\"http://schema.org/ListItem\"><a href=\"" + link + "\" class=\"navbar\" itemprop=\"item\"><span itemprop=\"name\">" + Tools.convertToHtmlTags(doc.getNavbar()) + "</span></a><meta itemprop=\"position\" content=\"" + counter + "\"></li>";
                        navbar += "\n</ol>";
                    }
                    else
                    {
                        navbar = navbar + " " + Constants.getString("navbarSeparator") + " " + Tools.convertToHtmlTags(doc.getNavbar());
                    }
                }
            }
        }
        request.setAttribute("navbar", navbar);
    }

    public static void setRequestData(GroupDetails group, GroupsDB groupsDB, HttpServletRequest request)
    {
        request.setAttribute("pageGroupDetails", group);

        request.setAttribute("group_name", group.getGroupName());
        request.setAttribute("group_navbar", group.getNavbar());
        request.setAttribute("group_htmlhead", group.getHtmlHead());
        request.setAttribute("group_htmlhead_recursive", groupsDB.getHtmlHeadRecursive(group.getGroupId()));
        request.setAttribute("group_field_a", group.getFieldA());
        request.setAttribute("group_field_b", group.getFieldB());
        request.setAttribute("group_field_c", group.getFieldC());
        request.setAttribute("group_field_d", group.getFieldD());
        request.setAttribute("group_lng", group.getLng());
    }

    /**
* Ak zadane CSS obsahuje novy riadok alebo ciarku spravi z neho automaticky Combine
* @param cssStyle String
* @return String
*/
private static String combineCss(String cssStyle)
{
       cssStyle = Tools.replace(cssStyle, "\n", ",");
        cssStyle = Tools.replace(cssStyle, "\r", "");

        if (cssStyle.contains(","))
        {
            return "/components/_common/combine.jsp?t=css&f="+cssStyle+"&v="+CombineTag.getVersion();
        }

        return cssStyle;
}

    public static void setRequestData(TemplateDetails temp, HttpServletRequest request)
	{
		request.setAttribute("templateDetails", temp);

		long tempGroupId = temp.getTemplatesGroupId();
		if (tempGroupId > 0)
        {
            TemplatesGroupBean tgb = TemplatesGroupDB.getInstance().getById(temp.getTemplatesGroupId());
            if (tgb != null)
            {
                request.setAttribute("templatesGroupDetails", tgb);

                if (Tools.isNotEmpty(tgb.getKeyPrefix()))
                {
                    RequestBean.addTextKeyPrefix(tgb.getKeyPrefix(), false);
                }
            }
            request.setAttribute("templates_group_id", tempGroupId);
        }

        RequestBean.addTextKeyPrefix("temp-"+temp.getTempId(), false);

		request.setAttribute("doc_temp_name", temp.getTempName());
		request.setAttribute("template_id", Integer.toString(temp.getTempId()));
		request.setAttribute("template_name", temp.getTempName());
		request.setAttribute("after_body", temp.getAfterBodyData());
		request.setAttribute("base_css_link", combineCss(temp.getBaseCssPath()));
        request.setAttribute("base_css_link_nocombine", temp.getBaseCssPath());
		request.setAttribute("base_css_link_noext", Tools.replace(temp.getBaseCssPath(), ".css", ""));
		if (temp.getCss() != null && temp.getCss().length() > 1)
		{
			request.setAttribute("css_link", combineCss(temp.getCss()));
            request.setAttribute("css_link_nocombine", temp.getCss());
			request.setAttribute("css_link_noext", Tools.replace(temp.getCss(), ".css", ""));
		}

        Ninja.includeNinja(request);
	}

    /**
     *  Aktualizuje kody pouzivatela !LOGGED_USER_XXX! v texte
     *
     *@param  user  Description of the Parameter
     *@param  text  Description of the Parameter
     *@return       Description of the Return Value
     */
    public static String updateUserCodes(Identity user, String text)
    {
        StringBuilder sb = new StringBuilder(text);
        return DocTools.updateUserCodes(user, sb).toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.debug(ShowDoc.class,"ShowDoc SERVLET CALLED - GET");
        execute(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.debug(ShowDoc.class,"ShowDoc SERVLET CALLED - POST");
        execute(request,response);
    }


    /**
     *  Description of the Method
     *
     * @param  request               Description of the Parameter
     * @param  response              Description of the Parameter
     * @exception  IOException       Description of the Exception
     * @exception ServletException   Description of the Exception
     */

    private void execute(
                                 HttpServletRequest request,
                                 HttpServletResponse response)
            throws IOException, ServletException
    {
        int doc_id = 1;
        Prop prop = Prop.getInstance(Constants.getString("defaultLanguage"));


        //update stranok podla casov

        //get session
        HttpSession session = request.getSession();
        if (session == null)
        {
            request.getRequestDispatcher("index").forward(request,response);
            return;
        }

        //over licenciu
        if (session.getAttribute("license_checked") == null)
        {
            if (!InitServlet.verify(request))
            {
                response.sendRedirect("https://www.interway.sk");
                return;
            }
        }
        session.setAttribute("license_checked", "true");

        Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
        if (user == null)
        {
            user = new Identity();
        }

        try
        {
            if (session.getAttribute("setCookie") != null)
            {
                Cookie myCookie = (Cookie) session.getAttribute("setCookie");
                Logger.println(this,"setting cookie: " + myCookie.getName());
                Tools.addCookie(myCookie, response,request);

                session.removeAttribute("setCookie");
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        String doShowdocAction = request.getParameter("doShowdocAction");
        if (isDoShowdocActionAllowed(doShowdocAction))
        {
            //submitujeme sa akoze na /showdoc.do, ale chceme vykonat nejaku akciu
            Logger.println(this,"doShowdocAction: " + doShowdocAction);

            String doShowdocActionCycle = (String)request.getAttribute("doShowdocActionCycle");
            if (doShowdocAction.equals(doShowdocActionCycle))
            {
                Logger.println(this,"doShowdocActionCycle - rekurzia, preskakujem!!!!!");
            }
            else
            {
                //aby sme sa nezacyklili
                request.setAttribute("doShowdocActionCycle", doShowdocAction);

                request.getRequestDispatcher(doShowdocAction).forward(request, response);
                return;
            }
        }

        String dmail = request.getHeader("dmail");

        if (dmail != null || request.getParameter("isDmail")!=null)
        {
            //vypis vsetky headre
            Enumeration<String> e2 = request.getHeaderNames();
            while (e2.hasMoreElements())
            {
                String name = e2.nextElement();
                String value = request.getHeader(name);
                Logger.debug(ShowDoc.class, "headers: "+name+"="+value);
            }
            //porovnaj to s ulozenou hodnotou
            if (dmail!=null && dmail.equals(ACTUAL_USER_HASH) && request.getParameter("userid")!=null)
            {
                request.setAttribute("NO WJTOOLBAR", "true");
                Connection db_conn = null;
                PreparedStatement ps = null;
                ResultSet db_result = null;
                try
                {
                    int userId = Integer.parseInt(request.getParameter("userid"));

                    //ziskaj usera z DB
                    db_conn = DBPool.getConnection(request);
                    ps = db_conn.prepareStatement("SELECT * FROM  users WHERE user_id=?");
                    ps.setInt(1, userId);
                    db_result = ps.executeQuery();
                    if (db_result.next())
                    {
                        UsersDB.fillUserDetails(user, db_result);
                        LogonTools.setUserToSession(session, user);
                        session.setMaxInactiveInterval(30);
                    }
                    else
                    {
                        Logger.error(this,"user neexistuje");
                    }

                    db_result.close();
                    ps.close();
                    db_conn.close();
                    db_result = null;
                    ps = null;
                    db_conn = null;
                }
                catch (Exception ex)
                {
                    Logger.error(ShowDoc.class, ex);
                }
                finally
                {
                    try
                    {
                        if (db_result != null)
                            db_result.close();
                        if (ps != null)
                            ps.close();
                        if (db_conn != null)
                            db_conn.close();
                    }
                    catch (Exception ignored)
                    {
                        //
                    }
                }
            }
        }

        //ochrana pred XSS utokom
        String xssRedirectUrl = getXssRedirectUrl(request);
        if (xssRedirectUrl != null)
        {
            response.sendRedirect(xssRedirectUrl);
            return;
        }

        GroupsDB groupsDB = GroupsDB.getInstance();

        int historyId = -1;
        try
        {
            //origdocid vyluka je kvoli zobrazeniu logon formu
            if (request.getParameter("historyid") != null && request.getParameter("origDocId")==null)
            {
                historyId = Integer.parseInt(request.getParameter("historyid"));
                if (!user.isAdmin() && Constants.getBoolean("docBlockPublicHistory"))
                {
                    //vynimka pre umoznenia preview. Je potrebne mat ale zadefinovane conf. premenne showDocExceptionIPs a showDocExceptionUserId
                    String allowIps = Constants.getString("showDocExceptionIPs"); //povoleny pristup z konkretnych IP, zaciatkoch IP oddelenych ciarkami
                    int showDocUserId = Tools.getIntValue(Constants.getString("showDocExceptionUserId"),-1); //pouzivatel, ktoreho mam prihlasit pre preview
                    boolean haveToLogon = true;
                    if(Tools.isNotEmpty(allowIps) && showDocUserId > 0)
                    {
                        boolean ret = true;
                        if (allowIps.length() > 1)
                        {
                            ret = false;
                            StringTokenizer st = new StringTokenizer(allowIps, ",");
                            String ip;
                            String myIP = Tools.getRemoteIP(request);
                            Logger.debug(ShowDoc.class,"ShowDoc: Checking: " + myIP + " vs " + allowIps);
                            while (st.hasMoreTokens())
                            {
                                ip = st.nextToken();
                                if (myIP.startsWith(ip.trim()))
                                {
                                    ret = true;
                                    break;
                                }
                            }
                        }
                        if(ret)
                        {
                            UserDetails ud = UsersDB.getUser(showDocUserId);
                            if(ud.getUserId() > 0)
                            {
                                user = new Identity(ud);
                                haveToLogon = false;
                            }
                        }

                    }
                    //dovolime to aj inym ludom, aby sa na to mohol pozriet aj klient alebo niekto
                    if(haveToLogon) request.getRequestDispatcher("logon_admin").forward(request,response);
                    return;
                }
            }
        }
        catch (Exception ignored)
        {
            //
        }

        try
        {
            if (historyId < 0)
            {
                //skus ziskat default doc_id ku grupe
                if (request.getParameter("groupid") != null)
                {
                    try
                    {
                        int group_id = Integer.parseInt(request.getParameter("groupid"));
                        GroupDetails group = groupsDB.getGroup(group_id);
                        doc_id = group.getDefaultDocId();
                    }
                    catch (Exception ignored)
                    {
                        //
                    }
                }
                //ak stale nemam doc_id ziskaj ho
                //0 je vtedy ked v DB v tabulke groups je default doc null
                if (doc_id == 1 || doc_id == 0)
                {
                    if (request.getParameter("docid") != null)
                    {
                        String docidParam = request.getParameter("docid");
                        int indexOfSharp = docidParam.indexOf('#');
                        if (indexOfSharp!=-1)
                        {
                            docidParam = docidParam.substring(0, indexOfSharp);
                        }
                        doc_id = Integer.parseInt(docidParam);
                    }
                    else
                    {
                        doc_id = Integer.parseInt((String) request.getAttribute("docid"));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            StatDB.addError(request.getRequestURI()+"?"+request.getQueryString(), prop.getText("admin.showdoc_error_message")+" 1");
            request.setAttribute("err_msg", prop.getText("admin.showdoc_default_error_message"));
            request.getRequestDispatcher("error").forward(request,response);
            return;
        }

        //TODO: check rights?

        DocDB docDB = DocDB.getInstance();
        TemplatesDB tempDB = TemplatesDB.getInstance();
        TemplateDetails temp;

        DocDetails doc = null;
        boolean inlineEditorAdmin = user != null && user.isAdmin() && "true".equals(request.getParameter("inlineEditorAdmin"));

        int group_id = -1;
        try
        {
            if (session.getAttribute(Constants.SESSION_GROUP_ID) != null)
            {
                group_id = Integer.parseInt((String) session.getAttribute(Constants.SESSION_GROUP_ID));
            }
        }
        catch (Exception ex)
        {
            if (doc_id == -1)
            {
                doc_id = -2;
            }
        }

        ShowDocBean showDocBean = new ShowDocBean();
        showDocBean.setRequest(request);
        showDocBean.setResponse(response);
        showDocBean.setDoc(null);
        showDocBean.setDocId(doc_id);
        showDocBean.setHistoryId(historyId);
        WebjetEvent<ShowDocBean> showdocEvent = new WebjetEvent<>(showDocBean, WebjetEventType.ON_START);
		showdocEvent.publishEvent();

        if (request.getAttribute("ShowdocAction.showDocData")!=null)
        {
            doc = (DocDetails)request.getAttribute("ShowdocAction.showDocData");
            if (doc != null) Logger.println(this,"----> IDEM Z PREVIEW, docid="+doc.getDocId()+" path="+doc.getVirtualPath());
        }
        else if (showDocBean.getForceShowDoc()!=null)
        {
            //vystupny doc objekt z eventu, ak sa nastavi, pouzije sa ako doc objekt pre zobrazenie web stranky cez /showdoc.do
            doc = showDocBean.getForceShowDoc();
        }
        else
        {
            if (doc_id < 0)
            {
                //ak je to admin
                if (user.isAdmin() && doc_id < -1)
                {
                    try
                    {
                        doc = docDB.getDoc(-doc_id);
                        doc_id = -1;
                    }
                    catch (Exception ignored)
                    {
                        //ignore
                    }
                }

                if (doc == null)
                {
                    doc = new DocDetails();
                    doc.setDocId(-1);
                    doc.setGroupId(group_id);
                    doc.setData("<html><body></body></html>");
                    doc.setTitle("novy dokument");

                    doc.setNavbar("");
                }
            }
            else
            {
                if (historyId > 0)
                {
                    doc = docDB.getDoc(-1, historyId);
                }
                else
                {
                    try
                    {
                        boolean useCache = true;
                        if (inlineEditorAdmin) useCache = false;
                        doc = docDB.getDoc(doc_id, -1, useCache);
                    }
                    catch (NullPointerException npe)
                    {
                        //nastala chyba pripojenia do DB - vypisme chybu
                        SetCharacterEncodingFilter.printDbErrorMessage(PathFilter.getOrigPath(request), request, response);

                        return;
                    }
                }
            }
        }

        if (doc == null)
        {
            Logger.debug(ShowDoc.class, "404 (doc is null)");
            request.getRequestDispatcher("/404.jsp").forward(request, response);
            return;
        }

        //	premapovanie web stranky na iny dokument
        int remapStart = doc.getData().indexOf(REMAP_STRING_START);
        if (remapStart!=-1)
        {
            try
            {
                int remapEnd = doc.getData().indexOf(REMAP_STRING_END);
                remapStart += REMAP_STRING_START.length();
                int remapDocId = Tools.getIntValue(doc.getData().substring(remapStart, remapEnd), -1);
                Logger.println(ShowDoc.class, "remap doc id="+remapDocId);
                if (remapDocId > 0)
                {
                    DocDetails remapDoc = docDB.getDoc(remapDocId);
                    if (remapDoc != null)
                    {
                        Logger.println(ShowDoc.class, "RempaDocTitle: " + remapDoc.getTitle());

                        request.setAttribute("remapDocOriginal", doc);

                        remapDoc.setTitle(doc.getTitle());
                        remapDoc.setNavbar(doc.getNavbar());
                        remapDoc.setGroupId(doc.getGroupId());
                        remapDoc.setTempId(doc.getTempId());

                        doc = remapDoc;
                    }
                    else
                    {
                        Logger.debug(ShowDoc.class, "404 (remapdoc is null)");
                        request.getRequestDispatcher("/404.jsp").forward(request, response);
                        return;
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.error(ShowDoc.class, ex);
            }
        }

        if (!user.isAdmin() && !doc.isAvailable() && historyId < 0 && !BlogTools.isEditable(doc.getGroupId(), user))
        {
            Logger.debug(ShowDoc.class, "404 (doc is not available)");
            request.getRequestDispatcher("/404.jsp").forward(request, response);
            return;
        }

        //ak nie je pouzivatel admin a showOnlyActualPublishedDoc=true, tak zobrazi len aktualne platny clanok vid. docDB.canBeShown(doc)
        if (!user.isAdmin() && Constants.getBoolean("showOnlyActualPublishedDoc") && !docDB.canBeShown(doc))
        {
            String canBeShownForUserAgent = Constants.getString("canBeShownForUserAgent");
            boolean found = false;
            String ua = request.getHeader("User-Agent");
            if (Tools.isNotEmpty(canBeShownForUserAgent) && ua != null)
            {
                String[] canBeShownForUserAgentTokens = Tools.getTokens(canBeShownForUserAgent, ",|", true);
                for(String t : canBeShownForUserAgentTokens)
                {
                    if(ua.contains(t))
                    {
                        found = true;
                        break;
                    }
                }
            }

            if(!found)
            {
                Logger.debug(ShowDoc.class, "404 (doc is not available)");
                request.getRequestDispatcher("/404.jsp").forward(request, response);
                return;
            }
        }

        //httpS presmerovanie
        if (Constants.getBoolean("httpsAvailable") && "GET".equalsIgnoreCase(request.getMethod()))
        {
            if (doc.isRequireSsl() && !Tools.isSecure(request))
            {
                //presmerovanie NON SSL na SSL
                String baseHref = Tools.getBaseHref(request);
                if (baseHref.startsWith("http:")) baseHref = "https:"+baseHref.substring(5);

                StringBuilder sb = new StringBuilder(baseHref);
                sb.append(PathFilter.getOrigPath(request));
                String qs = (String)request.getAttribute("path_filter_query_string");
                if (Tools.isNotEmpty(qs))
                {
                    sb.append('?').append(qs);
                }

                Logger.debug(ShowDoc.class, "Redirecting to SSL:"+ sb);

                response.sendRedirect(sb.toString());
                return;
            }
            if (!doc.isRequireSsl() && Tools.isSecure(request) && Tools.isNotEmpty(Constants.getString("httpsRedirectToNonSSLNodeNames")) && ("*".equals(Constants.getString("httpsRedirectToNonSSLNodeNames")) || Constants.getString("httpsRedirectToNonSSLNodeNames").contains(Constants.getString("clusterMyNodeName"))))
            {
                //presmerovanie SSL na NON SSL
                String baseHref = Tools.getBaseHref(request);
                if (baseHref.startsWith("https:")) baseHref = "http:"+baseHref.substring(6);

                StringBuilder sb = new StringBuilder(baseHref);
                sb.append(PathFilter.getOrigPath(request));
                String qs = (String)request.getAttribute("path_filter_query_string");
                if (Tools.isNotEmpty(qs))
                {
                    sb.append('?').append(qs);
                }

                Logger.debug(ShowDoc.class, "Redirecting to NON SSL:"+ sb);

                response.sendRedirect(sb.toString());
                return;
            }
        }

        //kontrola pristupovych prav

        //najskor zisti ako je na tom adresar
        GroupDetails group = groupsDB.getGroup(doc.getGroupId());
        //akoze posledna zachrana
        if (group == null) group = new GroupDetails();

        if (!Constants.getBoolean("webpagesAvailableInInternalFolders") && !user.isAdmin() && group.isInternal())
        {
            //ak je stranka v internom adresari nezobraz ju
            Logger.println(ShowDoc.class, "404 (doc is not available) - is in internal folder");
            Adminlog.add(Adminlog.TYPE_XSS, "Page is in internal folder, is not available for public: "+doc.getDocId(), doc.getDocId(), doc.getTempId());
            request.getRequestDispatcher("/404.jsp").forward(request, response);
            return;
        }

        setRequestData(group, groupsDB, request);

        if (Tools.isNotEmpty(group.getPasswordProtected()) && Tools.isEmpty(doc.getPasswordProtected()))
        {
            doc.setPasswordProtected(group.getPasswordProtected());
        }

        //zaheslovanie vsetkeho (bez potreby nastavovania v admin casti)
        String passwordProtectedAutoId = Constants.getString("passwordProtectedAutoId");
        if (Tools.isNotEmpty(passwordProtectedAutoId))
        {
            if (Tools.isEmpty(doc.getPasswordProtected()))
            {
                doc.setPasswordProtected(passwordProtectedAutoId);
            }
            // else odstraneny
            //tie co su zaheslovane nenastavujeme, inak sa tam vzdy dostane (lebo staci jedna vyhovujuca skupina)!!!
            //doc.setPasswordProtected(passwordProtectedAutoId+","+doc.getPasswordProtected());
        }

        request.setAttribute("groupParentIds", "," + groupsDB.getParents(group.getGroupId()));

        if (doc.getPasswordProtected() != null)
        {
            //zisti ci dany user ma pravo na skupinu pre web stranku
            boolean canAccess = DocDB.canAccess(doc, user);

            //	rekurzivne dozadu najdi LogonPageDocId
            int logonPageDocId = groupsDB.getRecursiveLogonPageDocId(group.getGroupId());
            if (logonPageDocId > 0 && doc.getLogonPageDocId() < 1)
            {
                doc.setLogonPageDocId(logonPageDocId);
            }

            if (doc.getLogonPageDocId() > 0 && doc.getLogonPageDocId() == doc_id)
            {
                Logger.debug(ShowDoc.class, "som logon stranka, zobrazujem");
                //je to rekurzia, stranka pre prihlasenie je tiez zaheslovana,
                //takze ju musime zobrazit !!!!
                canAccess = true;
            }

            if (!canAccess && Tools.isNotEmpty(AuthenticationFilter.getForbiddenURL()))
            {
                StringBuilder fullPath = new StringBuilder((String)request.getAttribute("path_filter_orig_path"));
                String qs = (String)request.getAttribute("path_filter_query_string");
                if (Tools.isNotEmpty(qs)) fullPath.append('?').append(qs);


                Logger.debug(ShowDoc.class, "testujem forbidden URL: "+ AuthenticationFilter.getForbiddenURL()+" fullPath="+fullPath);
                if (AuthenticationFilter.getForbiddenURL().equals(fullPath.toString()))
                {
                    canAccess = true;
                }
            }

            if (!canAccess)
            {
                Logger.debug(ShowDoc.class, "canAccess == false logonDocid="+doc.getLogonPageDocId()+" docId="+doc.getDocId());
                UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
                request.setAttribute("password_protected", userGroupsDB.convertIdsToNames(doc.getPasswordProtected()));
                request.setAttribute("password_protected_ids", doc.getPasswordProtected());

                String domainController = Constants.getString("NTLMDomainController");
                if (Tools.isNotEmpty(domainController))
                {
                    if (Tools.isNotEmpty(AuthenticationFilter.getForbiddenURL()))
                    {
                        //uz je prihlaseny, dame forbidden
                        StringBuilder fullPath = new StringBuilder((String)request.getAttribute("path_filter_orig_path"));
                        String qs = (String)request.getAttribute("path_filter_query_string");
                        if (Tools.isNotEmpty(qs)) fullPath.append('?').append(qs);

                        response.sendRedirect(Tools.addParameterToUrlNoAmp(AuthenticationFilter.getForbiddenURL(), "origUrl", fullPath.toString()));
                        return;
                    }
                    Logger.debug(ShowDoc.class, "spustam NTLM LOGON ACTION");
                    response.sendRedirect("/ntlm/logon.do?origDocId="+doc_id);
                    return;
                }

                if (doc.getLogonPageDocId() > 0)
                {
                    //forwardni sa na stranku s prihlasovacim dialogom
                    request.setAttribute("docDetailsOriginal", doc);
                    request.removeAttribute("docid");
                    request.getRequestDispatcher("/showdoc.do?docid="+doc.getLogonPageDocId()+"&origDocId="+doc.getDocId()).forward(request, response);
                    return;
                }
                else
                {
                    //	toto vytvori pseudo prihlasovaciu stranku
                    request.setAttribute("docDetailsOriginal", doc);
                    DocDetails origDoc = doc;
                    doc = new DocDetails();

                    //kopirovanie properties - je to tu rucne aby sa neskopirovalo nieco co sa nema (inak by sa dalo pouzit BeanUtils)
                    doc.setAvailable(origDoc.isAvailable());
                    doc.setTitle(origDoc.getTitle());
                    doc.setNavbar(origDoc.getNavbar());

                    doc.setTempId(origDoc.getTempId());
                    doc.setHeaderDocId(origDoc.getHeaderDocId());
                    doc.setFooterDocId(origDoc.getFooterDocId());
                    doc.setMenuDocId(origDoc.getMenuDocId());
                    doc.setRightMenuDocId(origDoc.getRightMenuDocId());

                    doc.setData("!INCLUDE(/components/user/logon.jsp)!");
                }
            }
            //ak sme az tu, dokument je pre usera pristupny
        }

        //tento parameter tam nastavuje login dialog pre priame stiahnutie suboru
        //vtedy neupdatujem last_doc_id, pretoze sa prepise na hodnotu login dialogu
        //pozri PathFilter.java
        if (request.getParameter("dontUpdateLastDocId")==null)
        {
            session.setAttribute("last_doc_id", doc_id);
        }

        //NOVA STATISTIKA
        try
        {
            if (!Constants.getBoolean("nginxProxyMode"))
            {
                //statistika to potrebuje
                request.setAttribute("group_id", Integer.toString(doc.getGroupId()));
                StatDB.add(session, request, response, doc_id);
            }
        }
        catch (Exception ex)
        {
            Logger.error(ShowDoc.class, ex);
        }

        if (Constants.getBoolean("nginxProxyMode"))
        {
            //setujeme iba ked sa negeneruje nocache cookie
            if (!PathFilter.isNoCacheCookieRequired(request)) {
                //v rezime nginx proxy vygenerujeme cache hlavicku
                int cacheTime = Constants.getInt("nginxProxyModePageCacheTime");
                if (cacheTime>0 && "GET".equalsIgnoreCase(request.getMethod()) && request.getAttribute("is404")==null)
                {
                    PathFilter.setCacheHeaders(cacheTime, response);
                }
            }
        }

        boolean skipExternalLink = false;
        if (inlineEditorAdmin) {
            //ak editujeme stranku v inline nevykonaj presmerovanie
            skipExternalLink = true;
        }

        //ak to nie je urcene pre editor a mame nastavene presmerovanie na externu linku
        if (skipExternalLink==false && request.getParameter("approveNoRedirect")==null && request.getParameter("notemp") == null && Tools.isNotEmpty(doc.getExternalLink()))
        {
            String externalLink = doc.getExternalLink();
            String qs = (String)request.getAttribute("path_filter_query_string");
            if (Tools.isNotEmpty(qs) && externalLink.indexOf('?')==-1 && !qs.contains("docid")) externalLink += "?"+qs;

            if (externalLink.startsWith("http://") || externalLink.startsWith("https://"))
            {
                response.sendRedirect(externalLink);
                return ;
            }
            else
            {
                response.setStatus(301);
                externalLink = Tools.getBaseHref(request)+externalLink;
                response.setHeader("Location", externalLink);
                return ;
            }
        }

        //ak mame nejake proxy data a stranka je prazdna, tak nastavme tie proxy data
        String proxyOutputData = (String)request.getAttribute("proxyOutputData");
        if (proxyOutputData!=null && (Tools.isEmpty(doc.getData()) || doc.getData().length()<20))
        {
            doc.setData(proxyOutputData);
        }

        if ("open".equals(request.getParameter("forum")) && request.getAttribute("docDetailsOriginal")==null)
        {
            String type = request.getParameter("type");
            if ("iframe".equals(type))
            {
                //request.setAttribute("doc_data", doc.getData());
                StringBuilder iframe = new StringBuilder("<iframe class='forumIframe' width='100%' height='190' ");
                iframe.append("src='/showdoc.do?forumiframe=yes&docid=").append(doc.getDocId()).append("'></iframe>");

                try
                {
                    int start = doc.getData().indexOf("!INCLUDE(/components/forum/forum.jsp");
                    if (start != -1)
                    {
                        int end = doc.getData().indexOf(")!", start);
                        if (end > start)
                        {
                            iframe.append(doc.getData(), start, end+2);
                        }
                    }
                }
                catch (Exception ex)
                {
                    Logger.error(ShowDoc.class, ex);
                }


                request.setAttribute("doc_data", iframe.toString());
            }
            else if ("perex".equals(type))
            {

                String perex = doc.getPerex();
                removeForumFromPerex(request, doc, perex);
            }
            else if ("none".equals(type))
            {
                String data = "";
                removeForumFromPerex(request, doc, data);
            }
            else
            {
                request.setAttribute("doc_data", doc.getData());
            }
        }
        else
        {
            request.setAttribute("doc_data", doc.getData());
        }

        if ("1".equals(request.getParameter("blogEdit")) && user.getUserId() > 0)
        {
            boolean canEdit = BlogTools.isEditable(group.getGroupId(), user);
            if (canEdit)
            {
                //request.setAttribute("doc_data", "!INCLUDE(/components/blog/blog_user_toolbar.jsp)!");
                request.setAttribute("doc_data_blog", doc.getData());
            }
        }

        setRequestData(doc, group, docDB, groupsDB, request);

        if (Tools.isNotEmpty(doc.getVirtualPath()) && doc.getVirtualPath().contains("404.html"))
        {
            request.setAttribute("404.allready.generated", "true");
            response.setStatus(404);
        }

        if (group.isForceTheUseOfGroupTemplate() || (inlineEditorAdmin && "true".equals(request.getParameter("inlineEditingNewPage")))) {
            doc.setTempId(group.getTempId());
        }
        temp = tempDB.getTemplate(doc.getTempId());

        if (temp == null)
        {
            StatDB.addError(request.getRequestURI()+"?"+request.getQueryString(), prop.getText("admin.showdoc_error_message.template_error"));
            request.setAttribute("err_msg", prop.getText("admin.showdoc_default_error_message"));
            //request.setAttribute("err_msg", "Požadovaný dokument neexistuje - template error");
            Adminlog.add(Adminlog.TYPE_RUNTIME_ERROR, "Missing template for page: "+doc.getDocId()+", required template id: "+doc.getTempId(), doc.getDocId(), doc.getTempId());
            request.getRequestDispatcher("/404.jsp").forward(request, response);
            return;
        }

        TemplateDetails tempBrowserDetector;
        BrowserDetector bd = BrowserDetector.getInstance(request);
        //skus najst sablonu podla BrowserDetector
        tempBrowserDetector = tempDB.getTemplate(temp, bd);
        if (tempBrowserDetector!=null)
        {
            //ak existuje sablona napr. Homepage-phone, pouzi ju
            temp = tempBrowserDetector;
        }

        doc.setTempName(temp.getTempName());

        //nastavujem ContentLanguage podla sablony stranky
        String lng = Tools.isNotEmpty(group.getLng()) ? group.getLng() : temp.getLng();
        String lngParam = Tools.getParameter(request, "language");
        if (lngParam != null)
        {
            if (lngParam.length()==2 || lngParam.length()==3) lng = lngParam;
        }

        PageLng.setUserLng(request, response, lng);
        //force:false to allow owerwrite content-language by ResponseHeaders app
        ResponseHeaderService.setContentLanguageHeader(PageLng.getUserLngIso(lng), false, request, response);

        setRequestData(temp, request);

        Logger.debug(this,"normal temp="+temp.getTempId()+" "+temp.getTempName());

        if (temp.getTempName().startsWith("nochange"))
        {
            try
            {
                Integer iTempId = (Integer)session.getAttribute("last_temp_id");
                if (iTempId!=null)
                {
                    TemplateDetails temp2 = tempDB.getTemplate(iTempId);
                    if (temp2!=null)
                    {
                        temp = temp2;

                        if (temp.getTempName().startsWith("mainpage"))
                        {
                            String name = temp.getTempName().substring(temp.getTempName().indexOf(' '));
                            temp2 = tempDB.getTemplate(name);
                            temp = temp2;
                        }

                    }
                }
            }
            catch (Exception ignored)
            {
                //
            }
        }
        else
        {
            //popup okno si nepamatame...
            if (!temp.getTempName().startsWith("popup"))
            {
                session.setAttribute("last_temp_id", temp.getTempId());
            }
        }

        //Logger.println(this,"session temp="+temp.getTempId()+" "+temp.getTempName());
        List<DocDetails> localDocsInGroup = null;

        if(Constants.getBoolean("templatesUseRecursiveSystemFolder") && Constants.getBoolean("templatesUseDomainLocalSystemFolder"))
            Logger.println(this,"POZOR: templatesUseDomainLocalSystemFolder=true je ignorovane, vypni templatesUseDomainLocalSystemFolder");

        if (Constants.getBoolean("templatesUseDomainLocalSystemFolder"))
        {
            GroupDetails localSystemGroup = GroupsDB.getInstance().getLocalSystemGroup();
            if (localSystemGroup != null)
            {
                //najdi rovnaky v aktualnom local priecinku
                localDocsInGroup = WebpagesService.getBasicDocDetailsByGroupRecursive(localSystemGroup.getGroupId(), false);
            }
        }
        else if(Constants.getBoolean("templatesUseRecursiveSystemFolder"))
        {
            GroupDetails systemFolder = GroupsDB.getInstance().getSystemGroupRecursive(doc.getGroupId());
            Logger.debug(ShowDoc.class, "recursiveSystemFolder="+(systemFolder != null ? systemFolder.getGroupId() : "null"));
            if (systemFolder != null)
            {
                localDocsInGroup = WebpagesService.getBasicDocDetailsByGroupRecursive(systemFolder.getGroupId(), false);
                Logger.debug(ShowDoc.class, "localDocsInGroup="+(localDocsInGroup != null ? localDocsInGroup.size() : "null"));
            }
        }

        if (localDocsInGroup == null && Tools.isNotEmpty(group.getLng())) {
            //aj pre nelokalny system skus nacitat zoznam stranok v system/header a system/footer
            localDocsInGroup = WebpagesService.getBasicDocDetailsByGroupRecursive(Constants.getInt("headerFooterGroupId"), false);
            localDocsInGroup.addAll(WebpagesService.getBasicDocDetailsByGroupRecursive(Constants.getInt("menuGroupId"), false));
        }

        setRequestDocData("doc_header", temp.getHeaderDocId(), temp.getHeaderDocData(), localDocsInGroup, request);

        setRequestDocData("doc_footer", temp.getFooterDocId(), temp.getFooterDocData(), localDocsInGroup, request);
        setRequestDocData("doc_menu", temp.getMenuDocId(), temp.getMenuDocData(), localDocsInGroup, request);
        setRequestDocData("doc_right_menu", temp.getRightMenuDocId(), temp.getRightMenuDocData(), localDocsInGroup, request);
        setRequestDocData("template_object_a", temp.getObjectADocId(), temp.getObjectADocData(), localDocsInGroup, request);
        setRequestDocData("template_object_b", temp.getObjectBDocId(), temp.getObjectBDocData(), localDocsInGroup, request);
        setRequestDocData("template_object_c", temp.getObjectCDocId(), temp.getObjectCDocData(), localDocsInGroup, request);
        setRequestDocData("template_object_d", temp.getObjectDDocId(), temp.getObjectDDocData(), localDocsInGroup, request);


        if (doc.getHeaderDocId() > 0)
        {
            DocDetails dd_header = docDB.getDoc(doc.getHeaderDocId());
            if (dd_header != null)
            {
                request.setAttribute("doc_header", dd_header.getData());
            }
        } else if (doc.getHeaderDocId()==-2) {
            request.setAttribute("doc_header", "");
        }

        if (doc.getFooterDocId() > 0)
        {
            DocDetails dd_footer = docDB.getDoc(doc.getFooterDocId());
            if (dd_footer != null)
            {
                request.setAttribute("doc_footer", dd_footer.getData());
            }
        } else if (doc.getFooterDocId()==-2) {
            request.setAttribute("doc_footer", "");
        }

        if (doc.getMenuDocId() > 0)
        {
            DocDetails dd_menu = docDB.getDoc(doc.getMenuDocId());
            if (dd_menu != null)
            {
                request.setAttribute("doc_menu", dd_menu.getData());
            }
        } else if (doc.getMenuDocId()==-2) {
            request.setAttribute("doc_menu", "");
        }

        if (doc.getRightMenuDocId() > 0)
        {
            DocDetails dd_menu = docDB.getDoc(doc.getRightMenuDocId());
            if (dd_menu != null)
            {
                request.setAttribute("doc_right_menu", dd_menu.getData());
            }
        } else if (doc.getRightMenuDocId()==-2) {
            request.setAttribute("doc_right_menu", "");
        }

        //firni event
        showDocBean.setDoc(doc);
        showdocEvent = new WebjetEvent<>(showDocBean, WebjetEventType.ON_END);
		showdocEvent.publishEvent();

        //uprav headre a footre
        if (request.getParameter("notemp") == null)
        {
            updateCodes(request, null, doc_id);
        }

        if (!doc.isSearchable())
        {
            PathFilter.setXRobotsTagValue("NOT_SEARCHABLE_PAGE", response);
        }

        String forward = temp.getForward();
        boolean allowAdminForward = false;

        if (request.getParameter("forumiframe") != null)
        {
            forward = "/components/forum/iframe.jsp";
            allowAdminForward = true;
        }
        if (request.getParameter("forwarddoccompare") != null)
        {
            forward = "/admin/tmp_compare_blank.jsp";
            allowAdminForward = true;
        }

        if (request.getParameter("forward") != null && request.getParameter("forward").endsWith(".jsp"))
        {
            forward = request.getParameter("forward");
        }

        if (Constants.getBoolean("springEnableShowdoc"))
        {
            RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (requestBean != null) {
                WebjetComponentParserInterface webjetComponentParser = requestBean.getSpringBean("webjetComponentParser", WebjetComponentParserInterface.class);
                if (webjetComponentParser != null) {
                    webjetComponentParser.run(request, response);
                    if (webjetComponentParser.isRedirected(response)) {
                        return;
                    }
                }
            }
        }

        BrowserDetector browser = BrowserDetector.getInstance(request);
        if (browser.isAmp()) {
            Amp amp = new Amp(request);
            amp.replaceInRequest();
        }

        if (inlineEditorAdmin) fixDataForInlineEditingAdmin(request, prop);

        //forward na JSP s designom
        if (forward.toLowerCase().endsWith(".jsp") || forward.toLowerCase().endsWith(".html"))
        {
            if (allowAdminForward == false) {
                //otestuj na nepovolene znaky
                if (FileBrowserTools.hasForbiddenSymbol(forward) )
                {
                    forward = "tmp_generic.jsp";
                }

                request.setAttribute("template_forward", forward);
                if (!temp.getForward().startsWith("/"))
                {
                    //skontroluj, ci sablona skutocne existuje

                forward = getForward(request, prop, temp, tempBrowserDetector, bd, forward);
                if (forward == null) return;

                    Logger.debug(this,"forward="+forward);
                }

                if (forward.startsWith("/templates")==false) {
                    forward = "/templates/"+forward;
                }
            }

            IwcmFile forwardFile = getTemplateFile(forward);
            if (!forwardFile.isFile())
            {
                Adminlog.add(Adminlog.TYPE_RUNTIME_ERROR, "Missing template for page: "+doc.getDocId()+", required template file: "+forward, doc.getDocId(), doc.getTempId());
                forward = "/404.jsp";
            }

            if (forward.toLowerCase().endsWith(".html")) {
                request.setAttribute("thymeleafTemplateFile", forwardFile.getVirtualPath());
                forward = "/thymeleaf/showdoc";
            }

            request.getRequestDispatcher(forward).forward(request, response);
        }
    }

    /**
     * Modifikuje existujuce request objekty pre inline editaciu v administracii
     * @param request
     */
    private void fixDataForInlineEditingAdmin(HttpServletRequest request, Prop prop) {
        if ("true".equals(request.getParameter("inlineEditingNewPage"))) {
            //jedna sa o novu stranku, ale fejkujeme to hlavnou strankou adresara
            //zmazeme povodne doc_data
            request.setAttribute("doc_data", "");
            request.setAttribute("title", prop.getText("editor.newDocumentName"));
        }
        //toto nepotrebujeme posielat, injectuje sa to v inline_page_toolbar.jsp
        request.setAttribute("doc_data", "");
        request.setAttribute("doc_header", "");
        request.setAttribute("doc_footer", "");
        request.setAttribute("doc_menu", "");
        request.setAttribute("doc_right_menu", "");
        request.setAttribute("template_object_a", "");
        request.setAttribute("template_object_b", "");
        request.setAttribute("template_object_c", "");
        request.setAttribute("template_object_d", "");
        request.setAttribute("perex_data", "");
    }

    private void removeForumFromPerex(HttpServletRequest request, DocDetails doc, String perex) {
        try
        {
            int start = doc.getData().indexOf("!INCLUDE(/components/forum/forum.jsp");
            if (start != -1)
            {
                int end = doc.getData().indexOf(")!", start);
                if (end > start)
                {
                    perex += doc.getData().substring(start, end+2);
                }
            }
        }
        catch (Exception ex)
        {
            Logger.error(ShowDoc.class, ex);
        }

        request.setAttribute("doc_data", perex);
    }

    private String getForward(HttpServletRequest request, Prop prop, TemplateDetails temp, TemplateDetails tempBrowserDetector, BrowserDetector bd, String forward) {
        try {
            //ak mame sablonu pre browserDevice, uz nehladame iny JSP subor pre forward
            if (tempBrowserDetector != null && tempBrowserDetector.getTempId() > 0) bd = null;

            if (temp != null && Tools.isNotEmpty(temp.getTemplateInstallName())) {
                File f = TemplatesDB.getDeviceTemplateFile(new File(Tools.getRealPath("/templates/" + temp.getTemplateInstallName())), forward, bd);
                if (f.exists()) {
                    Logger.debug(ShowDoc.class, "___FORWARDFILE (tempInstallName)="+ f.getAbsolutePath());
                    return "/templates/" + temp.getTemplateInstallName() + "/" + forward;
                }
            }

            if (Tools.isNotEmpty(Constants.getLogInstallName()))
            {
                File f = TemplatesDB.getDeviceTemplateFile(new File(Tools.getRealPath("/templates/" + Constants.getLogInstallName())), forward, bd);
                if (f.exists()) {
                    Logger.debug(ShowDoc.class, "___FORWARDFILE (logInstallName)="+ f.getAbsolutePath());
                    return "/templates/" + Constants.getLogInstallName() + "/" + forward;
                }
            }

            File f = TemplatesDB.getDeviceTemplateFile(new File(Tools.getRealPath("/templates/" + Constants.getInstallName())), forward, bd);
            if (f.exists()) {
                Logger.debug(ShowDoc.class, "___FORWARDFILE (installName)="+ f.getAbsolutePath());
                return "/templates/" + Constants.getInstallName() + "/" + forward;
            }

            f = TemplatesDB.getDeviceTemplateFile(new File(Tools.getRealPath("/templates/")), forward, bd);
            if (!f.exists()) {
                StatDB.addError(request.getRequestURI() + "?" + request.getQueryString(), prop.getText("admin.showdoc_error_message.template") + " " + forward + " " + prop.getText("admin.showdoc_error_message.not_exists") + "!");
                Logger.error(this, prop.getText("admin.showdoc_error_message.template") + " /templates/" + forward + " " + prop.getText("admin.showdoc_error_message.not_exists") + "!");
                //sablona neexistuje, asi som na testovacom serveri u jeeffa...

                forward = "tmp_generic.jsp";
            }
            // else zrusene, aby islo zadat forward aj s adresarom forward = f.getName();
        } catch (Exception e) {
            Logger.error(ShowDoc.class, e);
        }
        return forward;
    }


    /**
     * Vrati lokalne doc_data pre zadane doc_id (hlada napr. hlavicku v lokalnom System adresari pre danu domenu)
     */
    private static void setRequestDocData(String name, int defaultDocId, String defaultData, List<DocDetails> localDocsInGroup, HttpServletRequest request)
    {
        DocDB docDB = DocDB.getInstance();
        DocDetails originalDoc = docDB.getBasicDocDetails(defaultDocId, false);

        if (originalDoc != null && localDocsInGroup != null)
        {
            String groupLng = (String)request.getAttribute("group_lng");

            DocDetails foundDoc = null;
            if (Tools.isNotEmpty(groupLng))
            {
                //ak je nastaveny jazyk adresara, tak najprv kontrolujem jazykovu zhodu bud ako prefix jazyk + medzera napr. "EN header", alebo ako suffix pomlcka+jazyk napr. "header-DE"
                for (DocDetails localDoc : localDocsInGroup)
                {
                    if (localDoc.getTitle().equalsIgnoreCase(groupLng+" "+originalDoc.getTitle()) ||
                            localDoc.getTitle().equalsIgnoreCase(groupLng+"-"+originalDoc.getTitle()) ||
                            localDoc.getTitle().equalsIgnoreCase(originalDoc.getTitle()+"-"+groupLng))
                    {
                        foundDoc = localDoc;
                        break;
                    }
                    //ak mame nastavenu SK-Default hlavicka, skus odstranit SK- a porovnat ako EN-Default hlavicka
                    String originalTitle = originalDoc.getTitle();
                    if (originalTitle.length()>4 && originalTitle.charAt(2)=='-' && localDoc.getTitle().equalsIgnoreCase(groupLng+"-"+originalTitle.substring(3))) {
                        foundDoc = localDoc;
                        break;
                    }
                }
            }

            //ak nie je nastaveny jazyk, postupujem ako predtym a hladam na zaklade zhody titulov stranok
            if(foundDoc == null)
            {
                for (DocDetails localDoc : localDocsInGroup)
                {
                    if(localDoc.getDocId()!=defaultDocId && localDoc.getTitle().equalsIgnoreCase(originalDoc.getTitle()))
                    {
                        foundDoc = localDoc;
                        break;
                    }
                }
            }

            if(foundDoc != null)
            {
                String docData = foundDoc.getData();
                if (Tools.isEmpty(docData))
                {
                    foundDoc = docDB.getDocAndAddToCacheIfNot(foundDoc.getDocId());
                    docData = foundDoc.getData();
                }
                if (Tools.isNotEmpty(docData) || "cloud".equals(Constants.getInstallName()))
                {
                    //v cloude chceme zobrazit userovu paticku aj ked v nej zmazal text (chce ju mat prazdnu)
                    defaultData = docData;
                    defaultDocId = foundDoc.getDocId();
                    request.setAttribute(name+"-docDetails", foundDoc);
                }
            }
        }

        if (request.getSession().getAttribute(Constants.USER_KEY)!=null)
        {
            //ak je prihlaseny user setni objekt aby isla inline editacia inych objektov ako doc_data
            request.setAttribute(name+"-docDetails", originalDoc);
        }

        //nastav aj hodnotu docId pre inline editaciu
        request.setAttribute(name, defaultData);
        request.setAttribute(name+"-docId=", defaultDocId);
    }


    /**
     *  Aktualizuje kody v request (doc_header, doc_footer, doc_menu, doc_data)
     *
     *@param  request HttpServletRequest
     *@param  servletContext ServletContext
     *@param  docId int
     */
    public static void updateCodes(HttpServletRequest request, ServletContext servletContext, int docId)
    {
        try
        {
            HttpSession session = request.getSession();
            Identity user = null;
            try
            {
                //ziskaj meno lognuteho usera
                if (session.getAttribute(Constants.USER_KEY) != null)
                {
                    user = (Identity) session.getAttribute(Constants.USER_KEY);
                }
            }
            catch (Exception ex)
            {
                Logger.error(ShowDoc.class, ex);
            }

            // ------------ HEADER
            String text = (String) request.getAttribute("doc_header");
            if (text != null)
            {
                text = ShowDoc.updateCodes(user, text, docId, request, servletContext);
            }

            request.setAttribute("doc_header", text);

            // ------------------ FOOTER
            text = (String) request.getAttribute("doc_footer");
            if (text != null)
            {
                text = ShowDoc.updateCodes(user, text, docId, request, servletContext);
            }
            request.setAttribute("doc_footer", text);

            // ------------------ MENU
            text = (String) request.getAttribute("doc_menu");
            if (text != null)
            {
                text = ShowDoc.updateCodes(user, text, docId, request, servletContext);
            }
            request.setAttribute("doc_menu", text);

            // ------------------ MENU RIGHT
            text = (String) request.getAttribute("doc_right_menu");
            if (text != null)
            {
                text = ShowDoc.updateCodes(user, text, docId, request, servletContext);
            }
            request.setAttribute("doc_right_menu", text);

            // ------------------ html_data
            text = (String) request.getAttribute("html_data");
            if (text != null)
            {
                text = ShowDoc.updateCodes(user, text, docId, request, servletContext);
            }
            request.setAttribute("html_data", text);

            // ------------------ DOC
            text = (String) request.getAttribute("doc_data");
            if (text != null)
            {
                text = ShowDoc.updateCodes(user, text, docId, request, servletContext);
            }

            request.setAttribute("doc_data", text);
        }
        catch (Exception ignored)
        {
            //
        }
    }




    private IwcmFile getTemplateFile(String template)
    {
        return new IwcmFile(Tools.getRealPath(template));
    }

    public static void setActualUserHash(String hash) {
        ShowDoc.ACTUAL_USER_HASH = hash;
    }

    /**
     * Test if doShowdocAction request parameter is allowed/valid
     * @param doShowdocAction
     * @return
     */
    public static boolean isDoShowdocActionAllowed(String doShowdocAction) {
        if (doShowdocAction != null && doShowdocAction.length()>3 && !doShowdocAction.contains("WEB-INF") && doShowdocAction.endsWith(".do") && !doShowdocAction.contains("/admin/")) {
         return true;
        }
        return false;
    }
}
