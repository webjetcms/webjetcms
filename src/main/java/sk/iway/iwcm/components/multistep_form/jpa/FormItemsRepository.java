package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.List;

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

    @Transactional
    @Modifying
    void deleteAllByFormNameAndStepIdAndDomainId(String formName, Long stepId, Integer domainId);

    @Query("SELECT DISTINCT(fie.itemFormId) FROM FormItemEntity fie WHERE fie.stepId = :stepId AND fie.domainId = :domainId ORDER BY fie.sortPriority ASC")
    List<String> getItemsStepFormIds(@Param("stepId") Long stepId, @Param("domainId") Integer domainId);

    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.stepId = :stepId AND fie.domainId = :domainId AND fie.itemFormId IN :itemFormIds ORDER BY fie.sortPriority ASC")
    List<FormItemEntity> getStepItems(@Param("stepId") Long stepId, @Param("domainId") Integer domainId, @Param("itemFormIds") List<String> itemFormIds);

    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.stepId = :stepId AND fie.domainId = :domainId ORDER BY fie.sortPriority ASC")
    List<FormItemEntity> getAllStepItems(@Param("stepId") Long stepId, @Param("domainId") Integer domainId);

    @Query("SELECT fie.itemFormId FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.fieldType = :fieldType AND fie.domainId = :domainId")
    List<String> getItemFormIds(@Param("formName") String formName, @Param("fieldType") String fieldType, @Param("domainId") Integer domainId);

    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.stepId = :stepId AND fie.domainId = :domainId")
    List<FormItemEntity> findItemsToDuplicate(@Param("formName") String formName, @Param("stepId") Long stepId, @Param("domainId") Integer domainId);

    @Query("SELECT fie FROM FormItemEntity fie WHERE fie.formName = :formName AND fie.stepId = :stepId AND fie.domainId = :domainId AND fie.fieldType = :fieldType")
    FormItemEntity findSpecificField(@Param("formName") String formName, @Param("stepId") Long stepId, @Param("domainId") Integer domainId, @Param("fieldType") String fieldType);
}
