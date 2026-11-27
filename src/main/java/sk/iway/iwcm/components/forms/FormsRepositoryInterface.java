package sk.iway.iwcm.components.forms;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

import java.util.Date;
import java.util.List;

@NoRepositoryBean
public interface FormsRepositoryInterface<E> extends DomainIdRepository<E, Long> {

    /** Finds pages containing the form include with a quoted or unquoted name. */
    @Query("""
        SELECT d.id FROM DocDetails d
        WHERE d.data LIKE :encodedQuotePattern ESCAPE '~'
           OR d.data LIKE :doubleQuotePattern ESCAPE '~'
           OR d.data LIKE :singleQuotePattern ESCAPE '~'
           OR d.data LIKE :unquotedPattern ESCAPE '~'
        """)
    List<Long> findDocIdsByFormInclude(
            @Param("encodedQuotePattern") String encodedQuotePattern,
            @Param("doubleQuotePattern") String doubleQuotePattern,
            @Param("singleQuotePattern") String singleQuotePattern,
            @Param("unquotedPattern") String unquotedPattern);

    List<E> findAllByCreateDateIsNull();

    List<E> findAllByCreateDateIsNullAndDomainId(Integer domainId);

    int countAllByFormNameAndDomainId(String formName, Integer domainId);

    E findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(String formName, Integer domainId);

    E findFirstByFormNameAndDomainIdAndCreateDateIsNullOrderByIdAsc(String formName, Integer domainId);

    Page<E> findAllByFormNameAndDomainIdAndCreateDateNotNull(String formName, Integer domainId, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "UPDATE FormsEntity fe SET fe.lastExportDate = :lastExportDate WHERE fe.id IN :formIds")
    public void updateLastExportDate(@Param("lastExportDate")Date lastExportDate, @Param("formIds")List<Long> formIds);

    @Transactional
    void deleteByFormNameAndDomainId(String formName, Integer domainId);

    /** Deletes submitted responses while preserving the form's management records. */
    @Transactional
    void deleteAllByFormNameAndDomainIdAndCreateDateNotNull(String formName, Integer domainId);

    /** Deletes management records only after response deletion and any required definition cleanup succeed. */
    @Transactional
    void deleteAllByFormNameAndDomainIdAndCreateDateNull(String formName, Integer domainId);
}
