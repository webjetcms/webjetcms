package sk.iway.iwcm.components.users.userdetail;

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
public interface UserDetailsRepository extends JpaRepository<UserDetailsEntity, Long>, JpaSpecificationExecutor<UserDetailsEntity> {

    Page<UserDetailsEntity> findAllByAdmin(Boolean admin, Pageable pageable);

    List<UserDetailsEntity> findAllByAdmin(Boolean isAdmin);

    @Query(value = "SELECT user_id FROM users WHERE user_groups LIKE ?1 OR user_groups LIKE ?2 OR user_groups LIKE ?3 OR user_groups LIKE ?4", nativeQuery=true)
    List<Integer> getUserIdsByUserGroupsIds(String groupId1, String groupId2, String groupId3, String groupId4);

    @Query(value = "SELECT email FROM users WHERE user_groups LIKE ?1 OR user_groups LIKE ?2 OR user_groups LIKE ?3 OR user_groups LIKE ?4", nativeQuery=true)
    List<String> getUserEmailsByUserGroupsIds(String groupId1, String groupId2, String groupId3, String groupId4);

    @Query(value = "SELECT api_key FROM users WHERE user_id=?1", nativeQuery=true)
    String getApiKeyByUserId(Long userId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE UserDetailsEntity SET editableGroups = :editableGroups, writableFolders = :writableFolders WHERE id = :userId")
    void updateEditableGroupsWritableFolders(@Param("editableGroups") String editableGroups, @Param("writableFolders") String writableFolders, @Param("userId") Long userId);

    @Query(value = "SELECT mobile_device FROM users WHERE user_id=?1", nativeQuery=true)
    String getMobileDeviceByUserId(Long userId);

    @Query(value = "SELECT password_salt FROM users WHERE user_id=?1", nativeQuery=true)
    String getPasswordSaltByUserId(Long userId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE users SET mobile_device = ?2 WHERE user_id = ?1", nativeQuery=true)
    void updateMobileDeviceByUserId(Long userId, String mobileDevice);

    UserDetailsEntity findByEmail(String email);

    //For change password service
    List<UserDetailsEntity> findAllByEmailOrderByIdDesc(String email);
    List<UserDetailsEntity> findByLoginOrderByIdDesc(String login);
    //For change password service - only ADMINS
    List<UserDetailsEntity> findAllByEmailAndAdminTrueOrderByIdDesc(String email);
    List<UserDetailsEntity> findByLoginAndAdminTrueOrderByIdDesc(String login);

    //get users by id list
    List<UserDetailsEntity> findAllByIdIn(List<Long> ids);
}
