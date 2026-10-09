package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;

/** Verifies batched cluster summaries never broaden the authorized administrator set. */
class SessionClusterSummaryTest {
    /** One query reads all nodes while excluding foreign and invalidated sessions and tolerating malformed node data. */
    @Test
    @SuppressWarnings("unchecked")
    void filtersClusterRecordsToAuthorizedUsers() throws Exception {
        ResultSet valid = mock(ResultSet.class);
        when(valid.getString("content")).thenReturn("""
            [{"sessionId":"allowed","loggedUserId":2,"browserName":"Firefox","browserVersion":"143.1","operatingSystem":"Linux","operatingSystemVersion":"6.8","lastActivity":42},
             {"sessionId":"foreign","loggedUserId":3},
             {"sessionId":"invalid","loggedUserId":2,"remoteAddr":"INVALIDATE"}]
            """);
        ResultSet malformed = mock(ResultSet.class);
        when(malformed.getString("content")).thenReturn("[broken]");
        try (var cleanup = mockConstruction(SimpleQuery.class);
             var queries = mockConstruction(ComplexQuery.class, withSettings().defaultAnswer(RETURNS_SELF), (query, context) -> {
                 doAnswer(call -> {
                     Mapper<Object> mapper = call.getArgument(0);
                     mapper.map(valid); mapper.map(malformed);
                     return List.of();
                 }).when(query).list(any(Mapper.class));
             })) {
            var sessions = SessionClusterService.getSessionsForUsers(Set.of(2));
            assertEquals(1, sessions.size());
            assertEquals("allowed", sessions.get(0).getSessionId());
            assertEquals("Linux", sessions.get(0).getOperatingSystem());
            assertEquals("143.1", sessions.get(0).getBrowserVersion());
            assertEquals("6.8", sessions.get(0).getOperatingSystemVersion());
            assertEquals(1, queries.constructed().size());
        }
    }

    /** An empty authorized set returns before creating a database query or a cleanup command. */
    @Test
    void doesNotQueryForAnEmptyAuthorizedSet() {
        try (var queries = mockConstruction(ComplexQuery.class); var cleanup = mockConstruction(SimpleQuery.class)) {
            assertTrue(SessionClusterService.getSessionsForUsers(Set.of()).isEmpty());
            assertTrue(queries.constructed().isEmpty());
            assertTrue(cleanup.constructed().isEmpty());
        }
    }
}
