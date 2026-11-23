package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/** Covers presentation of encoded page titles without mutating the source records. */
class WebpagesServiceOptionsTest {
    /** Selector labels decode slashes while keeping stored full-path encoding and other entities intact. */
    @Test
    void decodesOnlyDisplaySlashes() {
        DocDetails doc = new DocDetails();
        doc.setDocId(42);
        doc.setTitle("autotest/Header &amp; footer");
        String storedTitle = doc.getTitle();
        DatatablePageImpl<DocDetails> page = new DatatablePageImpl<>(List.of());

        WebpagesService.addDocOptions(page, "headerDocId,footerDocId", List.of(doc));

        var option = page.getOptions().get("headerDocId,footerDocId").get(0);
        assertEquals("autotest/Header &amp; footer", option.getLabel());
        assertEquals("42", option.getValue());
        assertEquals(storedTitle, doc.getTitle());
        assertEquals("autotest&#47;Header &amp; footer", storedTitle);
    }
}
