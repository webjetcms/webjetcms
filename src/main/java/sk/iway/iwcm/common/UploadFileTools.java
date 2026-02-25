package sk.iway.iwcm.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.io.IwcmFile;

public class UploadFileTools {

    private UploadFileTools() {
        //utility class
    }

    /**
     * Compute the folder name for a new (unsaved) page by delegating to
     * {@link EditorService#computeVirtualPathForNewPage} so that duplicate-title pages
     * (which receive a -2, -3, ... URL suffix on save) use the correct upload folder
     * before the page is even saved.
     *
     * Falls back to DocTools.removeChars(newPageTitle, true) on any failure.
     *
     * @param newPageTitle - title of the new page
     * @param groupId - group (folder) where the new page will be created
     * @return folder name, e.g. "upratovanie-2"
     */
    private static String getNewPageVirtualPathFolderName(String newPageTitle, int groupId) {
        try {
            DocDetails editedDoc = new DocDetails();
            editedDoc.setDocId(-1);
            editedDoc.setTitle(newPageTitle);
            editedDoc.setNavbar(DB.prepareString(newPageTitle, 128));
            editedDoc.setVirtualPath("");
            editedDoc.setGroupId(groupId);

            GroupsDB groupsDB = GroupsDB.getInstance();
            EditorService.computeVirtualPathForNewPage(editedDoc, groupsDB);

            String virtualPath = editedDoc.getVirtualPath();
            if (Tools.isNotEmpty(virtualPath)) {
                String pageUrlName = getPageUrlName(virtualPath);
                if (Tools.isNotEmpty(pageUrlName)) {
                    return pageUrlName;
                }
            }
        } catch (Exception e) {
            Logger.error(UploadFileTools.class, e);
        }
        return DocTools.removeChars(newPageTitle, true);
    }

    private static String getPageUrlName(String url) {
        String pageUrlName = null;
        if (Tools.isNotEmpty(url) && url.length()>5)
        {
            if (url.endsWith("/"))
            {
                int predposlednaLomka = url.substring(0, url.length()-1).lastIndexOf("/");
                if (predposlednaLomka>1) pageUrlName = url.substring(predposlednaLomka+1, url.length()-1);
            }
            else if (url.endsWith(EditorService.DOT_HTML_EXT))
            {
                int poslednaLomka = url.lastIndexOf("/");
                if (poslednaLomka>1) pageUrlName = url.substring(poslednaLomka+1, url.length()-5);
            }
        }
        return pageUrlName;
    }

    private static String clearVirtualPath(String virtualPath)
    {
        if (virtualPath.endsWith(EditorService.DOT_HTML_EXT)) {
            virtualPath = virtualPath.substring(0, virtualPath.lastIndexOf(EditorService.DOT_HTML_EXT));
        }

        return virtualPath;
    }

    /**
     * Vrati subadresar pre upload obrazkov / suborov pre zadanu stranku, napr. /sk/produkty/webjet8 to sa nasledne prida k /images alebo /files a pouzije sa pre uload k "Aktualna stranka"
     * @param docId
     * @param groupId
     * @param prefix
     * @return
     * @deprecated pouzit {@link #getPageUploadSubDir(int, int, String, String)}
     */
    @Deprecated(forRemoval = false)
    public static String getPageUploadSubDir(int docId, int groupId, String prefix) {
        return getPageUploadSubDir(docId, groupId, null, prefix);
    }

    /**
     * Vrati subadresar pre upload obrazkov / suborov pre zadanu stranku, napr. /sk/produkty/webjet8 to sa nasledne prida k /images alebo /files a pouzije sa pre uload k "Aktualna stranka"
     * @param docId
     * @param groupId
     * @param newPageTitle - nazov novej web stranky, pouzije sa ak docId&lt;1
     * @param prefix
     * @return
     */
    public static String getPageUploadSubDir(int docId, int groupId, String newPageTitle, String prefix)
    {
        StringBuilder path = new StringBuilder();
        if (Constants.getBoolean("galleryUploadDirVirtualPath") && docId > 0)
        {
            String virtualPath = DocDB.getURLFromDocId(docId, null);
            if (Tools.isNotEmpty(virtualPath))
            {
                virtualPath = clearVirtualPath(virtualPath);
                if (Tools.isNotEmpty(prefix))
                    path.append(prefix);
                path.append(virtualPath);
                return path.toString();
            }
        }

        GroupsDB groupsDB = GroupsDB.getInstance();

        if (Tools.isNotEmpty(prefix))
        {
            path.append(prefix);
        }

        String domainAlias = AdminTools.getDomainNameFileAliasAppend();
        if (Tools.isNotEmpty(domainAlias))
        {
            if (prefix.startsWith("/images/gallery")) {
                path.setLength(0);
                path.append("/images"+domainAlias+prefix.substring("/images".length()));
            } else {
                path.append(domainAlias);
            }
        }

        String urlPath = groupsDB.getURLPath(groupId);
        //if the group has a virtual path, add it to the path
        if ("--------------------------".equals(urlPath)==false) path.append(urlPath);

        if (path.isEmpty()) {
            path.append("/");
        }

        if (Constants.getBoolean("elfinderCreateFolderForPages"))
        {
            String pageUrlName = null;
            if (docId > 0) {
                GroupDetails group = groupsDB.getGroup(groupId);
                DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
                if (doc != null && group != null && group.getDefaultDocId()!=doc.getDocId())
                {
                    String url = doc.getVirtualPath();
                    pageUrlName = getPageUrlName(url);

                    if (Tools.isEmpty(pageUrlName))
                    {
                        pageUrlName = DocTools.removeChars(doc.getTitle(), true);
                    }
                }
            } else {
                if (Tools.isNotEmpty(newPageTitle)) {
                    pageUrlName = getNewPageVirtualPathFolderName(newPageTitle, groupId);
                }
            }

            if (Tools.isNotEmpty(pageUrlName)) {
                if (path.toString().endsWith("/")==false) path.append("/");
                path.append(pageUrlName);
            }
        }

        path = new StringBuilder(DocTools.removeCharsDir(path.toString(), true).toLowerCase());

        // #18512 vratenie cesty len do urcitej hlbky
        int maxDeep = Constants.getInt("editorUploadActualPageMaxDeep");
        if (StringUtils.countMatches(path.toString(), "/") > maxDeep) {
            path.substring(0, StringUtils.ordinalIndexOf(path.toString(), "/", maxDeep + 1));
        }

        if (path.length()>3 && path.charAt(path.length()-1)=='/') {
            //odstrank koncove lomitko (3 hodiny debugovania zacykleneho otvarania priecinka ked group mal nastavene /sk/)
            path.deleteCharAt(path.length()-1);
        }

        return path.toString();
    }

    public static boolean isFileAllowed(String uploadType, String fileName, long fileSize, Identity user, HttpServletRequest request)
    {
        String permissionDeniedRequestKey = "permissionDenied";

        request.removeAttribute(permissionDeniedRequestKey);

        if (user == null || user.isDisabledItem("menuFbrowser") || request.getRequestURI().contains("/admin/upload/chunk"))
        {
            //#32245 pentesty ING Bank
            //ak user nema povolene subory, stale je dostupny elfinder napr. v banneroch a teoreticky moze nahrat skodlivy subor
            //zakazeme mu upload skodlivych suborov, nie je dovod aby takyto subor vedel nahrat
            String ext = FileTools.getFileExtension(fileName);
            if (ext.equals("jsp") || ext.equals("php") || ext.equals("class") || ext.equals("jar") || FileBrowserTools.hasForbiddenSymbol(fileName))
            {
                request.setAttribute(permissionDeniedRequestKey, "fileType");
                return false;
            }
        }

        int uploadMaxSize = getUploadMaxSize(user, uploadType);
        String uploadFileTypes = getUploadFileTypes(user, uploadType);

        if (uploadMaxSize > 0 && fileSize > uploadMaxSize)
        {
            request.setAttribute(permissionDeniedRequestKey, "fileSize");
            return false;
        }

        if (FileBrowserTools.hasForbiddenSymbol(fileName)) {
            return false;
        }

        if (Tools.isEmpty(uploadFileTypes) || "*".equals(uploadFileTypes)) return true;

        fileName = fileName.toLowerCase();

        String[] exts = Tools.getTokens(uploadFileTypes, ",", true);
        for (String ext : exts)
        {
            if (fileName.endsWith("."+ext)) return true;
        }

        request.setAttribute("permissionDenied", "fileType");

        return false;
    }


    /**
     * Vrati ciarkou oddeleny zoznam pripon suborov, ktore pouzivatel moze na server nahrat
     * @param user
     * @param type - image, alebo file
     * @return - gif,png,jpg,swf
     */
    public static String getUploadFileTypes(Identity user, String type)
    {
        if (user == null) return "aaaaa";

        if (user.isEnabledItem("editor_unlimited_upload")) return "";

        String uploadFileTypes = "";

        String prefix = "Default";
        if (user.isDisabledItem("editorMiniEdit")==false) prefix = "Basic";
        //RZA: pri drag&drope obrazka v editore je type "ckeditor"
        if ("image".equalsIgnoreCase(type) || "ckeditor".equalsIgnoreCase(type))
        {
            uploadFileTypes = Constants.getString("FCKConfig.UploadFileTypes["+prefix+"][image]");
        }
        else
        {
            uploadFileTypes = Constants.getString("FCKConfig.UploadFileTypes["+prefix+"][file]");
        }

        return uploadFileTypes;
    }

    /**
     * Vrati limit na velkost suboru, ktory moze pouzivatel na server nahrat
     * @param user
     * @param type - image, alebo file
     * @return - velkost suboru v B
     */
    public static int getUploadMaxSize(Identity user, String type)
    {
        if (user == null) return 0;

        if (user.isEnabledItem("editor_unlimited_upload")) return 0;

        int limit = 0;
        String limitStr = "";

        String prefix = "Default";
        if (user.isDisabledItem("editorMiniEdit")==false) prefix = "Basic";
        if ("image".equalsIgnoreCase(type))
            limitStr = Constants.getString("FCKConfig.UploadMaxSize["+prefix+"][image]");
        else
            limitStr = Constants.getString("FCKConfig.UploadMaxSize["+prefix+"][file]");

        if(Tools.isNotEmpty(limitStr))
        {
            //ak je zadana hodnota napr. 2*1024
            if(limitStr.indexOf('*') != -1)
            {
                String[] limitStrArray = Tools.getTokens(limitStr, "*");
                if(limitStrArray != null && limitStrArray.length > 0)
                {
                    limit = 1;
                    for(String limitStrValue : limitStrArray)
                        limit *= Tools.getIntValue(limitStrValue, 1);
                }
            }
            else
            {
                limit = Tools.getIntValue(limitStr, 0);
            }
        }

        return limit * 1024;
    }

    /**
     * Vrati subadresar pre upload obrazkov / suborov pre zadanu stranku, napr. /sk/produkty/webjet8 to sa nasledne prida k /images alebo /files a pouzije sa pre uload k "Aktualna stranka"
     * @param docId
     * @param groupId
     * @return
     */
    public static String getPageUploadSubDir(int docId, int groupId)
    {
        return getPageUploadSubDir(docId, groupId, null, null);
    }

    /**
     * Handle all processing required for gallery - create resized images, store upload date etc.
     * @param newFileIwcm
     * @param dateCreated
     * @return list of added files (resized images) - relative to virtual parent, e.g. s_image.jpg, o_image.jpg
     */
    public static List<String> handleGallery(IwcmFile newFileIwcm, Date dateCreated) {
		List<String> added = new ArrayList<>();
		String dir = newFileIwcm.getVirtualParent();
		if (FileTools.isImage(newFileIwcm.getName())) {
			if (GalleryDB.isGalleryFolder(dir)) {
				//we must replace o_ file because it will be used in resize process instead of new file
				IwcmFile orig = new IwcmFile(Tools.getRealPath(dir+"/o_"+newFileIwcm.getName()));
				if (orig.exists()) {
					FileTools.copyFile(newFileIwcm, orig);
				}

				GalleryDB.resizePicture(newFileIwcm.getAbsolutePath(), dir);
				added.add("s_"+newFileIwcm.getName());
				added.add("o_"+newFileIwcm.getName());
			} else if (Constants.getBoolean("imageAlwaysCreateGalleryBean")) {
				GalleryDB.setImage(dir, newFileIwcm.getName());
			}

			//zapise datum vytvorenia fotografie (ak vieme ziskat)
			if (dateCreated != null) {
				GalleryDB.setUploadDateImage(dir, newFileIwcm.getName(), dateCreated.getTime());
			}
            GalleryDB.clearInterestPoint(dir, newFileIwcm.getName());
		}
		return added;
	}
}
