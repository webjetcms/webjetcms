package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FormItemsConditionsRepository extends DomainIdRepository<FormItemsConditionEntity, Long> {

    List<FormItemsConditionEntity> findAllByFormItemIdAndDomainIdOrderBySortPriorityAsc(Long formItemId, Integer domainId);

    List<FormItemsConditionEntity> findAllByFormItemIdAndConditionTypeAndDomainIdOrderBySortPriorityAsc(Long formItemId, ConditionType conditionType, Integer domainId);

    @Query("SELECT fice.formItemId FROM FormItemsConditionEntity fice WHERE fice.formName = :formName AND fice.itemFormId = :itemFormId AND fice.domainId = :domainId")
    List<Long> getDependedItems(@Param("formName") String formName, @Param("itemFormId") String itemFormId, @Param("domainId") Integer domainId);
}