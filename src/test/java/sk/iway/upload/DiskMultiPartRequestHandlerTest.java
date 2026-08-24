package sk.iway.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class DiskMultiPartRequestHandlerTest {

    @Test
    void shouldNormalizeMacOsNfdFileNameBeforeFileUploadValidation() throws Exception {
        String boundary = "WebJetBoundary";
        String nfcFileName = "Snímka obrazovky.png";
        String nfdFileName = Normalizer.normalize(nfcFileName, Normalizer.Form.NFD);
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + nfdFileName + "\"\r\n"
                + "Content-Type: image/png\r\n"
                + "\r\n"
                + "content\r\n"
                + "--" + boundary + "--\r\n";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("multipart/form-data; boundary=" + boundary);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(body.getBytes(StandardCharsets.UTF_8));

        DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
        DiskMultiPartRequestHandler.UnicodeNormalizingFileUpload upload =
                new DiskMultiPartRequestHandler.UnicodeNormalizingFileUpload(factory);
        List<DiskFileItem> files = upload.parseRequest(request);

        assertFalse(Normalizer.isNormalized(nfdFileName, Normalizer.Form.NFC));
        assertEquals(1, files.size());
        assertEquals(nfcFileName, files.get(0).getName());
    }
}
