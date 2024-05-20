package sk.iway.iwcm.dmail.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface StatClicksRepository extends JpaRepository<StatClicksEntity, Long>, JpaSpecificationExecutor<StatClicksEntity> {

    Page<StatClicksEntity> findByCampainId(Long campainId, Pageable pageable);

    @Transactional
    @Modifying
    public void deleteByCampainId(Long campainId);

    //email_id is ambigious in entity, we must use nativequery
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM emails_stat_click WHERE email_id = ?1", nativeQuery=true)
    void deleteByEmailId(Long emailId);
}