package sk.iway.iwcm.branch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Explicitly scoped access to files working rows. */
public interface BranchFileRepository extends JpaRepository<BranchFileEntity,Long>
{
    List<BranchFileEntity> findByBranchId(Long branchId);
    Optional<BranchFileEntity> findByBranchIdAndPathKey(Long branchId, String key);
}
