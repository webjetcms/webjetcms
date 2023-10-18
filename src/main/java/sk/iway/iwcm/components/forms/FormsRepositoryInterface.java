package sk.iway.iwcm.components.forms;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@NoRepositoryBean
public interface FormsRepositoryInterface<E> extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {

    List<E> findAllByCreateDateIsNullAndDomainId(Integer domainId);

    int countAllByFormNameAndDomainId(String formName, Integer domainId);

    E findTopByFormNameAndDomainIdOrderByCreateDateDesc(String formName, Integer domainId);

    E findFirstByFormNameAndDomainIdAndCreateDateIsNullOrderByIdAsc(String formName, Integer domainId);

    Page<E> findAllByFormNameAndDomainIdAndCreateDateNotNull(String formName, Integer domainId, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "UPDATE FormsEntity SET lastExportDate = :lastExportDate WHERE id IN :formIds")
    public void updateLastExportDate(@Param("lastExportDate")Date lastExportDate, @Param("formIds")List<Long> formIds);

    @Transactional
    void deleteByFormNameAndDomainId(String formName, Integer domainId);
}
