package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FormItemsRepository extends DomainIdRepository<FormItemEntity, Long> {

    @Transactional
    @Modifying
    void deleteAllByStepIdAndDomainId(Long stepId, Integer domainId);

    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.stepId = :stepId AND fie.domainId = :domainId ORDER BY fie.sortPriority ASC")
    List<FormItemEntity> getAllStepItems(@Param("stepId") Long stepId, @Param("domainId") Integer domainId);

    @Query("SELECT fie.itemFormId FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.fieldType = :fieldType AND fie.domainId = :domainId")
    List<String> getItemFormIds(@Param("formName") String formName, @Param("fieldType") String fieldType, @Param("domainId") Integer domainId);

    @Query("SELECT count(fie.id) FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.domainId = :domainId AND fie.fieldType IN :fieldTypes")
    int countItemsThatHasType(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("fieldTypes") List<String> fieldTypes);

    @Transactional
    @Modifying
    void deleteAllByFormNameAndStepIdAndDomainId(String formName, Long stepId, Integer domainId);

    @Transactional
    @Modifying
    void deleteAllByFormNameAndDomainId(String formName, Integer domainId);

    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.stepId = :stepId AND fie.domainId = :domainId")
    List<FormItemEntity> findItemsToDuplicate(@Param("formName") String formName, @Param("stepId") Long stepId, @Param("domainId") Integer domainId);

    List<FormItemEntity> findAllByFormNameAndDomainId(String formName, Integer domainId);

    // Basically, we need find first only because joined radio buttons that have same itemFormId but are separate items
    FormItemEntity findFirstByFormNameAndItemFormIdAndDomainIdOrderBySortPriorityAsc(String formName, String itemFormId, Integer domainId);

    Integer countByFormNameAndStepIdAndSortPriorityAndIdNot(String formName, Long stepId, Integer sortPriority, Integer id);

    List<FormItemEntity> findAllByFormNameAndStepIdInAndDomainId(String formName, List<Integer> stepIds, Integer domainId);

    @Query("SELECT COUNT(fie.id) FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.id = :id AND fie.itemFormId = :itemFormId AND fie.domainId = :domainId")
    Optional<Integer> countItemsByIdAndItemFormId(@Param("formName") String formName, @Param("id") Long id, @Param("itemFormId") String itemFormId, @Param("domainId") Integer domainId);

    List<FormItemEntity> findAllByFormNameAndIdInAndDomainId(String formName, List<Long> ids, Integer domainId);
}
