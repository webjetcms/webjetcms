package sk.iway.iwcm.components.basket.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BasketInvoicePaymentsRepository extends JpaRepository<BasketInvoicePaymentEntity, Long> {
    Page<BasketInvoicePaymentEntity> findAllByInvoiceId(Long invoiceId, Pageable pageable);
}
