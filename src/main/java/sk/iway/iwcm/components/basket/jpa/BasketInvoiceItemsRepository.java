package sk.iway.iwcm.components.basket.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface BasketInvoiceItemsRepository extends DomainIdRepository<BasketInvoiceItemEntity, Long> {
    Page<BasketInvoiceItemEntity> findAllByInvoiceIdAndDomainId(Long invoiceId, Integer domainId, Pageable pageable);
}