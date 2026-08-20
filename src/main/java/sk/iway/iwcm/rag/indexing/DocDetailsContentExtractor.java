package sk.iway.iwcm.rag.indexing;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.util.EditorUtils;
import sk.iway.iwcm.rag.service.RagEntityType;

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

    public static final RagEntityType ENTITY_TYPE = RagEntityType.DOCUMENT;

    @Override
    public String extractText(DocDetails doc) {
        if (doc == null) return "";

        try {
            String data = doc.getData();
            if (data == null) data = "";

            // Use isLucene=true to preserve diacritics for better embedding quality
            String text = EditorUtils.getDataAsc(data, doc, true, null);

            if(text == null)  text = "";

            // Text can contain a lot of whitespace, trim whitespace's
            return text.replaceAll("\\s+", " ");
        } catch (Exception e) {
            Logger.error(DocDetailsContentExtractor.class, "Error extracting text from doc " + doc.getDocId() + ": " + e.getMessage());
            return "";
        }
    }

    @Override
    public RagEntityType getEntityType() {
        return ENTITY_TYPE;
    }
}
