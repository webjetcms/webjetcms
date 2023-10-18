package sk.iway.iwcm.components.gallery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GalleryDimensionRepository extends JpaRepository<GalleryDimension, Long> {
    Optional<GalleryDimension> findFirstByPathAndDomainId(String path, int domainId);
    Optional<GalleryDimension> findFirstByPathLikeAndDomainId(String path, int domainId);
}