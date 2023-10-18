package sk.iway.iwcm.components.users.permgroups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.users.PermissionGroupBean;

@Repository
public interface PermissionGroupRepository extends JpaRepository<PermissionGroupBean, Long>{

}
