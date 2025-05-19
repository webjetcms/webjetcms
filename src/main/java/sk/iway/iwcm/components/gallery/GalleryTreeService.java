package sk.iway.iwcm.components.gallery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.settings.AdminSettingsService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.ConstantsV9;
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
                .filter(item -> DB.internationalToEnglish(item.getText()).toLowerCase().contains(wantedValueLC))
                .toList();
        } else if("startwith".equals(treeSearchType)) {
            return items.stream()
                .filter(item -> DB.internationalToEnglish(item.getText()).toLowerCase().startsWith(wantedValueLC))
                .toList();
        } else if("endwith".equals(treeSearchType)) {
            return items.stream()
                .filter(item -> DB.internationalToEnglish(item.getText()).toLowerCase().endsWith(wantedValueLC))
                .toList();
        } else if("equals".equals(treeSearchType)) {
            return items.stream()
                .filter(item -> DB.internationalToEnglish(item.getText()).equalsIgnoreCase(wantedValueLC))
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

        List<GalleryDimension> dirs = repository.findByPathLikeAndPathLikeAndDomainId(url+"%", "%"+treeSearchValue+"%", CloudToolsForCore.getDomainId());

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

        return Arrays.asList(directory.listFiles(file -> {
            if (!file.isDirectory()) {
                return false;
            }
            Set<String> blacklistedNames = getBlacklistedNames();
            //odstran domenove aliasy z inych domen
            if (blacklistedNames.isEmpty() && blacklistedNames.contains(file.getName())) return false;

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
        if (Constants.getBoolean("multiDomainEnabled")) {
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

}
