package sk.iway.iwcm.components.forms;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RegExpRepository extends JpaRepository<RegExpEntity, Long> {

    @Query("SELECT ree.id FROM RegExpEntity ree WHERE ree.type IN :types")
    List<Long> findRegexIdsByTypeIn(@Param("types") List<String> types);
}
