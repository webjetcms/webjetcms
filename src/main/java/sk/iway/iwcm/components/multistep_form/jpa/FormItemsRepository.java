package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
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

    List<FormItemEntity> findAllByStepIdAndDomainIdOrderBySortPriorityAsc(Long stepId, Integer domainId);
}
