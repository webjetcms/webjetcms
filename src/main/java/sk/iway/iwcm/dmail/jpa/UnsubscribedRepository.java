package sk.iway.iwcm.dmail.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Odhlasene emaily z hromadneho emailu
 */

@Repository
public interface UnsubscribedRepository extends JpaRepository<UnsubscribedEntity, Long>, JpaSpecificationExecutor<UnsubscribedEntity>{

}
