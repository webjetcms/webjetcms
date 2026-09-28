package sk.iway.iwcm.editor.approve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.editor.rest.GroupSchedulerDto;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoMapper;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;

/** Verifies bounded approval previews and compatibility with the complete client-side table. */
class GroupsApproveRestControllerTest {

    /** Explicit pagination preserves the repository's sorting, total count, and approval metadata. */
    @Test
    void paginatedResultsRetainTotalsAndApprovalMetadata() {
        GroupSchedulerDtoRepository repository = mock(GroupSchedulerDtoRepository.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("size", "6");
        PageRequest pageable = PageRequest.of(1, 6, Sort.by("saveDate").descending());
        GroupSchedulerDto change = change(271L, "Pending deletion");
        change.setIsDelete(true);
        change.setSaveDate(new Date(1_800_000_000_000L));
        change.setUserFullName("Approval requester");
        when(repository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(change), pageable, 19));

        Page<GroupDetails> result = controller(repository, request).getAllItems(pageable);

        assertEquals(19, result.getTotalElements());
        assertEquals(pageable, result.getPageable());
        assertEquals(1, result.getNumberOfElements());
        GroupDetails group = result.getContent().get(0);
        assertEquals(271, group.getSchedulerId());
        assertEquals(11, group.getGroupId());
        assertEquals("Pending deletion", group.getGroupName());
        assertTrue(group.getIsDelete());
        assertEquals(change.getSaveDate(), group.getSaveDate());
        assertEquals("Approval requester", group.getUserFullName());
        verify(repository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(repository);
    }

    /** The existing client-side table omits size and must still receive every matching group. */
    @Test
    void missingSizePreservesCompleteClientSideResults() {
        GroupSchedulerDtoRepository repository = mock(GroupSchedulerDtoRepository.class);
        when(repository.findAll(any(Specification.class)))
            .thenReturn(List.of(change(271L, "First change"), change(272L, "Second change")));

        Page<GroupDetails> result = controller(repository, new MockHttpServletRequest())
            .getAllItems(PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of(271, 272), result.getContent().stream().map(GroupDetails::getSchedulerId).toList());
        verify(repository).findAll(any(Specification.class));
        verifyNoMoreInteractions(repository);
    }

    /** Approval response metadata cannot be supplied by clients or copied into a newly scheduled change. */
    @Test
    void approvalMetadataRemainsReadOnly() throws Exception {
        GroupDetails group = new ObjectMapper().readValue(
            "{\"groupName\":\"Pending change\",\"saveDate\":1800000000000,\"userFullName\":\"Client value\"}", GroupDetails.class);
        assertEquals("Pending change", group.getGroupName());
        assertNull(group.getSaveDate());
        assertNull(group.getUserFullName());

        group.setSaveDate(new Date(1_800_000_000_000L));
        group.setUserFullName("Original requester");
        GroupSchedulerDto scheduled = GroupSchedulerDtoMapper.INSTANCE.groupToGroupSchedulerDto(group);
        assertNull(scheduled.getSaveDate());
        assertNull(scheduled.getUserFullName());
    }

    private static GroupsApproveRestController controller(GroupSchedulerDtoRepository repository, MockHttpServletRequest request) {
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(42);
        GroupsApproveRestController controller = new GroupsApproveRestController(repository) {
            @Override
            public Identity getUser() {
                return user;
            }
        };
        controller.setRequest(request);
        return controller;
    }

    private static GroupSchedulerDto change(long id, String name) {
        GroupSchedulerDto change = new GroupSchedulerDto();
        change.setId(id);
        change.setGroupId(11);
        change.setGroupName(name);
        return change;
    }
}
