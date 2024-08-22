package sk.iway.iwcm.components.enumerations.model;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EnumerationDataRepository extends JpaRepository<EnumerationDataBean, Long>, JpaSpecificationExecutor<EnumerationDataBean> {

    Page<EnumerationDataBean> findAllByTypeIdAndHiddenFalse(Integer enumerationTypeId, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "UPDATE EnumerationDataBean SET hidden = 1 WHERE enumerationDataId = :enumerationDataId")
    void deleteEnumDataById(@Param("enumerationDataId")Integer enumerationDataId);

    @Query(value = "SELECT * FROM enumeration_data WHERE enumeration_data_id = ?1 and hidden = 0", nativeQuery=true)
    EnumerationDataBean getNonHiddenByEnumId(Long enumerationDataId);

    @Query(value = "SELECT * FROM enumeration_data WHERE enumeration_data_id = ?1", nativeQuery=true)
    EnumerationDataBean getEnumId(Integer enumerationDataId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE enumeration_data SET hidden = 1 WHERE enumeration_type_id = ?1", nativeQuery=true)
    void deleteAllEnumDataByEnumTypeId(Integer enumerationTypeId);

    @Query(value = "SELECT child_enumeration_type_id FROM enumeration_data WHERE enumeration_data_id = ?1", nativeQuery=true)
    Integer getChildEnumTypeIdByEnumDataId(Integer enumerationDataId);

    @Query(value = "SELECT parent_enumeration_data_id FROM enumeration_data WHERE enumeration_data_id = ?1", nativeQuery=true)
    Integer getParentEnumDataIdByEnumDataId(Integer enumerationDataId);

    @Query(value = "SELECT hidden FROM enumeration_data WHERE enumeration_data_id = ?1", nativeQuery=true)
    Object getHiddenByEnumerationDataId(Integer enumerationDataId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE enumeration_data SET child_enumeration_type_id = null WHERE enumeration_type_id = ?1", nativeQuery=true)
    void denyChildEnumerationTypeByTypeId(Long enumerationTypeId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE enumeration_data SET parent_enumeration_data_id = null WHERE enumeration_type_id = ?1", nativeQuery=true)
    void denyParentEnumerationDataByTypeId(Long enumerationTypeId);

    @Query(value = "SELECT edb.id FROM EnumerationDataBean edb WHERE edb.string1 = :string1 AND edb.typeId = :typeId")
    Integer getIdByString1AndTypeId(@Param("string1")String string1, @Param("typeId")Integer typeId);

    @Query(value = "SELECT MAX(edb.sortPriority) FROM EnumerationDataBean edb WHERE edb.typeId = :typeId")
    Optional<Integer> findMaxSortPriorityByTypeId(@Param("typeId")Integer typeId);
}