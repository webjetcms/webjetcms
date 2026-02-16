package sk.iway.iwcm.common;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.mock.MockHttpServletResponse;
import sk.iway.Password;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.AtrBean;
import sk.iway.iwcm.doc.AtrDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.WJResponseWrapper;

public class EditorToolsForCore {
    public static String RENDER_DATA_SEPARATOR = "\n\n-----------------------------------------------------------------\n\n";

    private EditorToolsForCore() {

    }

    /**
     * Nastavi formu virtualPath
     * @param my_form
     */
    public static void setVirtualPath(EditorForm my_form)
    {
        GroupsDB groupsDB = GroupsDB.getInstance();
        String domain = groupsDB.getDomain(my_form.getGroupId());
        if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML && my_form.getVirtualPath().startsWith("javascript:")==false)
        {
            boolean mustGenerateVirtualPath = false;
            if (Tools.isNotEmpty(my_form.getVirtualPath()))
            {
                int actualDocId = DocDB.getDocIdFromURL(my_form.getVirtualPath(), domain);
                if (actualDocId > 0 && actualDocId != my_form.getDocId())
                {
                    mustGenerateVirtualPath = true;
                    my_form.setVirtualPath("");
                }
            }

            if (mustGenerateVirtualPath || Tools.isEmpty(my_form.getVirtualPath()) || my_form.getVirtualPath().indexOf('/')==-1)
            {
                //nastavime ako treba
                String groupDiskPath = DocDB.getGroupDiskPath(groupsDB.getGroupsAll(), my_form.getGroupId());
                DocDetails doc = new DocDetails();
                doc.setDocId(my_form.getDocId());
                doc.setTitle(my_form.getTitle());
                doc.setNavbar(DB.prepareString(my_form.getNavbar(), 128));
                doc.setVirtualPath(my_form.getVirtualPath());
                doc.setGroupId(my_form.getGroupId());
                String virtualPath = DocDB.getURL(doc, groupDiskPath);
                String koncovka = virtualPath.endsWith("/") ? "/" : ".html";
                String editorPageExtension = Constants.getString("editorPageExtension");

                for (int i=1; i<1000; i++)
                {
                    if(virtualPath != null && virtualPath.length() > 255)
                    {
                        String vpTmp = virtualPath.substring(0, virtualPath.length()-koncovka.length());
                        vpTmp = DB.prepareString(vpTmp, 255-koncovka.length())+koncovka;
                        virtualPath = vpTmp;
                    }

                    int allreadyDocId = DocDB.getDocIdFromURL(virtualPath, domain);
                    Logger.debug(EditorToolsForCore.class, "setVirtualPath: allreadyDocId for virtualPath: "+virtualPath + " ,docid: "+allreadyDocId);
                    if (allreadyDocId <= 0 || allreadyDocId==my_form.getDocId())
                    {
                        break;
                    }
                    doc.setTitle(my_form.getTitle()+" "+i);
                    doc.setNavbar(DB.prepareString(my_form.getNavbar(), 128)+" "+i);

                    if ("/".equals(editorPageExtension))
                    {
                        //nastav cistu, handluje sa to nastavenim title s cislom vyssie
                        doc.setVirtualPath("");
                    }
                    else
                    {
                        if (my_form.getVirtualPath().endsWith(".html"))
                        {
                            doc.setVirtualPath(Tools.replace(my_form.getVirtualPath(), ".html", "-" + i + ".html"));
                            koncovka = "-" + i + ".html";
                        } else if (my_form.getVirtualPath().endsWith("/"))
                        {
                            doc.setVirtualPath(my_form.getVirtualPath() + i + ".html");
                            koncovka = i + ".html";
                        } else if (Tools.isEmpty(my_form.getVirtualPath()))
                        {
                            doc.setVirtualPath(Tools.replace(my_form.getTitle() + ".html", "/", "-"));
                            my_form.setVirtualPath(doc.getVirtualPath());
                            koncovka = ".html";
                        }
                    }

                    virtualPath = DocDB.getURL(doc, groupDiskPath);
                }

                my_form.setVirtualPath(DocDB.normalizeVirtualPath(virtualPath));

                Logger.println(EditorToolsForCore.class, "nastaveny virtual path na:"+virtualPath+";");
            }
            else if ("cloud".equals(Constants.getInstallName()))
            {
                //tiket 15910 - kontrola specialnych znakov v URL
                String cleaned = DocTools.removeCharsDir(DB.internationalToEnglish(my_form.getVirtualPath())).toLowerCase();
                if(!cleaned.equals(my_form.getVirtualPath()))
                {
                    my_form.setVirtualPath(DocDB.normalizeVirtualPath(cleaned));
                    Logger.println(EditorToolsForCore.class, "virtual path upraveny na:"+my_form.getVirtualPath()+";");
                }
            }
        }
    }

    public static String getDataAsc(String data, EditorForm ef, boolean isLucene, HttpServletRequest request)
    {
        boolean isRendered = false;
        if (Constants.getBoolean("fulltextExecuteApps") && Tools.isEmpty(ef.getExternalLink()))
        {
            DocDetails doc = ef.toDocDetails();

            String renderedData = renderIncludes(doc, true, request);
            if (Tools.isNotEmpty(renderedData))
            {
                //odpaz domeny a blbosti
                String domain = Tools.getBaseHref(request);
                renderedData = Tools.replace(renderedData, domain, "");

                domain = Tools.getBaseHrefLoopback(request);
                renderedData = Tools.replace(renderedData, domain, "");

                if (request!=null)
                {
                    domain = "http://" + request.getServerName();
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = "https://" + request.getServerName();
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = "http://" + DocDB.getDomain(request);
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = "https://" + DocDB.getDomain(request);
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = ":"+request.getLocalPort()+"/";
                    renderedData = Tools.replace(renderedData, domain, "/");
                }

                domain = ":8080/";
                renderedData = Tools.replace(renderedData, domain, "/");

                domain = ":"+ Constants.getInt("httpServerPort")+"/";
                renderedData = Tools.replace(renderedData, domain, "/");

                data = renderedData;
                isRendered = true;
            }
        }

        String dataAsc;

        if (isLucene || isRendered) dataAsc = data;
        else dataAsc = (DB.internationalToEnglish(data).trim()).toLowerCase();

        dataAsc = removeHtmlTagsKeepLength(dataAsc);

        if (ef != null)
        {
            //ak tam nie je title, dopln
            String titleAsc = (DB.internationalToEnglish(ef.getTitle()).trim()).toLowerCase();
            //aby spravne hladalo aj v nazvoch suborov len s pouzitim contains
            titleAsc = Tools.replace(titleAsc, "_", " ");
            titleAsc = Tools.replace(titleAsc, "-", " ");
            titleAsc = Tools.replace(titleAsc, ".", " ");
            titleAsc = Tools.replace(titleAsc, "/", " ");
            if (isLucene)
            {
                if (dataAsc.indexOf(ef.getTitle())==-1) dataAsc += " "+ef.getTitle()+"\n";
            }
            else
            {
                if (dataAsc.indexOf(titleAsc)==-1) dataAsc += "<h1>"+titleAsc+"</h1>\n";
            }

            DocDB docDB = DocDB.getInstance();
            //ak treba dopln keywords
            if (Constants.getBoolean("fulltextIncludeKeywords"))
            {
                try
                {
                    String perexGroupIds[] = ef.getPerexGroup();
                    if (perexGroupIds != null)
                    {
                        String keywords = null;
                        for (String keyword : perexGroupIds)
                        {
                            keyword = docDB.convertPerexGroupIdToName(Tools.getIntValue(keyword, -1));
                            if (Tools.isEmpty(keyword)) continue;
                            if (keyword.startsWith("#") || keyword.startsWith("@") || keyword.startsWith("_")) keyword=keyword.substring(1);
                            if (keywords == null) keywords = keyword;
                            else keywords += ", "+keyword;
                        }
                        if (Tools.isNotEmpty(keywords))
                        {
                            if (isLucene) dataAsc += (DB.internationalToEnglish(keywords).trim()).toLowerCase()+"\n";
                            else dataAsc += "<div style='display:none' class='fulltextKeywords'>"+(DB.internationalToEnglish(keywords).trim()).toLowerCase()+"</div>\n";
                        }
                    }
                } catch (Exception ex) {}
            }
            if (Constants.getBoolean("fulltextIncludePerex") && Tools.isNotEmpty(ef.getHtmlData()))
            {
                if (isLucene) dataAsc += (DB.internationalToEnglish(ef.getHtmlData()).trim()).toLowerCase()+"\n";
                else dataAsc += "<div style='display:none' class='fulltextPerex'>"+(DB.internationalToEnglish(ef.getHtmlData()).trim()).toLowerCase()+"</div>\n";
            }
            //ak treba dopln atributy
            if (Constants.getBoolean("fulltextIncludeAttributes"))
            {
                try
                {
                    List<AtrBean> attrs = AtrDB.getAtributes(ef.getDocId(), null, null);	//ziskam vsetky atributy pre danu stranku
                    if (attrs != null && attrs.size() > 0)
                    {
                        String attributes = null;
                        for (AtrBean attr : attrs)
                        {
                            if (attr == null) continue;
                            if(Tools.isNotEmpty(attr.getAtrName()) && Tools.isNotEmpty(attr.getValueString()))
                            {
                                if (attributes == null) attributes = attr.getAtrName() + "=" + attr.getValueString();
                                else attributes += ", "+attr.getAtrName() + "=" + attr.getValueString();
                            }
                        }

                        if (Tools.isNotEmpty(attributes))
                        {
                            if (isLucene) dataAsc += (DB.internationalToEnglish(attributes).trim()).toLowerCase()+"\n";
                            else dataAsc += "<div style='display:none' class='fulltextAttributes'>"+(DB.internationalToEnglish(attributes).trim()).toLowerCase()+"</div>\n";
                        }
                    }
                } catch (Exception ex) {}
            }

            //#22131
            if(Tools.isNotEmpty(Constants.getString("fulltextDataAscMethod")))
            {
                String className = "";
                String methodName = "";
                try {
                    className = Constants.getString("fulltextDataAscMethod").substring(0, Constants.getString("fulltextDataAscMethod").lastIndexOf("."));
                    methodName = Constants.getString("fulltextDataAscMethod").substring(Constants.getString("fulltextDataAscMethod").lastIndexOf(".") + 1);

                    Class<?> clazz = Class.forName(className);
                    Method method = clazz.getMethod(methodName, EditorForm.class);
                    String returned = (String)method.invoke(null, ef);
                    if(Tools.isNotEmpty(returned))
                        dataAsc += returned;
                } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
                    Logger.debug(EditorToolsForCore.class, "ReflectionLoader - " + className + "." + methodName + " exception");
                    sk.iway.iwcm.Logger.error(e);
                }
            }
        }

        dataAsc = Tools.replaceStrings(dataAsc, "searchIndexReplaceStrings", false);

        return dataAsc;
    }

    public static String renderIncludes(DocDetails doc, boolean addInternationalToEnglishSection, HttpServletRequest request)
    {
        GroupsDB groupsDB = GroupsDB.getInstance();
        TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
        GroupDetails group = groupsDB.getGroup(doc.getGroupId());
        if (temp != null && group != null)
        {
            String lng = Tools.isNotEmpty(temp.getLng()) ? temp.getLng() : "";
            if (Tools.isNotEmpty(group.getLng())) lng = group.getLng();
            PageLng.setUserLng(request, null, lng);

            ShowDoc.setRequestData(doc, group, DocDB.getInstance(), groupsDB, request);
            ShowDoc.setRequestData(group, groupsDB, request);
            ShowDoc.setRequestData(temp, request);
        }

        String data = doc.getData();

        return renderIncludes(data, addInternationalToEnglishSection, request);
    }

    public static String renderIncludes(String data, boolean addInternationalToEnglishSection, HttpServletRequest request)
    {
        if (request != null) request.setAttribute("renderingIncludes", true);
        String renderedData = null;
        String includeFileName = "/components/_common/fulltext_preview.jsp";
        StringBuilder htmlCode = null;

        try
        {
            if (request!=null)
            {

                WJResponseWrapper respWrapper = null;
                MockHttpServletResponse response = new MockHttpServletResponse();
                respWrapper = new WJResponseWrapper(response, request);

                request.setAttribute("fulltext_preview", data);

                request.getRequestDispatcher(includeFileName).include(request, respWrapper);
                if (Tools.isEmpty(respWrapper.redirectURL))
                {
                    htmlCode = new StringBuilder(respWrapper.strWriter.getBuffer().toString());
                }
            }
            else
            {
                //ak nemame request musime spravit loopback connect
                Cache c = Cache.getInstance();
                String CACKE_KEY = "fulltext_preview-"+ Tools.getNow()+"-"+ Password.generatePassword(10);
                c.setObject(CACKE_KEY, data, 5);

                String downloaded = Tools.downloadUrl(Tools.getBaseHrefLoopback(null)+includeFileName+"?key="+CACKE_KEY);
                if (Tools.isNotEmpty(downloaded))
                {
                    htmlCode = new StringBuilder(downloaded);
                }
                c.removeObject(CACKE_KEY);
            }

            if (htmlCode != null && htmlCode.length()>0)
            {
                htmlCode = WriteTagToolsForCore.fixXhtml(htmlCode, request);
                //prevent spam nerobime, pretoze nam to potom vo form mail dava chybu JS do emailu htmlCode = WriteTagToolsForCore.preventSpam(htmlCode, request);
                htmlCode = WriteTagToolsForCore.replaceWriteText(htmlCode, request);

                renderedData = Tools.replace(htmlCode, "&nbsp;", " ").toString();

                if (addInternationalToEnglishSection)
                {
                    String lcInternational = DB.internationalToEnglish(renderedData).toLowerCase();

                    renderedData = renderedData + RENDER_DATA_SEPARATOR + lcInternational;
                }
            }

        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
            Logger.debug(EditorToolsForCore.class, "renderIncludes ERROR, htmlData:"+htmlCode);
        }

        //nastala exception, radsej to do FT indexu nedame
        if (renderedData!=null && renderedData.contains("iwcm.tags.WriteTag.writeText")) renderedData = null;
        if (request != null) request.removeAttribute("renderingIncludes");

        return renderedData;
    }

    /**
     * Remove from string html tags and keep the length of string
     */
    public static String removeHtmlTagsKeepLength(String html_text)
    {
        if (Tools.isEmpty(html_text)) return("");

        //Logger.debug(EditorDB.class, "Html size: "+html_text.length()+" Html text: "+html_text);

        int c;
        int tt = 0;
        boolean intag = false;
        StringBuilder s = new StringBuilder();
        String temp = "";

        boolean script = false;
        int levels = 0;

        html_text = removeCommandKeepLength(html_text, "!INCLUDE", ")!");
        html_text = removeCommandKeepLength(html_text, "!REMAP_PAGE(", ")!");
        html_text = removeCommandKeepLength(html_text, "!PARAMETER(", ")!");
        html_text = removeCommandKeepLength(html_text, "!REQUEST(", ")!");
        html_text = removeCommandKeepLength(html_text, "!LOGGED_USER", "!");

        try
        {
            int failsafe = 0;
            c = html_text.charAt(tt);
            while (tt < (html_text.length()) && failsafe++ < 100000)
            {
                if (c == '<')
                {
                    //start of a tag
                    intag = true;
                    temp = "";
                    //clear script string
                }
                if ((c == '<') && (script == false) && (intag == true))
                {
                    levels++;
                }
                if ((c == '>') && (script == false) && (intag == true))
                {
                    levels--;
                }

                if ((!intag) && (script == false))
                {
                    // not tag and not java script
                    s.append((char) c);
                }

                if(intag)	//if in tag, add space - keep length
                {
                    s.append(" ");
                }
                if (temp.compareToIgnoreCase("script") == 0)
                {
                    // equal returns 0
                    script = true;
                    temp = "";
                }
                if (temp.compareToIgnoreCase("/script") == 0)
                {
                    script = false;
                    temp = "";
                }
                if (temp.compareToIgnoreCase("style") == 0)
                {
                    // equal returns 0
                    script = true;
                    temp = "";
                }
                if (temp.compareToIgnoreCase("/style") == 0)
                {
                    script = false;
                    temp = "";
                }

                if ((c == '>') && (script == false) && (levels == 0))
                {
                    intag = false;
                    levels = 0;
                }
                tt++;
                if(tt == (html_text.length())) break;
                c = html_text.charAt(tt);

                if (intag == true)
                {
                    temp += (char) c;
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(EditorToolsForCore.class, e.getMessage());
            sk.iway.iwcm.Logger.error(e);
        }

        return s.toString();
    }

    /**
     * Odstrani z HTML kodu riadiace bloky typu !INCLUDE(...)!, !PARAM(...)!, pricom zachova dlzku retazca
     */
    public static String removeCommandKeepLength(String html_text, String commandStart, String commandEnd)
    {
        commandStart = commandStart.toLowerCase();
        StringBuilder html = new StringBuilder(html_text);

        StringBuilder htmlLowerCase = new StringBuilder(html_text.toLowerCase());

        try
        {
            //Logger.println(this,"html:"+html);
            int failsafe = 0;
            int startIndex;
            int endIndex;
            while (htmlLowerCase.indexOf(commandStart) != -1 && failsafe < 50)
            {
                failsafe++;
                startIndex = htmlLowerCase.indexOf(commandStart);
                if (startIndex != -1)
                {
                    endIndex = htmlLowerCase.indexOf(commandEnd, startIndex + commandStart.length());
                    if (endIndex > startIndex)
                    {
                        for(int i = startIndex; i < endIndex + commandEnd.length(); i++)
                        {
                            html.setCharAt(i, ' ');
                            htmlLowerCase.setCharAt(i, ' ');
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        return html.toString();
    }

    /**
     * Pripravi data_asc pre full text hladanie (ak vkladate do DB priamo - mimo saveEditorForm)
     * @param data
     * @param ef
     * @return
     */
    public static String getDataAsc(String data, EditorForm ef)
    {
        return getDataAsc(data, ef, false);
    }

    public static String getDataAsc(String data, EditorForm ef, boolean isLucene)
    {
        return getDataAsc(data, ef, false, null);
    }

    /**
     * naplni EditForm zo suboru zadaneho pomocou dir,file
     *
     * @return
     */
    public static EditForm fillEditFormFromFile(String dir, String file, boolean alsoData, Identity user)
    {
        String realDir = "";
        StringBuilder contextFile = new StringBuilder();
        EditForm editForm = new EditForm();
        try
        {
            if (user != null && ("jeeff".equals(user.getLoginName()) || "lbalat".equals(user.getLoginName()))
                        && dir.indexOf(':') != -1)
            {
                realDir = dir;
            }
            else
            {
                realDir = Tools.getRealPath(dir);
            }
            if (file != null)
            {
                IwcmFile f = new IwcmFile(realDir + File.separatorChar + file);
                if (f.exists() && alsoData)
                {
                    InputStreamReader isr = new InputStreamReader(new IwcmInputStream(realDir + File.separatorChar + file),
                                Constants.FILE_ENCODING);
                    char buff[] = new char[8000];
                    int len;
                    String line;
                    while ((len = isr.read(buff)) != -1)
                    {
                        line = new String(buff, 0, len);
                        contextFile.append(line);
                        if (EditTools.parseLine(line) == false)
                        {
                            // je to binarne, ani nemusime pokracovat...
                            break;
                        }
                    }
                    isr.close();
                    editForm.setData(contextFile.toString());
                }
                editForm.setFile(file);
                editForm.setOrigFile(file);
            }
            else
            {
                editForm.setData("");
                editForm.setFile("");
                editForm.setOrigFile("");
            }
            editForm.setDir(dir);
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        return editForm;
    }
}
