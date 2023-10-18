package sk.iway.iwcm.doc.attributes.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface DocAtrDefRepository extends DomainIdRepository<DocAtrDefEntity, Long> {

    @Query(value = "SELECT distinct(atr_group) FROM doc_atr_def WHERE domain_id=?1 ORDER BY atr_group", nativeQuery=true)
    List<String> findDistinctGroups(Integer domainId);

    @Query("SELECT d FROM DocAtrDefEntity d LEFT JOIN FETCH d.docAtrEntities a ON a.docId = :docId WHERE d.domainId = :domainId ORDER BY d.orderPriority")
    List<DocAtrDefEntity> findAllByDocId(@Param("docId") Integer docId, @Param("domainId") Integer domainId);
}
