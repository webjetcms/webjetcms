package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Constants;
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

    /** Document access respects the trusted domain, trash and folder rights, including single-domain sites. */
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
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
                MockedStatic<DocDB> docLookup = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groupLookup = mockStatic(GroupsDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            constants.when(() -> Constants.getBoolean("multiDomainEnabled")).thenReturn(true);
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
            domains.when(CloudToolsForCore::getDomainName).thenReturn("public.example");
            when(groups.isInTrash(5)).thenReturn(true);
            assertFalse(access.canViewDocument(user, "public.example", document));
            when(groups.isInTrash(5)).thenReturn(false);
            when(user.isEnabledItem("cmp_stat_seeallgroups")).thenReturn(false);
            assertFalse(access.canViewDocument(user, "public.example", document));
            groupLookup.when(() -> GroupsDB.isGroupEditable(user, 5)).thenReturn(true);
            assertSame(document, access.requireDocument(request, 123));

            domains.when(CloudToolsForCore::getDomainName).thenReturn("unknown");
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                    () -> access.currentDomain(request)).getStatusCode());
            constants.when(() -> Constants.getBoolean("multiDomainEnabled")).thenReturn(false);
            when(group.getDomainName()).thenReturn("");
            assertEquals("", access.currentDomain(request));
            assertSame(document, access.requireDocument(request, 123));
        }
    }
}
