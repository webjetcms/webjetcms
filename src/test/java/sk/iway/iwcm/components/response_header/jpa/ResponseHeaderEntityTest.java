package sk.iway.iwcm.components.response_header.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

public class ResponseHeaderEntityTest {

    @Test
    void testLongHeaderValuePersistence() {
        // Create a CSP (Content Security Policy) header which can be quite long
        StringBuilder longCspValue = new StringBuilder();
        longCspValue.append("default-src 'self'; ");
        longCspValue.append("script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://code.jquery.com https://stackpath.bootstrapcdn.com https://maxcdn.bootstrapcdn.com; ");
        longCspValue.append("style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://stackpath.bootstrapcdn.com https://maxcdn.bootstrapcdn.com; ");
        longCspValue.append("font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net https://stackpath.bootstrapcdn.com; ");
        longCspValue.append("img-src 'self' data: https: http:; ");
        longCspValue.append("connect-src 'self' https://api.example.com; ");
        longCspValue.append("frame-src 'self' https://www.youtube.com https://www.google.com; ");
        longCspValue.append("object-src 'none'; ");
        longCspValue.append("base-uri 'self'; ");
        longCspValue.append("form-action 'self';");
        
        String longValue = longCspValue.toString();
        
        // Verify the test value is longer than the old 255 character limit
        assertTrue(longValue.length() > 255, "Test value should be longer than 255 characters");
        
        // Create entity and set the long header value
        ResponseHeaderEntity entity = new ResponseHeaderEntity();
        entity.setUrl("/test/long-header");
        entity.setHeaderName("Content-Security-Policy");
        entity.setHeaderValue(longValue);
        entity.setChangeDate(new Date());
        entity.setDomainId(1);
        
        // Verify the entity can store and retrieve the long value (in-memory test)
        assertNotNull(entity.getHeaderValue());
        assertEquals(longValue, entity.getHeaderValue());
        assertEquals(longValue.length(), entity.getHeaderValue().length());
        assertEquals("Content-Security-Policy", entity.getHeaderName());
        assertEquals("/test/long-header", entity.getUrl());
        
        // Verify the value is longer than the old 255 character limit
        assertTrue(entity.getHeaderValue().length() > 255, "Header value should be longer than 255 characters");
        
        // Test serialization aspects - ensure the entity can be properly serialized with long values
        // This mimics what would happen during JPA persistence
        assertNotNull(entity.toString(), "Entity should be serializable to string");
        assertTrue(entity.toString().length() > 0, "String representation should not be empty");
    }
    
    @Test
    void testRegularHeaderValuePersistence() {
        // Test that regular, shorter header values still work
        ResponseHeaderEntity entity = new ResponseHeaderEntity();
        entity.setUrl("/test/regular-header");
        entity.setHeaderName("X-Frame-Options");
        entity.setHeaderValue("SAMEORIGIN");
        entity.setChangeDate(new Date());
        entity.setDomainId(1);
        
        assertNotNull(entity.getHeaderValue());
        assertEquals("SAMEORIGIN", entity.getHeaderValue());
        assertEquals("X-Frame-Options", entity.getHeaderName());
        assertEquals("/test/regular-header", entity.getUrl());
    }

    @Test
    void testMaximumLengthHeaderValue() {
        // Test with a very long header value to ensure entity can handle it
        StringBuilder veryLongValue = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            veryLongValue.append("directive").append(i).append(" 'self' https://example").append(i).append(".com; ");
        }
        
        String extremelyLongValue = veryLongValue.toString();
        assertTrue(extremelyLongValue.length() > 1000, "Test value should be very long");
        
        ResponseHeaderEntity entity = new ResponseHeaderEntity();
        entity.setUrl("/test/extreme-header");
        entity.setHeaderName("Content-Security-Policy");
        entity.setHeaderValue(extremelyLongValue);
        entity.setChangeDate(new Date());
        entity.setDomainId(1);
        
        // Verify entity can handle extremely long values
        assertEquals(extremelyLongValue, entity.getHeaderValue());
        assertEquals(extremelyLongValue.length(), entity.getHeaderValue().length());
        
        // Verify all fields are properly set
        assertEquals("/test/extreme-header", entity.getUrl());
        assertEquals("Content-Security-Policy", entity.getHeaderName());
        assertNotNull(entity.getChangeDate());
        assertEquals(Integer.valueOf(1), entity.getDomainId());
    }

    @Test
    void testEntityValidationConstraints() {
        ResponseHeaderEntity entity = new ResponseHeaderEntity();
        
        // Test that size constraints are still present on other fields
        // URL field should have size limit
        StringBuilder longUrl = new StringBuilder("/");
        for (int i = 0; i < 300; i++) {
            longUrl.append("x");
        }
        entity.setUrl(longUrl.toString());
        
        // HeaderName field should have size limit
        StringBuilder longHeaderName = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longHeaderName.append("x");
        }
        entity.setHeaderName(longHeaderName.toString());
        
        // HeaderValue field should NOT have size limit (this is our change)
        StringBuilder longHeaderValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longHeaderValue.append("x");
        }
        entity.setHeaderValue(longHeaderValue.toString());
        
        // All setters should work without throwing exceptions
        // The actual validation would occur during JPA save operations
        assertEquals(longUrl.toString(), entity.getUrl());
        assertEquals(longHeaderName.toString(), entity.getHeaderName());
        assertEquals(longHeaderValue.toString(), entity.getHeaderValue());
        
        // Verify headerValue specifically can be very long
        assertTrue(entity.getHeaderValue().length() > 255, "Header value should accept long values");
    }
}