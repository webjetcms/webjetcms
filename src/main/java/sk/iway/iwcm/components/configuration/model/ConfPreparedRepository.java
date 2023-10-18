package sk.iway.iwcm.components.configuration.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfPreparedRepository extends JpaRepository<ConfPreparedEntity, Long>, JpaSpecificationExecutor<ConfPreparedEntity>{
    Page<ConfPreparedEntity> findByName(Pageable pageable, String name);
    Page<ConfPreparedEntity> findByNameAndDatePreparedIsNotNull(Pageable pageable, String name);
    Page<ConfPreparedEntity> findByNameAndDatePreparedIsNull(Pageable pageable, String name);
}
