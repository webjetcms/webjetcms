package sk.iway.iwcm.components.form_settings.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FormSettingsRepository extends DomainIdRepository<FormSettingsEntity, Long> {
    FormSettingsEntity findByFormNameAndDomainId(String formName, Integer domainId);

    @Query("SELECT fse.id FROM FormSettingsEntity fse WHERE fse.formName = :formName AND fse.domainId = :domainId")
    Long findId(@Param("formName") String formName, @Param("domainId") Integer domainId);

    @Query("SELECT fse.rowView FROM FormSettingsEntity fse WHERE fse.formName = :formName AND fse.domainId = :domainId")
    Boolean isRowView(@Param("formName") String formName, @Param("domainId") Integer domainId);

    @Query("SELECT fse.doubleOptIn FROM FormSettingsEntity fse WHERE fse.formName = :formName AND fse.domainId = :domainId")
    Boolean isDoubleOptIn(@Param("formName") String formName, @Param("domainId") Integer domainId);
}