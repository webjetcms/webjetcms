package sk.iway.iwcm.branch;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

/** Package metadata access with explicit website domain scope. */
public interface BranchRepository extends DomainIdRepository<BranchEntity, Long>
{
}
