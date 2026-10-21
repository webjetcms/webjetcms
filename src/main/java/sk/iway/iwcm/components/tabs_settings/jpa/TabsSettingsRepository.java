package sk.iway.iwcm.components.tabs_settings.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

/**
 * Persists tab settings and checks for duplicate class/tab settings in the current domain.
 */
@Repository
public interface TabsSettingsRepository extends DomainIdRepository<TabsSettingsEntity, Long> {

    List<TabsSettingsEntity> findAllByClassNameAndDomainId(String className, int domainId);

    boolean existsByClassNameAndTabIdAndDomainIdAndIdNot(String className, String tabId, int domainId, Long id);
}
