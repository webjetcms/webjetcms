package sk.iway.iwcm.components.basket.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface BasketInvoicesRepository extends DomainIdRepository<BasketInvoiceEntity, Long> {
    Page<BasketInvoiceEntity> findAllByLoggedUserIdAndDomainId(Integer loggedUserId, Integer domainId, Pageable pageable);

    @Query("SELECT bie.statusId FROM BasketInvoiceEntity bie WHERE bie.id = :id AND bie.domainId = :domainId")
    Integer getStatusId(@Param("id") Long id, @Param("domainId") Integer domainId);
}
