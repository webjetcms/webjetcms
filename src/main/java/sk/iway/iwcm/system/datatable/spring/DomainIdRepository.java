package sk.iway.iwcm.system.datatable.spring;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repozitar pre pracu s tabulkami obsahujucich domain_id, ktore oddeluje zaznamy v tabulke podla domeny
 * dokumentacia k pouzitiu v docs/developer/datatables/domainid.md, vsetko zabezpecuje DatatableRestControllerV2,
 * staci ak vas repozitar extenduje tuto triedu, priklad:
 * public interface QuestionsAnswersRepository extends DomainIdRepository<QuestionsAnswersEntity, Long> {
 */
@NoRepositoryBean
public interface DomainIdRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    Optional<T> findFirstByIdAndDomainId(Long id, int domainId);

    List<T> findAllByDomainId(int domainId);
    Page<T> findAllByDomainId(int domainId, Pageable pageable);

    //netreba, riesime volanim findFirstByIdAndDomainId a naslednym delete(entity)
    //void deleteByIdAndDomainId(Long id, int domainId);
}
