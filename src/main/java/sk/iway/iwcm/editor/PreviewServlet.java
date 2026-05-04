package sk.iway.iwcm.editor;


import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.system.spring.SpringUrlMapping;

@WebServlet(name = "previewServlet",
        urlPatterns = {"/preview.do"}
)
public class PreviewServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.println(ShowDoc.class,"Preview SERVLET CALLED - GET");
        execute(request,response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.println(ShowDoc.class,"Preview SERVLET CALLED - POST");
        execute(request,response);
    }


    /**
     *  Description of the Method
     *
     * @exception  IOException       Description of the Exception
     * @exception  ServletException  Description of the Exception
     * @param  request               Description of the Parameter
     * @param  response              Description of the Parameter
     */
    public void execute(HttpServletRequest request,
                        HttpServletResponse response)
            throws IOException, ServletException
    {
        int doc_id = 1;

        //get session
        HttpSession session = request.getSession();
        if (session == null)
        {
            request.getRequestDispatcher("index").forward(request,response);
            return;
        }
        Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
        if (user != null && user.isAdmin())
        {
            //ok
        }
        else
        {
            SpringUrlMapping.redirectToLogon(response);
            return;
        }

        try
        {
            if (doc_id == 1 || doc_id == 0)
            {
                if (request.getParameter("docId") != null)
                {
                    doc_id = Integer.parseInt(request.getParameter("docId"));
                }
            }
        }
        catch (Exception ex)
        {
            request.setAttribute("err_msg", "Požadovaný dokument neexistuje 1");
            request.getRequestDispatcher("error").forward(request,response);
            return;
        }

        DocDB docDB = DocDB.getInstance();

        DocDetails doc = null;

        doc = docDB.getDoc(doc_id);
        if (doc == null)
        {
            //je to novy dokument
            doc = new DocDetails();
        }

        doc.setDocId(getInt(request, "docId"));
        doc.setTitle(recode(request, "title"));
        doc.setData(EditorDB.getCleanBody(recode(request, "data")));
        doc.setExternalLink(recode(request, "externalLink"));
        doc.setNavbar(recode(request, "navbar"));
        doc.setDateCreated(Tools.getNow());
        if (Tools.isNotEmpty(recode(request, "publishStart"))) doc.setPublishStart(DB.getTimestamp(recode(request, "publishStart"), recode(request, "publishStartTime")));
        if (Tools.isNotEmpty(recode(request, "publishEnd"))) doc.setPublishEnd(DB.getTimestamp(recode(request, "publishEnd"), recode(request, "publishEndTime")));
        doc.setAuthorId(user.getUserId());
        doc.setAuthorEmail(user.getEmail());
        doc.setAuthorName(user.getFullName());
        doc.setGroupId(getInt(request, "groupId"));
        doc.setTempId(getInt(request, "tempId"));
        doc.setAvailable(true);
        doc.setSortPriority(Tools.getIntValue(recode(request, "sortPriority"), 10));
        doc.setHeaderDocId(getInt(request, "headerDocId"));
        doc.setMenuDocId(getInt(request, "menuDocId"));
        doc.setFooterDocId(getInt(request, "footerDocId"));
        doc.setPasswordProtected(getMultiSelect("passwordProtected", request));
        doc.setHtmlHead(recode(request, "htmlHead"));
        doc.setHtmlData(recode(request, "htmlData"));
        doc.setPerexPlace(recode(request, "perexPlace"));
        doc.setPerexImage(recode(request, "perexImage"));
        doc.setPerexGroupString(getMultiSelect("perexGroup", request));
        if (Tools.isNotEmpty(recode(request, "eventDate"))) doc.setEventDate(DB.getTimestamp(recode(request, "eventDate"), recode(request, "eventTime")));
        doc.setVirtualPath(recode(request, "virtualPath"));
        doc.setRightMenuDocId(getInt(request, "rightMenuDocId"));
        doc.setFieldA(recode(request, "fieldA"));
        doc.setFieldB(recode(request, "fieldB"));
        doc.setFieldC(recode(request, "fieldC"));
        doc.setFieldD(recode(request, "fieldD"));
        doc.setFieldE(recode(request, "fieldE"));
        doc.setFieldF(recode(request, "fieldF"));
        doc.setFieldG(recode(request, "fieldG"));
        doc.setFieldH(recode(request, "fieldH"));
        doc.setFieldI(recode(request, "fieldI"));
        doc.setFieldJ(recode(request, "fieldJ"));
        doc.setFieldK(recode(request, "fieldK"));
        doc.setFieldL(recode(request, "fieldL"));
        doc.setFieldM(recode(request, "fieldM"));
        doc.setFieldN(recode(request, "fieldN"));
        doc.setFieldO(recode(request, "fieldO"));
        doc.setFieldP(recode(request, "fieldP"));
        doc.setFieldQ(recode(request, "fieldQ"));
        doc.setFieldR(recode(request, "fieldR"));
        doc.setFieldS(recode(request, "fieldS"));
        doc.setFieldT(recode(request, "fieldT"));

        request.setAttribute("is_available", recode(request, "available"));

        String domainName = recode(request, "domainName");
        if (Tools.isNotEmpty(domainName))
        {
            session.setAttribute("preview.editorDomainName", domainName);
        }

        request.setAttribute("path_filter_orig_path", doc.getVirtualPath());

        request.setAttribute("ShowdocAction.showDocData", doc);

        //vypneme XSS filter, inak by zrusilo preview ak by HTML obsahoval javascript
        request.setAttribute("xssTestDisabled", "true");

        //kontroluje sa v InlineEditor.isInlineEditingEnabled() - nezobrazime inlineEditor ak sa jedna o preview
        request.setAttribute("isPreview", true);

        //vypnutie xss ochrany aby fungoval preview pre flash objekty
        response.setHeader("X-XSS-Protection", "0");

        request.getRequestDispatcher("/showdoc.do?showDocData=true").forward(request, response);

        return;
    }


    /**
     *  Gets the int attribute of the PreviewAction object
     *
     *@param  request  Description of the Parameter
     *@param  name     Description of the Parameter
     *@return          The int value
     */
    private int getInt(HttpServletRequest request, String name)
    {
        try
        {
            return (Integer.parseInt(request.getParameter(name)));
        }
        catch (Exception ex)
        {

        }
        return (-1);
    }


    /**
     *  Description of the Method
     *
     *@param  request  Description of the Parameter
     *@param  name     Description of the Parameter
     *@return          Description of the Return Value
     */
    private String recode(HttpServletRequest request, String name)
    {
        String input = request.getParameter(name);
        if (input == null)
        {
            return ("");
        }
        //Logger.println(this,"Recoding: "+input);
        return (input.trim());
    }


    /**
     * Skonvertuje multi select hodnoty na retazec oddeleny ciarkami
     * @param name
     * @param request
     * @return
     */
    private String getMultiSelect(String name, HttpServletRequest request)
    {
        String passwordProtected = "";
        String passParams[] = request.getParameterValues("passwordProtected");
        if (passParams != null && passParams.length>0)
        {
            for (int i=0; i<passParams.length; i++)
            {
                if (Tools.isEmpty(passParams[i])) continue;

                if (Tools.isEmpty(passwordProtected)) passwordProtected = passParams[i];
                else passwordProtected += "," + passParams[i];
            }
        }
        return passwordProtected;
    }


}
