package sk.iway.iwcm.components.gallery;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface GalleryRepository extends DomainIdRepository<GalleryEntity, Long> {

    GalleryEntity findByImagePathAndImageNameAndDomainId(String imagePath, String imageName, int domainId);

    @Query(value = "SELECT ge.id FROM GalleryEntity ge WHERE ge.imagePath = :imagePath AND ge.imageName = :imageName AND ge.domainId = :domainId")
    Optional<Long> findIdByImagePathAndImageNameAndDomainId(@Param("imagePath") String imagePath, @Param("imageName") String imageName, @Param("domainId") int domainId);
}