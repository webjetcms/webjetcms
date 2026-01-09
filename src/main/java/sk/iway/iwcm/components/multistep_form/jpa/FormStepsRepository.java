package sk.iway.iwcm.components.multistep_form.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FormStepsRepository extends DomainIdRepository<FormStepEntity, Long> {
    List<FormStepEntity> findAllByFormNameAndDomainId(String formName, Integer domainId);

    List<FormStepEntity> findAllByFormNameAndDomainIdOrderBySortPriorityAsc(String formName, Integer domainId);

    @Query("SELECT DISTINCT fse.formName FROM FormStepEntity fse WHERE fse.domainId = :domainId")
    public List<String> getMultistepFormNames(@Param("domainId") Integer domainId);

    @Query("SELECT COUNT(fse.id) FROM FormStepEntity fse WHERE fse.formName = :formName AND fse.id = :id AND fse.domainId = :domainId")
    public int validationStepCount(@Param("formName") String formName, @Param("id") Long id, @Param("domainId") Integer domainId);

    @Query("SELECT COUNT(fse.id) FROM FormStepEntity fse WHERE fse.formName = :formName AND fse.domainId = :domainId")
    public int getNumberOfSteps(@Param("formName") String formName, @Param("domainId") Integer domainId);
}