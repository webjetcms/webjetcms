package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;

/** Verifies comparison labels retain the editor's per-type keys and current translations. */
class BranchMergeLabelsTest {
    @Test void usesInheritedAndNestedEditorMetadataForEveryComparedField() {
        Prop prop=mock(Prop.class);when(prop.getText(anyString())).thenAnswer(call->call.getArgument(0));
        var labels=BranchMergeLabels.translate(prop);
        assertEquals("editor.tab.html_header",labels.get("DOCUMENT").get("html_head"));
        assertEquals("groupedit.htmlHead",labels.get("GROUP").get("html_head"));
        assertEquals("editor.field_a",labels.get("DOCUMENT").get("field_a"));
        assertEquals("groupedit.field_a",labels.get("GROUP").get("field_a"));
        assertEquals("components.media.field_a",labels.get("MEDIA").get("field_a"));
        assertEquals("editor.superior_directory",labels.get("DOCUMENT").get("group_id"));
        assertEquals("group.superior_directory",labels.get("GROUP").get("parent_group_id"));
        assertEquals("editor.media.title (en)",labels.get("MEDIA").get("media_title_en"));
        for(BranchType type:BranchType.values()) {
            Set<String> expected=new HashSet<>(BranchFields.forType(type).supported());
            if(type==BranchType.MEDIA)expected.add("group_ids_snapshot");
            assertEquals(expected,labels.get(type.name()).keySet(),type.name());
        }
    }

    @Test void readsChangedOriginalTranslationWithoutCachingItsValue() {
        IwayProperties properties=new IwayProperties();
        Prop keys=mock(Prop.class);
        when(keys.getText(anyString())).thenAnswer(call->{String key=call.getArgument(0);properties.setProperty(key,key);return key;});
        BranchMergeLabels.translate(keys);
        Prop prop=mock(Prop.class,CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(prop,"properties",properties);
        ReflectionTestUtils.setField(prop,"language","sk");
        properties.setProperty("editor.tab.html_header","HTML kód do hlavičky");
        assertEquals("HTML kód do hlavičky",BranchMergeLabels.translate(prop).get("DOCUMENT").get("html_head"));
        properties.setProperty("editor.tab.html_header","Zákaznícka hlavička <HTML>");
        assertEquals("Zákaznícka hlavička <HTML>",BranchMergeLabels.translate(prop).get("DOCUMENT").get("html_head"));
    }
}
