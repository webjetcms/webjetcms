package sk.iway.iwcm.doc;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface DocHistoryRepository extends JpaRepository<DocHistory, Long>, JpaSpecificationExecutor<DocHistory> {

    List<DocHistory> findByDocIdAndPublishStartDate(Integer docId, Date publishStartDate);

    //
    @Query(value = "SELECT history_id FROM documents_history WHERE doc_id = ?1 AND history_id < ?2 AND publicable = ?3 AND author_id = ?4", nativeQuery=true)
    public List<Integer> findOldHistoryIds(int docId, Long historyId, boolean publicable, int authorId);

    //
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM DocHistory WHERE id IN :historyIds AND approvedBy = -1 AND authorId = :authorId")
    public void deleteHistoryOnPublish(@Param("historyIds")List<Integer> historyIds, @Param("authorId")int authorId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM DocHistory WHERE id = :historyIds AND publicable = :publicable")
    public void deleteByIdPublicable(@Param("historyIds")Long historyId, @Param("publicable")boolean publicable);

    //
    @Query(value = "SELECT history_id FROM documents_history WHERE doc_id = ?1 AND history_id < ?2", nativeQuery=true)
    public List<Integer> findOldHistoryIds(int docId, int historyId);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE DocHistory SET actual = :actual, awaitingApprove = :awaitingApprove, syncStatus = 1 WHERE id IN :historyIds")
    public void updateActualHistory(@Param("actual")boolean actual, @Param("awaitingApprove")String awaitingApprove, @Param("historyIds")List<Integer> historyIds);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE DocHistory SET awaitingApprove = :awaitingApprove, disapprovedBy = :disapprovedBy, syncStatus = 1 WHERE id = :historyId")
    public void rejectDocHistory(@Param("awaitingApprove")String awaitingApprove, @Param("disapprovedBy")Integer disapprovedBy, @Param("historyId")Integer historyId);

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE DocHistory SET awaitingApprove = :awaitingApprove, syncStatus = 1 WHERE docId = :docId")
    public void updateAwaitingApprove(@Param("awaitingApprove")String awaitingApprove, @Param("docId")Integer docId);

    @Query(value = "SELECT max(history_id) AS max FROM documents_history WHERE doc_id=? AND actual=?", nativeQuery=true)
    public Optional<Integer> findMaxHistoryId(int docId, boolean actual);

    @Query(value = "SELECT max(history_id) AS max FROM documents_history WHERE doc_id=?", nativeQuery=true)
    public Integer findMaxHistoryId(int docId);

    @Query(value = "SELECT group_id FROM documents_history WHERE history_id=?", nativeQuery=true)
    public Optional<Integer> findGroupIdById(Long id);

    @Query(value = "SELECT dh FROM DocHistory dh WHERE dh.docId IN :docIds AND dh.actual = 1")
    public List<DocHistory> findByDocIdInActual(@Param("docIds")List<Integer> docIds);

    public Optional<DocHistory> findTopByDocIdOrderBySaveDateDesc(Integer docId);
}