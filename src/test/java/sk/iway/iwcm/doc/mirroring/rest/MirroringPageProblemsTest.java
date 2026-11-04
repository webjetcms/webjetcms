package sk.iway.iwcm.doc.mirroring.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.mirroring.dto.MirroringDTO;
import sk.iway.iwcm.i18n.Prop;

/** Checks branch-scoped diagnostics with real mapping validation and mocked database/cache access. */
class MirroringPageProblemsTest {
    /** Counterparts outside the branch reveal errors, while foreign-domain and unrelated pages are excluded. */
    @Test
    void checksCompleteMappingsForRequestedPagesOnly() throws Exception {
        DocDB docs = mock(DocDB.class);
        GroupsDB groups = mock(GroupsDB.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<Integer, Integer> rows = Map.of(10, 500, 11, 500, 12, 500, 20, 600, 21, 600, 22, 600, 23, 600);
        try (MockedStatic<DocDB> docAccess = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groupAccess = mockStatic(GroupsDB.class);
                MockedStatic<TemplatesDB> templates = mockStatic(TemplatesDB.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
                MockedStatic<Prop> translations = mockStatic(Prop.class);
                MockedConstruction<ComplexQuery> queries = mockConstruction(ComplexQuery.class, (query, context) -> {
                    when(query.setSql(anyString())).thenReturn(query);
                    when(query.setParams(any(Object[].class))).thenReturn(query);
                    when(query.list(any())).thenAnswer(invocation -> {
                        Mapper<?> mapper = invocation.getArgument(0);
                        for (Map.Entry<Integer, Integer> entry : rows.entrySet()) {
                            ResultSet row = mock(ResultSet.class);
                            when(row.getInt("doc_id")).thenReturn(entry.getKey());
                            when(row.getInt("sync_id")).thenReturn(entry.getValue());
                            mapper.map(row);
                        }
                        return List.of();
                    });
                })) {
            docAccess.when(DocDB::getInstance).thenReturn(docs);
            groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
            constants.when(() -> Constants.getArray("languages")).thenReturn(new String[] { "sk", "en" });
            constants.when(() -> Constants.getBoolean("multiDomainEnabled")).thenReturn(true);
            domains.when(CloudToolsForCore::getDomainName).thenReturn("autotest.example");
            translations.when(() -> Prop.getInstance(request)).thenReturn(mock(Prop.class));

            addPage(docs, groups, 10, "sk", "/sk/page", "autotest.example");
            addPage(docs, groups, 11, "en", "/en/nested/page", "autotest.example");
            addPage(docs, groups, 12, "en", "/en/page", "autotest.example");
            addPage(docs, groups, 20, "sk", "/sk/clean", "autotest.example");
            addPage(docs, groups, 21, "en", "/en/clean", "autotest.example");
            addPage(docs, groups, 22, "en", "/foreign/nested/page", "foreign.example");
            // Page 23 has disappeared from the cache, and page 30 has no sync ID.
            Map<Integer, MirroringDTO> problems = MirroringService.getPageProblems(request, List.of(10, 20, 30));

            assertEquals(java.util.Set.of(10), problems.keySet());
            assertEquals(500L, problems.get(10).getId());
            assertEquals(Boolean.TRUE, problems.get(10).getMultipleErr());
            assertEquals(Boolean.TRUE, problems.get(10).getNestingWarn());
            assertEquals(1, queries.constructed().size());
            ComplexQuery query = queries.constructed().get(0);
            verify(query).setParams(new Object[] { 10, 20, 30 });
            verify(query).setSql("SELECT doc_id, sync_id FROM documents WHERE sync_id IN"
                + " (SELECT sync_id FROM documents WHERE sync_id>0 AND doc_id IN (?,?,?))");
            for (int id : rows.keySet()) verify(docs).getBasicDocDetails(id, false);
            verifyNoMoreInteractions(docs);
        }
    }

    /** Empty branches need neither a database query nor initialization of CMS caches. */
    @Test
    void skipsEmptyBranches() {
        try (MockedConstruction<ComplexQuery> queries = mockConstruction(ComplexQuery.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class)) {
            assertTrue(MirroringService.getPageProblems(new MockHttpServletRequest(), List.of()).isEmpty());
            assertTrue(queries.constructed().isEmpty());
            docs.verifyNoInteractions();
        }
    }

    private void addPage(DocDB docs, GroupsDB groups, int id, String language, String path, String domain) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(id);
        group.setLng(language);
        group.setDomainName(domain);
        when(groups.getGroup(id)).thenReturn(group);
        DocDetails doc = new DocDetails();
        doc.setDocId(id);
        doc.setGroupId(id);
        doc.setFullPath(path);
        when(docs.getBasicDocDetails(id, false)).thenReturn(doc);
    }
}
