package sk.iway.iwcm.components.media;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;
import sk.iway.spirit.model.Media;

@Repository
public interface MediaRepository extends DomainIdRepository<Media, Long> {

    //add custom repository to find media's with connection to doc
    //finding by mediaFkId(docId) and mediaFkTableName("documents")
    Page<Media> findAllByMediaFkIdAndMediaFkTableNameAndDomainId(Integer mediaFkId, String mediaFkTableName, Integer domainId, Pageable pageable);

    List<Media> findAllByMediaFkIdAndMediaFkTableNameAndDomainId(Integer mediaFkId, String mediaFkTableName, Integer domainId);

    //
    @Query(value = "SELECT distinct(media_fk_id) FROM media WHERE media_link = ?1 AND media_fk_table_name = ?2", nativeQuery=true)
    List<Long> findMediaIds(String mediaLink, String mediaFkTableName );

    //
    @Transactional
    @Modifying
    @Query(value = "UPDATE media SET media_link = ?1 WHERE media_link = ?2", nativeQuery=true)
    void updateMedia(String newMediaLink, String oldMediaLink );

    @Transactional
    @Modifying
    @Query(value = "UPDATE Media SET mediaLink = :newMediaLink WHERE mediaLink = :oldMediaLink AND mediaFkId IN :mediaFkIds AND mediaFkTableName='documents'")
    void updateMedia(@Param("newMediaLink")String newMediaLink, @Param("oldMediaLink")String oldMediaLink, @Param("mediaFkIds")List<Long> mediaFkIds);
}
