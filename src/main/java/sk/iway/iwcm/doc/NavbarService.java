package sk.iway.iwcm.doc;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import org.apache.commons.lang.StringUtils;

/**
 * Service for generating navbar (breadcrumb navigation)
 * Dispatches to appropriate implementation based on navbarDefaultType configuration
 */
public class NavbarService {

    public String getNavbar(DocDetails doc, HttpServletRequest request) {

        String navbar2;
        String navbarDefaultType = Constants.getString("navbarDefaultType");

        // Get appropriate navbar implementation
        NavbarInterface navbarImpl = getNavbarImplementation(navbarDefaultType);

        if (navbarImpl != null)
        {
            navbar2 = navbarImpl.getNavbar(doc.getGroupId(), doc.getDocId(), request);
        }
        else
        {
            // Fallback to standard implementation
            navbarImpl = new NavbarStandard();
            navbar2 = navbarImpl.getNavbar(doc.getGroupId(), doc.getDocId(), request);
        }

        String navbar;
        if (navbar2.length() > 2)
        {
            navbar = navbar2;
            try {
                GroupsDB groupsDB = GroupsDB.getInstance();
                DocDB docDB = DocDB.getInstance();
                //najskor zisti ako je na tom adresar
                GroupDetails group = groupsDB.getGroup(doc.getGroupId());

                //ak to nie je default doc pre grupu tak sprav linku
                if (group != null && doc.getDocId() != group.getDefaultDocId() && doc.isShowInNavbar(request))
                {
                    if (doc.getNavbar().length() > 2)
                    {
                        if ("rdf".equalsIgnoreCase(navbarDefaultType) && navbar.indexOf("</div>")!=-1)
                        {
                            navbar = navbar.substring(0, navbar.length()-6) + " " + Constants.getString("navbarSeparator")+" <span>"+Tools.convertToHtmlTags(doc.getNavbar())+"</span></div>";
                        }
                        else if ("schema.org".equalsIgnoreCase(navbarDefaultType))
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
            } catch (Exception e) {
                Logger.error(NavbarService.class, "Error while generating navbar", e);
            }
        } else {
            navbar = doc.getNavbar();
        }

        return navbar;
    }

	/**
	 * Get appropriate navbar implementation based on navbarDefaultType
	 * @param navbarDefaultType - value from Constants.getString("navbarDefaultType")
	 * @return NavbarInterface implementation or null if not found
	 */
	private NavbarInterface getNavbarImplementation(String navbarDefaultType) {
		if (Tools.isEmpty(navbarDefaultType) || "normal".equalsIgnoreCase(navbarDefaultType)) {
			return new NavbarStandard();
		}

		if ("rdf".equalsIgnoreCase(navbarDefaultType)) {
			return new NavbarRDF();
		}

		if ("schema.org".equalsIgnoreCase(navbarDefaultType)) {
			return new NavbarSchemaOrg();
		}

		// Try to load custom implementation
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
}