package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UsersDB;

/** Verifies authorization and domain boundaries shared by heat-map endpoints. */
class HeatMapAccessTest {

    private final HeatMapAccess access = new HeatMapAccess();

    /** Date-picker prefixes and inclusive day boundaries work across a daylight-saving transition. */
    @Test
    void validatesDatePickerRangeAndDayBoundaries() {
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        java.time.LocalDate from = java.time.LocalDate.of(2025, 3, 29);
        java.time.LocalDate to = java.time.LocalDate.of(2025, 3, 30);
        String value = "daterange:" + from.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                + "-" + to.atTime(12, 0).atZone(zone).toInstant().toEpochMilli();
        java.util.Date[] dates = access.dateRange(value);
        assertEquals(from.atStartOfDay(zone).toInstant(), dates[0].toInstant());
        assertEquals(to.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1), dates[1].toInstant());
        for (String invalid : new String[] { "-", "123-", "daterange:abc", "0-1", "9999999999999" }) {
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                    () -> access.dateRange(invalid)).getStatusCode());
        }
    }

    private Identity statisticsUser() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("cmp_stat")).thenReturn(true);
        return user;
    }

    /** Anonymous, public, and administrative users without statistics rights cannot request pages. */
    @Test
    void requiresAnAdministrativeStatisticsUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Identity user = statisticsUser();
        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class)) {
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                    () -> access.requireDocument(request, 123)).getStatusCode());

            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            when(user.isAdmin()).thenReturn(false);
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                    () -> access.requireDocument(request, 123)).getStatusCode());

            when(user.isAdmin()).thenReturn(true);
            when(user.isEnabledItem("cmp_stat")).thenReturn(false);
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                    () -> access.requireDocument(request, 123)).getStatusCode());
        }
    }

    /** Viewing every statistics folder must not grant access to another domain or the trash. */
    @Test
    void keepsDomainAndTrashBoundariesForUsersWithAllGroupsPermission() {
        Identity user = statisticsUser();
        when(user.isEnabledItem("cmp_stat_seeallgroups")).thenReturn(true);
        DocDetails document = mock(DocDetails.class);
        when(document.getGroupId()).thenReturn(5);
        GroupDetails group = mock(GroupDetails.class);
        when(group.getGroupId()).thenReturn(5);
        when(group.getDomainName()).thenReturn("public.example");
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroup(5)).thenReturn(group);

        try (MockedStatic<GroupsDB> groupLookup = mockStatic(GroupsDB.class)) {
            groupLookup.when(GroupsDB::getInstance).thenReturn(groups);

            assertTrue(access.canViewDocument(user, "PUBLIC.EXAMPLE", document));
            assertFalse(access.canViewDocument(user, "other.example", document));
            when(groups.isInTrash(5)).thenReturn(true);
            assertFalse(access.canViewDocument(user, "public.example", document));
            when(groups.getGroup(5)).thenReturn(null);
            assertFalse(access.canViewDocument(user, "public.example", document));
        }
    }

    /** Limited users need editable-folder access, but a currently disabled page may have valid history. */
    @Test
    void checksFolderAccessWithoutRequiringCurrentPublication() {
        Identity user = statisticsUser();
        DocDetails document = mock(DocDetails.class);
        when(document.getGroupId()).thenReturn(5);
        when(document.isAvailable()).thenReturn(false);
        GroupDetails group = mock(GroupDetails.class);
        when(group.getGroupId()).thenReturn(5);
        when(group.getDomainName()).thenReturn("public.example");
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroup(5)).thenReturn(group);

        try (MockedStatic<GroupsDB> groupLookup = mockStatic(GroupsDB.class)) {
            groupLookup.when(GroupsDB::getInstance).thenReturn(groups);

            assertFalse(access.canViewDocument(user, "public.example", document));
            groupLookup.when(() -> GroupsDB.isGroupEditable(user, 5)).thenReturn(true);
            assertTrue(access.canViewDocument(user, "public.example", document));
            assertFalse(access.canViewDocument(user, "public.example", null));
        }
    }

    /** Document lookup uses the trusted current domain and never a request-supplied domain. */
    @Test
    void validatesRequestedDocumentAgainstTrustedDomain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("domain", "untrusted.example");
        Identity user = statisticsUser();
        when(user.isEnabledItem("cmp_stat_seeallgroups")).thenReturn(true);
        DocDetails document = mock(DocDetails.class);
        when(document.getGroupId()).thenReturn(5);
        GroupDetails group = mock(GroupDetails.class);
        when(group.getGroupId()).thenReturn(5);
        when(group.getDomainName()).thenReturn("public.example");
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroup(5)).thenReturn(group);
        DocDB docs = mock(DocDB.class);
        when(docs.getBasicDocDetails(123, false)).thenReturn(document);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
                MockedStatic<DocDB> docLookup = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groupLookup = mockStatic(GroupsDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            domains.when(CloudToolsForCore::getDomainName).thenReturn("PUBLIC.EXAMPLE");
            docLookup.when(DocDB::getInstance).thenReturn(docs);
            groupLookup.when(GroupsDB::getInstance).thenReturn(groups);

            assertEquals("public.example", access.currentDomain(request));
            assertSame(document, access.requireDocument(request, 123));
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> access.requireDocument(request, 456)).getStatusCode());

            domains.when(CloudToolsForCore::getDomainName).thenReturn("other.example");
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> access.requireDocument(request, 123)).getStatusCode());
        }
    }

    /** An unresolved domain must not silently broaden a statistics query to all tenants. */
    @Test
    void rejectsAnUnknownDomain() {
        try (MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class)) {
            domains.when(CloudToolsForCore::getDomainName).thenReturn("unknown");
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                    () -> access.currentDomain(new MockHttpServletRequest())).getStatusCode());
        }
    }
}
