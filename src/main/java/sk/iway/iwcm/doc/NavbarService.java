package sk.iway.iwcm.doc;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import org.apache.commons.lang.StringUtils;

/**
 * Generovanie navigacnej listy (navbar) pre rozne typy zobrazenia
 */
public class NavbarService implements NavbarInterface {

    public String getNavbar(DocDetails doc, HttpServletRequest request) {
		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();

        String navbar = doc.getNavbar();
        String navbar2;
        String navbarDefaultType = Constants.getString("navbarDefaultType");
        
        //najskor zisti ako je na tom adresar
        GroupDetails group = groupsDB.getGroup(doc.getGroupId());
        
        // Try to load custom implementation
        NavbarInterface customNavbar = getCustomNavbarImplementation(navbarDefaultType);
        
        if (customNavbar != null)
        {
            // Use custom implementation for RDF format
            if ("rdf".equalsIgnoreCase(navbarDefaultType))
            {
                navbar2 = customNavbar.getNavbarRDF(doc.getGroupId(), doc.getDocId(), request);
            }
            // Use custom implementation for schema.org format
            else if ("schema.org".equalsIgnoreCase(navbarDefaultType))
            {
                navbar2 = customNavbar.getNavbarSchema(doc.getGroupId(), doc.getDocId(), request);
            }
            // Use custom implementation for standard format
            else
            {
                navbar2 = customNavbar.getNavbar(doc.getGroupId(), doc.getDocId(), request);
            }
        }
        else if ("rdf".equalsIgnoreCase(navbarDefaultType))
        {
            navbar2 = getNavbarRDF(doc.getGroupId(), doc.getDocId(), request.getSession());
        }
        else if ("schema.org".equalsIgnoreCase(navbarDefaultType))
        {
            navbar2 = getNavbarSchema(doc.getGroupId(), doc.getDocId(), request.getSession());
        }
        else
        {
            navbar2 = getNavbar(doc.getGroupId(), doc.getDocId(), request.getSession());
        }
        if (navbar2.length() > 2)
        {
            navbar = navbar2;
            //ak to nie je default doc pre grupu tak sprav linku
            if (doc.getDocId() != group.getDefaultDocId() && doc.isShowInNavbar(request))
            {
                if (doc.getNavbar().length() > 2)
                {
                    if ("rdf".equalsIgnoreCase(Constants.getString("navbarDefaultType")) && navbar.indexOf("</div>")!=-1)
                    {
                        navbar = navbar.substring(0, navbar.length()-6) + " " + Constants.getString("navbarSeparator")+" <span>"+Tools.convertToHtmlTags(doc.getNavbar())+"</span></div>";
                    }
                    else if ("schema.org".equalsIgnoreCase(Constants.getString("navbarDefaultType")))
                    {
                        int counter = StringUtils.countMatches(navbar, "<li") + 1;
                        String link = docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request);
                        navbar = navbar.substring(0, navbar.length() - 5);
                        navbar = navbar + "	<li class=\"is-item\" itemprop=\"itemListElement\" itemscope=\"\" itemtype=\"http://schema.org/ListItem\"><a href=\"" + link + "\" class=\"navbar\" itemprop=\"item\"><span itemprop=\"name\">" + Tools.convertToHtmlTags(doc.getNavbar()) + "</span></a><meta itemprop=\"position\" content=\"" + counter + "\"></li>";
                        navbar += "\n</ol>";
                    }
                    else
                    {
                        navbar = navbar + " " + Constants.getString("navbarSeparator") + " " + Tools.convertToHtmlTags(doc.getNavbar());
                    }
                }
            }
        }

        return navbar;
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

    /**
	 * Vrati HTML kod pre Breadcrumb navigaciu vo formate RDF
	 * http://support.google.com/webmasters/bin/answer.py?hl=en&amp;topic=1088474&amp;hlrm=en&amp;answer=185417&amp;ctx=topic
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

	/**
	 * Try to load custom navbar implementation if navbarDefaultType is a class name
	 * @param navbarDefaultType - value from Constants.getString("navbarDefaultType")
	 * @return Custom NavbarInterface implementation or null if not a valid class or standard type
	 */
	private NavbarInterface getCustomNavbarImplementation(String navbarDefaultType) {
		// Check if it's a standard type (not a custom class)
		if (Tools.isEmpty(navbarDefaultType) || 
		    "normal".equalsIgnoreCase(navbarDefaultType) || 
		    "rdf".equalsIgnoreCase(navbarDefaultType) || 
		    "schema.org".equalsIgnoreCase(navbarDefaultType)) {
			return null;
		}
		
		// Try to load the class as a custom implementation
		try {
			Class<?> clazz = Class.forName(navbarDefaultType);
			if (NavbarInterface.class.isAssignableFrom(clazz)) {
				return (NavbarInterface) clazz.getDeclaredConstructor().newInstance();
			} else {
				Logger.error(NavbarService.class, "Class " + navbarDefaultType + " does not implement NavbarInterface");
			}
		} catch (ClassNotFoundException e) {
			Logger.debug(NavbarService.class, "Class " + navbarDefaultType + " not found, using standard navbar implementation");
		} catch (Exception e) {
			Logger.error(NavbarService.class, "Error while initializing custom navbar implementation: " + navbarDefaultType, e);
		}
		
		return null;
	}

	@Override
	public String getNavbarRDF(int groupId, int docId, HttpServletRequest request) {
		return getNavbarRDF(groupId, docId, request.getSession());
	}

	@Override
	public String getNavbarSchema(int groupId, int docId, HttpServletRequest request) {
		return getNavbarSchema(groupId, docId, request.getSession());
	}

	@Override
	public String getNavbar(int groupId, int docId, HttpServletRequest request) {
		return getNavbar(groupId, docId, request.getSession());
	}
}