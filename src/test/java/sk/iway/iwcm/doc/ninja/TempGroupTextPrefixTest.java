package sk.iway.iwcm.doc.ninja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link TempGroup#getTextPrefix()} returns the stored prefix
 * instead of calling itself, which ended with StackOverflowError.
 */
class TempGroupTextPrefixTest {

    @Test
    void textPrefixIsNullWithoutTemplateGroupAndCanBeSet() {
        Page page = mock(Page.class);
        when(page.getDoc()).thenReturn(null);
        Ninja ninja = mock(Ninja.class);
        when(ninja.getPage()).thenReturn(page);

        TempGroup tempGroup = new TempGroup(ninja);

        assertNull(tempGroup.getTextPrefix());

        tempGroup.setTextPrefix("custom");
        assertEquals("custom", tempGroup.getTextPrefix());
    }
}
