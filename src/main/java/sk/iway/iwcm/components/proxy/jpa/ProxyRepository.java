package sk.iway.iwcm.components.proxy.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProxyRepository extends JpaRepository<ProxyBean, Long>, JpaSpecificationExecutor<ProxyBean> {
}
