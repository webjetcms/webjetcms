package sk.iway.iwcm.system;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence operations for URL redirects, including domain-restricted bulk
 * operations used by redirect cleaning.
 */
@Repository
public interface RedirectsRepository extends JpaRepository<UrlRedirectBean, Long>, JpaSpecificationExecutor<UrlRedirectBean> {

    /**
     * Loads redirects belonging to the selected domain and optionally redirects
     * from the independent unnamed scope represented by a {@code null} or empty
     * domain. The cleaning service never combines these scopes during analysis.
     *
     * @param domainName normalized selected domain
     * @param includeUnnamed whether redirects without a domain are included
     *        alongside a selected named domain
     * @return redirects accessible from the selected domain
     */
    @Query("SELECT redirect FROM UrlRedirectBean redirect WHERE " +
        "(redirect.domainName = :domainName OR " +
        "((:includeUnnamed = true OR :domainName = '') AND (redirect.domainName IS NULL OR redirect.domainName = '')))")
    List<UrlRedirectBean> findAllForRedirectCleaning(
        @Param("domainName") String domainName,
        @Param("includeUnnamed") boolean includeUnnamed
    );

    /**
     * Bulk update of the target URL for redirect cleaning, restricted to records accessible from the current domain.
     * Records with an empty/NULL domain are accessible as the independent unnamed
     * scope; named records must match the selected domain.
     *
     * @param ids redirect identifiers to update
     * @param newUrl new target URL
     * @param domainName normalized selected domain
     * @return number of updated rows
     */
    @Transactional
    @Modifying
    @Query("UPDATE UrlRedirectBean redirect SET redirect.newUrl = :newUrl WHERE redirect.urlRedirectId IN :ids AND " +
        "(redirect.domainName IS NULL OR redirect.domainName = '' OR redirect.domainName = :domainName)")
    int updateNewUrlForRedirectCleaning(@Param("ids") List<Long> ids, @Param("newUrl") String newUrl, @Param("domainName") String domainName);

    /**
     * Bulk delete for redirect cleaning, restricted to records accessible from the current domain.
     * Records with an empty/NULL domain are accessible as the independent unnamed
     * scope; named records must match the selected domain.
     *
     * @param ids redirect identifiers to delete
     * @param domainName normalized selected domain
     * @return number of deleted rows
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UrlRedirectBean redirect WHERE redirect.urlRedirectId IN :ids AND " +
        "(redirect.domainName IS NULL OR redirect.domainName = '' OR redirect.domainName = :domainName)")
    int deleteForRedirectCleaning(@Param("ids") List<Long> ids, @Param("domainName") String domainName);
}
