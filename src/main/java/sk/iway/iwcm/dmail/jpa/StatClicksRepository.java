package sk.iway.iwcm.dmail.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface StatClicksRepository extends JpaRepository<StatClicksEntity, Long>, JpaSpecificationExecutor<StatClicksEntity> {

    Page<StatClicksEntity> findByCampainId(Long campainId, Pageable pageable);

    @Transactional
    @Modifying
    public void deleteByCampainId(Long campainId);

    @Transactional
    @Modifying
    public void deleteByEmailId(Long emailId);
}