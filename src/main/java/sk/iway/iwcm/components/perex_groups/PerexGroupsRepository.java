package sk.iway.iwcm.components.perex_groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerexGroupsRepository extends JpaRepository<PerexGroupsEntity, Long> {
    List<PerexGroupsEntity> findAllByOrderByPerexGroupNameAsc();
}
