<%@page import="sk.iway.iwcm.InitServlet"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.DB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Arrays"%>
<%@page import="sk.iway.iwcm.Identity"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.PathFilter"%>
<%@page import="java.util.List"%>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@page pageEncoding="utf-8" import="sk.iway.iwcm.system.*,sk.iway.iwcm.admin.layout.*"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><iwcm:checkLogon admin="true"/><%!

private static String replaceV9MenuLink(String link, HttpSession session) {
	String v9link = sk.iway.iwcm.admin.layout.MenuService.replaceV9MenuLink(link);
	if (Tools.isNotEmpty(v9link)) return v9link;
	return link;
}

private static List<ModuleInfo> sortItemsByName(List<ModuleInfo> customItems, Prop prop)
{
	//zoradenie modulov podla nazvov v danom jazyku bez ohladu na diakritiku a male pismena
		Collections.sort(customItems, new Comparator(){

			Prop prop = Prop.getInstance();

			public int compare(Object o1, Object o2)
			{
				ModuleInfo m1 = (ModuleInfo) o1;
				ModuleInfo m2 = (ModuleInfo) o2;

				String localName1 = DB.internationalToEnglish((prop.getText(m1.getLeftMenuNameKey())).toLowerCase());
				String localName2 = DB.internationalToEnglish((prop.getText(m2.getLeftMenuNameKey())).toLowerCase());

				return (localName1).compareTo(localName2);
			}
		});

	return customItems;
}

private static List<ModuleInfo> getModulesWithoutOwnTab(Identity iwcm_useriwcm, String group)
{
	List<ModuleInfo> modules = Modules.getInstance().getUserMenuItems(iwcm_useriwcm);
	List<String> notModules = Arrays.asList(new String[]{
				"/admin/listgroups.do","/admin/listtemps.do","/admin/listusers.do",
				"/admin/fbrowser.browse.do","/admin/conf_editor.jsp"});
	List<ModuleInfo> storage = new ArrayList<ModuleInfo>();
	for (ModuleInfo module : modules)
	{
	   if (group.equals(module.getGroup())) continue;

		if (!notModules.contains(module.getLeftMenuLink()))
			storage.add(module);
	}

	return storage;
}

private static List<ModuleInfo> filterNoLink(List<ModuleInfo> list, Identity user)
{
	List<ModuleInfo> filtered = new ArrayList<ModuleInfo>();

	if (list == null) return filtered;

	for (ModuleInfo m : list)
	{
		if (Tools.isEmpty(m.getLeftMenuLink())) continue;
		if (m.getLeftMenuLink().indexOf("javascript:void()")!=-1) continue;

		if (Tools.isNotEmpty(m.getItemKey()) && user.isDisabledItem(m.getItemKey())) continue;

		filtered.add(m);
	}

	return filtered;
}

private static String getCurrentPathBestMatch(List<ModuleInfo> customItems, String currentPath, Identity user)
{
	if ("/admin/".equals(currentPath)) return currentPath;
	if ("/admin/index.jsp".equals(currentPath)) return "/admin/";

	String bestMatch = null;
	String bestMatchNoParams = null;
	String currentPathNoParams = currentPath;
	int index = currentPathNoParams.indexOf("?");
	if (index!=-1) currentPathNoParams = currentPathNoParams.substring(0, index);
	//System.out.println("currentPathNoParams: " + currentPathNoParams);

	//najskor prever ci mame presnu zhodu aj s parametrami
	for (ModuleInfo m : customItems)
	{
		if (m.getSubmenus()!=null && m.getSubmenus().size()>0)
		{
			for (ModuleInfo sub : m.getSubmenus())
			{
				if (sub.getLeftMenuLink().equals(currentPath)) bestMatch = currentPath;
				if (sub.getLeftMenuLink().startsWith(currentPathNoParams) && (bestMatchNoParams == null || (sub.getLeftMenuLink().length() > bestMatchNoParams.length()))) bestMatchNoParams = sub.getLeftMenuLink();
			}
		}
	}

	if (bestMatch != null) return bestMatch;
	if (bestMatchNoParams != null) return bestMatchNoParams;

	if (bestMatch == null && bestMatchNoParams == null && currentPath != null && currentPath.startsWith("/components/"))
	{
		//asi nejaka podstranka, skus najst best match podla adresara
		String dirPath = currentPath.substring(0, currentPath.lastIndexOf("/")+1);
		//System.out.println("layout_sidebar.getCurrentPathBestMatch, currentPath="+currentPath+" dirPath="+dirPath);

		for (ModuleInfo m : customItems)
		{
			if (m.getSubmenus()!=null && m.getSubmenus().size()>0)
			{
				for (ModuleInfo sub : m.getSubmenus())
				{
					if (sub.getLeftMenuLink().startsWith(dirPath)) return sub.getLeftMenuLink();
				}
			}

			if (m.getLeftMenuLink().startsWith(dirPath)) return m.getLeftMenuLink();
		}
	}

	//System.out.println("layout_sidebar.getCurrentPathBestMatch, returning currentPath="+currentPath);

	return currentPath;
}

private static String renderLeftMenu(String group, List<ModuleInfo> customItems, String currentPath, Identity user, Prop prop, HttpSession session)
{
	StringBuilder html = new StringBuilder();

	if ("components".equals(group))
	{
		int modulesCount = getModulesWithoutOwnTab(user, group).size();
		//System.out.println("modulesCount="+modulesCount);
		if (modulesCount < 1) return "";

		List<ModuleInfo> sorted = new ArrayList<ModuleInfo>();
		sorted.addAll(customItems);
		customItems = sortItemsByName(sorted, prop);
	}

	String menuClass = "";
	if ("welcome".equals(group)) menuClass="start";

	boolean isAnythinkActive = false;

	if ("users".equals(group))
	{
		int modulesCount = getModulesWithoutOwnTab(user, group).size();
		//System.out.println("modulesCount="+modulesCount);
		if (modulesCount < 1) return "";
	}

	String currentPathBestmatch = getCurrentPathBestMatch(customItems, currentPath, user).trim();
	//System.out.println("Rendering left menu, currentPath: "+currentPath+" bestMatch: "+currentPathBestmatch);

	boolean found = false;
	for (ModuleInfo m : customItems)
	{
		if (group.equals(m.getGroup())==false) continue;
		found = true;

		//toto vo WJ8 neexistovalo, preto to nemozeme zobrazit
		if ("cmp_media".equals(m.getItemKey()) || "editor_edit_perex".equals(m.getItemKey())) continue;

		List<ModuleInfo> submenus = filterNoLink(m.getSubmenus(user), user);

		//System.out.println("submenus ("+group+")="+submenus+" mi="+m);

		StringBuilder subMenus = new StringBuilder();
		boolean isSubmenuActive = false;

		if (submenus.size()>1)
        {
			subMenus.append("\n		<ul class=\"sub-menu\">\n");
	        for (ModuleInfo sub : submenus)
	        {
				if (sub.getLeftMenuLink().equals(currentPathBestmatch))
				{
					isSubmenuActive = true;
				}

				if (m.getHideSubmenu() || (Tools.isNotEmpty(sub.getLeftMenuLink()) && sub.getLeftMenuLink().contains("javascript:void") || sub.getLeftMenuLink().contains("javascript:;"))) {
					continue;
				}

				//if (Tools.isNotEmpty(sub.getItemKey()) && user.isDisabledItem(sub.getItemKey())) continue;

	      	    subMenus.append("\n			<li");

	      	 	if (sub.getLeftMenuLink().equals(currentPathBestmatch))
				{
					subMenus.append(" class=\"active\"");
				}

	      	   //System.out.println("TESTING MENU: "+sub.getLeftMenuLink()+" vs "+currentPath);

	      	   subMenus.append("><a id=\"menuXX\" href=\"");
	      	   subMenus.append(replaceV9MenuLink(sub.getLeftMenuLink(), session));
	      	   subMenus.append("\">");
	      	   subMenus.append(prop.getText(sub.getLeftMenuNameKey()));
	      	   subMenus.append("</a></li>");
	        }
	        subMenus.append("\n		</ul>");
        }

		html.append("\n	<li class=\"").append(menuClass);
		//System.out.println(m.getLeftMenuLink()+" vs "+currentPathBestmatch+ (m.getLeftMenuLink().trim().equals(currentPathBestmatch)));
		if (isSubmenuActive || m.getLeftMenuLink().trim().equals(currentPathBestmatch))
		{
			//System.out.println("ACTIVE");
			html.append(" active");
			isAnythinkActive = true;
		}
		html.append("\">\n		<a href=\"").append(replaceV9MenuLink(m.getLeftMenuLink(), session)).append("\"");
		if (Tools.isNotEmpty(m.getMenuIcon())) html.append(" class=\"hasIcon\"");
		html.append(">");
		if (Tools.isNotEmpty(m.getMenuIcon())) html.append("\n			<i class=\"icon-").append(m.getMenuIcon()).append("\"></i>");
		html.append("\n			<span class=\"title\">");
		html.append(prop.getText(m.getLeftMenuNameKey()));
        html.append("</span>");

        if (submenus.size()>0 && !m.getHideSubmenu())
        {
      	  html.append("<span class=\"arrow");
      	  if (isAnythinkActive && isSubmenuActive) html.append(" open");
      	  html.append("\"></span>");
        }

        html.append("\n		</a>");

        if (!m.getHideSubmenu()) {
        	html.append(subMenus);
        }

        html.append("\n	</li>\n");
	}

	if (found && ("components".equals(group) || "config".equals(group) || "users".equals(group)))
	{
		//System.out.println("html.length()="+html.length());

		if ("config".equals(group) || "components".equals(group))
		{
			//System.out.println("------> HTML: "+html.toString());
			if (html.length() < 10) return "";
		}

		StringBuilder htmlPrepend = new StringBuilder();
		htmlPrepend.append("\n<li");
		if (isAnythinkActive) htmlPrepend.append(" class=\"active\"");
		htmlPrepend.append(">\n   <a href=\"javascript:;\">");
		htmlPrepend.append("      <i class=\"icon-");

		if ("components".equals(group)) htmlPrepend.append("present");
		else if ("users".equals(group)) htmlPrepend.append("users");
		else  htmlPrepend.append(prop.getText("settings"));

		htmlPrepend.append("\"></i><span class=\"title\">");

		if ("components".equals(group)) htmlPrepend.append(prop.getText("components.modules.title"));
		else if ("users".equals(group)) htmlPrepend.append(prop.getText("menu.users"));
		else  htmlPrepend.append(prop.getText("menu.config"));

		htmlPrepend.append("</span><span class=\"arrow");
		if (isAnythinkActive) htmlPrepend.append(" open");
		htmlPrepend.append("\"></span></a>\n\n");

		htmlPrepend.append("<ul class=\"sub-menu\">");

		html.insert(0, htmlPrepend);


		html.append("\n		</ul>");

		html.append("\n\n\n</li>");
	}

	return html.toString();

}
%><%

	MenuService menuService = new MenuService(request);
	List<MenuBean> menu = menuService.getMenu();

	%>
	<div class="page-sidebar-wrapper">
		<div class="page-sidebar ly-sidebar">
			<div class="md-large-menu">
				<div class="md-large-menu__wrapper clearfix">
					<% for (MenuBean menuItem : menu) { pageContext.setAttribute("menuItem", menuItem); %>
						<div class='<%= menuItem.isActive() ? "md-large-menu__item md-large-menu__item--open md-large-menu__item--active" : "md-large-menu__item"%>'>
							<div class="md-large-menu__item__headline">${menuItem.text}</div>
							<a class="md-large-menu__item__link" href="${menuItem.href}" data-menu-id="${menuItem.group}">
								<i class="${menuItem.icon}"></i>
							</a>
						</div>
					<% } %>
				</div>
			</div>

			<div class="autoscroller">
				<div class="page-sidebar-menu menu-wrapper" data-auto-scroll="false" data-slide-speed="200">
					<% for (MenuBean menuItem : menu) { pageContext.setAttribute("menuItem", menuItem); %>
						<div data-menu-id="${menuItem.group}" class='<%= menuItem.isActive() ? "md-main-menu md-main-menu--open" : "md-main-menu"%>'>
							<% for (MenuBean subMenuItem : menuItem.getChildrens()) { pageContext.setAttribute("subMenuItem", subMenuItem); %>
								<div class="md-main-menu__item" class="<%
											out.print(subMenuItem.isActive() ? "md-main-menu__item md-main-menu__item--active" : "md-main-menu__item");
											if (!subMenuItem.getChildrens().isEmpty()) out.print("md-main-menu__item--has-children");
											%>">
									<a class="md-main-menu__item__link" href="${subMenuItem.href}">
										<i class="${subMenuItem.icon}"></i>
										${subMenuItem.text}
									</a>
									<% if (!subMenuItem.getChildrens().isEmpty()) { %>
										<div class="md-main-menu__item__sub-menu">
											<% for (MenuBean thirdMenuItem : subMenuItem.getChildrens()) { pageContext.setAttribute("thirdMenuItem", thirdMenuItem); %>
												<div class="md-main-menu__item__sub-menu__item<%
													if (thirdMenuItem.isActive()) out.print(" md-main-menu__item__sub-menu__item--active");
													%>">
													<a class="md-main-menu__item__sub-menu__item__link" href="${thirdMenuItem.href}">
														${thirdMenuItem.text}
													</a>
												</div>
											<% } %>
										</div>
									<% } %>
								</div>
							<% } %>
						</div>
					<% } %>
				</div>
			</div>

			<%
				if (InitServlet.isTypeCloud()==false && Constants.getBoolean("enableStaticFilesExternalDir"))
				{
					List<String> userDomains = GroupsDB.getInstance().getUserRootDomainNames(UsersDB.getCurrentUser(request));
					request.setAttribute("userDomains", userDomains);
					request.setAttribute("actualDomain", DocDB.getDomain(request));
					%>
					<script type="text/javascript">
						function changeDomain()
						{
							if (confirm("<iwcm:text key='admin.top.domena.confirm'/>")) {
								$.ajax({method: "POST", url: "/admin/skins/webjet8/change_domain_ajax.jsp", data: {domain: $("#actualDomainNameSelectId").val()}}).done(function(msg) {location.reload();});
							}
						}
					</script>
					<div class="js-domain-toggler" id="actualDomainNameDiv" <% if (userDomains.size()<1) out.print("style='display: none;'"); %>>
						<i class="ti ti-device-desktop"></i>
						<select  name="actualDomainNameSelect" id="actualDomainNameSelectId" onchange="changeDomain();">
						<c:forEach items="${userDomains}" var="domain">
							<option value="${domain}" <c:if test="${domain == actualDomain}">selected="selected"</c:if>>${domain}</option>
						</c:forEach>
						</select>
					</div>
					<%
				}
			%>

			<div style="display: none;" id="refresherDataDiv"></div>
		</div>
	</div>


	<script type="text/javascript">

		$(function () {

			// =======================
			// MENU EVENTS INIT
			// =======================


			$(".md-main-menu__item__link").on("click", function (e) {

				if ($(this).siblings(".md-main-menu__item__sub-menu").length) {
					e.preventDefault();

					//find first child A element and click on it
					var firstChild = $(this).parent().find(".md-main-menu__item__sub-menu a");
					if (firstChild.length>0) {
						firstChild[0].click();
						return;
					}

					//failsafe if no child A element found
					$(this).parent(".md-main-menu__item").toggleClass('md-main-menu__item--open');
				}
			});

			$(".md-large-menu__item__link").on("click", function (e) {

				if ($(this).parent(".md-large-menu__item").hasClass("md-large-menu__item--active")) {
					e.preventDefault();
				} else {

					$(".md-large-menu__item").not($(this).parent(".md-large-menu__item")).removeClass("md-large-menu__item--active");
					$(this).parent(".md-large-menu__item").addClass("md-large-menu__item--active");

					var menuId = $(this).attr("data-menu-id");

					$(".md-main-menu").removeClass("md-main-menu--open");
					$('.md-main-menu[data-menu-id="' + menuId + '"]').addClass("md-main-menu--open");
					$(".md-main-menu__item").removeClass("md-main-menu__item--open");
				}

			});

			var current = "<%
				String currentUrl = PathFilter.getOrigPath(request);
				currentUrl = MenuService.replaceV9MenuLink(currentUrl);
				currentUrl = Tools.replace(currentUrl, "-details", "");
				currentUrl = Tools.replace(currentUrl, "-detail", "");
				out.print(currentUrl);
			%>";
			var currentWithQs = "<%
				String qs = (String)request.getAttribute("path_filter_query_string");
				if (qs == null) qs = "";
				else qs = sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(qs);
				out.print(currentUrl+"?"+qs);
			%>";
			if (current.endsWith("/") === false && current.indexOf(".") === -1) {
				current += "/";
			}
			var currentWithHash = currentWithQs+window.location.hash;
			currentWithHash = currentWithHash.replace("?#", "#");
			//console.log("current=", current, "currentWithQs=", currentWithQs, "currentWithHash=", currentWithHash);
			$('.md-main-menu__item__link, .md-main-menu__item__sub-menu__item__link').each(function () {
				var $this = $(this);

				//console.log("Comparing: this=", $this.attr('href'), "current=", current, " eq=", ($this.attr('href')==current));
				if ($this.attr('href') === current || $this.attr('href') === currentWithQs || $this.attr('href') === currentWithHash) {
					$this.parents(".md-main-menu__item__sub-menu__item").addClass("md-main-menu__item__sub-menu__item--active");

					let mainMenuItem = $this.parents(".md-main-menu__item");
					mainMenuItem.addClass("md-main-menu__item--active");
					//mainMenuItem.addClass("md-main-menu__item--open");

					let mainMenu = $this.parents(".md-main-menu");
					mainMenu.addClass("md-main-menu--open");
					let menuId = mainMenu.data("menu-id");
					$('.md-large-menu__item__link[data-menu-id="' + menuId + '"]').parents(".md-large-menu__item").addClass("md-large-menu__item--open md-large-menu__item--active");

					let mainTitle = mainMenuItem.find(".md-main-menu__item__link").text();

					//move submenu items to tabs
					let tabs = mainMenuItem.find(".md-main-menu__item__sub-menu div");
					//console.log("tabs=", tabs);
					if (tabs.length>0) {
						//iterate over all tabs, find A elements and create UL - LI structure
						let ul = $("<ul class='nav' role='tablist'></ul>");
						tabs.each(function() {
							let $this = $(this);
							let aOriginal = $this.find("a");
							let active = $this.hasClass("md-main-menu__item__sub-menu__item--active") ? " active" : "";
							let li = $("<li class='nav-item' role='presentation'></li>");
							let a = $("<a class='nav-link"+active+"' role='tab'>"+aOriginal.text()+"</a>");
							a.attr("href", aOriginal.attr("href"));
							li.append(a);
							ul.append(li);

							if (" active"===active) {
								hideFirstBreadcrumbItem(a, mainTitle);
							}
						});
						//wrap UL with md-tabs div
						ul = $("<div class='md-tabs md-tabs-dropdown'></div>").append(ul);

						//append UL to main menu
						$(".ly-submenu").html(ul);
						$("body").addClass("ly-submenu-active");
					}

					//set main title
					let $headerTitle = $(".header-title");
					$headerTitle.text(mainTitle);
					hideFirstBreadcrumbItem($headerTitle, mainTitle);

					//handle tabs click - we need also to execute link so it cant be BS tabs
					$(".ly-submenu .md-tabs li").on("click", "a", function(e) {
						let $this = $(this);
						let isActive = $this.hasClass("active");
						$this.parents(".md-tabs").find("li a.active").removeClass("active");
						if (isActive) {
							$this.addClass("active");
							//this is click on active tab burger menu on small device, prevent click, just open/close menu
							if ($this.closest('ul').css("position")=="relative") {
								$this.closest('ul').toggleClass("open");
								e.preventDefault();
							}
						} else {
							$this.addClass("active");
							$this.closest('ul').removeClass("open");
						}
					});
				}
			});

			//scrolll selected left menu item into view
			let $menuItem = $("div.md-main-menu__item--active");
			if ($menuItem.length>0) {
				let top = $menuItem.position().top;
				if (top > 150) {
					setTimeout(()=> {
						//scrollbarMenu.scrollTop = top-22;
						$(".page-sidebar-menu").slimScroll({
							scrollToY: (top-52)+'px'
						});
					}, 50);
				}
			}

			$('.md-main-menu__item').each(function () {
				var $this = $(this);
				var hasSomeV9 = false;
				$this.find("a").each(function () {
					if ($(this).attr('href').indexOf("/v9") !== -1 || $(this).attr('href').indexOf("/apps") === 0) {
						hasSomeV9 = true;
					}
				});
			});

			//ak ma ul.nav-tabs schovaj title
			if ($("ul.nav-tabs").length>0) {
				$(".row.title").addClass("title-hidden");
				$("ul.nav-tabs").addClass("title-hidden");
			}

			$(".page-sidebar .menu-wrapper a").on("click", function () {
				var $this = $(this);
            	if ($this.parent().find(".md-main-menu__item__sub-menu").length>0) return;
				var href = $(this).attr("href");
				if (href.indexOf("javascript:") === 0) return;
				WJ.selectMenuItem(href);
			});

			WJ.openPopupDialog = function(url) {
				openPopupDialogFromLeftMenu(url);
			}
			WJ.selectMenuItem = function(href) {
				//oznaci menu polozku podla zadaneho href atributu
				$(".md-large-menu__wrapper .md-large-menu__item").removeClass("md-large-menu__item--open md-large-menu__item--active");
				$("div.page-sidebar-menu div.md-main-menu--open").removeClass("md-main-menu--open");
				$("div.page-sidebar-menu div.md-main-menu__item--open").removeClass("md-main-menu__item--open md-main-menu__item--active");
				$("div.page-sidebar-menu div.md-main-menu__item__sub-menu__item--active").removeClass("md-main-menu__item__sub-menu__item--active");

				var $this = $("a[href='"+href+"']");

				$this.parents(".md-main-menu__item__sub-menu__item").addClass("md-main-menu__item__sub-menu__item--active");

				$this.parents(".md-main-menu__item").addClass("md-main-menu__item--active");
				//$this.parents(".md-main-menu__item").addClass("md-main-menu__item--open");

				$this.parents(".md-main-menu").addClass("md-main-menu--open");
				var menuId = $this.parents(".md-main-menu").data("menu-id");
				$('.md-large-menu__item__link[data-menu-id="' + menuId + '"]').parents(".md-large-menu__item").addClass("md-large-menu__item--open md-large-menu__item--active");
			}
		});

		function hideFirstBreadcrumbItem(a, mainTitle) {
			//if there is element in .md-breadcrumb .nav .nav-item .nav-link with same name hide it
			let breadcrumb = $(".ly-content .md-breadcrumb .nav .nav-item");
			let hidenCount = 0;
			breadcrumb.each(function() {
				let $this = $(this);
				//console.log("Comparing: this=", $this.text(), "a=", a.text());
				if ($this.text()==a.text() || $this.text()==mainTitle) {
					$this.hide();
					hidenCount++;
				}
			});
			if (breadcrumb.length===hidenCount) {
				//console.log("Hiding whole breadcrumb, length=", breadcrumb.length, "hidenCount=", hidenCount);
				//if only one element in breadcrumb hide whole breadcrumb
				breadcrumb.first().parents(".md-breadcrumb").hide();
			}
		}

		function openPopupDialogFromLeftMenu(url)
		{
			var options = "status=no,menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=450,height=200";
			var popupwindow = null;
			popupwindow=window.open(url, "", options);
			if (window.focus && popupwindow!=null)
			{
				popupwindow.focus();
			}
		}

	</script>
