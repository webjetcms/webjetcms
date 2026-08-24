package sk.iway.iwcm.system.stripes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.Normalizer;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockPart;

class MultipartWrapperTest {

    @Test
    void shouldPreserveDirectoryWhenUploadPathAndFileNameUseNfd() throws Exception {
        String nfcFileName = "Snímka obrazovky.png";
        String nfcDirectory = "/Priečinok";
        String nfdFileName = Normalizer.normalize(nfcFileName, Normalizer.Form.NFD);
        String nfdUploadPath = Normalizer.normalize(nfcDirectory + "/" + nfcFileName, Normalizer.Form.NFD);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/elfinder-connector/");
        request.setParameter("upload_path[]", nfdUploadPath);
        request.addPart(new MockPart("upload[]", nfdFileName, new byte[0]));

        MultipartWrapper wrapper = new MultipartWrapper();
        wrapper.build(request, null, -1);

        assertEquals(nfcDirectory + "/" + nfcFileName, wrapper.getFileParameterNames().nextElement());
    }
}
