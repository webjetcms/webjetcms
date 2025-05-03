package sk.iway.iwcm.admin.layout;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.PermissionGroupDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * MenuService - generovanie menu pre admin cast z dat v objekte Modules
 */
public class MenuService {

    List<MenuBean> menu;

    Identity user;
    Prop prop;
    String lng;
    List<ModuleInfo> customItems;

    //mapovanie mi.getItemKey na skupinu vo WJ9
    static final Map<String, String> groupsMap;
    //mapovanie mi.getItemKey na ikonu vo WJ9
    static final Map<String, String> iconsMap;
    //mapovanie mi.getItemKey na Sort order
    static final Map<String, Integer> menuOrderMap;
    //mapovanie mi.getNameKey na mapu
    static final Map<String, String> leftMenuNameKeyMap;

    //mapovanie starej a novej URL
    static final Map<String, String> menuLinkReplaces;

    static {
        groupsMap = new HashMap<>();
        iconsMap = new HashMap<>();
        menuOrderMap = new HashMap<>();
        menuLinkReplaces = new HashMap<>();
        leftMenuNameKeyMap = new HashMap<>();
        String[][] groups = {
            //welcome
            { "cmp_stat", "welcome" },
            { "cmp_server_monitoring", "welcome" },
            { "cmp_adminlog", "welcome" },
            { "menuMessages", "welcome" },
            //website
            { "cmp_clone_structure", "website" },
            { "cmp_attributes", "website" },
            { "cmp_redirects", "website" },
            { "export_offline", "website" },
            //news
            { "cmp_blog", "news" },
            { "cmp_news", "news" },
            { "cmp_diskusia", "news" },
            //files
            { "cmp_file_archiv", "files" },
            { "make_zip_archive", "files" }
        };
        for (String[] pair : groups) {
            groupsMap.put(pair[0], pair[1]);
        }
        String[][] icons = {
            //welcome
            { "welcome", "ti ti-home" },
            { "cmp_stat", "ti ti-chart-area-line" },
            { "cmp_server_monitoring", "ti ti-device-heart-monitor" },
            { "cmp_adminlog", "ti ti-shield-search" },
            { "cmp_in-memory-logging", "ti ti-player-skip-back" },
            { "cmp_adminlog_logging", "ti ti-files" },
            { "menuMessages", "ti ti-mail-opened" },
            { "welcomeShowLoggedAdmins", "ti ti-user-shield" },
            //website
            { "menuWebpages", "ti ti-file-text" },
            { "cmp_clone_structure", "ti ti-copy" },
            { "cmp_attributes", "ti ti-row-insert-bottom" },
            { "cmp_redirects", "ti ti-external-link" },
            { "export_offline", "ti ti-file-zip" },
            { "editorMiniEdit", "ti ti-filter" },
            { "editorFullMenu", "ti ti-box-padding" },
            { "addPage", "ti ti-plus" },
            { "pageSave", "ti ti-device-floppy" },
            { "pageSaveAs", "ti ti-copy" },
            { "deletePage", "ti ti-trash" },
            { "editDir", "ti ti-pencil" },
            { "addSubdir", "ti ti-plus" },
            { "deleteDir", "ti ti-trash" },
            { "editor_edit_perex", "ti ti-hash" },
            { "editor_show_hidden_folders", "ti ti-folders" },
            { "cmp_sync", "ti ti-refresh" },
            { "imageEditor", "ti ti-polaroid" },
            //news
            { "cmp_blog", "ti ti-article" },
            { "cmp_news", "ti ti-news" },
            { "cmp_diskusia", "ti ti-messages" },
            { "components.news.edit_templates", "ti ti-stack-2" },
            //apps
            { "cmp_abtesting", "ti ti-a-b" },
            { "menuEmail", "ti ti-mail" },
            { "cmp_calendar", "ti ti-calendar-month" },
            { "cmp_form", "ti ti-forms" },
            { "menuInquiry", "ti ti-chart-histogram" },
            { "menuQa", "ti ti-help" },
            { "menuGallery", "ti ti-photo" },
            { "menuGDPR", "ti ti-shield-half" },
            { "menuGDPRDelete", "ti ti-trash" },
            { "menuGDPRregexp", "ti ti-devices-search" },
            { "menuBanner", "ti ti-ad" },
            { "cmp_basket", "ti ti-shopping-bag" },
            { "cmp_enumerations", "ti ti-table" },
            { "cmp_proxy", "ti ti-network" },
            { "cmp_reservation", "ti ti-calendar-check" },
            { "cmp_restaurant_menu", "ti ti-tools-kitchen-2" },
            { "cmp_seo", "ti ti-seo" },
            { "cmp_tooltip", "ti ti-help" },
            { "cmp_export", "ti ti-database-export" },
            { "cmp_quiz", "ti ti-message-circle-question" },
            { "cmp_calendar_approve", "ti ti-thumb-up" },
            { "cmp_banner_add", "ti ti-plus" },
            { "cmp_banner_seeall", "ti ti-notes" },
            { "cmp_insert_script", "ti ti-code" },
            { "cmp_menu", "ti ti-dots-vertical" },
            { "sharedIcons", "ti ti-share-2" },
            { "cmp_content-block", "ti ti-triangle-square-circle" },
            //files
            { "menuFbrowser", "ti ti-folders" },
            { "cmp_file_archiv", "ti ti-book-2" },
            { "make_zip_archive", "ti ti-database-export" },
            { "fbrowser_delete_directory", "ti ti-trash" },
            { "menuFileArchivExportFiles", "ti ti-upload" },
            { "menuFileArchivImportFiles", "ti ti-download" },
            { "menuFileArchivManagerCategory", "ti ti-subtask" },
            { "cmp_fileArchiv_edit_del_rollback", "ti ti-pencil" },
            { "cmp_fileArchiv_advanced_settings", "ti ti-tool" },
            { "editor_unlimited_upload", "ti ti-infinity" },
            //templates
            { "menuTemplates", "ti ti-template" },
            { "menuTemplatesGroup", "ti ti-layers-subtract" },
            //users
            { "menuUsers", "ti ti-users" },
            { "user.admin.userGroups", "ti ti-users-group" },
            { "users.perm_groups", "ti ti-user-shield" },
            { "users.edit_admins", "ti ti-shield-cog" },
            { "users.edit_public_users", "ti ti-users-group" },
            //config
            { "menuConfig", "ti ti-adjustments" },
            { "edit_text", "ti ti-language" },
            { "cmp_crontab", "ti ti-settings-automation" },
            { "modUpdate", "ti ti-cloud-download" },
            { "modRestart", "ti ti-power" },
            { "conf.show_all_variables", "ti ti-user-shield" },
            { "prop.show_all_texts", "ti ti-user-shield" },
            { "replaceAll", "ti ti-arrows-left-right" }
        };
        for (String[] pair : icons) {
            iconsMap.put(pair[0], pair[1]);
        }
        String[][] order = {
            //welcome
            { "cmp_adminlog", "7041" },
            //website
            { "cmp_redirects", "2020" },
            { "cmp_attributes", "2050" },
            { "cmp_clone_structure", "7010" },
            //news
            { "cmp_blog", "3030" }
        };
        for (String[] pair : order) {
            menuOrderMap.put(pair[0], Tools.getIntValue(pair[1], 0));
        }
        String[][] linkReplaces = {
            //{ "", ""},
            //welcome
            { "/admin/", "/admin/v9/"},
            //apps
            { "/admin/photogallery.do?gpage=1", "/admin/v9/apps/gallery/"},
            { "/admin/photogallery.do", "/admin/v9/apps/gallery/"},
            { "javascript:WJ.openPopupDialog('/components/gallery/admin_new_dir.jsp');", ""},
            { "javascript:WJ.openPopupDialog('/components/gallery/admin_settings.jsp');", ""},
            { "/components/gallery/admin_gallery_stats.jsp", ""},
            { "/components/tooltip/admin_list.jsp", "/apps/tooltip/admin/"},
            { "/components/qa/admin_list.jsp", "/apps/qa/admin/"},
            { "/components/export/admin_list.jsp", "/apps/export-dat/admin/"},
            { "/components/enumerations/admin_enum_list.jsp", "/apps/enumeration/admin/"},
            { "/components/enumerations/admin_enum_type_list.jsp", "/apps/enumeration/admin/enumeration-type/"},
            { "/components/banner/banner_stat.jsp", "/apps/banner/admin/banner-stat/"},
            { "/components/proxy/admin_list.jsp", "/apps/proxy/admin/"},
            { "/components/news/admin_news_list.jsp", "/apps/news/admin/"},
            { "/components/quiz/admin_list.jsp", "/apps/quiz/admin/"},
            { "/components/restaurant_menu/admin_list_meals.jsp", "/apps/restaurant-menu/admin/meals/"},
            { "/components/restaurant_menu/admin_new_menu.jsp", "/apps/restaurant-menu/admin/"},
            { "/components/restaurant_menu/admin_list_menu.jsp", ""},

            //dmail
            { "/components/dmail/admin_campaigns.jsp", "/apps/dmail/admin/"},
            { "javascript:WJ.openPopupDialog('/admin/email.do');", ""},
            { "/components/dmail/admin-domainlimits-list.jsp", "/apps/dmail/admin/domain-limits/"},
            { "/components/dmail/admin_unsubscribed.jsp", "/apps/dmail/admin/unsubscribed/"},
            { "javascript:WJ.openPopupDialog('/components/dmail/admin-domainlimits-edit.jsp');", ""},
            { "/components/inquiry/admin_inquiry_list.jsp", "/apps/inquiry/admin/"},

            //calendar
            { "/admin/listevents.do", "/apps/calendar/admin/"},
            { "/components/calendar/admin_edit_type.jsp", "/apps/calendar/admin/calendar-types/"},
            { "/components/calendar/admin_neschvalene_udalosti.jsp", "/apps/calendar/admin/non-approved-events/"},
            { "/components/calendar/admin_suggest_evens.jsp", "/apps/calendar/admin/suggest-events/"},

            { "/components/insert_script/admin_insert_script_list.jsp", "/admin/v9/apps/insert-script/"},
            //ovladaci panel
            { "/components/adminlog/adminlog.jsp", "/admin/v9/apps/audit-search/"},
            { "/components/adminlog/adminlog_notify_list.jsp", "/admin/v9/apps/audit-notifications/"},
            { "/admin/conf_editor.jsp", "/admin/v9/settings/configuration/"},
            { "/components/crontab/crontab_list.jsp", "/admin/v9/settings/cronjob/"},
            { "javascript:WJ.openPopupDialog('/components/crontab/crontab_edit.jsp?crontabId=-1');", ""},
            { "/components/adminlog/web_pages.jsp", "/admin/v9/apps/audit-changed-webpages/"},
            { "/components/adminlog/adminlog_wait_publishing.jsp", "/admin/v9/apps/audit-awaiting-publish-webpages/"},

            { "/components/redirects/admin_list.jsp?url=&kategoria=1&searchSubmit=", "/admin/v9/settings/redirect/"},
            { "javascript:WJ.openPopupDialog('/components/redirects/admin_edit.jsp?urlRedirect.urlRedirectId=-1');", ""},
            { "/components/redirects/admin_domainredir_list.jsp?url=&kategoria=1&searchSubmit=", "/admin/v9/settings/domain-redirect/"},
            { "javascript:WJ.openPopupDialog('/components/redirects/admin_domainredir_edit.jsp?urlRedirect.urlRedirectId=-1');", ""},

            { "/components/data_deleting/admin_deleting_stats.jsp", "/admin/v9/settings/database-delete/"},
            { "/components/data_deleting/admin_deleting_emails.jsp", "/admin/v9/settings/database-delete/"},
            { "/components/data_deleting/admin_deleting_history.jsp", "/admin/v9/settings/database-delete/"},
            { "/components/data_deleting/admin_deleting_monitoring.jsp", "/admin/v9/settings/database-delete/"},
            { "/components/data_deleting/admin_deleting_audit.jsp", "/admin/v9/settings/database-delete/"},
            { "/components/data_deleting/admin_deleting_cache.jsp", "/admin/v9/settings/cache-objects/"},
            { "/components/data_deleting/admin_deleting_persistentcache.jsp", "/admin/v9/settings/persistent-cache-objects/"},
            { "/components/data_deleting/admin_deleting_imgcache.jsp", ""},
            { "/admin/prop_search.jsp", "/admin/v9/settings/translation-keys/"},
            { "javascript:WJ.openPopupDialog('/admin/prop_edit.jsp?key=&lng=&value=');", ""},
            { "/admin/prop_export-import.jsp", ""},
            { "/admin/prop_missing.jsp", "/admin/v9/settings/missing-keys/"},
            //sablony
            { "/admin/temps_group_list.jsp", "/admin/v9/templates/temps-groups-list/"},
            { "/admin/temps_list.jsp", "/admin/v9/templates/temps-list/"},
            //web stranky
            { "/admin/webpages/", "/admin/v9/webpages/web-pages-list/"},
            { "/components/attributes/admin_list.jsp", "/admin/v9/webpages/attributes/"},
            { "javascript:WJ.openPopupDialog('/components/attributes/admin_add.jsp')", ""},
            { "javascript:WJ.openPopupDialog('/components/attributes/admin_import_excel.jsp')", ""},
            //pouzivatelia
            { "/admin/listusers.do", "/admin/v9/users/user-list/"},
            { "/admin/listusers.do?groups=true", "/admin/v9/users/user-groups/"},
            { "/admin/user_management/admin_user_perm_groups.jsp", "/admin/v9/users/permission-groups/"},
            //GDPR
            { "/components/gdpr/admin_list_regexp.jsp", "/apps/gdpr/admin/regexps/"},
            { "/components/gdpr/admin_gdpr_data_deleting.jsp", "/apps/gdpr/admin/data-deleting/"},
            { "/components/gdpr/admin_cookie_list.jsp", "/apps/gdpr/admin/"},
            { "javascript:WJ.openPopupDialog('/components/gdpr/admin_add_new_cookie.jsp');", ""},
            { "/components/gdpr/admin_list.jsp", "/apps/gdpr/admin/search/"},
            //banner
            { "/components/banner/banner_editor.jsp", "/apps/banner/admin/"},
            { "javascript:WJ.openPopupDialog('/components/banner/banner_editor_popup.jsp');", ""},
            //forms
            { "/admin/formlist.do?showArchived=false", "/apps/form/admin/"},
            { "/admin/formlist.do?showArchived=true", "/apps/form/admin/archived/"},
            { "/components/form/admin_reg_exp_list.jsp", "/apps/form/admin/regexps/"},
            //Reservation
            { "/components/reservation/admin_object_list.jsp", "/apps/reservation/admin/reservation-objects/"},
            { "/components/reservation/admin_reservation_list.jsp", "/apps/reservation/admin/"},
            //stat
            {"/components/stat/stat_intro.jsp", "/apps/stat/admin/"},
            {"/components/stat/stat_days_new.jsp", "/apps/stat/admin/"},
            {"/components/stat/stat_weeks_new.jsp", ""},
            {"/components/stat/stat_months_new.jsp", ""},
            {"/components/stat/stat_hours_new.jsp", ""},
            { "/components/stat/stat_top_new.jsp", "/apps/stat/admin/top/"},
            { "/components/stat/stat_country.jsp", "/apps/stat/admin/country/"},
            { "/components/stat/stat_referer.jsp", "/apps/stat/admin/referer/"},
            { "/components/stat/stat_browser.jsp", "/apps/stat/admin/browser/"},
            { "/components/stat/stat_errors.jsp", "/apps/stat/admin/error/"},
            { "/components/stat/stat_userlogon.jsp", "/apps/stat/admin/logon-user/"},
            { "/components/stat/stat_searchengines.jsp", "/apps/stat/admin/search-engines/"},
            { "/components/stat/stat_complete.jsp", ""},
            { "/components/stat/admin_heat_map.jsp", ""},
            //seo
            { "/components/seo/admin_bots.jsp", "/apps/seo/admin/"},
            { "/components/seo/admin_management_keywords.jsp", "/apps/seo/admin/management-keywords/"},
            { "/components/seo/admin_number_keywords.jsp", "/apps/seo/admin/number-keywords/"},
            { "/components/seo/admin_positions.jsp", "/apps/seo/admin/positions/"},
            { "/components/seo/admin_stat_keywords.jsp", "/apps/seo/admin/stat-keywords/"},
            //server monitoring
            { "/components/server_monitoring/admin_basic_info.jsp", "/apps/server_monitoring/admin/"},
            { "/components/server_monitoring/admin_monitoring_all.jsp", "/apps/server_monitoring/admin/records/"},
            { "/components/server_monitoring/admin_components_monitoring.jsp", "/apps/server_monitoring/admin/components/"},
            { "/components/server_monitoring/admin_documents_monitoring.jsp", "/apps/server_monitoring/admin/documents/"},
            { "/components/server_monitoring/admin_sql_monitoring.jsp", "/apps/server_monitoring/admin/sql/"},

            //
            { "/components/forum/admin_diskusia_zoznam.jsp", "/apps/forum/admin/"},
            { "/components/blog/blog_comments.jsp", "/apps/blog/admin/"},
            { "/components/blog/blog_admin.jsp", "/apps/blog/admin/bloggers/"},

            { "/components/abtesting/admin_abtesting.jsp", "/apps/abtesting/admin/"},

            //ESHOP
            { "/components/basket/admin_payment_methods.jsp", "/apps/eshop/admin/payment-methods/"},
            { "/components/basket/admin_invoices_list.jsp", "/apps/basket/admin/"},
            { "/components/basket/admin_pricelist.jsp", "/apps/basket/admin/product-list"},

            { "/admin/elFinder/", "/admin/v9/files/index/"},
            { "/admin/elFinder/dialog.jsp", "/admin/v9/files/dialog"},
            { "/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_link.jsp", "/admin/v9/files/wj_link"},
            { "/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_image.jsp", "/admin/v9/files/wj_image"},

            {"/admin/adminlog/logging/", "/admin/v9/apps/audit-log-levels/"},

            //File archiv
            {"/components/file_archiv/file_list.jsp", "/apps/file-archive/admin/"},

            //Search
            {"/admin/searchall.jsp", "/admin/v9/search/index/"},
            {"/admin/skins/webjet6/searchall.jsp", "/admin/v9/search/index/"}
        };
        for (String[] pair : linkReplaces) {
            menuLinkReplaces.put(pair[0], pair[1]);
        }
        String[][] leftMenuNameKeyReplaces = {
            {"stat_menu.days", "components.stat.visits"}
        };
        for (String[] pair : leftMenuNameKeyReplaces) {
            leftMenuNameKeyMap.put(pair[0], pair[1]);
        }
    }

    public MenuService(HttpServletRequest request) {

        this.user = UsersDB.getCurrentUser(request);
        if (this.user == null) return;
        this.prop = Prop.getInstance(request);
        this.lng = Prop.getLng(request, false);

        //ziskaj standardny zoznam modulov
        List<ModuleInfo> allModules = Modules.getInstance().getUserMenuItems(user);
        customItems = Modules.filterDomain(allModules, DocDB.getDomain(request));

        sortModules(customItems);

        generateMenu();
    }

    /**
     * Usporiada zoznam ModuleInfo podla WJ9 menuOrder (volanim getMenuOrder)
     * @param customItems
     */
    private static void sortModules(List<ModuleInfo> customItems) {
        //usortuj ich podla noveho sortingu
        Collections.sort(customItems, new Comparator<ModuleInfo>()
        {
            @Override
            public int compare(ModuleInfo m1, ModuleInfo m2)
            {
                int order = getMenuOrder(m1) - getMenuOrder(m2);
                if (order == 0)
                {
                   order = m1.getNameKey().compareTo(m2.getNameKey());
                }
                return (order);
            }
        });
    }

    public List<MenuBean> getMenu() {
        return menu;
    }

    /**
     * Vrati skupinu pre WJ9 z povodnej skupiny
     */
    private static String getGroup9(ModuleInfo m)
    {
        String group = groupsMap.get(m.getItemKey());
        if (group == null) group = m.getGroup();

        //Logger.debug(MenuService.class, "getGroupV9, key: "+m.getItemKey()+" group:"+group+" sort:"+m.getMenuOrder());

        return group;
    }

    /**
     * Vrati ikonu pre WJ9 podla povodneho modulu
     */
    private static String getIcon9(ModuleInfo m)
    {
        return getIcon9(m.getItemKey(), m.getMenuIcon());
    }

    /**
     * Vrati ikonu pre WJ9 podla mena modulu
     * @param itemKey - kluc modulu, napr. cmp_media
     * @param defaultIcon - ikona, ked nie je definovana
     * @return
     */
    private static String getIcon9(String itemKey, String defaultIcon)
    {
        String icon = iconsMap.get(itemKey);
        if (icon == null) icon = "ti ti-"+defaultIcon;

        return icon;
    }

    /**
     * Vrati poradie usporiadania pre WJ9
     * @param m
     * @return
     */
    private static int getMenuOrder(ModuleInfo m)
    {
        Integer sortOrder = menuOrderMap.get(m.getItemKey());
        if (sortOrder != null) return sortOrder.intValue();

        return m.getMenuOrder();
    }

    /**
     * Vrati linku na kliknutie v menu pre WJ9
     * @param m
     * @return
     */
    private static String getMenuLinkV9(ModuleInfo m)
    {
        String href = m.getLeftMenuLink();
        href = Tools.replace(href, "openPopupDialogFromLeftMenu(", "WJ.openPopupDialog(");
        href = replaceV9MenuLink(href);
        return href;
    }

    /**
     * Na zaklade zadanej linky vrati upravenu linku pre WJ9
     * @param v8link
     * @return
     */
    public static String replaceV9MenuLink(String v8link) {
        String link = menuLinkReplaces.get(v8link);
        if (link != null) return link;
        return v8link;
    }

    /**
     * Na zaklade prekladoveho kluca vrati upraveny kluv pre WJ9
     * @param nameKey
     * @return
     */
    public static String getLeftMenuNameKeyV9(String nameKey) {
        String key = leftMenuNameKeyMap.get(nameKey);
        if (key != null) return key;
        return nameKey;
    }

    /**
     * Vrati zoznam korenovych menu poloziek
     * @param prop
     * @return
     */
    public static List<MenuBean> getRootItems(Prop prop) {
        List<MenuBean> roots = new ArrayList<>();

        roots.add((new MenuBean()).setGroup("welcome").setText(prop.getText("menu.root.welcome")).setIcon("ti ti-home"));
        roots.add((new MenuBean()).setGroup("website").setText(prop.getText("menu.root.websites")).setIcon("ti ti-file-text"));
        roots.add((new MenuBean()).setGroup("news").setText(prop.getText("menu.root.news")).setIcon("ti ti-edit"));
        roots.add((new MenuBean()).setGroup("components").setText(prop.getText("components.modules.title")).setIcon("ti ti-apps"));
        roots.add((new MenuBean()).setGroup("files").setText(prop.getText("menu.root.files")).setIcon("ti ti-folder-open"));
        roots.add((new MenuBean()).setGroup("templates").setText(prop.getText("menu.root.templates")).setIcon("ti ti-layout"));
        roots.add((new MenuBean()).setGroup("users").setText(prop.getText("menu.root.users")).setIcon("ti ti-users"));
        roots.add((new MenuBean()).setGroup("config").setText(prop.getText("menu.root.config")).setIcon("ti ti-settings"));

        return roots;
    }

    /**
     * Vygeneruje menu do menu objektu
     */
    private void generateMenu() {
        menu = new ArrayList<>();

        menu.addAll(getRootItems(prop));

        for (MenuBean rootItem : menu) {
            // out.println(renderLeftMenu(group, customItems, actualMenuPath, user, prop));
            addMenuItems(rootItem);
        }

        List<MenuBean> filtered = new ArrayList<>();
        //remove empty root items
        for (MenuBean rootItem : menu) {
            if (rootItem.getChildrens().isEmpty()==false) {
                filtered.add(rootItem);
            }
        }
        menu = filtered;
    }

    /**
     * K root polozke doplnit 2. a 3. uroven menu
     * @param rootItem
     */
    private void addMenuItems(MenuBean rootItem) {

        List<MenuBean> childrens = new ArrayList<>();

        for (ModuleInfo m : customItems) {
            if (rootItem.getGroup().equals(getGroup9(m)) == false) continue;

            MenuBean children = (new MenuBean())
            .setGroup(rootItem.getGroup())
            .setText(prop.getText(getLeftMenuNameKeyV9(m.getLeftMenuNameKey())))
            .setHref(getMenuLinkV9(m))
            .setIcon(getIcon9(m));
//                .setCustom(m.isCustom());

            List<MenuBean> thirdChildrens = new ArrayList<>();
            List<ModuleInfo> subMenus = filterNoLink(m.getSubmenus(user), user);

            if (subMenus.size()==1 && children.getHref().equals(getMenuLinkV9(subMenus.get(0)))) {
                //v submenu je len 1 polozka a zhoduje sa s parentom, toto nebudeme renderovat
            } else {
                for (ModuleInfo sub : subMenus) {
                    MenuBean third = (new MenuBean())
                        .setGroup(rootItem.getGroup())
                        .setText(prop.getText(getLeftMenuNameKeyV9(sub.getLeftMenuNameKey())))
                        .setHref(getMenuLinkV9(sub));

                    thirdChildrens.add(third);
                }
            }

            if (children.getHref().contains("void()") && thirdChildrens.isEmpty()) {
                //ak hlavny item ma javascript:void linku a nema ziadnych childov nezobrazime
            } else {
                childrens.add(children);
            }

            //if user doesnt have perms for children.href use first subitem from thirdChildrens
            if (thirdChildrens.isEmpty() == false && Tools.isNotEmpty(m.getItemKey()) && user.isEnabledItem(m.getItemKey())==false) {
                children.setHref(thirdChildrens.get(0).getHref());
            }

            children.setChildrens(thirdChildrens);
        }

        if (rootItem.getGroup().equals("components")) {
            //aplikacie sortni podla abecedy
            Locale loc = LayoutService.getUserLocale(lng);
            Collator coll = Collator.getInstance(loc);
            childrens = childrens.stream().sorted(Comparator.comparing(MenuBean::isCustom).reversed().thenComparing(MenuBean::getText, coll)).collect(Collectors.toList());
        }

        rootItem.setChildrens(childrens);
    }

    /**
     * Odfiltruje menu polozky, ktore obsahuju prazdne javascript:void() alebo na ktore pouzivatel nema pravo
     * @param list
     * @param user
     * @return
     */
    private static List<ModuleInfo> filterNoLink(List<ModuleInfo> list, Identity user) {
        List<ModuleInfo> filtered = new ArrayList<>();

        if (list == null)
            return filtered;

        for (ModuleInfo m : list) {
            if (Tools.isEmpty(m.getLeftMenuLink()))
                continue;
            if (m.getLeftMenuLink().contains("javascript:void()") || getMenuLinkV9(m).contains("javascript:void()"))
                continue;

            if (Tools.isNotEmpty(m.getItemKey()) && user.isEnabledItem(m.getItemKey())==false)
                continue;

            if (Tools.isEmpty(getMenuLinkV9(m))) continue;

            filtered.add(m);
        }

        return filtered;
    }

    // -------------- ZOBRAZENIE PRAV --------------------------------------------------------

    /**
     * Vrati zoznam VSETKYCH prav pre editaciu pouzivatela (vsetky prava)
     * Je to tu v MenuService nestastne, ale kedze sa robi viac operacii (zmena ikon, skupiny)
     * tak sa mi to zdalo lepsie
     * @return
     */
    public static List<JsTreeItem> getAllPermissions() {
        List<JsTreeItem> perms = new ArrayList<>();
        Prop prop = Prop.getInstance();

        List<ModuleInfo> allModules = Modules.getInstance().getAvailableModules();
        sortModules(allModules);

        //ziskaj zoznam skupin prav a sprav hashtabulku CSS tried
        List<PermissionGroupBean> permGroups = (new PermissionGroupDB()).getAll();
        HashMap<String, StringBuilder> permGroupsClasses = new HashMap<>();
        for (PermissionGroupBean group : permGroups) {
            for (String permName : group.getPermissionNames()) {
                permName = getPermsIdWithPrefix(permName);
                StringBuilder classes = permGroupsClasses.get(permName);
                if (classes == null) {
                    classes = new StringBuilder();
                    classes.append(" permgroup");
                    permGroupsClasses.put(permName, classes);
                }
                classes.append(" permgroup-").append(group.getId());
            }
        }

        List<MenuBean> roots = MenuService.getRootItems(prop);
        for (MenuBean root : roots) {
            JsTreeItem rootItem = new JsTreeItem();
            rootItem.setId(getPermsIdWithPrefix(root.getGroup())+"-leaf");
            rootItem.setText(root.getText());
            rootItem.setIcon(root.getIcon());
            rootItem.setParent("#");

            perms.add(rootItem);

            perms.addAll(getAllPermissionsChildren(rootItem, allModules, permGroupsClasses, prop));
        }

        return perms;
    }

    /**
     * Prida 2. a 3. uroven prav pre zobrazenie vsetkych prav v editacii pouzivatela
     */
    private static List<JsTreeItem> getAllPermissionsChildren(JsTreeItem rootItem, List<ModuleInfo> allModules, HashMap<String, StringBuilder> permGroupsClasses, Prop prop) {
        List<JsTreeItem> childs = new ArrayList<>();
        Set<String> checkDuplicity = new HashSet<>();

        for (ModuleInfo m : allModules) {
            if ((m.isUserItem()==false && (m.getSubmenus()==null || m.getSubmenus().isEmpty())) || m.getItemKey().length()<3) continue;

            if (rootItem.getId().equals(getPermsIdWithPrefix(getGroup9(m))+"-leaf") == false) continue;

            JsTreeItem child = new JsTreeItem();
            child.setText(prop.getText(getLeftMenuNameKeyV9(m.getLeftMenuNameKey())));
            child.setParent(rootItem.getId());
            child.setId(getPermsIdWithPrefix(m.getItemKey()));
            child.setIcon(getIcon9(m));

            childs.add(child);

            if (m.getSubmenus()!=null && m.getSubmenus().isEmpty()==false)
			{
                //pridaj subemnu polozky
                boolean hasItem = false;
                for (ModuleInfo sub : m.getSubmenus()) {
                    if (sub.isAvailable()==false || sub.isUserItem()==false || sub.getItemKey()==null || sub.getItemKey().length()<3) continue;

                    //v Zoznam web stranok preskakujeme Editacia media skupin, lebo to mame separatne ako polozku
                    if ("editor_edit_media_group".equals(sub.getItemKey()) && "menuWebpages".equals(m.getItemKey())) continue;
                    //v Zoznam web stranok preskakujeme Editacia znaciek, lebo to mame separatne ako polozku
                    if ("editor_edit_perex".equals(sub.getItemKey()) && "menuWebpages".equals(m.getItemKey())) continue;

                    //skratene menu v editore - editorMiniEdit je inverzne pravo, otacame ho na iny typ
                    if ("editorMiniEdit".equals(sub.getItemKey())) {
                        ModuleInfo subNew = new ModuleInfo();
                        subNew.setItemKey("editorFullMenu");
                        subNew.setLeftMenuNameKey("user.editorFullMenu");
                        sub = subNew;
                    }

                    if (hasItem==false) {
                        //mame aspon jednu polozku, child teda nie je pouzivany ako vyber ale ako uzol stromu
                        //pridame ho znova akoby prvu polozku submenu
                        child.setId(child.getId()+"-leaf");
                        child.setText(child.getText());
                        addPermGroupClasses(child, permGroupsClasses);

                        //pridaj povodnu polozku ako prvy child (ak to je userItem)
                        if (m.isUserItem()) {
                            JsTreeItem subItem = new JsTreeItem();
                            subItem.setText(prop.getText(getLeftMenuNameKeyV9(m.getLeftMenuNameKey())));
                            subItem.setParent(child.getId());
                            subItem.setId(getPermsIdWithPrefix(m.getItemKey()));
                            subItem.setIcon(getIcon9(m));
                            addPermGroupClasses(subItem, permGroupsClasses);

                            childs.add(subItem);
                        }

                        hasItem = true;
                    }
                    JsTreeItem subItem = new JsTreeItem();
                    subItem.setText(prop.getText(getLeftMenuNameKeyV9(sub.getLeftMenuNameKey())));
                    subItem.setParent(child.getId());
                    subItem.setId(getPermsIdWithPrefix(sub.getItemKey()));
                    subItem.setIcon(getIcon9(sub));
                    addPermGroupClasses(subItem, permGroupsClasses);

                    if (checkDuplicity.contains(subItem.getId())==false) {
                        childs.add(subItem);
                        checkDuplicity.add(subItem.getId());
                    } else {
                        Logger.debug(MenuService.class, "duplicity: "+subItem.getId());
                    }
                }
                if (hasItem==false) addPermGroupClasses(child, permGroupsClasses);
            } else {
                addPermGroupClasses(child, permGroupsClasses);
            }
        }
        return childs;
    }

    /**
     * prida do liClass polozky CSS styly indikujuce skupinu prav
     * @param item
     * @param permGroupsClasses
     */
    private static void addPermGroupClasses(JsTreeItem item, HashMap<String, StringBuilder> permGroupsClasses) {
        StringBuilder classes = permGroupsClasses.get(item.getId());
        if (classes != null) {
            item.addLiClass(classes.toString());
        }
    }

    private static final String PERM_PREFIX = "perms_";
    /**
     * Technicky len prida unikatny prefix pred ID modulu, aby sa nahodou niekde neopakoval
     * @param id
     * @return
     */
    public static String getPermsIdWithPrefix(String id) {
        return PERM_PREFIX+id;
    }

    /**
     * Odstrani perms_ prefix z mena prava (ten pridavame aby nebola kolizia ID elementov)
     * @param perms
     * @return
     */
    public static String removePermsIdPrefix(String perms) {
        if (perms.startsWith(PERM_PREFIX)) return perms.substring(PERM_PREFIX.length());
        return perms;
    }

    /**
     * Vrati pravo pre zadanu URL adresu (linku)
     * @param url
     * @return
     */
    public static String getPerms(String url) {

        //normalizuj linku
        String normalized = url;
        if (normalized.startsWith("/")==false) normalized = "/"+normalized;
        normalized = Tools.replace(normalized, "//", "/");
        normalized = Tools.replace(normalized, "/dist/views/", "/");

        List<ModuleInfo> allModules = Modules.getInstance().getModules();
        for (ModuleInfo m : allModules) {
            if (checkLink(m, normalized)) return m.getItemKey();

            if (m.getSubmenus()!=null) {
                for (ModuleInfo sm : m.getSubmenus()) {
                    if (checkLink(sm, normalized)) {
                        if (Tools.isNotEmpty(sm.getItemKey())) return sm.getItemKey();
                        return m.getItemKey();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Overi, ci sa zadana URL adresa zhoduje s linkou v module
     * @param m
     * @param normalized
     * @return
     */
    private static boolean checkLink(ModuleInfo m, String normalized) {

        if (m.getLeftMenuLink().equalsIgnoreCase(normalized) || m.getLeftMenuLink().equalsIgnoreCase(normalized+"/")) return true;

        //pre prehlad
        if (getMenuLinkV9(m).equalsIgnoreCase(normalized) || getMenuLinkV9(m).equalsIgnoreCase(normalized+"/")) return true;

        return false;
    }
}