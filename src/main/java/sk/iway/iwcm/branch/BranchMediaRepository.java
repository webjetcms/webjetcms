package sk.iway.iwcm.branch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Explicitly scoped access to media working rows. */
public interface BranchMediaRepository extends JpaRepository<BranchMediaEntity,Long>
{
    List<BranchMediaEntity> findByBranchId(Long branchId);
    Optional<BranchMediaEntity> findByBranchIdAndMediaId(Long branchId, Integer key);
}
