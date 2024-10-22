package sk.iway.iwcm.components.gallery;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GalleryDimensionRepository extends DomainIdRepository<GalleryDimension, Long> {
    Optional<GalleryDimension> findFirstByPathAndDomainId(String path, int domainId);
    List<GalleryDimension> findByPathLikeAndDomainId(String path, int domainId);
    Optional<GalleryDimension> findFirstByPathLikeAndDomainId(String path, int domainId);
}