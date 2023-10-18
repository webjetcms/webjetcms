package sk.iway.iwcm.components.perex_groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerexGroupsRepository extends JpaRepository<PerexGroupsEntity, Long> {
    
}
