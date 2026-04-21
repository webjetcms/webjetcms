package sk.iway.iwcm.rag.indexing;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.util.EditorUtils;

/**
 * Content extractor for DocDetails entities.
 * Uses EditorUtils.getDataAsc with isLucene=true to:
 * - Preserve diacritics (no internationalToEnglish)
 * - Render INCLUDE macros (if fulltextExecuteApps=true)
 * - Strip HTML tags
 * - Append title, keywords, perex, attributes
 */
@Component
public class DocDetailsContentExtractor implements ContentExtractor<DocDetails> {

    public static final String ENTITY_TYPE = "document";

    @Override
    public String extractText(DocDetails doc) {
        if (doc == null) return "";

        try {
            String data = doc.getData();
            if (data == null) data = "";

            // Use isLucene=true to preserve diacritics for better embedding quality
            String text = EditorUtils.getDataAsc(data, doc, true, null);
            return text != null ? text : "";
        } catch (Exception e) {
            Logger.error(DocDetailsContentExtractor.class, "Error extracting text from doc " + doc.getDocId() + ": " + e.getMessage());
            return "";
        }
    }

    @Override
    public String getEntityType() {
        return ENTITY_TYPE;
    }
}
