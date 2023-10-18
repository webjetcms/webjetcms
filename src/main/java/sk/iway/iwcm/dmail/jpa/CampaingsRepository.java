package sk.iway.iwcm.dmail.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaingsRepository extends JpaRepository<CampaingsEntity, Long> {
    //TODO: upravit na pouzitie domain_id, bude potrebne potom spravit aj konverziu existujucich zaznamov podla URL/DocDetails na spravne domain_id
}
