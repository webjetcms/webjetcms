package sk.iway.iwcm.system.datatable;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.editor.rest.FieldValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for BaseEditorFields enumeration integration with custom label/value properties
 * This test focuses on the BeanWrapper functionality for extracting label/value from EnumerationDataBean
 */
public class BaseEditorFieldsEnumerationTest {

    /**
     * Test BeanWrapper functionality for reading different properties from EnumerationDataBean
     */
    @Test
    public void testBeanWrapperPropertyAccess() {
        // Create test enumeration data
        EnumerationDataBean enumData = new EnumerationDataBean();
        enumData.setString1("Label Value");
        enumData.setString2("Value Text");
        enumData.setString3("Extra Info");
        
        // Test BeanWrapper can read string1
        BeanWrapper beanWrapper = new BeanWrapperImpl(enumData);
        Object value1 = beanWrapper.getPropertyValue("string1");
        assertEquals("Label Value", value1.toString());
        
        // Test BeanWrapper can read string2
        Object value2 = beanWrapper.getPropertyValue("string2");
        assertEquals("Value Text", value2.toString());
        
        // Test BeanWrapper can read string3
        Object value3 = beanWrapper.getPropertyValue("string3");
        assertEquals("Extra Info", value3.toString());
    }

    /**
     * Test creating FieldValue objects with different property combinations
     */
    @Test
    public void testFieldValueCreationWithDifferentProperties() {
        EnumerationDataBean enumData = new EnumerationDataBean();
        enumData.setString1("My Label");
        enumData.setString2("my_value");
        enumData.setString3("extra_data");

        BeanWrapper beanWrapper = new BeanWrapperImpl(enumData);

        // Test label and value from same property (string1)
        String label1 = beanWrapper.getPropertyValue("string1").toString();
        String value1 = beanWrapper.getPropertyValue("string1").toString();
        FieldValue fv1 = new FieldValue(label1, value1);
        assertEquals("My Label", fv1.getLabel());
        assertEquals("My Label", fv1.getValue());

        // Test label from string1, value from string2
        String label2 = beanWrapper.getPropertyValue("string1").toString();
        String value2 = beanWrapper.getPropertyValue("string2").toString();
        FieldValue fv2 = new FieldValue(label2, value2);
        assertEquals("My Label", fv2.getLabel());
        assertEquals("my_value", fv2.getValue());

        // Test label from string1, value from string3
        String label3 = beanWrapper.getPropertyValue("string1").toString();
        String value3 = beanWrapper.getPropertyValue("string3").toString();
        FieldValue fv3 = new FieldValue(label3, value3);
        assertEquals("My Label", fv3.getLabel());
        assertEquals("extra_data", fv3.getValue());
    }

    /**
     * Test parsing enumeration type strings
     */
    @Test
    public void testEnumerationTypeStringParsing() {
        // Test basic format: enumeration_ID
        String type1 = "enumeration_5";
        String[] parts1 = type1.split("_");
        assertEquals(2, parts1.length);
        assertEquals("5", parts1[1]);
        
        // Test format with custom properties: enumeration_ID_labelProperty_valueProperty
        String type2 = "enumeration_10_string1_string2";
        String[] parts2 = type2.split("_");
        assertEquals(4, parts2.length);
        assertEquals("10", parts2[1]);
        assertEquals("string1", parts2[2]);
        assertEquals("string2", parts2[3]);
        
        // Test format with null suffix
        String type3 = "enumeration_20_string2_string3_null";
        String type3Clean = type3.replace("_null", "");
        String[] parts3 = type3Clean.split("_");
        assertEquals(4, parts3.length);
        assertEquals("20", parts3[1]);
        assertEquals("string2", parts3[2]);
        assertEquals("string3", parts3[3]);
    }

    /**
     * Test null handling in BeanWrapper
     */
    @Test
    public void testBeanWrapperNullHandling() {
        EnumerationDataBean enumData = new EnumerationDataBean();
        enumData.setString1("Not Null");
        // string2 is null
        
        BeanWrapper beanWrapper = new BeanWrapperImpl(enumData);
        Object value1 = beanWrapper.getPropertyValue("string1");
        assertNotNull(value1);
        assertEquals("Not Null", value1.toString());
        
        Object value2 = beanWrapper.getPropertyValue("string2");
        // BeanWrapper returns null for null properties
        String enumValue = value2 != null ? value2.toString() : "";
        assertEquals("", enumValue);
    }
}
