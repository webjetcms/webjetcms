package sk.iway.iwcm.doc;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocDetailsRepository extends JpaRepository<DocDetails, Long>, JpaSpecificationExecutor<DocDetails> {

    Page<DocDetails> findAll(Pageable pageable);

    Page<DocDetails> findAllByTempId(int tempId, Pageable pageable);

    //pre vyhladavanie podla user grupy (password_protected)
    @Query(value = "SELECT d FROM DocDetails d WHERE (d.passwordProtected LIKE :groupId1 OR d.passwordProtected LIKE :groupId2 OR d.passwordProtected LIKE :groupId3 OR d.passwordProtected LIKE :groupId4)")
    Page<DocDetails> findAllByPasswordProtectedLike(@Param("groupId1")String groupId1, @Param("groupId2")String groupId2, @Param("groupId3")String groupId3, @Param("groupId4")String groupId4, Pageable pageable);

    //tieto WJ admin nepotrebuje, ale ponechavame pre pripadne pouzitie v custom moduloch
    Page<DocDetails> findAllByGroupId(Integer groupId, Pageable pageable);
    Page<DocDetails> findAllByGroupIdIn(Integer[] groupIds, Pageable pageable);
    Page<DocDetails> findAllByGroupIdIn(int[] groupIds, Pageable pageable);

    List<DocDetails> findAllByGroupId(Integer groupId);

    //
    DocDetails findById(int docId);

    //
    @Query(value = "SELECT max(doc_id) FROM documents WHERE group_id = ?1 AND title = ?2", nativeQuery=true)
    public Long findMaxIdByGroupIdAndTitle(Integer groupId, String title);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE documents SET external_link = ?1 WHERE external_link = ?2", nativeQuery=true)
    public void updateRedirect(String newExternalLink, String oldExternalLink );

    @Transactional
    @Modifying
    @Query(value = "UPDATE DocDetails SET externalLink = :newExternalLink WHERE externalLink = :oldExternalLink AND group_id IN :subGroupsIds")
    public void updateRedirect(@Param("newExternalLink")String newExternalLink, @Param("oldExternalLink")String oldExternalLink, @Param("subGroupsIds")int[] subGroupsIds);

    //
    List<DocDetails> findByDataLike(String data);
    List<DocDetails> findByDataLikeAndGroupIdIn(String data, int[] subPagesids);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE documents SET data = ?1, data_asc = ?2, sync_status = 1 WHERE doc_id = ?3", nativeQuery=true)
    public void updateAfterUrlReplace(String data, String dataAsc, int docId);

    //
    @Query(value = "SELECT group_id FROM documents WHERE doc_id = ?1", nativeQuery=true)
    public Long getGroupId(int docId);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE documents SET temp_id=?1 WHERE temp_id=?2", nativeQuery=true)
    public void replaceTemplate(Long templateIdThatWillReplace, Long templateIdToByReplaced);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE DocDetails SET available = :available, syncStatus = 1 WHERE id = :docId")
    public void updateAvailableStatus(@Param("available")boolean available, @Param("docId")Integer docId);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE DocDetails SET available = :available, syncStatus = 1, groupId = :groupId WHERE id = :docId")
    public void moveToTrash(@Param("available")boolean available, @Param("groupId")Integer groupId, @Param("docId")Integer docId);

    //
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM DocDetails dd WHERE dd.id IN :docIds")
    public void deleteByDocIdIn(@Param("docIds")List<Long> docIds);

    Page<DocDetails> findAllByOrderByDateCreatedDesc(Pageable pageable);

    //select data which contains (/components/eshop/ OR /components/basket) AND (product-list.jsp OR products.jsp)
    @Query(value = "SELECT DISTINCT(dd.groupId) FROM DocDetails dd WHERE (dd.data LIKE :path1 OR dd.data LIKE :path2) AND (dd.data LIKE :file1 OR dd.data LIKE :file2)")
    List<Integer> getDistGroupIdsByDataLike(@Param("path1")String path1, @Param("path2")String path2, @Param("file1")String file1, @Param("file2")String file2);

    //Used in DocPublicableService
    @Transactional
    @Modifying
    @Query(value = "UPDATE DocDetails dd SET dd.available = :available, dd.disableAfterEnd = :disableAfterEnd WHERE dd.id = :docId")
    public void updateAvailableAndDisabledAfterEnd(@Param("available")boolean available, @Param("disableAfterEnd")boolean disableAfterEnd, @Param("docId")Integer docId);

    List<DocDetails> findAllByDisableAfterEndTrue();
}