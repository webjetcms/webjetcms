package sk.iway.iwcm.components.gallery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.settings.AdminSettingsService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.UrlRedirectDB;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Service
@RequestScope
public class GalleryTreeService {

    private final GalleryDimensionRepository repository;
    private HttpServletRequest request;

    private static final String ADMIN_SETTINGS_KEY = "jstreeSettings_gallery";

    public GalleryTreeService(GalleryDimensionRepository repository, HttpServletRequest request) {
        this.repository = repository;
        this.request = request;
    }

    /**
     * Get List of GalleryJsTreeItem for specified URL address (e.g. /images/gallery)
     * @param url
     * @return
     */
    public List<GalleryJsTreeItem> getItems(String url) {

        String dir = getRequest().getParameter("dir");
        String treeSearchValue = Tools.getStringValue(request.getParameter("treeSearchValue"), null);
        Identity user = UsersDB.getCurrentUser(getRequest());

        List<IwcmFile> files = new ArrayList<>();
        if(Tools.isNotEmpty(treeSearchValue)) {
            files.addAll( getAllTreeFiles(url, treeSearchValue) );
        } else {
            files = getFiles(url);
        }

        List<GalleryJsTreeItem> items = files.stream().map(f -> new GalleryJsTreeItem(f, dir, repository, user)).toList();

        if(Tools.isNotEmpty(treeSearchValue)) {
            //Filter items by item.getText
            Set<GalleryJsTreeItem> filteredItems = new HashSet<>( filterItems(items, treeSearchValue) );

            //Set parent for filtered items
            for(GalleryJsTreeItem item : items) {
                if(dir.equals(item.getId())) {
                    //Root item
                    item.getState().setOpened(false);
                    item.getState().setSelected(false);
                    item.setParent("#");
                } else {
                    String parent = item.getVirtualPath().substring(0, item.getVirtualPath().lastIndexOf("/"));
                    item.setParent(parent);
                }
            }

            //Find parents of filtered items
            addParents(filteredItems, items, dir);

            //Replace all items with filtered items (+ parents)
            items = new ArrayList<>(filteredItems);
        }

        items = sortFilesBasedOnUserSettings(items, user);

        return items;
    }

    /**
     * Loop through filteredItems and add parents to them. Use allItems as source of parents.
     * @param filteredItems
     * @param allItems
     * @param dir
     */
    private void addParents(Set<GalleryJsTreeItem> filteredItems, List<GalleryJsTreeItem> allItems, String dir) {
        Set<GalleryJsTreeItem> parentsToAdd = new HashSet<>();

        //child - entity whos parent we looking for
        for(GalleryJsTreeItem child : filteredItems) {

            //Loop until we find root (id == dir)
            GalleryJsTreeItem parent = child;
            int failsafe = 0;
            while(true) {
                //Check if it's root
                if(dir.equals(parent.getId())) break;

                //No root - find parent
                for(GalleryJsTreeItem potencialParent : allItems) {
                    if(potencialParent.getId().equals(parent.getParent())) {
                        parentsToAdd.add(potencialParent);
                        parent = potencialParent;
                        break;
                    }
                }

                failsafe++;
                if(failsafe > 100) {
                    Logger.debug(GalleryTreeService.class, "Failsafe triggered, breaking loop to prevent infinite loop method addParents, GalleryJsTreeItem id: " + child.getId());
                    break; // Prevent infinite loop
                }
            }
        }

        filteredItems.addAll(parentsToAdd);
    }

    /**
     * Filter items by text property. Search is case insensitive and diacritics insensitive.
     * @param items
     * @param searchText
     * @return
     */
    private List<GalleryJsTreeItem> filterItems(List<GalleryJsTreeItem> items, String searchText) {
        String treeSearchType = Tools.getStringValue(getRequest().getParameter("treeSearchType"), "");
        final String wantedValueLC = DB.internationalToEnglish(searchText).toLowerCase();

        //Filter by serach value and search type
        if("contains".equals(treeSearchType)) {
            return items.stream()
                .filter(item -> DB.internationalToEnglish(item.getText()).toLowerCase().contains(wantedValueLC)
                    || (item.getSecondText() != null && DB.internationalToEnglish(item.getSecondText()).toLowerCase().contains(wantedValueLC)))
                .toList();
        } else if("startwith".equals(treeSearchType)) {
            return items.stream()
                .filter(item -> DB.internationalToEnglish(item.getText()).toLowerCase().startsWith(wantedValueLC)
                    || (item.getSecondText() != null && DB.internationalToEnglish(item.getSecondText()).toLowerCase().startsWith(wantedValueLC)))
                .toList();
        } else if("endwith".equals(treeSearchType)) {
            return items.stream()
                .filter(item -> DB.internationalToEnglish(item.getText()).toLowerCase().endsWith(wantedValueLC)
                    || (item.getSecondText() != null && DB.internationalToEnglish(item.getSecondText()).toLowerCase().endsWith(wantedValueLC)))
                .toList();
        } else if("equals".equals(treeSearchType)) {
            return items.stream()
                .filter(item -> DB.internationalToEnglish(item.getText()).equalsIgnoreCase(wantedValueLC)
                    || (item.getSecondText() != null && DB.internationalToEnglish(item.getSecondText()).equalsIgnoreCase(wantedValueLC)))
                .toList();
        } else return new ArrayList<>();
    }

    /**
     * Get all files in directory and subdirectories.
     * For performance reason, we are
     * not searching in file system, we are searching only in database
     * @param url - root directory url
     * @param treeSearchValue - search value
     * @return
     */
    private List<IwcmFile> getAllTreeFiles(String url, String treeSearchValue) {
        Set<String> paths = new HashSet<>();

        List<GalleryDimension> dirs = repository.findByPathLikeAndNameLikeAndDomainId(url+"%", "%"+treeSearchValue+"%", CloudToolsForCore.getDomainId());

        dirs.forEach(gallery -> {
            if (FileTools.isDirectory(gallery.getPath()) == false) return;

            paths.add(gallery.getPath());

            //append all parents files starts with url if not allready in map
            //eg. for /images/gallery/2021/01/01/1.jpg add /images/gallery/2021/01/01, /images/gallery/2021, /images/gallery
            String parent = gallery.getPath();
            while(parent.contains("/")) {
                parent = parent.substring(0, parent.lastIndexOf("/"));
                if(paths.contains(parent) == false && parent.startsWith(url)) paths.add(parent);
            }
        });

        //convert paths into list of string and sort it
        List<String> pathsSorted = paths.stream().sorted().collect(Collectors.toList());

        //convert paths into IwcmFile list
        List<IwcmFile> list = new ArrayList<>();
        for(String path : pathsSorted) {
            list.add(new IwcmFile(Tools.getRealPath(path)));
        }

        return list;
    }

    private List<IwcmFile> getFiles(String url) {
        IwcmFile directory = new IwcmFile(Tools.getRealPath(url));
        final String urlFinal = url;

        if(url == null) return Arrays.asList();

        final Set<String> blacklistedNames = getBlacklistedNames();

        return Arrays.asList(directory.listFiles(file -> {
            if (!file.isDirectory()) {
                return false;
            }

            //odstran domenove aliasy z inych domen
            if (blacklistedNames.isEmpty()==false && blacklistedNames.contains(file.getName())) return false;

            //toto chceme vzdy
            if ("gallery".equals(file.getName())) return true;
            if (file.getVirtualPath().contains("gallery")) return true;

            //ak ma /images/tento-priecinok podpriecinok gallery tiez ho pridaj (testuje sa len pre prvu uroven)
            if ("/images".equals(urlFinal) && FileTools.isDirectory(urlFinal+"/"+file.getName()+"/gallery")) return true;

            //ak je nastaveny GalleryDimension povazuj to tiez za galeriu
            Optional<GalleryDimension> gallery = repository.findFirstByPathLikeAndDomainId(urlFinal+"/"+file.getName()+"%", CloudToolsForCore.getDomainId());
            if (gallery.isPresent()) return true;

            return false;
        }));

    }

    private List<GalleryJsTreeItem> sortFilesBasedOnUserSettings(List<GalleryJsTreeItem> files, Identity user) {
        String treeSortType = getTreeSortType(user);
        boolean treeSortOrderAsc = isTreeSortOrderAsc(user);

        Comparator<GalleryJsTreeItem> comparator;
        if ("lastModify".equals(treeSortType)) {
            comparator = Comparator.comparing(GalleryJsTreeItem::getLastModified);
        } else if ("createDate".equals(treeSortType)) {
            comparator = Comparator.comparing(GalleryJsTreeItem::getCreateDate);
        } else {
            //DEFAULT OPTION -> sort by "title"
            comparator = Comparator.comparing(GalleryJsTreeItem::getText, String.CASE_INSENSITIVE_ORDER);
        }

        if (treeSortOrderAsc == false) {
            comparator = comparator.reversed();
        }

        return files.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

	private  String getTreeSortType(UserDetails user) {
        AdminSettingsService ass = new AdminSettingsService(user);
		String sortType = ass.getJsonValue(ADMIN_SETTINGS_KEY, "treeSortType");

		if(sortType == null || Tools.isEmpty(sortType) == true) return "priority";
		else return sortType;
	}

	private boolean isTreeSortOrderAsc(UserDetails user) {
		AdminSettingsService ass = new AdminSettingsService(user);
		return ass.getJsonBooleanValue(ADMIN_SETTINGS_KEY, "treeSortOrderAsc");
	}

    /**
     * Black listed names are all domain aliases except current domain alias
     * It's to hide other than current domain alias in multidomain setup (without external folders)
     * @return
     */
    private Set<String> getBlacklistedNames() {
        Set<String> blacklistedNames = new HashSet<>();
        if (Constants.getBoolean("multiDomainEnabled") && FilePathTools.isExternalDirs()==false) {
            String domainAlias = MultiDomainFilter.getDomainAlias(DocDB.getDomain(getRequest()));
            if (Tools.isNotEmpty(domainAlias)) {
                //blacklistni ostatne aliasy
                List<ConfDetails> aliases = ConstantsV9.getValuesStartsWith("multiDomainAlias:");
                for (ConfDetails conf : aliases) {
                    String alias = conf.getValue();
                    if (Tools.isNotEmpty(alias) && alias.equals(domainAlias)==false) blacklistedNames.add(alias);
                }
            }
        }
        return blacklistedNames;
    }

    private HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Checks whether the current user has write access to the given virtual path.
     * The path must start with {@code /images} and the user must have folder write permission.
     * If access is denied, the {@code result} and {@code error} keys are set in the provided map.
     *
     * @param map         response map where {@code result=false} and an {@code error} message are set on failure
     * @param virtualPath virtual path to check (e.g. {@code /images/gallery/subfolder})
     * @return {@code true} if access is allowed, {@code false} otherwise
     */
    public boolean checkPathAccess(Map<String, Object> map, String virtualPath) {
        Prop prop = Prop.getInstance( getRequest() );
        Identity user = UsersDB.getCurrentUser( getRequest() );

        if (!virtualPath.startsWith("/images")) {
            map.put("result", false);
            map.put("error", prop.getText("java.GalleryTreeRestController.directory_id_not_in_images", virtualPath));
            return false;
        }

        if (user == null || user.isFolderWritable(virtualPath) == false) {
            map.put("result", false);
            map.put("error", prop.getText("components.gallery.folderIsNotEditable", virtualPath));
            return false;
        }
        return true;
    }

    /**
     * Finds a {@link GalleryDimension} by its virtual path and moves it under the specified parent path.
     * The following operations are performed on a successful move:
     * <ul>
     *   <li>Updates the {@link GalleryDimension} path in the database.</li>
     *   <li>Renames the physical directory on the file system.</li>
     *   <li>Updates {@code image_path} in the {@code gallery} table.</li>
     *   <li>Registers a 301 redirect from the old path to the new path.</li>
     *   <li>Optionally replaces all occurrences of the old path in page content (doc).</li>
     * </ul>
     * On any error the {@code result=false} and an {@code error} message are placed into the result map.
     *
     * @param virtualPath  current virtual path of the gallery folder (e.g. {@code /images/gallery/old})
     * @param parentPath   target parent virtual path under which the folder will be moved (e.g. {@code /images/gallery/new-parent})
     * @param result       response map populated with {@code result} (boolean) and optionally {@code error} (String)
     * @param updateInDoc  if {@code true}, replaces all occurrences of the old path in page content
     */
    public void findAndMoveGalleryFolder(String virtualPath, String parentPath, Map<String, Object> result, boolean updateInDoc) {
        Prop prop = Prop.getInstance( getRequest() );

        Optional<GalleryDimension> firstByPath = repository.findFirstByPathAndDomainId(virtualPath, CloudToolsForCore.getDomainId());
        if (firstByPath.isPresent()) {
            try {
                GalleryDimension galleryDimension = firstByPath.get();
                String originalPath = galleryDimension.getPath();
                String newPath = parentPath + "/" + galleryDimension.getNameFromPath(); //NOSONAR

                if (checkPathAccess(result, newPath) == false) {
                    return;
                }

                galleryDimension.setPath(newPath);
                repository.save(galleryDimension);

                IwcmFile file = new IwcmFile(Tools.getRealPath(virtualPath));
                boolean renamed = file.renameTo(new IwcmFile(Tools.getRealPath(newPath)));
                result.put("result", renamed);

                if (!renamed) {
                    result.put("error", prop.getText("java.GroupsTreeRestController.move.renamed_failed"));
                    // Vraciam do povodneho adresaru aj DB entitu
                    galleryDimension.setPath(originalPath);
                    repository.save(galleryDimension);

                    return;
                } else {
                    //update all gallery items
                    new SimpleQuery().execute("UPDATE gallery SET image_path=? WHERE image_path=?", newPath, virtualPath);

                    // Add redirect - always
                    UrlRedirectDB.addRedirect("regexp:^" + Tools.replace(virtualPath, "/", "\\/") + "\\/(.+)", newPath + "/$1", CloudToolsForCore.getDomainName(), 301);

                    //update paths in doc
                    if(updateInDoc) {
                        DocDB.getInstance().replaceTextAll(virtualPath, newPath);
                    }
                }
            } catch (Exception e) {
                Logger.error(GalleryTreeService.class, e);
                result.put("result", false);
                result.put("error", prop.getText("java.GroupsTreeRestController.move.save_failed"));
                return;
            }
        }

        result.put("result", true);
    }

    public NotifyBean updateInDocWarning(Prop prop) {
        return new NotifyBean(prop.getText("components.gallery.move_warning.title"), prop.getText("components.gallery.move_warning.desc"), NotifyBean.NotifyType.WARNING, 10000);
    }
}