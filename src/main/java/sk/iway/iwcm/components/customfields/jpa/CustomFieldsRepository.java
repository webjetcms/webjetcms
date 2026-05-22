package sk.iway.iwcm.components.customfields.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CustomFieldsRepository extends JpaRepository<CustomFieldsEntity, Long>, JpaSpecificationExecutor<CustomFieldsEntity> {

    @Query("SELECT cfe FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.entityId IS NULL")
    List<CustomFieldsEntity> findAllGlobalCustomFields(String className);

    List<CustomFieldsEntity> findAllByClassNameAndEntityId(String className, Long entityid);

    @Query("SELECT cfe.id FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.alphabet = :alphabet AND cfe.entityId = :entityId")
    Optional<Long> countEntities(String className, String alphabet, Long entityId);

    @Query("SELECT cfe.id FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.alphabet = :alphabet AND cfe.entityId IS NULL")
    Optional<Long> countNullEntities(String className, String alphabet);
}
