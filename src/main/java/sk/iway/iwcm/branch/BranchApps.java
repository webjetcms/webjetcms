package sk.iway.iwcm.branch;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Applications whose parameter editors support publishing branches. Preview rendering uses the ordinary WriteTag pipeline. */
public final class BranchApps {
    private static final Set<String> SUPPORTED_EDITORS = Set.of(
        "sk.iway.iwcm.components.menu.MenuApp", "sk.iway.iwcm.components.news.NewsApp"
    );

    private BranchApps() { }

    /** Keeps LIVE app selection unchanged and rejects unsupported branch editors before initialization. */
    public static void checkEditorSupport(String className) {
        if (BranchContexts.current().isBranch() && (className==null || !SUPPORTED_EDITORS.contains(className)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This app editor is unavailable in a publishing branch");
    }

}
