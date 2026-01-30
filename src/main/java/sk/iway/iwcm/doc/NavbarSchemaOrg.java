package sk.iway.iwcm.doc;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 * Schema.org navbar implementation - generates breadcrumb navigation in schema.org format
 */
public class NavbarSchemaOrg implements NavbarInterface {

    @Override
    public String getNavbar(int groupId, int docId, HttpServletRequest request) {
        return getNavbarSchema(groupId, docId, request.getSession());
    }

    /**
     * Vrati HTML kod pre Breadcrumb navigaciu vo formate schema.org
     * @param groupId - ID adresara
     * @param docId - ID aktualnej web stranky
     * @param session
     * @return
     */
    public String getNavbarSchema(int groupId, int docId, HttpSession session)
    {
        GroupsDB groupsDB = GroupsDB.getInstance();
        DocDB docDB = DocDB.getInstance();

        String htmlCode = "";

        boolean finished = false;
        int currentGroupId = groupId;
        int depth = 0;
        int max_depth = 30;
        GroupDetails group;
        int realMaxDepth = 1;

        // potrebujem zistit realny pocet urovni
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

                Map<String, String> aparams = groupsDB.parseAparam(group.getNavbarName());
                if (group.getDefaultDocId() > 0 && (!aparams.containsKey("class") || aparams.containsKey("class") && !aparams.get("class").equalsIgnoreCase("is-headline"))) {
                    realMaxDepth++;
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

        depth = 0;
        currentGroupId = groupId;
        finished = false;
        int skippedCount = 0;

        while (finished == false && depth++<max_depth)
        {
            group = groupsDB.findGroup(currentGroupId);
            if (group != null && group.getGroupId()!=group.getParentGroupId())
            {
                if (group.isInternal()==true || group.getShowInNavbar(session)==GroupDetails.MENU_TYPE_HIDDEN)
                {
                    currentGroupId = group.getParentGroupId();
                    skippedCount++;
                    continue;
                }

                String groupNavbar = Tools.convertToHtmlTags(group.getNavbarName());

                Map<String, String> aparams = groupsDB.parseAparam(groupNavbar);
                if (group.getDefaultDocId() < 1 || aparams.containsKey("class") && aparams.get("class").equalsIgnoreCase("is-headline")) {
                    currentGroupId = group.getParentGroupId();
                    skippedCount++;
                    continue;
                }

                if (group.getDefaultDocId() > 0 && groupNavbar.length() > 1 && (groupNavbar.indexOf("<a") < 0 || groupNavbar.toLowerCase().indexOf("<aparam") >= 0))
                {
                    //odstrani <aparam>
                    String text = groupNavbar.replaceAll("(?i)<aparam.*>", "");
                    groupNavbar = "\n	<li class=\"is-item\" itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/ListItem\">" +
                            "<a href='" + docDB.getDocLink(group.getDefaultDocId()) + "' class='navbar' itemprop=\"item\">" +
                            "<span itemprop=\"name\">" + Tools.convertToHtmlTags(text) + "</span>" +
                            "</a>" +
                            "<meta itemprop=\"position\" content=\"" + (realMaxDepth - depth + skippedCount) + "\" /></li>";
                }

                if (groupNavbar.length() > 1 && "&nbsp;".equals(group.getNavbarName())==false)
                {
                    if (Constants.getBoolean("navbarRenderAllLinks")==false && group.getDefaultDocId()==docId)
                    {
                        String text = group.getNavbarName().replaceAll("(?i)<aparam.*>", "");
                        String newPath = Tools.convertToHtmlTags(text);

                        //ochrana pred duplikovanim cesty (ak mame root a v nom mame hlavnu stranku v podadresari s rovnakym nazvom)
                        if (htmlCode.startsWith(newPath)==false) htmlCode = newPath + htmlCode;
                    }
                    else
                    {
                        //tu nepotrebujeme nahradu, tu uz len pridavame moznosti dokopy
                        String newPath = groupNavbar;

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

        htmlCode = "\n<ol itemscope itemtype=\"http://schema.org/BreadcrumbList\">" + htmlCode+ "\n</ol>";
        return (htmlCode);
    }

    @Override
    public String getNavbarForNonDefaultDoc(sk.iway.iwcm.doc.DocDetails doc, String navbar, HttpServletRequest request) {
        DocDB docDB = DocDB.getInstance();
        // Implement custom logic for non-default document navbar
        int counter = StringUtils.countMatches(navbar, "<li") + 1;
        String link = docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request);
        navbar = navbar.substring(0, navbar.length() - 5);
        navbar = navbar + "	<li class=\"is-item\" itemprop=\"itemListElement\" itemscope=\"\" itemtype=\"http://schema.org/ListItem\"><a href=\"" + link + "\" class=\"navbar\" itemprop=\"item\"><span itemprop=\"name\">" + Tools.convertToHtmlTags(doc.getNavbar()) + "</span></a><meta itemprop=\"position\" content=\"" + counter + "\"></li>";
        navbar += "\n</ol>";
        return navbar;
    }
}
