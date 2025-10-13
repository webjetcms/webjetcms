package sk.iway.iwcm.components.response_header.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ResponseHeaderEntityTest {

    @Test
    void testLongHeaderValue() {
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
        assert longValue.length() > 255 : "Test value should be longer than 255 characters";
        
        // Create entity and set the long header value
        ResponseHeaderEntity entity = new ResponseHeaderEntity();
        entity.setUrl("/test");
        entity.setHeaderName("Content-Security-Policy");
        entity.setHeaderValue(longValue);
        
        // Verify the entity can store and retrieve the long value
        assertNotNull(entity.getHeaderValue());
        assertEquals(longValue, entity.getHeaderValue());
        assertEquals(longValue.length(), entity.getHeaderValue().length());
        
        // Verify the value is longer than the old 255 character limit
        assert entity.getHeaderValue().length() > 255 : "Header value should be longer than 255 characters";
    }
    
    @Test
    void testRegularHeaderValue() {
        // Test that regular, shorter header values still work
        ResponseHeaderEntity entity = new ResponseHeaderEntity();
        entity.setUrl("/test");
        entity.setHeaderName("X-Frame-Options");
        entity.setHeaderValue("SAMEORIGIN");
        
        assertNotNull(entity.getHeaderValue());
        assertEquals("SAMEORIGIN", entity.getHeaderValue());
    }
}