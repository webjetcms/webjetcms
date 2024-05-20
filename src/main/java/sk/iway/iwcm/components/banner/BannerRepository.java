package sk.iway.iwcm.components.banner;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface BannerRepository extends DomainIdRepository<BannerBean, Long> {
    List<BannerBean> findDistinctAllByBannerGroupLikeAndDomainId(String bannerGroup, int domainId);
}