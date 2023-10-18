package sk.iway.iwcm.components.users.userdetail;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
