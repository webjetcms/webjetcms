package sk.iway.iwcm.components.insertScript;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsertScriptRepository extends JpaRepository<InsertScriptBean, Long> {

    List<InsertScriptBean> findDistinctAllByPositionLikeAndDomainId(String position, Integer domainId);

}
