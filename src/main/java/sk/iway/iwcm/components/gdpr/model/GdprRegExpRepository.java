package sk.iway.iwcm.components.gdpr.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GdprRegExpRepository extends JpaRepository<GdprRegExpBean, Long>, JpaSpecificationExecutor<GdprRegExpBean> {

    Page<GdprRegExpBean> findAllByDomainId(int domainId, Pageable pageable);
}
