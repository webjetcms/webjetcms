package sk.iway.iwcm.branch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Explicitly scoped access to documents working rows. */
public interface BranchDocumentRepository extends JpaRepository<BranchDocumentEntity,Long>
{
    List<BranchDocumentEntity> findByBranchId(Long branchId);
    Optional<BranchDocumentEntity> findByBranchIdAndDocId(Long branchId, Integer key);
}
