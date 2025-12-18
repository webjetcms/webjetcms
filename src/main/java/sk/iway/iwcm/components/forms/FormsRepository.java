package sk.iway.iwcm.components.forms;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FormsRepository extends FormsRepositoryInterface<FormsEntity>{

    @Query("SELECT COUNT(fe.id) FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.createDate IS NOT NULL")
    Integer getNumberOfSubmitted(@Param("formName") String formName, @Param("domainId") Integer domainId);

    @Transactional
    @Modifying
    @Query("DELETE FROM FormsEntity fe WHERE fe.formName = :formName AND fe.domainId = :domainId AND fe.userId = :userId AND fe.createDate IS NOT NULL")
    void deleteAllUserSubmitted(@Param("formName") String formName, @Param("domainId") Integer domainId, @Param("userId") Long userId);
}