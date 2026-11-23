package sk.iway.iwcm.system.datatable.editorlocking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EditorLockingRestControllerTest {

    @Test
    void openAndCloseAcceptEntityIdLargerThanIntegerMaximum() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EditorLockingRestController()).build();
        long entityId = 3_075_370_019_140_624L;
        String path = "/" + entityId + "/settings-configuration";

        EditorLockingBean bean = new EditorLockingBean();
        bean.setEntityId(entityId);
        assertEquals(entityId, bean.getEntityId());

        mockMvc.perform(get("/admin/rest/editorlocking/open" + path))
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        mockMvc.perform(get("/admin/rest/editorlocking/close" + path))
            .andExpect(status().isOk());
    }
}
