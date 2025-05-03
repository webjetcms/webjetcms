package sk.iway.iwcm.components.basket.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BasketInvoicePaymentsRepository extends JpaRepository<BasketInvoicePaymentEntity, Long> {
    Page<BasketInvoicePaymentEntity> findAllByInvoiceId(Long invoiceId, Pageable pageable);
    List<BasketInvoicePaymentEntity> findAllByInvoiceIdAndConfirmedTrue(Long invoiceId);

    List<BasketInvoicePaymentEntity> findAllByInvoiceId(Long invoiceId);
    List<BasketInvoicePaymentEntity> findAllByInvoiceIdAndConfirmed(Long invoiceId, Boolean confirmed);
}
