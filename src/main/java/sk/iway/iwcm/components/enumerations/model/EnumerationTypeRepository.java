package sk.iway.iwcm.components.enumerations.model;

import java.util.List;

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
public interface EnumerationTypeRepository extends JpaRepository<EnumerationTypeBean, Long>, JpaSpecificationExecutor<EnumerationTypeBean> {
    @Query(value = "SELECT * FROM enumeration_type WHERE enumeration_type_id = ?1 AND hidden = 0", nativeQuery=true)
    EnumerationTypeBean getNonHiddenByEnumId(Integer enumerationTypeId);

    @Query(value = "SELECT * FROM enumeration_type WHERE enumeration_type_id = ?1", nativeQuery=true)
    EnumerationTypeBean getByEnumId(Integer enumerationTypeId);

    @Query(value = "SELECT * FROM enumeration_type WHERE hidden = 0 ORDER BY enumeration_type_id", nativeQuery=true)
    List<EnumerationTypeBean> getAllNonHiddenOrderedById();

    EnumerationTypeBean findFirstByHiddenOrderById(Integer hidden);

    @Transactional
    @Modifying
    @Query(value = "UPDATE EnumerationTypeBean SET hidden = 1 WHERE enumerationTypeId = :enumerationTypeId")
    void deleteEnumTypeById(@Param("enumerationTypeId")Integer enumerationTypeId);

    Page<EnumerationTypeBean> findAllByHiddenFalse(Pageable pagebale);

    @Query(value = "SELECT hidden FROM enumeration_type WHERE enumeration_type_id = ?1", nativeQuery=true)
    Object getHiddenByEnumTypeId(Integer enumerationTypeId);

    @Query(value = "SELECT child_enumeration_type_id FROM enumeration_type WHERE enumeration_type_id = ?1", nativeQuery=true)
    Integer getChildEnumTypeId(Integer enumerationTypeId);

    @Query(value = "SELECT allow_child_enumeration_type FROM enumeration_type WHERE enumeration_type_id = ?1", nativeQuery=true)
    Object isAllowChildEnumerationType(Long enumerationTypeId);

    @Query(value = "SELECT allow_parent_enumeration_data FROM enumeration_type WHERE enumeration_type_id = ?1", nativeQuery=true)
    Object isAllowParentEnumerationData(Long enumerationTypeId);

    @Query(value = "SELECT etb.id FROM EnumerationTypeBean etb WHERE etb.typeName = :typeName")
    Integer getIdByTypeName(@Param("typeName")String typeName);
}
