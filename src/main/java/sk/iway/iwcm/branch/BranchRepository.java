package sk.iway.iwcm.branch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package metadata access with explicit website domain scope. */
public interface BranchRepository extends JpaRepository<BranchEntity,Long>
{
    List<BranchEntity> findByDomainNameOrderByIdDesc(String domainName);
    Optional<BranchEntity> findByIdAndDomainName(Long id, String domainName);
}
