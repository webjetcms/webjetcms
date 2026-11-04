package sk.iway.iwcm.branch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Explicitly scoped access to groups working rows. */
public interface BranchGroupRepository extends JpaRepository<BranchGroupEntity,Long>
{
    List<BranchGroupEntity> findByBranchId(Long branchId);
    Optional<BranchGroupEntity> findByBranchIdAndGroupId(Long branchId, Integer key);
}
