package sk.iway.iwcm.components.enumerations.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Tools;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test API compatibility for old EnumerationDataBean class
 */
@SuppressWarnings("java:S5785")
class EnumerationDataBeanTest {

    private EnumerationDataBean enumerationDataBean;

    @BeforeEach
    public void setUp() {
        enumerationDataBean = new EnumerationDataBean();
    }

    @Test
    void testGetAndSetId() {
        Long id = 1L;
        enumerationDataBean.setId(id);
        assertEquals(id, enumerationDataBean.getId());

        enumerationDataBean.setEnumerationDataId(20);
        assertEquals(20L, enumerationDataBean.getId());
        assertTrue(enumerationDataBean.getEnumerationDataId()==enumerationDataBean.getId().intValue());
    }

    @Test
    void testGetAndSetBoolean() {
        boolean b = true;
        enumerationDataBean.setBoolean1(b);
        enumerationDataBean.setBoolean2(false);
        enumerationDataBean.setBoolean3(b);
        enumerationDataBean.setBoolean4(false);
        assertTrue(b==enumerationDataBean.isBoolean1());
        assertTrue(false==enumerationDataBean.isBoolean2());
        assertTrue(b==enumerationDataBean.isBoolean3());
        assertTrue(false==enumerationDataBean.isBoolean4());
        //enumerationDataBean.setBoolean1(null);
        //assertTrue(false==enumerationDataBean.isBoolean1());
        //assertNull(enumerationDataBean.getBoolean1());
    }

    @Test
    void testChildEnumerationType() {
        EnumerationTypeBean tb = new EnumerationTypeBean();
        tb.setId(10L);
        enumerationDataBean.setChildEnumerationType(tb);
        assertEquals(tb.getId(), enumerationDataBean.getChildEnumerationType().getId());
    }

    @Test
    void testGetAndSetDate() {
        Date date = new Date();
        enumerationDataBean.setDate1(date);
        assertEquals(date, enumerationDataBean.getDate1());

        assertEquals(Tools.formatDateTimeSeconds(date), enumerationDataBean.getDate1Str());
    }

    @Test
    void testGetAndSetDecimal() {
        BigDecimal value = new BigDecimal("123.45");
        enumerationDataBean.setDecimal1(value);
        assertEquals(value, enumerationDataBean.getDecimal1());
    }

    @Test
    void testGetAndSetHidden() {
        enumerationDataBean.setHidden(true);
        assertTrue(true==enumerationDataBean.isHidden());
        enumerationDataBean.setHidden(false);
        assertTrue(false==enumerationDataBean.isHidden());
        //enumerationDataBean.setHidden(null);
        //assertTrue(false==enumerationDataBean.isHidden());
        //assertNull(enumerationDataBean.getHidden());
    }

    @Test
    void testGetAndSetParentEnumerationData() {
        EnumerationDataBean parent = new EnumerationDataBean();
        parent.setId(100L);
        enumerationDataBean.setParentEnumerationData(parent);
        assertEquals(parent.getId(), enumerationDataBean.getParentEnumerationData().getId());
    }

    @Test
    void testGetAndSetSortPriority() {
        int sort = 12345;
        enumerationDataBean.setSortPriority(sort);
        assertTrue(sort == enumerationDataBean.getSortPriority());
    }

    @Test
    void testGetAndSetString() {
        String value = "Lorem Ipsum";
        enumerationDataBean.setString1(value);
        assertEquals(value, enumerationDataBean.getString1());
    }

    @Test
    void testGetAndSetType() {
        EnumerationTypeBean type = new EnumerationTypeBean();
        type.setId(300L);
        enumerationDataBean.setType(type);
        assertEquals(type.getId(), enumerationDataBean.getType().getId());
        assertNull(enumerationDataBean.getTypeId());
    }
}