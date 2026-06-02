package sk.iway.iwcm.components.customfields.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CustomFieldsRepository extends JpaRepository<CustomFieldsEntity, Long>, JpaSpecificationExecutor<CustomFieldsEntity> {

    @Query("SELECT cfe FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.entityId = 0 AND cfe.bonusClassName = ''")
    List<CustomFieldsEntity> findAllGlobalCustomFields(String className);

    @Query("SELECT cfe FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.entityId = :entityId AND cfe.bonusClassName = ''")
    List<CustomFieldsEntity> findAllByClassNameAndEntityId(String className, Long entityId);

    @Query("SELECT cfe FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.bonusClassName = :bonusClassName AND cfe.bonusEntityId = :bonusEntityId")
    List<CustomFieldsEntity> findAllByClassNameAndBonusContext(String className, String bonusClassName, Long bonusEntityId);

    @Query("SELECT cfe.id FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.alphabet = :alphabet AND cfe.entityId = :entityId")
    Optional<Long> getEntityId(String className, String alphabet, Long entityId);

    @Query("SELECT cfe.id FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.alphabet = :alphabet AND cfe.entityId IS NULL")
    Optional<Long> getNullEntityId(String className, String alphabet);
}
