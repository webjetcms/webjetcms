package sk.iway.iwcm.components.users.userdetail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDetailsSelfRepository extends JpaRepository<UserDetailsSelfEntity, Long>, JpaSpecificationExecutor<UserDetailsSelfEntity> {


}
