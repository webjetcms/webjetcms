package sk.iway.iwcm.components.seo.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagementKeywordsRepository extends JpaRepository<ManagementKeywordsEntity, Long>, JpaSpecificationExecutor<ManagementKeywordsEntity> {
    
    @Query(value = "SELECT DISTINCT author FROM seo_keywords", nativeQuery=true)
    List<Integer> getDistinctUserIds();

    @Query(value = "SELECT DISTINCT domain FROM seo_keywords", nativeQuery=true)
    List<String> getDistinctDomains();

    @Query(value = "SELECT DISTINCT domain FROM seo_keywords WHERE domain LIKE ?1", nativeQuery=true)
    List<String> getDistinctDomainsLike(String param);

    @Query(value = "SELECT search_bot FROM seo_keywords WHERE search_bot LIKE ?1 GROUP BY search_bot ORDER BY COUNT(search_bot) DESC", nativeQuery=true)
    List<String> getMostUsedBotsLike(String param);
}
