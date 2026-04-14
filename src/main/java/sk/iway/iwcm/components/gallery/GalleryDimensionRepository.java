package sk.iway.iwcm.components.gallery;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GalleryDimensionRepository extends DomainIdRepository<GalleryDimension, Long> {
    Optional<GalleryDimension> findFirstByPathAndDomainId(String path, int domainId);
    List<GalleryDimension> findByPathLikeAndDomainId(String path, int domainId);

    @Query("SELECT gd FROM GalleryDimension gd WHERE gd.path LIKE :baseFolder AND (gd.path LIKE :searchContains OR gd.name LIKE :searchContains) AND gd.domainId = :domainId")
    List<GalleryDimension> findByPathLikeOrNameLikeAndDomainId(@Param("baseFolder") String baseFolder, @Param("searchContains") String searchContains, @Param("domainId") int domainId);

    Optional<GalleryDimension> findFirstByPathLikeAndDomainId(String path, int domainId);
}