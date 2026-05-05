package sk.iway.iwcm.editor.rest;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupSchedulerDtoRepository extends JpaRepository<GroupSchedulerDto, Long>, JpaSpecificationExecutor<GroupSchedulerDto> {

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNotNull(Pageable pageable, Long groupId);

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNull(Pageable pageable, Long groupId1);

    //get latest scheduler/history for group
    GroupSchedulerDto findFirstByGroupIdAndWhenToPublishNullOrderBySaveDateDesc(Long groupId);

    //find all pending approval records for a group (awaiting_approve IS NOT NULL)
    java.util.List<GroupSchedulerDto> findByGroupIdAndAwaitingApproveIsNotNull(Integer groupId);

    java.util.List<GroupSchedulerDto> findAll(Specification<GroupSchedulerDto> spec);

    //secure lookup during approve action - only records that are actually pending approval
    Optional<GroupSchedulerDto> findByIdAndAwaitingApproveIsNotNull(Long id);

    //get MAX id where awaitingApprove is null OR (awaitingApprove is not null AND disapprovedBy is not null)
    @Query("SELECT MAX(g.id) FROM GroupSchedulerDto g WHERE g.groupId = :groupId AND g.awaitingApprove IS NULL AND g.disapprovedBy IS NULL")
    Long findActualUsedGroupScheduler(@Param("groupId") Integer groupId);

    Optional<GroupSchedulerDto> findByIdAndIsDeleteFalse(Long id);
    Optional<GroupSchedulerDto> findByIdAndIsDeleteTrue(Long id);

    // Number of scheduler records for group
    Integer countByGroupId(Integer groupId);
}
