package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

/**
 * Persists multistep form items and provides queries for editing, validation,
 * duplication, and validation-error statistics within a domain.
 */
@Repository
public interface FormItemsRepository extends DomainIdRepository<FormItemEntity, Long> {

    @Transactional
    @Modifying
    void deleteAllByStepIdAndDomainId(Long stepId, Integer domainId);

    /**
     * Loads all items of a step in their configured display order.
     *
     * @param stepId step identifier
     * @param domainId domain owning the items
     * @return items ordered by ascending sort priority, including layout items and radio options
     */
    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.stepId = :stepId AND fie.domainId = :domainId ORDER BY fie.sortPriority ASC")
    List<FormItemEntity> getAllStepItems(@Param("stepId") Long stepId, @Param("domainId") Integer domainId);

    /**
     * Loads the logical field identifiers used to detect identifier collisions in a form.
     *
     * @param formName logical form name
     * @param domainId domain owning the form
     * @return identifiers across all field types and steps, including duplicate radio identifiers
     */
    @Query("SELECT fie.itemFormId FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.domainId = :domainId")
    List<String> getItemFormIds(@Param("formName") String formName, @Param("domainId") Integer domainId);

    /**
     * Counts items whose configured type matches any of the supplied types.
     *
     * @param formName logical form name
     * @param domainId domain owning the form
     * @param fieldTypes field types to include in the count
     * @return number of matching item entities across the form
     */
    @Query("SELECT count(fie.id) FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.domainId = :domainId AND fie.fieldType IN :fieldTypes")
    int countItemsThatHasType(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("fieldTypes") List<String> fieldTypes);

    @Transactional
    @Modifying
    void deleteAllByFormNameAndStepIdAndDomainId(String formName, Long stepId, Integer domainId);

    @Transactional
    @Modifying
    void deleteAllByFormNameAndDomainId(String formName, Integer domainId);

    /**
     * Loads the source items to copy when duplicating a form step.
     *
     * @param formName logical form name
     * @param stepId source step identifier
     * @param domainId domain owning the form
     * @return existing source entities; no copies are created by this query
     */
    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.stepId = :stepId AND fie.domainId = :domainId")
    List<FormItemEntity> findItemsToDuplicate(@Param("formName") String formName, @Param("stepId") Long stepId, @Param("domainId") Integer domainId);

    List<FormItemEntity> findAllByFormNameAndDomainId(String formName, Integer domainId);

    /**
     * Loads items whose steps belong to the same form and domain, in form display order.
     *
     * @param formName logical form name
     * @param domainId domain owning both the items and their steps
     * @return all matching items ordered by step and item sort priority, including individual radio options
     */
    @Query("SELECT fie FROM FormItemEntity fie, FormStepEntity step WHERE fie.formName = :formName AND fie.domainId = :domainId AND fie.stepId = step.id AND step.formName = :formName AND step.domainId = :domainId ORDER BY step.sortPriority ASC, fie.sortPriority ASC")
    List<FormItemEntity> findAllForValidation(@Param("formName") String formName, @Param("domainId") Integer domainId);

    /**
     * Resolves the first item for a logical field, which can be shared by several radio options.
     *
     * @param formName logical form name
     * @param itemFormId logical field identifier
     * @param domainId domain owning the form
     * @return matching item with the lowest sort priority, or {@code null} when none exists
     */
    FormItemEntity findFirstByFormNameAndItemFormIdAndDomainIdOrderBySortPriorityAsc(String formName, String itemFormId, Integer domainId);

    /**
     * Increments the validation-error counter of every item matching the supplied field identifiers.
     * A missing counter is treated as zero; radio options sharing an identifier are all updated.
     *
     * @param formName logical form name
     * @param domainId domain owning the form
     * @param itemFormIds logical identifiers of fields with validation errors
     * @return number of item entities updated
     */
    @Transactional
    @Modifying
    @Query("UPDATE FormItemEntity fie SET fie.errorCount = COALESCE(fie.errorCount, 0) + 1 WHERE fie.formName = :formName AND fie.domainId = :domainId AND fie.itemFormId IN :itemFormIds")
    int incrementErrorCountByItemFormIds(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("itemFormIds") List<String> itemFormIds);

    Integer countByFormNameAndStepIdAndSortPriorityAndIdNot(String formName, Long stepId, Integer sortPriority, Integer id);

    List<FormItemEntity> findAllByFormNameAndStepIdInAndDomainId(String formName, List<Long> stepIds, Integer domainId);

    /**
     * Checks whether an item has the requested logical identifier and belongs to the form and domain.
     *
     * @param formName logical form name
     * @param id item entity identifier
     * @param itemFormId expected logical field identifier
     * @param domainId domain owning the form
     * @return count wrapped in an optional: one for a match, zero otherwise
     */
    @Query("SELECT COUNT(fie.id) FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.id = :id AND fie.itemFormId = :itemFormId AND fie.domainId = :domainId")
    Optional<Integer> countItemsByIdAndItemFormId(@Param("formName") String formName, @Param("id") Long id, @Param("itemFormId") String itemFormId, @Param("domainId") Integer domainId);

    List<FormItemEntity> findAllByFormNameAndIdInAndDomainId(String formName, List<Long> ids, Integer domainId);
}
