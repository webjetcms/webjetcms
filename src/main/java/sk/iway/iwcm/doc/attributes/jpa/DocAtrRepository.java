package sk.iway.iwcm.doc.attributes.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocAtrRepository extends JpaRepository<DocAtrEntity, Long>, JpaSpecificationExecutor<DocAtrEntity> {

    @Query(value = "SELECT distinct(value_string) FROM doc_atr WHERE atr_id=?1 ORDER BY value_string", nativeQuery=true)
    List<String> findAutoSelect(Integer atrId);

    //delete all atr for doc
    @Transactional
    @Modifying
    void deleteAllByDocId(Integer docId);
}
