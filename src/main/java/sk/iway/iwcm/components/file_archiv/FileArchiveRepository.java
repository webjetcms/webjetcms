package sk.iway.iwcm.components.file_archiv;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FileArchiveRepository extends DomainIdRepository<FileArchivatorBean, Long> {

    List<FileArchivatorBean> findDistinctAllByProductLikeAndFilePathLikeAndDomainId(String product, String filePath, int domainId);

    List<FileArchivatorBean> findDistinctAllByCategoryLikeAndFilePathLikeAndDomainId(String category, String filePath, int domainId);

    //Find history / waiting versions depending on the uploaded flag (0 - waiting, -1 - uploaded)
    Page<FileArchivatorBean> findAllByReferenceIdAndUploadedAndDomainId(Long referenceId, int uploaded, int domainId, Pageable pageable);
    List<FileArchivatorBean> findAllByReferenceIdAndUploadedAndDomainId(Long referenceId, int uploaded, int domainId);

    //Find all versions of the file
    List<FileArchivatorBean> findAllByReferenceIdAndDomainId(Long referenceId, int domainId);

    //vrati posledny nahrany subor vo vlakne
    FileArchivatorBean findFirstByReferenceIdOrderByOrderIdAsc(int orderId);

    @Query("SELECT DISTINCT fab.referenceId FROM FileArchivatorBean fab WHERE fab.uploaded = 0 AND fab.referenceId > 0 AND fab.domainId = :domainId")
    List<Integer> findDistinctReferenceIds(@Param("domainId")Integer domainId);

    @Query("SELECT DISTINCT fab.category FROM FileArchivatorBean fab WHERE fab.domainId = :domainId")
    List<String> getDistinctCategory(@Param("domainId")Integer domainId);

    @Query("SELECT DISTINCT fab.product FROM FileArchivatorBean fab WHERE fab.domainId = :domainId")
    List<String> getDistinctProduct(@Param("domainId")Integer domainId);

    @Transactional
    @Modifying
    @Query("UPDATE FileArchivatorBean fab SET fab.category = :newCategory WHERE fab.category = :oldCategory AND fab.domainId = :domainId")
    public void updateCategory(@Param("oldCategory")String oldCategory, @Param("newCategory")String newCategory, @Param("domainId")Integer domainId);

    @Transactional
    @Modifying
    @Query("UPDATE FileArchivatorBean fab SET fab.product = :newProduct WHERE fab.product = :oldProduct AND fab.domainId = :domainId")
    public void updateProduct(@Param("oldProduct")String oldProduct, @Param("newProduct")String newProduct, @Param("domainId")Integer domainId);

    @Query("SELECT fab FROM FileArchivatorBean fab WHERE CONCAT(fab.filePath, fab.fileName) LIKE :pattern AND fab.domainId = :domainId AND fab.referenceId = -1 AND fab.uploaded = -1 GROUP BY CONCAT(fab.filePath, fab.fileName)")
    List<FileArchivatorBean> findAllMainFilesUploadedNotPatternsVirtualPathLike(@Param("pattern")String pattern, @Param("domainId")Integer domainId);

    List<FileArchivatorBean> findAllByReferenceToMainAndDomainId(String referenceToMain, Integer domainId);
    List<FileArchivatorBean> findAllByReferenceToMainAndReferenceIdAndDomainId(String referenceToMain, Integer referenceId, Integer domainId);

    List<FileArchivatorBean> findAllByUploadedAndDomainId(Integer uploaded, Integer domainId);

    @Transactional
    @Modifying
    @Query("UPDATE FileArchivatorBean fab SET fab.referenceToMain = :newReferenceToMain WHERE fab.referenceToMain = :oldReferenceToMain AND fab.domainId = :domainId")
    public void updateReferenceToMain(@Param("oldReferenceToMain")String oldReferenceToMain, @Param("newReferenceToMain")String newReferenceToMain, @Param("domainId")Integer domainId);

    @Transactional
    @Modifying
    @Query("UPDATE FileArchivatorBean fab SET fab.referenceToMain = :referenceToMain WHERE fab.referenceId = :referenceId AND fab.domainId = :domainId")
    public void updateReferenceToMain(@Param("referenceId")Long referenceId, @Param("referenceToMain")String referenceToMain, @Param("domainId")Integer domainId);

    @Query("SELECT fab.referenceToMain FROM FileArchivatorBean fab WHERE fab.id = :id AND fab.domainId = :domainId")
    public String getReferenceToMain(@Param("id")Long id,  @Param("domainId")Integer domainId);

    @Query("SELECT COUNT(fab.id) FROM FileArchivatorBean fab WHERE fab.referenceToMain = :referenceToMain AND fab.domainId = :domainId")
    public Integer countReferencesToMain(@Param("referenceToMain")String referenceToMain, @Param("domainId")Integer domainId);

    @Query("SELECT fab FROM FileArchivatorBean fab WHERE fab.globalId IN :globalIds AND fab.referenceId = -1 AND fab.uploaded = -1 AND (fab.referenceToMain IS NULL OR fab.referenceToMain = '') AND fab.domainId = :domainId")
    List<FileArchivatorBean> findAllMainFilesUploadedNotPatternIdsIn(@Param("globalIds") List<Integer> globalIds, @Param("domainId") Integer domainId, Pageable pageable);

    @Query("SELECT fab FROM FileArchivatorBean fab WHERE fab.referenceId = -1 AND fab.uploaded = -1 AND (fab.referenceToMain IS NULL OR fab.referenceToMain = '') AND fab.domainId = :domainId")
    List<FileArchivatorBean> findAllMainFilesUploadedNotPattern(@Param("domainId") Integer domainId, Pageable pageable);

    @Query("SELECT fab.id FROM FileArchivatorBean fab WHERE fab.filePath = :filePath AND fab.fileName = :fileName AND fab.domainId = :domainId")
    Optional<Long> findIdByFilePathAndFileName(@Param("filePath") String filePath, @Param("fileName") String fileName, @Param("domainId") Integer domainId);
}