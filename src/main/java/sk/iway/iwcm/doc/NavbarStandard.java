package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 * Standard navbar implementation - generates simple text-based breadcrumb navigation
 */
public class NavbarStandard implements NavbarInterface {

    @Override
    public String getNavbar(int groupId, int docId, HttpServletRequest request) {
        return getNavbar(groupId, docId, request.getSession());
    }

    /**
     * Vrati navigacnu listup, pre zadane docId (aktualne) nezrenderuje odkaz
     * @param groupId
     * @param docId
     * @param session
     * @return
     */
    public String getNavbar(int groupId, int docId, HttpSession session)
    {
        GroupsDB groupsDB = GroupsDB.getInstance();

        String path = "";
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

                if (group.getNavbar().length() > 1 && "&nbsp;".equals(group.getNavbarName())==false)
                {
                    if (Constants.getBoolean("navbarRenderAllLinks")==false && group.getDefaultDocId()==docId)
                    {
                        String newPath = " "+Constants.getString("navbarSeparator")+" " + group.getNavbarName().replaceAll("(?i)<aparam.*>","");
                        //ochrana pred duplikovanim cesty (ak mame root a v nom mame hlavnu stranku v podadresari s rovnakym nazvom)
                        if (path.startsWith(newPath)==false) path = newPath + path;
                    }
                    else
                    {
                        String navbarName = group.getNavbar();
                        if (navbarName.contains("*||")) navbarName = Tools.replace(navbarName, "*||", "</");
                        if (navbarName.contains("*|")) navbarName = Tools.replace(navbarName, "*|", "<");
                        if (navbarName.contains("|*")) navbarName = Tools.replace(navbarName, "|*", ">");
                        String newPath = " "+Constants.getString("navbarSeparator")+" " + navbarName;
                        if (path.startsWith(newPath)==false) path = newPath + path;
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
            if (path.startsWith(" "+Constants.getString("navbarSeparator")+" "))
            {
                path = path.substring(Constants.getString("navbarSeparator").length() + 2).trim();
            }
        }
        catch (Exception ex)
        {
        }
        path = Tools.convertToHtmlTags(path);
        return (path);
    }

    @Override
    public String getNavbarForNonDefaultDoc(sk.iway.iwcm.doc.DocDetails doc, String navbar, HttpServletRequest request) {
        navbar = navbar + " " + Constants.getString("navbarSeparator") + " " + Tools.convertToHtmlTags(doc.getNavbar());
        return navbar;
    }
}
