package sk.iway.iwcm.components.gallery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface GalleryRepository extends DomainIdRepository<GalleryEntity, Long> {

    GalleryEntity findByImagePathAndImageNameAndDomainId(String imagePath, String imageName, int domainId);

    Page<GalleryEntity> findAllByPerexGroupIsNotNull(Pageable pageable);
}