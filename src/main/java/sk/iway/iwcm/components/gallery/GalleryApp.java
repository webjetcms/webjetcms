package sk.iway.iwcm.components.gallery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;

@WebjetComponent("sk.iway.iwcm.components.gallery.GalleryApp")
@WebjetAppStore(nameKey = "components.gallery.title", descKey = "components.gallery.desc", itemKey="menuGallery", imagePath = "/components/gallery/editoricon.png", galleryImages = "/components/gallery/", componentPath = "/components/gallery/gallery.jsp")
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "componentIframe", title = "components.gallery.images", content = "")
})
@Getter
@Setter
public class GalleryApp extends WebjetComponentAbstract {

    private static final String STYLE_FILE_PREFIX = "gallery-";
    private static final String STYLE_FILE_SUFFIX = ".jsp";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title="components.gallery.visual_style")
    private String style;

    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "basic", className = "dt-tree-dir-simple", title="components.gallery.dir", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery")
            }
        )
    })
    private String dir = "/images/gallery";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title="components.gallery.also_subfolders")
    private boolean recursive = false;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "basic", title="components.news.pageSize")
    private int itemsOnPage = 10;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title="&nbsp;", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.gallery.sort.mode")
            },
            options = {
                @DataTableColumnEditorAttr(key = "components.gallery.sort.alphabet", value = "title"),
                @DataTableColumnEditorAttr(key = "components.gallery.sort.date", value = "date"),
                @DataTableColumnEditorAttr(key = "components.gallery.sort.priority", value = "priority")
            }
        )
    })
    private String orderBy = "title";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title="&nbsp;", editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.gallery.sort.asc", value = "asc"),
                @DataTableColumnEditorAttr(key = "components.gallery.sort.desc", value = "desc")
            }
        )
    })
    private String orderDirection = "asc";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title="components.gallery.showShortDescription", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.gallery.showOnThumbsPage")
            })
    })
    private boolean thumbsShortDescription = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title="components.gallery.showShortDescription", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.gallery.showOnDetailPage")
            })
    })
    private boolean shortDescription = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title="components.gallery.showLongDescription")
    private boolean longDescription = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title="components.gallery.showAuthor")
    private boolean author = true;


    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "basic", title="components.news.perexGroup", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.tab.filter"),
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
            }
        )
    })
	private Integer[] perexGroup;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframe", title="&nbsp;")
    private String iframe  = "/admin/v9/apps/gallery/?dir={dir}";

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        String uploadSubdir = UploadFileTools.getPageUploadSubDir(componentRequest.getDocId(), componentRequest.getGroupId(), componentRequest.getPageTitle(), "/images/gallery");
        IwcmFile uploadDirFile = new IwcmFile(Tools.getRealPath(uploadSubdir));
        if ("/images/gallery".equals(dir)) {
            if (uploadDirFile.exists() == false) {
                boolean created = uploadDirFile.mkdirs();
                if (created) dir = uploadSubdir;
            } else {
                dir = uploadSubdir;
            }
        }
        style = Constants.getString("galleryDefaultStyle");
    }

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        //add options for style
        List<OptionDto> styleOptions = new ArrayList<>();
        Prop prop = Prop.getInstance(request);

		styleOptions.add(new OptionDto(prop.getText("components.gallery.visual_style.prettyPhoto"), "prettyPhoto", null));
		styleOptions.add(new OptionDto(prop.getText("components.gallery.visual_style.photoSwipe"), "photoSwipe", null));
		Set<String> addedStyles = new HashSet<>();
		addedStyles.add("prettyPhoto");
		addedStyles.add("photoSwipe");

		//add all JSP files from the installation-specific and common gallery folders
		for (String name : getStyleNames()) {
            addPair(name, styleOptions, prop, addedStyles);
		}

		//check if the current style is in the list
		if (Tools.isNotEmpty(getStyle()))
		{
			addPair(getStyle(), styleOptions, prop, addedStyles);
		}

        options.put("style", styleOptions);

        //add perex groups
        List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(componentRequest.getGroupId());
        List<OptionDto> perexGroupOptions = new ArrayList<>();
        for (PerexGroupBean pg : perexGroups) {
            perexGroupOptions.add(new OptionDto(pg.getPerexGroupName(), ""+pg.getPerexGroupId(), null));
        }
        options.put("perexGroup", perexGroupOptions);

        return options;
    }

    /**
     * Returns additional gallery styles from the installation-specific directory and the common gallery directory.
     * The installation-specific directory is processed first to match the rendering priority in gallery.jsp.
     *
     * @return unique gallery style names
     */
    public static List<String> getStyleNames() {
        List<IwcmFile> styleDirectories = new ArrayList<>();
        String installName = Constants.getInstallName();
        if (Tools.isNotEmpty(installName)) {
            styleDirectories.add(new IwcmFile(Tools.getRealPath("/components/" + installName + "/gallery/")));
        }
        styleDirectories.add(new IwcmFile(Tools.getRealPath("/components/gallery/")));

        return getStyleNames(styleDirectories.toArray(new IwcmFile[0]));
    }

    static List<String> getStyleNames(IwcmFile... styleDirectories) {
        Set<String> styleNames = new LinkedHashSet<>();
        if (styleDirectories == null) return new ArrayList<>(styleNames);

        for (IwcmFile styleDirectory : styleDirectories) {
            if (styleDirectory == null) continue;

            IwcmFile[] files = styleDirectory.listFiles();
            if (files == null) continue;

            for (IwcmFile file : files) {
                if (file == null || file.isFile() == false || file.length()<10) continue;

                String fileName = file.getName();
                if (fileName.startsWith(STYLE_FILE_PREFIX) == false || fileName.endsWith(STYLE_FILE_SUFFIX) == false) continue;
                if (fileName.length() <= STYLE_FILE_PREFIX.length() + STYLE_FILE_SUFFIX.length()) continue;
                if ("gallery-basket.jsp".equals(fileName)) continue; //skip basket style, it is not a visual style

                String styleName = fileName.substring(STYLE_FILE_PREFIX.length(), fileName.length() - STYLE_FILE_SUFFIX.length());
                styleNames.add(styleName);
            }
        }

        return new ArrayList<>(styleNames);
    }

    /**
     * Try to translate the name of the style and add it to the list of options
     * @param name
     * @param styleOptions
     * @param prop
     * @param addedStyles
     */
    private void addPair(String name, List<OptionDto> styleOptions, Prop prop, Set<String> addedStyles)
	{
		if (addedStyles.add(name) == false) return;

		String desc = prop.getText("components.gallery.visual_style."+name);
		if (desc.startsWith("components.gallery")) desc = name;

		styleOptions.add(new OptionDto(desc, name, null));
	}


}
