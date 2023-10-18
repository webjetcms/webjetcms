package sk.iway.iwcm.components.export;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ExportDatRepository extends JpaRepository<ExportDatBean, Long>, JpaSpecificationExecutor<ExportDatBean> {

    Page<ExportDatBean> findAll(Pageable pageable);
}
