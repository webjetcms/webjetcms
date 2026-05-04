package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 * RDF navbar implementation - generates breadcrumb navigation in RDF format
 * http://support.google.com/webmasters/bin/answer.py?hl=en&amp;topic=1088474&amp;hlrm=en&amp;answer=185417&amp;ctx=topic
 */
public class NavbarRDF implements NavbarInterface {

    @Override
    public String getNavbar(int groupId, int docId, HttpServletRequest request) {
        return getNavbarRDF(groupId, docId, request.getSession());
    }

    /**
     * Vrati HTML kod pre Breadcrumb navigaciu vo formate RDF
     * @param groupId - ID adresara
     * @param docId - ID aktualnej web stranky
     * @param session
     * @return
     */
    public String getNavbarRDF(int groupId, int docId, HttpSession session)
    {
        GroupsDB groupsDB = GroupsDB.getInstance();
        DocDB docDB = DocDB.getInstance();

        String htmlCode = "";

        boolean finished = false;
        int currentGroupId = groupId;
        int depth = 0;
        int max_depth = 30;
        GroupDetails group;
        while (finished == false && depth++<max_depth)
        {
            group = groupsDB.findGroup(currentGroupId);
            if (group != null && group.getGroupId()!=group.getParentGroupId())
            {
                if (group.isInternal()==true || group.getShowInNavbar(session)==GroupDetails.MENU_TYPE_HIDDEN)
                {
                    currentGroupId = group.getParentGroupId();
                    continue;
                }

                String groupNavbar = group.getNavbarName();
                if (group.getDefaultDocId() > 0 && groupNavbar.length() > 1 && (groupNavbar.indexOf("<a") < 0 || groupNavbar.toLowerCase().indexOf("<aparam") >= 0))
                {
                    //odstrani <aparam>
                    String text = Tools.convertToHtmlTags(groupNavbar.replaceAll("(?i)<aparam.*>",""));
                    groupNavbar = "<span typeof=\"v:Breadcrumb\"><a href='"+docDB.getDocLink(group.getDefaultDocId())+"' class='navbar' rel=\"v:url\" property=\"v:title\">" + text + "</a></span>";
                }

                if (groupNavbar.length() > 1 && "&nbsp;".equals(group.getNavbarName())==false)
                {
                    if (Constants.getBoolean("navbarRenderAllLinks")==false && group.getDefaultDocId()==docId)
                    {
                        String newPath = " "+Constants.getString("navbarSeparator")+" " + Tools.convertToHtmlTags(group.getNavbarName().replaceAll("(?i)<aparam.*>",""));
                        //ochrana pred duplikovanim cesty (ak mame root a v nom mame hlavnu stranku v podadresari s rovnakym nazvom)
                        if (htmlCode.startsWith(newPath)==false) htmlCode = newPath + htmlCode;
                    }
                    else
                    {
                        String newPath = " "+Constants.getString("navbarSeparator")+" " + groupNavbar;
                        if (htmlCode.startsWith(newPath)==false) htmlCode = newPath + htmlCode;
                    }
                }
                currentGroupId = group.getParentGroupId();
            }
            else
            {
                //group doesn't exist
                finished = true;
            }
            //we are on the root group
            if (currentGroupId == 0)
            {
                finished = true;
            }
        }
        try
        {
            //odstran zobak na zaciatku
            if (htmlCode.startsWith(" "+Constants.getString("navbarSeparator")+" "))
            {
                htmlCode = htmlCode.substring(Constants.getString("navbarSeparator").length() + 2).trim();
            }
        }
        catch (Exception ex)
        {
        }

        htmlCode = "<div class=\"breadcrumbrdf\" xmlns:v=\"http://rdf.data-vocabulary.org/#\">" + htmlCode + "</div>";
        return (htmlCode);
    }

    @Override
    public String getNavbarForNonDefaultDoc(sk.iway.iwcm.doc.DocDetails doc, String navbarHtml, HttpServletRequest request) {
        navbarHtml = navbarHtml.substring(0, navbarHtml.length()-6) + " " + Constants.getString("navbarSeparator")+" <span>"+Tools.convertToHtmlTags(doc.getNavbar())+"</span></div>";
        return navbarHtml;
    }
}
