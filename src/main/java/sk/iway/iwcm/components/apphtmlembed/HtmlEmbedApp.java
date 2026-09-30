package sk.iway.iwcm.components.apphtmlembed;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.tags.support.ResponseUtils;

@WebjetComponent("sk.iway.iwcm.components.apphtmlembed.HtmlEmbedApp")
@WebjetAppStore(
    nameKey = "components.app-htmlembed.title",
    descKey = "components.app-htmlembed.desc",
    itemKey = "cmp_app-htmlembed",
    imagePath = "/components/app-htmlembed/editoricon.png",
    galleryImages = "/components/app-htmlembed/",
    componentPath = "/components/app-htmlembed/embed.jsp")
@Getter
@Setter
public class HtmlEmbedApp extends WebjetComponentAbstract {

    private static final String PREVIEW_CODE_START =
        "<pre class=\"html-embed-preview-code\" style=\"white-space: pre-wrap; overflow-wrap: anywhere; margin: 4px;\">";
    private static final String PREVIEW_CODE_END = "</pre>";

    @DataTableColumn(
        inputType = DataTableColumnType.BASE64,
        tab = "basic",
        title="components.app-htmlembed.editor_components.vloz_html_kod",
        className = "textarea-code-small",
        editor = {
            @DataTableColumnEditor(message = "components.app-htmlembed.desc")
        }
    )
    private String html;

    /**
     * Returns escaped source code for script-only embeds in the component preview.
     * The original embed is returned in all other cases, including normal page rendering.
     *
     * @param htmlCode HTML embed source code
     * @param previewMode true when rendering the editor component preview
     * @return HTML to render
     */
    public static String getPreviewHtmlCode(String htmlCode, boolean previewMode) {
        if (previewMode == false || isScriptOnly(htmlCode) == false) return htmlCode;

        // The parent WriteTag processes component output, so hide WebJET macro delimiters from it.
        String escapedHtmlCode = ResponseUtils.filter(htmlCode).replace("!", "&#33;");
        return PREVIEW_CODE_START + escapedHtmlCode + PREVIEW_CODE_END;
    }

    /**
     * Checks whether the fragment contains only top-level script elements, whitespace and comments.
     *
     * @param htmlCode HTML embed source code
     * @return true when the fragment contains at least one script and no renderable HTML
     */
    public static boolean isScriptOnly(String htmlCode) {
        if (Tools.isEmpty(htmlCode)) return false;

        boolean scriptFound = false;
        try {
            for (Node node : Jsoup.parseBodyFragment(htmlCode).body().childNodes()) {
                if (node instanceof TextNode textnode) {
                    if (textnode.getWholeText().isBlank()) continue;
                    return false;
                }
                if (node instanceof Comment) continue;
                if (node instanceof Element element && "script".equals(element.normalName())) {
                    scriptFound = true;
                    continue;
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return scriptFound;
    }
}
