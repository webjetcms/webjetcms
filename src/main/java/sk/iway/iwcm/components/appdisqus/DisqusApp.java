package sk.iway.iwcm.components.appdisqus;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.appdisqus.DisqusApp")
@WebjetAppStore(
    nameKey = "components.app-disqus.title", 
    descKey = "components.app-disqus.desc", 
    itemKey = "cmp_app-disqus", 
    imagePath = "/components/app-disqus/editoricon.png", 
    galleryImages = "/components/app-disqus/", 
    componentPath = "/components/app-disqus/disqus.jsp"
)
@Getter
@Setter
public class DisqusApp extends WebjetComponentAbstract {
    
    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.app-disqus_info"
    )
    private String explain;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-disqus.siteName"
    )
    private String login;
    
}