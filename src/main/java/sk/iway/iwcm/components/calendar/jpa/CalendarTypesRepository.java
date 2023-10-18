package sk.iway.iwcm.components.calendar.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarTypesRepository extends JpaRepository<CalendarTypesEntity, Long>{

    Page<CalendarTypesEntity> findAllByDomainId(Integer domainId, Pageable pageable);

    List<CalendarTypesEntity> findAllByDomainId(Integer domainId);

    CalendarTypesEntity findByIdAndDomainId(Long id, Integer domainId);
}
