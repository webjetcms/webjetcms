package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;

/** Verifies inheritance and global switches without selecting a device-specific template. */
class StatisticsModeTest {
    /** Existing records inherit ordinary statistics, while explicit template settings override their group. */
    @Test
    void resolvesInheritanceAndGlobalDisabling() {
        assertEquals(StatisticsMode.STATISTICS, StatisticsMode.resolve(null, null, false, true));
        assertEquals(StatisticsMode.STATISTICS, StatisticsMode.resolve(99, 99, false, true));
        assertEquals(StatisticsMode.HEATMAP, StatisticsMode.resolve(0, 2, false, true));
        assertEquals(StatisticsMode.STATISTICS, StatisticsMode.resolve(1, 3, false, true));
        assertEquals(StatisticsMode.OFF, StatisticsMode.resolve(3, 2, false, true));
        assertEquals(StatisticsMode.STATISTICS, StatisticsMode.resolve(2, 0, false, false));
        assertEquals(StatisticsMode.OFF, StatisticsMode.resolve(2, 2, true, true));
    }

    /** Folder enforcement and refreshed template-group settings take effect on the next resolution. */
    @Test
    void usesForcedFolderTemplateAndRefreshedGroupCache() {
        DocDetails document = new DocDetails();
        document.setTempId(10);
        document.setGroupId(20);
        GroupDetails folder = mock(GroupDetails.class);
        when(folder.isForceTheUseOfGroupTemplate()).thenReturn(true);
        when(folder.getTempId()).thenReturn(11);
        GroupsDB folders = mock(GroupsDB.class);
        when(folders.getGroup(20)).thenReturn(folder);
        TemplateDetails template = new TemplateDetails();
        template.setTemplatesGroupId(30L);
        TemplatesDB templates = mock(TemplatesDB.class);
        when(templates.getTemplate(11)).thenReturn(template);
        TemplatesGroupBean group = new TemplatesGroupBean();
        group.setStatisticsMode(2);
        TemplatesGroupDB groups = mock(TemplatesGroupDB.class);
        when(groups.getByIdCached(30L)).thenReturn(group);
        try (MockedStatic<GroupsDB> folderLookup = mockStatic(GroupsDB.class);
                MockedStatic<TemplatesDB> templateLookup = mockStatic(TemplatesDB.class);
                MockedStatic<TemplatesGroupDB> groupLookup = mockStatic(TemplatesGroupDB.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            folderLookup.when(GroupsDB::getInstance).thenReturn(folders);
            templateLookup.when(TemplatesDB::getInstance).thenReturn(templates);
            groupLookup.when(TemplatesGroupDB::getInstance).thenReturn(groups);
            constants.when(() -> Constants.getBoolean("statEnableClickTracking")).thenReturn(true);
            assertEquals(StatisticsMode.HEATMAP, StatisticsMode.forDocument(document));
            group.setStatisticsMode(3);
            assertEquals(StatisticsMode.OFF, StatisticsMode.forDocument(document));
            template.setStatisticsMode(1);
            assertEquals(StatisticsMode.STATISTICS, StatisticsMode.forDocument(document));
        }
    }
}
