package sk.iway.iwcm.components.forms;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FormsRepository extends FormsRepositoryInterface<FormsEntity>{

    @Query("SELECT COUNT(fe.id) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NOT NULL")
    Integer getNumberOfSubmitted(@Param("formName") String formName, @Param("domainId") Integer domainId);
}