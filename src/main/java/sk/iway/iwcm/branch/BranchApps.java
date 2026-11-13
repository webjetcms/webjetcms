package sk.iway.iwcm.branch;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Applications whose parameter editors and component previews support publishing branches. */
public final class BranchApps {
    private static final Map<String,String> SUPPORTED_APPS = Map.of(
        "sk.iway.iwcm.components.menu.MenuApp", "/components/menu/menu_ul_li.jsp",
        "sk.iway.iwcm.components.news.NewsApp", "/components/news/news-velocity.jsp"
    );

    private BranchApps() { }

    /** Keeps LIVE app selection unchanged and rejects unsupported branch editors before initialization. */
    public static void checkEditorSupport(String className) {
        if (BranchContexts.current().isBranch() && (className==null || !SUPPORTED_APPS.containsKey(className)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This app editor is unavailable in a publishing branch");
    }

    /** Accepts exactly one INCLUDE pointing at a supported application's renderer. */
    public static boolean isSupportedPreview(String componentCode) {
        if (componentCode==null) return false;
        String code=componentCode.trim();
        if (!code.startsWith("!INCLUDE(") || !code.endsWith(")!") || code.indexOf(")!")!=code.length()-2) return false;
        int separator=code.indexOf(',',9);
        String path=code.substring(9,separator<0 ? code.length()-2 : separator);
        return SUPPORTED_APPS.containsValue(path);
    }
}
