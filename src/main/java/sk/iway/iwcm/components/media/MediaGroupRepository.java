package sk.iway.iwcm.components.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sk.iway.spirit.model.MediaGroupBean;

@Repository
public interface MediaGroupRepository extends JpaRepository<MediaGroupBean, Long> {

}
