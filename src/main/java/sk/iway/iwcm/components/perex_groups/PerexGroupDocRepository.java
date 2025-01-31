package sk.iway.iwcm.components.perex_groups;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PerexGroupDocRepository extends JpaRepository<PerexGroupDocEntity, Long> {

    List<PerexGroupDocEntity> findAllByDocId(Long docId);

    @Transactional
    @Modifying
    void deleteAllByPerexGroupId(Long perexGroupId);

    @Transactional
    @Modifying
    void deleteAllByDocId(Long docId);
}
