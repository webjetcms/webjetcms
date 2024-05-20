package sk.iway.iwcm.components.forum.jpa;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface DocForumRepository extends DomainIdRepository<DocForumEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE document_forum SET stat_replies = stat_replies+1, stat_last_post = ?1 WHERE forum_id = ?2 AND parent_id = -1 AND domain_id = ?3", nativeQuery=true)
    public void updateSubtopic(Date statLastPost, Long forumId, Integer domainId);

    Optional<DocForumEntity> findByIdAndDomainIdOrderByQuestionDateAsc(Long id, Integer domainId);
    Optional<DocForumEntity> findByIdAndDomainIdOrderByQuestionDateDesc(Long id, Integer domainId);

    Optional<DocForumEntity> findById(Long forumId);

    @Query(value = "SELECT new sk.iway.iwcm.components.forum.jpa.DocForumIdAndParentId(fe.id, fe.parentId) FROM DocForumEntity fe WHERE fe.docId = :docId AND fe.parentId > 0 AND fe.deleted = :deleted AND fe.confirmed = :confirmed AND fe.domainId = :domainId ORDER BY fe.id ASC")
    List<DocForumIdAndParentId> getDocForumListForumIdParentId(
        @Param("docId")Integer docId,
        @Param("deleted")Boolean deleted,
        @Param("confirmed")Boolean confirmed,
        @Param("domainId")Integer domainId
    );

    @Query(value = "SELECT new sk.iway.iwcm.components.forum.jpa.DocForumIdAndParentId(fe.id, fe.parentId) FROM DocForumEntity fe WHERE fe.docId = :docId AND fe.deleted = :deleted AND fe.confirmed = :confirmed AND fe.domainId = :domainId ORDER BY fe.id ASC")
    List<DocForumIdAndParentId> getDocForumListForumIdParentIdWithRootParents(
        @Param("docId")Integer docId,
        @Param("deleted")Boolean deleted,
        @Param("confirmed")Boolean confirmed,
        @Param("domainId")Integer domainId
    );

    @Query(value = "SELECT fe FROM DocForumEntity fe WHERE fe.docId = :docId AND fe.parentId IN :parentIds AND fe.deleted = :deleted AND fe.confirmed = :confirmed AND fe.domainId = :domainId ORDER BY fe.questionDate ASC")
    List<DocForumEntity> getDocForumListAsc(
        @Param("docId")Integer docId,
        @Param("parentIds")List<Integer> parentIds,
        @Param("deleted")Boolean deleted,
        @Param("confirmed")Boolean confirmed,
        @Param("domainId")Integer domainId
    );

    @Query(value = "SELECT fe FROM DocForumEntity fe WHERE fe.docId = :docId AND fe.parentId IN :parentIds AND fe.deleted = :deleted AND fe.confirmed = :confirmed AND fe.domainId = :domainId ORDER BY fe.questionDate DESC")
    List<DocForumEntity> getDocForumListDesc(
        @Param("docId")Integer docId,
        @Param("parentIds")List<Integer> parentIds,
        @Param("deleted")Boolean deleted,
        @Param("confirmed")Boolean confirmed,
        @Param("domainId")Integer domainId
    );

    @Query(value = "SELECT fe FROM DocForumEntity fe WHERE fe.docId = :docId AND fe.parentId = -1 AND fe.deleted = :deleted AND fe.domainId = :domainId ORDER BY fe.statLastPost DESC")
    List<DocForumEntity> getForumStat(@Param("docId")Integer docId, @Param("deleted")Boolean deleted, @Param("domainId")Integer domainId);

    Optional<DocForumEntity> findFirstByDocIdAndDomainIdOrderByQuestionDateDesc(Integer docId, Integer domainId);
}