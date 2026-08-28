package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;

class ConfDBTest {

    @Test
    void setRuntimeValueAppliesSpecialLinkTypeRepresentation() {
        assertEquals(String.valueOf(Constants.LINK_TYPE_HTML), ConfDB.normalizeRuntimeValue("linkType", "html"));
        assertEquals(String.valueOf(Constants.LINK_TYPE_DOCID), ConfDB.normalizeRuntimeValue("linkType", "docid"));

        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            ConfDB.setRuntimeValue("linkType", "html");
            ConfDB.setRuntimeValue("linkType", "docid");

            constants.verify(() -> Constants.setInt("linkType", Constants.LINK_TYPE_HTML));
            constants.verify(() -> Constants.setInt("linkType", Constants.LINK_TYPE_DOCID));
        }
    }
}
