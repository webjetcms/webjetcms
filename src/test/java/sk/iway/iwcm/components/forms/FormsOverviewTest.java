package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.GroupsDB;

/** Verifies the relocated form overview's access checks, periods and bounded response. */
class FormsOverviewTest {
    private final FormsRepository repository = mock(FormsRepository.class);
    private final FormsServiceImpl service = spy(new FormsServiceImpl(repository, null, null, null));

    /** Invalid filters fail before any repository access. */
    @Test
    void rejectsInvalidFilters() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.getOverview(null, "example.test", 365, null)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.getOverview(null, "example.test", 7, " ")).getStatusCode());
        verifyNoInteractions(repository);
    }

    /** The module permission and administrator login remain necessary for the service. */
    @Test
    void requiresFormPermissionAndCurrentDomain() {
        Identity user = mock(Identity.class);
        assertThrows(AccessDeniedException.class, () -> service.getOverview(null, "example.test", 7, null));
        when(user.isAdmin()).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> service.getOverview(user, "example.test", 7, null));
        when(user.isEnabledItem("cmp_form")).thenReturn(true);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
            () -> service.getOverview(user, "", 7, null)).getStatusCode());
        verifyNoInteractions(repository);
    }

    /** Calendar periods include today's submissions across a daylight-saving transition. */
    @Test
    void recentFormsIncludeSevenCalendarDatesThroughToday() {
        ZoneId zone = ZoneId.of("Europe/Bratislava");
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:34:56Z"), zone);
        var range = FormsServiceImpl.recentDays(7, clock);
        assertEquals(clock.millis(), range.until());
        assertEquals(LocalDate.of(2026, 3, 26).atStartOfDay(zone).toInstant().toEpochMilli(), range.from());
        long todaySubmission = Instant.parse("2026-04-01T09:00:00Z").toEpochMilli();
        assertTrue(todaySubmission >= range.from() && todaySubmission < range.until());
    }

    /** Authorized form choices and full counts accompany only the six newest submission previews. */
    @Test
    @SuppressWarnings("unchecked")
    void returnsAuthorizedChoicesAndRejectsMissingSelections() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("cmp_form")).thenReturn(true);
        FormsEntity form = new FormsEntity();
        form.setId(42L);
        form.setFormName("Contact / EN");
        form.setCreateDate(new Date());
        doReturn(List.of(form)).when(service).getFormsList(user);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call -> {
            Pageable pageable = call.getArgument(1);
            assertEquals(6, pageable.getPageSize());
            assertTrue(pageable.getSort().getOrderFor("createDate").isDescending());
            return new PageImpl<>(List.of(form), pageable, 12);
        });
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroupsAll()).thenReturn(List.of());
        try (var groupLookup = mockStatic(GroupsDB.class)) {
            groupLookup.when(GroupsDB::getInstance).thenReturn(groups);
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.getOverview(user, "example.test", 7, "Unavailable")).getStatusCode());
            verifyNoInteractions(repository);
            Map<String, Object> result = service.getOverview(user, "example.test", 7, form.getFormName());
            assertEquals(12L, result.get("total"));
            List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
            assertEquals("/apps/form/admin/detail/?formName=Contact+%2F+EN", items.get(0).get("url"));
            assertEquals(form.getCreateDate().getTime(), items.get(0).get("date"));
            assertEquals(List.of(Map.of("id", form.getFormName(), "title", form.getFormName())), result.get("options"));
        }
    }
}
