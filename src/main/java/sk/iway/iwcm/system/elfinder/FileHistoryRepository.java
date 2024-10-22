package sk.iway.iwcm.system.elfinder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FileHistoryRepository extends DomainIdRepository<FileHistoryEntity, Long> {   
    Page<FileHistoryEntity> findAllByFileUrlAndDomainIdOrderByChangeDateDesc(String fileUrl, Integer domainId, Pageable pageable);
}