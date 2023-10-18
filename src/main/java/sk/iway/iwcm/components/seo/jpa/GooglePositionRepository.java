package sk.iway.iwcm.components.seo.jpa;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository

public interface GooglePositionRepository extends JpaRepository<GooglePositionEntity, Long>, JpaSpecificationExecutor<GooglePositionEntity> {
    Page<GooglePositionEntity> findAllByKeywordIdAndDayDateBetween(Integer keywordId, Date from, Date to, Pageable  pageable);
}
