package sk.iway.iwcm.components.banner;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface BannerRepository extends DomainIdRepository<BannerBean, Long> {
    @Query("SELECT DISTINCT b.bannerGroup FROM BannerBean b WHERE b.bannerGroup LIKE :term AND b.domainId = :domainId")
    List<String> findDistinctBannerGroupsByGroupLikeAndDomainId(@Param("term") String term, @Param("domainId") int domainId);
}