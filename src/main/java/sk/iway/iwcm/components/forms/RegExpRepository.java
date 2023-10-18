package sk.iway.iwcm.components.forms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegExpRepository extends JpaRepository<RegExpEntity, Long> {

}
