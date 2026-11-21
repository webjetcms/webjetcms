package sk.iway.iwcm.components.media;

import java.util.List;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import sk.iway.spirit.model.MediaGroupBean;

@Repository
public interface MediaGroupRepository extends JpaRepository<MediaGroupBean, Long> {
    @QueryHints(@QueryHint(name="eclipselink.refresh", value="true"))
    List<MediaGroupBean> findAllByOrderByIdAsc();
}
