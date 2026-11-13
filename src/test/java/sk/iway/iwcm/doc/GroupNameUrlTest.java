package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies that non-breaking spaces cannot pollute folder names or generated URL segments. */
class GroupNameUrlTest extends BaseWebjetTest {
    private static final String NAME = "Akciové ponuky v e-shope";

    /** Folder metadata treats Unicode and HTML non-breaking spaces as ordinary spaces. */
    @ParameterizedTest
    @ValueSource(strings = { " ", "\u00a0", "\u202f", "&nbsp;", "&nbsp; ", "&#160;", "&#xA0;", "&#8239;", "&#x202f;", "&NonBreakingSpace;" })
    void normalizesFolderName(String space) {
        GroupDetails group = new GroupDetails();
        group.setGroupName("Akciové ponuky v" + space + "e-shope");
        assertEquals(NAME, group.getGroupName());
    }

    /** Legacy menu labels must produce clean URLs without changing their stored HTML. */
    @ParameterizedTest
    @ValueSource(strings = { " ", "\u00a0", "\u202f", "&nbsp;", "&nbsp; ", "&#160;", "&#xA0;", "&#8239;", "&#x202f;", "&NonBreakingSpace;" })
    void generatesUrlFromLegacyMenuLabel(String space) {
        boolean originalRemove = Constants.getBoolean("urlRemoveSpojky");
        try {
            Constants.setBoolean("urlRemoveSpojky", false);
            GroupDetails group = new GroupDetails();
            group.setGroupName(NAME);
            String navbar = "Akciové ponuky v" + space + "e-shope";
            group.setNavbar(navbar);

            assertEquals("akciove-ponuky-v-e-shope", group.getUrlDirName());
            assertEquals(navbar, group.getNavbarName());
        } finally {
            Constants.setBoolean("urlRemoveSpojky", originalRemove);
        }
    }

    /** Equivalent page and folder titles must not trigger saves, redirects or another mirroring cycle. */
    @ParameterizedTest
    @ValueSource(strings = { "\u00a0", "&nbsp;", "&nbsp; ", "&#160;", "&#xA0;" })
    void doesNotRenameFolderForEquivalentPageTitle(String space) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(42);
        group.setParentGroupId(1);
        group.setDefaultDocId(7);
        group.setFullPath("/Slovensky/" + NAME);
        group.setGroupName(NAME);
        GroupsDB groups = mock(GroupsDB.class);

        try (var access = mockStatic(GroupsDB.class, CALLS_REAL_METHODS)) {
            access.when(GroupsDB::getInstance).thenReturn(groups);
            when(groups.getGroup(42)).thenReturn(group);

            assertFalse(DocDB.changeGroupTitle(42, 7, "Akciové ponuky v" + space + "e-shope", true));
            verify(groups).getGroup(42);
            verifyNoMoreInteractions(groups);
        }
    }

    /** Normalizing spaces must preserve encoded path separators, ampersands and existing formatting. */
    @Test
    void preservesOtherEntitiesAndMarkup() {
        String name = "R&amp;D &#47; <strong>Team</strong>";
        GroupDetails group = new GroupDetails();
        group.setGroupName(name);
        assertEquals(name, group.getGroupName());
    }
}
