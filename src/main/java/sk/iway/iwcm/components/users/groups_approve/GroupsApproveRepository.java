package sk.iway.iwcm.components.users.groups_approve;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.doc.GroupDetails;

/**
 * Pouzivatelia - mapovanie schvalovania
 */
@Repository
public interface GroupsApproveRepository extends JpaRepository<GroupsApproveEntity, Long>, JpaSpecificationExecutor<GroupsApproveEntity> {

    Page<GroupsApproveEntity> findByUserId(int userId, Pageable pageable);

    List<GroupsApproveEntity> findByUserId(int userId);

    public List<GroupsApproveEntity> findByGroup(GroupDetails group);
}
