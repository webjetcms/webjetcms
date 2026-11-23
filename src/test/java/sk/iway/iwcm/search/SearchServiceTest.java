package sk.iway.iwcm.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Executes quick-search permission predicates against the configured database through Spring Data. */
class SearchServiceTest extends BaseWebjetTest {
    private static final int DOMAIN = 910000;
    private static final int FIRST = 910001;
    private static final int SECOND = 910002;
    private static final int CHILD = 910003;
    private static final int DENIED = 910004;
    private static final int FOREIGN = 920001;

    /** All-root searches include body matches and explicit pages while preserving legacy scope and domain isolation. */
    @Test
    void searchesAllPermittedRootsWithoutChangingSelectedRootSearch() {
        DBPool.getInstance();
        DBPool.jpaInitialize();
        EntityManager manager = DBPool.getEntityManagerFactory("iwcm").createEntityManager();
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroup(anyInt())).thenAnswer(call -> group(call.getArgument(0)));
        when(groups.findGroup(anyInt())).thenAnswer(call -> group(call.getArgument(0)));
        when(groups.getGroupsTree(FIRST, true, true)).thenReturn(List.of(group(FIRST)));
        when(groups.getGroupsTree(SECOND, true, true)).thenReturn(List.of(group(SECOND), group(CHILD)));
        when(groups.getGroupsTree(FOREIGN, true, true)).thenReturn(List.of(group(FOREIGN)));
        when(groups.expandGroupIdsToChilds(any(int[].class), eq(true))).thenCallRealMethod();

        try (MockedStatic<GroupsDB> groupAccess = mockStatic(GroupsDB.class, CALLS_REAL_METHODS);
             MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class)) {
            groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
            domains.when(CloudToolsForCore::getRootGroupIds).thenReturn(String.valueOf(DOMAIN));
            manager.getTransaction().begin();
            DocDetailsRepository pages = new JpaRepositoryFactory(manager).getRepository(DocDetailsRepository.class);
            String query = "autotest-quick-search-" + UUID.randomUUID();
            long first = save(pages, FIRST, DOMAIN, query);
            long second = save(pages, CHILD, DOMAIN, query + "-second-only");
            long denied = save(pages, DENIED, DOMAIN, query);
            long explicit = save(pages, DENIED, DOMAIN, query);
            save(pages, FOREIGN, 920000, query);
            manager.clear();

            Identity user = new Identity();
            user.setEditableGroups(FIRST + "," + SECOND + "," + FOREIGN);
            user.setEditablePages(String.valueOf(explicit));
            Map<String, String> params = new HashMap<>(Map.of("searchText", query, "searchType", "docs", "searchRootGroupId", "-1"));
            assertEquals(Set.of(first, explicit), search(pages, user, params), "Legacy default scope must remain the first permitted root");
            params.put("searchText", query + "-second-only");
            assertEquals(Set.of(), search(pages, user, params), "Legacy default scope misses a body-only match in the second subtree");

            params.put("searchAllGroups", "true");
            assertEquals(Set.of(second), search(pages, user, params), "Quick search must find the body-only match in the second subtree");
            params.put("searchText", query);
            assertEquals(Set.of(first, second, explicit), search(pages, user, params), "Quick search must include the second subtree but exclude denied and foreign-domain pages");
            params.put("searchRootGroupId", String.valueOf(DENIED));
            assertEquals(Set.of(first, second, explicit), search(pages, user, params), "An unpermitted root parameter must not broaden all-root search");

            params.remove("searchAllGroups");
            params.put("searchRootGroupId", String.valueOf(SECOND));
            assertEquals(Set.of(second, explicit), search(pages, user, params), "Advanced search must retain its selected-root filter");

            params.put("searchAllGroups", "true");
            user.setEditableGroups("");
            assertEquals(Set.of(explicit), search(pages, user, params), "Page-only access must not become unrestricted");
            user.setEditablePages("");
            assertEquals(Set.of(first, second, denied, explicit), search(pages, user, params), "Unrestricted users still search only the active domain");
            user.setEditableGroups("999999");
            assertEquals(Set.of(), search(pages, user, params), "Missing permitted roots must fail closed");
        } finally {
            if (manager.getTransaction().isActive()) manager.getTransaction().rollback();
            manager.close();
        }
    }

    private static Set<Long> search(DocDetailsRepository pages, Identity user, Map<String, String> params) {
        Specification<DocDetails> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            SearchService.getWebPagesData(params, user, predicates, builder, root);
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        var result = pages.findAll(specification, PageRequest.of(0, 20));
        assertEquals(result.getContent().size(), pages.findAll(specification, PageRequest.of(0, 1)).getTotalElements(), "The result count must use the same permission predicates");
        return result.getContent().stream().map(DocDetails::getId).collect(Collectors.toSet());
    }

    private static long save(DocDetailsRepository pages, int groupId, int domainRoot, String query) {
        DocDetails page = new DocDetails();
        page.setTitle("autotest-permission-search-fixture");
        page.setNavbar(page.getTitle());
        page.setData(query);
        page.setDataAsc(query);
        page.setDateCreated(System.currentTimeMillis());
        page.setGroupId(groupId);
        page.setRootGroupL1(domainRoot);
        return pages.saveAndFlush(page).getId();
    }

    private static GroupDetails group(int id) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(id);
        return group;
    }
}
