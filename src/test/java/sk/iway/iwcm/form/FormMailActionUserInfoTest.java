package sk.iway.iwcm.form;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;

/** Verifies request-based visitor notifications remain compatible with both method signatures. */
class FormMailActionUserInfoTest {

    /** Replaces original and uppercase placeholders and preserves multiple request values when form data is absent. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void usesRequestParametersWithoutFormData(boolean originalSignature) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("last-name-1", "Doe");
        request.setParameter("interests", "First", "Second");
        DocDetails notification = new DocDetails();
        notification.setTitle("Confirmation");
        notification.setData("!last-name-1! / !LAST-NAME-1! / !interests!");
        DocDB docDB = mock(DocDB.class);
        when(docDB.getDoc(123)).thenReturn(notification);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class);
                MockedStatic<SendMail> mail = mockStatic(SendMail.class)) {
            docs.when(DocDB::getInstance).thenReturn(docDB);

            if (originalSignature) {
                FormMailAction.sendUserInfo(123, 42, "visitor@example.com", List.of(), Map.of(), request);
            } else {
                FormMailAction.sendUserInfo(123, 42, "visitor@example.com", List.of(), Map.of(), request, null);
            }

            mail.verify(() -> SendMail.send(any(), any(), eq("visitor@example.com"), isNull(), isNull(), isNull(),
                eq("Confirmation"), eq("<html><body>Doe / Doe / First;;\nSecond</body></html>"), anyString(), eq("")));
        }
    }
}
