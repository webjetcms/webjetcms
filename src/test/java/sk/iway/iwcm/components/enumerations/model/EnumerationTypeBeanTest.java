package sk.iway.iwcm.components.enumerations.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test API compatibility for old EnumerationTypeBean class
 */
@SuppressWarnings("java:S5785")
class EnumerationTypeBeanTest {

    private EnumerationTypeBean enumerationTypeBean;

    @BeforeEach
    public void setUp() {
        enumerationTypeBean = new EnumerationTypeBean();
    }

    @Test
    void testGetAndSetId() {
        Long id = 1L;
        enumerationTypeBean.setId(id);
        assertEquals(id, enumerationTypeBean.getId());
    }

    @Test
    void testGetAndSetHidden() {
        enumerationTypeBean.setHidden(true);
        assertTrue(true==enumerationTypeBean.isHidden());
        enumerationTypeBean.setHidden(false);
        assertTrue(false==enumerationTypeBean.isHidden());
        //enumerationTypeBean.setHidden(null);
        assertTrue(false==enumerationTypeBean.isHidden());
        //assertNull(enumerationTypeBean.getHidden());
    }

    @Test
    void testGetAndSetAllowChildEnumerationType() {
        enumerationTypeBean.setAllowChildEnumerationType(true);
        assertTrue(true==enumerationTypeBean.isAllowChildEnumerationType());
        enumerationTypeBean.setAllowChildEnumerationType(false);
        assertTrue(false==enumerationTypeBean.isAllowChildEnumerationType());
        //enumerationTypeBean.setAllowChildEnumerationType(null);
        assertTrue(false==enumerationTypeBean.isAllowChildEnumerationType());
        //assertNull(enumerationTypeBean.getAllowChildEnumerationType());
    }

    @Test
    void testGetAndSetAllowParentEnumerationData() {
        enumerationTypeBean.setAllowParentEnumerationData(true);
        assertTrue(true==enumerationTypeBean.isAllowParentEnumerationData());
        enumerationTypeBean.setAllowParentEnumerationData(false);
        assertTrue(false==enumerationTypeBean.isAllowParentEnumerationData());
        //enumerationTypeBean.setAllowParentEnumerationData(null);
        assertTrue(false==enumerationTypeBean.isAllowParentEnumerationData());
        //assertNull(enumerationTypeBean.getAllowParentEnumerationData());
    }

}
