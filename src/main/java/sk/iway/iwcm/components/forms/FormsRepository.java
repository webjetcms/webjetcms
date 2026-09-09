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

/**
 * Provides domain-scoped queries for form definition rows and submitted responses.
 *
 * A form definition row has no creation date, while submitted response rows have one.
 */
@Repository
public interface FormsRepository extends FormsRepositoryInterface<FormsEntity>{

    /**
     * Counts submitted responses for a form in a domain.
     *
     * @param formName form identifier
     * @param domainId domain identifier
     * @return number of rows with a submission date
     */
    @Query("SELECT COUNT(fe.id) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NOT NULL")
    Integer getNumberOfSubmitted(@Param("formName") String formName, @Param("domainId") Integer domainId);

    /**
     * Deletes all submitted responses created by a user for a form in a domain.
     *
     * Form definition rows are preserved.
     *
     * @param formName form identifier
     * @param domainId domain identifier
     * @param userId user whose submitted responses are deleted
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.userId = :userId AND fe.createDate IS NOT NULL")
    void deleteAllUserSubmitted(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("userId") Long userId);

    /**
     * Counts all definition and response rows for a form in a domain.
     *
     * @param formName form identifier
     * @param domainId domain identifier
     * @return total number of matching rows
     */
    @Query("SELECT COUNT(fe.id) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId")
    Integer countFormName(@Param("formName") String formName, @Param("domainId") Integer domainId);

    /**
     * Assigns the {@code unknown} type to legacy form definition rows without a type.
     */
    @Transactional
    @Modifying
    @Query("UPDATE FormsEntity fe SET fe.formType = 'unknown' WHERE (fe.formType IS NULL OR fe.formType = '') AND fe.createDate IS NULL")
    void setUnknownFormType();

    /**
     * Returns submitted form data within an inclusive date range.
     *
     * @param formName form identifier
     * @param domainId domain identifier
     * @param dateFrom earliest submission date to include
     * @param dateTo latest submission date to include
     * @param pageable pagination and sorting configuration
     * @return data payloads for the requested page of matching submissions
     */
    @Query("SELECT fe.data FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate BETWEEN :dateFrom AND :dateTo")
    List<String> getFormAllData(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

    /**
     * Finds the earliest creation timestamp stored on a form definition row.
     *
     * @param formName form identifier
     * @param domainId domain identifier
     * @return creation time in epoch seconds, or an empty optional when unavailable
     */
    @Query("SELECT MIN(fe.duration) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NULL")
    Optional<Long> getFormCreationDuration(@Param("formName") String formName, @Param("domainId") Integer domainId);

    /**
     * Finds the date of the earliest submitted response for a form in a domain.
     *
     * @param formName form identifier
     * @param domainId domain identifier
     * @return earliest submission date, or an empty optional when no response exists
     */
    @Query("SELECT MIN(fe.createDate) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NOT NULL")
    Optional<Date> getMinFormCreateDate(@Param("formName") String formName, @Param("domainId") Integer domainId);
}
