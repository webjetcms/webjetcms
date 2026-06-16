package sk.iway.iwcm.components.forms;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FormsRepository extends FormsRepositoryInterface<FormsEntity>{

    @Query("SELECT COUNT(fe.id) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NOT NULL")
    Integer getNumberOfSubmitted(@Param("formName") String formName, @Param("domainId") Integer domainId);

    @Transactional
    @Modifying
    @Query("DELETE FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.userId = :userId AND fe.createDate IS NOT NULL")
    void deleteAllUserSubmitted(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("userId") Long userId);

    @Query("SELECT COUNT(fe.id) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId")
    Integer countFormName(@Param("formName") String formName, @Param("domainId") Integer domainId);

    @Transactional
    @Modifying
    @Query("UPDATE FormsEntity fe SET fe.formType = 'unknown' WHERE (fe.formType IS NULL OR fe.formType = '') AND fe.createDate IS NULL")
    void setUnknownFormType();

    @Query("SELECT fe.data FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate BETWEEN :dateFrom AND :dateTo")
    List<String> getFormAllData(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

    // By some mistake is there possibility that there are more than one records where fe.createDate IS NULL
    @Query("SELECT fe.id FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NULL ORDER BY fe.id ASC")
    Optional<Long> getFormId(@Param("formName") String formName, @Param("domainId") Integer domainId, Pageable pageable);

    @Query("SELECT fe.duration FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NULL")
    Optional<Long> getFormCreationDuration(@Param("formName") String formName, @Param("domainId") Integer domainId);

    @Query("SELECT MIN(fe.createDate) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NOT NULL")
    Optional<Date> getMinFormCreateDate(@Param("formName") String formName, @Param("domainId") Integer domainId);
}
