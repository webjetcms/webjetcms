package sk.iway.iwcm.grideditor.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;
import sk.iway.iwcm.grideditor.dto.BlockDto;
import sk.iway.iwcm.grideditor.dto.CategoryDto;
import sk.iway.iwcm.grideditor.dto.GroupDto;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/grideditor")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
public class GridEditorController {
    private String textKeyPrefix = "components.grideditor.textkey.";

    /**
     * vrati vsetky FAVORITE bloky.
     * @param request
     * @return
     */
    @RequestMapping(path = "/templates/favorite", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryDto>> getTempaltesFavoriteJSON(@RequestParam(required = false) Integer templateGroupId, HttpServletRequest request) {

        if ( !isUserAllowed(request) ) return null;

        List<CategoryDto> categoryList = new ArrayList<>();

        String rootDirPath = getFavouritesDirPath(templateGroupId, request) + "data/";
        IwcmFile dir = new IwcmFile(Tools.getRealPath(rootDirPath));

        if (dir.exists() && dir.canRead()){
            categoryList = getCategoryList(request, dir, rootDirPath, true );
        }
        return ResponseEntity.ok( categoryList );
    }

    /**
     * vrati vsetky kategorie vratane vsetkych skupin a blokov aj s html kodmi.
     * @param request
     * @return
     */
    @RequestMapping(path = "/templates/library", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryDto>> getTempaltesLibraryJSON(@RequestParam(required = false) Integer templateGroupId, HttpServletRequest request) {

        if ( !isUserAllowed(request) ) return null;

        List<CategoryDto> categoryList = new ArrayList<>();

        String rootDirPath = getDataDirPath(templateGroupId, request);
        IwcmFile dir = new IwcmFile(Tools.getRealPath(rootDirPath));

        if (dir.exists() && dir.canRead()){
            categoryList = getCategoryList(request, dir, rootDirPath, true );
        }
        return ResponseEntity.ok( categoryList );
    }

    private List<CategoryDto> getCategoryList(HttpServletRequest request, IwcmFile dir, String rootDirPath, boolean includeBlocks){
        List<CategoryDto> categoryList = new ArrayList<>();

        IwcmFile categories[] = FileTools.sortFilesByName(dir.listFiles());

        for (int i=0; i<categories.length; i++)
        {
            CategoryDto cat = getCategoryObjectForSelectGroup(request, categories[i], rootDirPath, includeBlocks);
            categoryList.add(cat);
        }
        return categoryList;
    }





    @RequestMapping(path = "/categories", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryDto>> getSelectGroupJSON(@RequestParam(required = false) Integer templateGroupId, HttpServletRequest request) {

        List<CategoryDto> categoryList = new ArrayList<>();

        if ( !isUserAllowed(request) ){
            return null;
        }

        String rootDirPath = getDataDirPath(templateGroupId, request);
        IwcmFile dir = new IwcmFile(Tools.getRealPath(rootDirPath));

        if (dir.exists() && dir.canRead()){
            categoryList = getCategoryList(request, dir, rootDirPath, false );
        }
        return ResponseEntity.ok( categoryList );
    }


    /**
     * Vrati cestu k adresaru s html subormi sablony,
     * ak je zadane templateGroupId tak z /templates/installName/MENO_SABLONY/data/
     * inak z /components/grideditor/data/
     *
     */
    private String getDataDirPath(Integer templateGroupId, HttpServletRequest request)
    {
        if (templateGroupId != null && templateGroupId.intValue()>0)
        {
            TemplatesGroupBean tgroup = TemplatesGroupDB.getInstance().getById((long)templateGroupId.intValue());
            if (tgroup != null && Tools.isNotEmpty(tgroup.getDirectory()))
            {
                //skus pohladat v dist adresari
                String templateDataDirPath = WriteTagToolsForCore.getCustomPath("/templates/"+tgroup.getDirectory()+"/dist/pagebuilder/", request);
                if (FileTools.exists(templateDataDirPath))
                {
                    //Logger.debug(GridEditorController.class, "getDataDirPath, returning "+templateDataDirPath);
                    return templateDataDirPath;
                }
                //ak neexistuje dist, skus priamo pagebuilder adresar v sablone
                templateDataDirPath = WriteTagToolsForCore.getCustomPath("/templates/"+tgroup.getDirectory()+"/pagebuilder/", request);
                if (FileTools.exists(templateDataDirPath))
                {
                    //Logger.debug(GridEditorController.class, "getDataDirPath, returning "+templateDataDirPath);
                    return templateDataDirPath;
                }
            }
        }

        //ak neexistuju pre sablonu pouzi default
        String dataDirPath = WriteTagToolsForCore.getCustomPath("/components/grideditor/data/", request);
        //Logger.debug(GridEditorController.class, "getDataDirPath, returning "+dataDirPath);
        return dataDirPath;
    }

    /**
     * Vrati cestu k adresaru pre oblubene bloky (ulozene pouzivatelom),
     * ak je zadane templateGroupId tak z /files/protected/grideditor/MENO_SABLONY/
     * inak /files/protected/grideditor/
     */
    private String getFavouritesDirPath(Integer templateGroupId, HttpServletRequest request)
    {
        if (templateGroupId != null && templateGroupId.intValue()>0)
        {
            TemplatesGroupBean tgroup = TemplatesGroupDB.getInstance().getById((long)templateGroupId.intValue());
            if (tgroup != null && Tools.isNotEmpty(tgroup.getDirectory()))
            {
                String templateFavDirPath = WriteTagToolsForCore.getCustomPath("/files/protected/pagebuilder/"+tgroup.getDirectory()+"/", request);
                //tu na rozdiel od citania pripravenych blokov netestujeme, ci adresar existuje, ak nie, tak ho ulozenie vytvori
                //Logger.debug(GridEditorController.class, "getFavouritesDirPath, returning "+templateFavDirPath);
                return templateFavDirPath;
            }
        }

        //ak neexistuju pre sablonu pouzi default
        String favDirPath = WriteTagToolsForCore.getCustomPath("/files/protected/grideditor/", request);
        //Logger.debug(GridEditorController.class, "getFavouritesDirPath, returning "+favDirPath);
        return favDirPath;
    }


    /**
     * Vrati novy vytvoreny objekt GroupDto, ktory sa vklada do json pre selectGroup
     * @param request
     * @param file
     * @param rootDirPath
     * @param catId
     * @return - objekt GroupDto, ktory sa vklada do json pre selectGroup
     */
    private GroupDto getGroupObjectForSelectGroup(HttpServletRequest request, IwcmFile file, String rootDirPath, String catId, boolean includeBlocks){
        GroupDto group = new GroupDto();

        String virtualPath = file.getVirtualPath();
        String filePathNoExtension = FileTools.getFilePathWithoutExtension(virtualPath);
        String groupId = filePathNoExtension.replace(rootDirPath,"");
        String textKey = groupId.replace(catId+"/","");
        String imagePath = getImagePath(filePathNoExtension, request);

        textKey = getTextKey(request, textKey);

        group.setId(encodeId(groupId));
        group.setFilePath(filePathNoExtension);
        group.setTextKey(textKey);
        group.setImagePath(imagePath);

        if(file.isFile()){

            String fileType = FileTools.getFileExtension(virtualPath);

            switch(fileType){
                case "txt":
                case "html":
                    group.setAction("block");
                    break;
                case "js":
                    group.setAction("script");
                    break;
                default:
                    //group.setAction("unknown-action");
                    // TODO: ostatne ignorujeme?
                    // ake files este mozme zobrat?
                    return null;
            }

            String content = FileTools.readFileContent(virtualPath, "UTF-8");
            String style = getBlockStyle(virtualPath);

            group.setStyle(style);
            group.setContent(content);
            group.setPremium( isPremium(groupId) );
        }else {
            if(includeBlocks){
                IwayProperties pageBuilderProp = null;
                try {
                    pageBuilderProp = new IwayProperties("utf-8");
                    String lng = Prop.getLng(request, false);
                    IwcmFile propFile = new IwcmFile(file, "pagebuilder_"+lng+".properties");
                    if (propFile.exists()==false){
                        propFile = new IwcmFile(file, "pagebuilder.properties");
                    }
                    if (propFile.exists()) {
                        pageBuilderProp.load(propFile);
                        group.setTextKey(pageBuilderProp.getProperty("title", group.getTextKey()));
                        group.setTags(Tools.getTokens(pageBuilderProp.getProperty("tags", null), ",", true));
                    }
                } catch (Exception e){
                    Logger.error(GridEditorController.class, "Error loading pagebuilder properties from file: "+file.getVirtualPath(), e);
                }

                List<BlockDto> blockList = getBlockList(request, rootDirPath, groupId.replace(catId+"/",""), pageBuilderProp);
                group.setBlocks(blockList);
            }
        }

        return group;
    }

    /**
     * Vrati novy vytvoreny objekt CategoryDto, ktory sa vklada do json pre selectGroup
     * @param f
     * @param rootDirPath
     * @return - objekt CategoryDto, ktory sa vklada do json pre selectGroup
     */
    private CategoryDto getCategoryObjectForSelectGroup(HttpServletRequest request, IwcmFile f, String rootDirPath, boolean includeBlocks){
        CategoryDto cat = new CategoryDto();

        if(f.isDirectory()){
            String catId = FileTools.getFilePathWithoutExtension(f.getVirtualPath()).replace(rootDirPath,"");
            catId = encodeId(catId);
            List<GroupDto> groupList = new ArrayList<>();
            IwcmFile dir = new IwcmFile(Tools.getRealPath(rootDirPath + f.getName()));

            if (dir.exists() && dir.canRead()){
                IwcmFile groups[] = FileTools.sortFilesByName(dir.listFiles());


                for(int j = 0; j < groups.length; j++) {
                    GroupDto group = getGroupObjectForSelectGroup(request,groups[j], rootDirPath, catId, includeBlocks);

                    if( group != null) groupList.add(group);
                }
            }

            String textKey = getTextKey( request, f.getName() );

            cat.setId(catId);
            cat.setTextKey(textKey);
            cat.setGroups(groupList);
        } else if (f.isFile())  {
            // TODO: tuna ziaden file nema byt - nejak upozorni
        }
        return cat;
    }

    @RequestMapping(path = "/category", method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryDto> getSidebarJSON(@RequestParam(required = false) Integer templateGroupId, @RequestParam String categoryDirId, HttpServletRequest request) {

        CategoryDto category = new CategoryDto();

        if ( !isUserAllowed(request) ){
            return null;
        }

        String categoryDirName = decodeId(categoryDirId);

        if(!Tools.isEmpty(categoryDirName)) {
            List<GroupDto> groupList = new ArrayList<>();

            String rootDirPath = getDataDirPath(templateGroupId, request) + categoryDirName;
            IwcmFile dir = new IwcmFile(Tools.getRealPath(rootDirPath));

            if (dir.exists() && dir.canRead()) {

                IwcmFile groups[] = FileTools.sortFilesByName(dir.listFiles());

                for (int i = 0; i < groups.length; i++) {
                    if(groups[i].isDirectory()) {
                        // TODO: toto je teraz asi prve z dvoch overeni - odstranit to druhe.
                        GroupDto group = getGroupObjectForSideBar(request, groups[i], rootDirPath, categoryDirName);
                        groupList.add(group);
                    }
                }
            }else {
                // dany priecinok neexistuje
            }
            //TODO: do setId by malo dat podla mna categoryDirId, nie categoryDirName
            category.setId(encodeId(categoryDirName));
            category.setTextKey( getTextKey(request, categoryDirName) );
            category.setGroups(groupList);
        }
        return ResponseEntity.ok( category );
    }

    /**
     * Vrati novy vytvoreny objekt BlockDto, ktory sa vklada do json pre SideBar
     * @param blockFile
     * @param rootDirPath
     * @param groupDirName
     * @return - objekt BlockDto, ktory sa vklada do json pre SideBar
     */
    private BlockDto getBlockObjectForSideBar(HttpServletRequest request, IwcmFile blockFile, String rootDirPath, String groupDirName, IwayProperties pageBuilderProp){
        BlockDto block = new BlockDto();

        String virtualPath = blockFile.getVirtualPath();
        String filePathNoExtension = FileTools.getFilePathWithoutExtension(virtualPath);
        String blockId = filePathNoExtension.replace(rootDirPath+"/","");
        String textKey = blockId.replace(groupDirName+"/","");
        textKey = getTextKey(request, textKey);
        if (pageBuilderProp != null) textKey = pageBuilderProp.getProperty("title."+FileTools.getFileNameWithoutExtension(blockFile.getName()), textKey);
        String imagePath = getImagePath(filePathNoExtension, request);
        String style = getBlockStyle(virtualPath);

        if (imagePath.indexOf("default")==-1)
        {
            imagePath = "/thumb"+imagePath+"?ip=1&w=330";
        }

        String html = "";
        String action = "";

        switch( FileTools.getFileExtension(virtualPath) ){
            case "html":
                // Fallthrough
            case  "txt":
                html = FileTools.readFileContent(virtualPath, "UTF-8");
                action = "block";
                break;
            default:
                // TODO: co s ostatnymi?
                return null;
        }

        block.setStyle(style);
        block.setAction(action);
        block.setFilePath(filePathNoExtension);
        block.setId(encodeId(blockId));
        block.setTextKey(textKey);
        block.setImagePath(imagePath);
        block.setContent(html);
        block.setPremium( isPremium(blockId) );

        return block;
    }

    private List<BlockDto> getBlockList(HttpServletRequest request, String rootDirPath, String groupDirName, IwayProperties pageBuilderProp){
        List<BlockDto> blockList = new ArrayList<>();
        IwcmFile tempDir = new IwcmFile(Tools.getRealPath(rootDirPath+"/"+groupDirName));

        IwcmFile blocks[] = FileTools.sortFilesByName(tempDir.listFiles());

        for (int j = 0; j < blocks.length; j++) {
            BlockDto block = getBlockObjectForSideBar(request, blocks[j], rootDirPath, groupDirName, pageBuilderProp);
            if(block != null) blockList.add(block);
        }

        return blockList;
    }

    /**
     * Vrati novy vytvoreny objekt GroupDto, ktory sa vklada do json pre SideBar
     * @param f
     * @param rootDirPath
     * @param categoryDirName
     * @return - objekt GroupDto, ktory sa vklada do json pre SideBar
     */
    private GroupDto getGroupObjectForSideBar(HttpServletRequest request, IwcmFile f, String rootDirPath, String categoryDirName ){
        GroupDto group = new GroupDto();
        String groupDirName = f.getName();

        if(f.isDirectory()){
            List<BlockDto> blockList = getBlockList(request,rootDirPath,groupDirName, null);

            String textKey = categoryDirName+"/"+groupDirName;

            // TODO: tuna je zasa id naplnane obycajnym textom.
            group.setId(encodeId(textKey) );
            group.setTextKey( getTextKey(request, textKey));
            group.setBlocks(blockList);
        }else if(f.isFile()){
            // TODO toto je subor v danom tabe - asi sa ma vylistovat len v selectGroup a nie v sidebare
            // popripade mozme vytvorit fiktivny priecinok(tuna objekt GroupDto) - NAJPOUZIVANEJSIE - a tam by sme dali tieto files
        }
        return group;
    }

    @RequestMapping(path = "/delete/element", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteElement(HttpServletRequest request, String filePath, Integer templateGroupId){
        Map<String, Object> result = new HashMap<>();

        if(filePath==null || templateGroupId == null || request == null){
            result.put("result",false);
            result.put("reason","wrong params");
            return ResponseEntity.ok(result);
        }

        String allowedPath = getFavouritesDirPath(templateGroupId,request);
        if(filePath.startsWith(allowedPath)==false || FileBrowserTools.hasForbiddenSymbol(filePath)){
            result.put("result",false);
            result.put("reason","filePath not allowed: "+filePath);
            return ResponseEntity.ok(result);
        }

        filePath = Tools.getRealPath(filePath + ".html");

        IwcmFile file = new IwcmFile(filePath);

        if(file.exists()){
            boolean delete = file.delete();
            result.put("result",delete);

            if(delete){
                Adminlog.add(Adminlog.TYPE_FILE_DELETE, "NPB / Grideditor file deleted: " + filePath, -1, -1);
                Logger.debug(GridEditorController.class, "mazem subor: " + filePath);

                //  DELETE SAVED STYLES
                filePath = filePath.replaceFirst("/data/","/data_style/");
                file = new IwcmFile(filePath);
                if(file.exists()){
                    boolean stylesDeleted= file.delete();
                    if(delete) {
                        result.put("styles_deleted", stylesDeleted);
                        Adminlog.add(Adminlog.TYPE_FILE_DELETE, "NPB / Grideditor file deleted: " + filePath, -1, -1);
                        Logger.debug(GridEditorController.class, "mazem subor: " + filePath);
                    }else {
                        result.put("styles_reason","Error occured when deleting styles file "+filePath);
                        Adminlog.add(Adminlog.TYPE_FILE_DELETE, "NPB / Grideditor unsuccessful file delete: " + filePath, -1, -1);
                        Logger.debug(GridEditorController.class, "nepodarilo sa zmazat subor: " + filePath);
                    }
                }else {
                    result.put("styles_deleted",false);
                    result.put("styles_reason","Style file does not exist");
                    Adminlog.add(Adminlog.TYPE_FILE_DELETE, "NPB / Grideditor unsuccessful file delete: " + filePath+". File does not exists.", -1, -1);
                    Logger.debug(GridEditorController.class, "nepodarilo sa zmazat subor, pretoze neexistuje: " + filePath);
                }
            }else {
                result.put("reason","Error occured when deleting file "+filePath);
                Adminlog.add(Adminlog.TYPE_FILE_DELETE, "NPB / Grideditor unsuccessful file delete: " + filePath, -1, -1);
                Logger.debug(GridEditorController.class, "nepodarilo sa zmazat subor: " + filePath);
            }
        }else {
            result.put("result",false);
            result.put("reason","File does not exist");
            Adminlog.add(Adminlog.TYPE_FILE_DELETE, "NPB / Grideditor unsuccessful file delete: " + filePath+". File does not exists.", -1, -1);
            Logger.debug(GridEditorController.class, "nepodarilo sa zmazat subor, pretoze neexistuje: " + filePath);
        }

        return ResponseEntity.ok(result);
    }


    @RequestMapping(path = "/save/element", method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveElement(HttpServletRequest request, String html, String name, String type, String style, @RequestParam(required = false) Integer templateGroupId) {

        Map<String, Object> result = new HashMap<>();

        if ( !isUserAllowed(request) ){
            result.put("result",false);
            result.put("reason","user not allowed");
            return ResponseEntity.ok(result);
        }

        if(Tools.isEmpty(html) || Tools.isEmpty(name)){
            result.put("result",false);
            result.put("html",html);
            result.put("name",name);
            result.put("reason","one of required parameters is empty");
            return ResponseEntity.ok(result);
        }

        if(FileBrowserTools.hasForbiddenSymbol(name)){
            result.put("result",false);
            result.put("reason","Name contains not allowed character");
            return ResponseEntity.ok(result);
        }

        name = DB.internationalToEnglish(name).toLowerCase().replace(" ", "_");
        name = DocTools.removeChars(name);

        String dirToSaveTo = "undefined"; // je to element na celu sirku -> zacina na <section
        if(Tools.isEmpty(type)) {
            if (html.indexOf("<section ") == 0) dirToSaveTo = "full_width";
            if (html.contains("container") && html.indexOf("container") < html.indexOf(">")) dirToSaveTo = "container";
            if (html.contains("col-") && html.indexOf("col-") < html.indexOf(">")) dirToSaveTo = "column";
        }else {
            dirToSaveTo = type;
        }

        int suffix_counter = 1;
        String dirPath = getFavouritesDirPath(templateGroupId, request) + "data/" +dirToSaveTo + "/";
        String fileName = name;
        String filePath = dirPath+fileName+".html";
        IwcmFile file = new IwcmFile(Tools.getRealPath(filePath));

        while(file.exists()){
            fileName = name+"("+suffix_counter+++")";
            filePath = dirPath+fileName+".html";
            file = new IwcmFile(Tools.getRealPath(filePath));
        }

        boolean saved = FileTools.saveFileContent(filePath,html,"UTF-8");

        if (saved) {
            Adminlog.add(Adminlog.TYPE_FILE_CREATE, "NPB / Grideditor file created: " + filePath, -1, -1);
            Logger.debug(GridEditorController.class, "ukladam subor: " + filePath);
        }
        else {
            Adminlog.add(Adminlog.TYPE_FILE_CREATE, "NPB / Grideditor file NOT created correctly: " + filePath, -1, -1);
            Logger.debug(GridEditorController.class, "Nepodarilo sa ulozit subor: " + filePath);
        }

        // style
        boolean isStyleIncluded = ( style != null && style.indexOf("<style")==0 );
        String styleFilePath = getFavouritesDirPath(templateGroupId, request) + "data_style/" + dirToSaveTo + "/" + fileName+".html";
        if( isStyleIncluded ){

            boolean styleSaved = FileTools.saveFileContent(styleFilePath,style,"UTF-8");

            if (styleSaved) {
                Adminlog.add(Adminlog.TYPE_FILE_CREATE, "NPB / Grideditor style file created: " + styleFilePath, -1, -1);
                Logger.debug(GridEditorController.class, "ukladam style subor: " + styleFilePath);
            }
            else {
                Adminlog.add(Adminlog.TYPE_FILE_CREATE, "NPB / Grideditor style file NOT created correctly: " + styleFilePath, -1, -1);
                Logger.debug(GridEditorController.class, "Nepodarilo sa ulozit style subor: " + styleFilePath);
            }
        }




        result.put("result",true);
        result.put("saved",saved);
        result.put("filePath",filePath);
        result.put("fileName",fileName);
        if(isStyleIncluded) {
            result.put("styleFilePath",styleFilePath);
            result.put("style",style);
        }
        result.put("html",html);

        return ResponseEntity.ok(result);
    }


    /**
     * Vrati style element pre element ulozeny vo favorites
     * @param blockPath
     * @return
     */
    private String getBlockStyle(String blockPath)
    {
        if(blockPath.startsWith("/files/protected/"))
        {
            String stylePath = blockPath.replace("/data/", "/data_style/");
            IwcmFile file = new IwcmFile(Tools.getRealPath(stylePath));

            if( file.exists() && file.isFile() ){
                return FileTools.readFileContent(stylePath, "UTF-8");
            }
        }
        return "";
    }


    /**
     * Overi, ci vkladany blok nieje premiovy.
     * @param id
     * @return - zatial stale FALSE
     */
    private boolean isPremium(String id){
        // TODO: dokoncit verifikaciu
        return false;
    }

    /**
     * Vrati string zakodovany v base64.
     * @param input
     * @return - string - text zakodovany v base64
     */
    private String encodeId(String input){
        return new String( Base64.encodeBase64(input.getBytes()) );
    }

    /**
     * Vrati string dekodovany z base64.
     * @param id
     * @return - string - text dekodovany z base64
     */
    private String decodeId(String id){
        return new String( Base64.decodeBase64(id.getBytes()) );
    }

    /**
     * Vrati obrazok k danemu suboru - ak neexistuje, vrati default obrazok
     * @param filePath - cesta k obrazku bez extension
     * @return - String
     */
    private String getImagePath (String filePath, HttpServletRequest request){

        //Logger.debug(GridEditorController.class, "getImagePath, filePath="+filePath);

        // AK EXISTUJE JPG
        String path = filePath + ".jpg";
        IwcmFile img = new IwcmFile(Tools.getRealPath(path));
        //Logger.debug(GridEditorController.class, "getImagePath, testing jpg="+img.getAbsolutePath());
        if (img.exists()) return path;

        // AK EXISTUJE PNG
        path = filePath + ".png";
        img = new IwcmFile(Tools.getRealPath(path));
        //Logger.debug(GridEditorController.class, "getImagePath, testing png="+img.getAbsolutePath());
        if (img.exists()) return path;


        // AK EXISTUJE SVG
        path = filePath + ".svg";
        img = new IwcmFile(Tools.getRealPath(path));
        //Logger.debug(GridEditorController.class, "getImagePath, testing svg="+img.getAbsolutePath());
        if (img.exists()) return path;

        // AK NEEXISTUJE ANI JEDEN, vrat default obrazok
        path = getDataDirPath(null, request) + "default.png";
        img = new IwcmFile(Tools.getRealPath(path));

        return path;
    }

    private boolean isUserAllowed(HttpServletRequest request){
        Identity user = UsersDB.getCurrentUser(request);
        return ( user!=null && user.isAdmin() );
    }

    private String getTextKey(HttpServletRequest request, String textKey){


        Prop prop = Prop.getInstance(request);
        String value = prop.getText(Tools.replace( textKeyPrefix+Tools.replace(textKey, "/", ".") , "..", "."));

        // ak nema taky kluc - vrat relativnu cestu od /grideditor/data/
        if(value.indexOf(textKeyPrefix) == 0)
        {
            String keyFixed = textKey;
            try
            {
                int lomka = keyFixed.lastIndexOf("/");
                if (lomka > 0) keyFixed = keyFixed.substring(lomka + 1);
                keyFixed = Tools.replace(keyFixed, "_", " ");
            }
            catch (Exception ex) {}
            return keyFixed;
        }

        return value;
    }
}
