package sk.iway.iwcm.components.basket.payment_methods.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface PaymentMethodRepository extends DomainIdRepository<PaymentMethodEntity, Long> {

    public PaymentMethodEntity findByPaymentMethodNameAndDomainId(String paymentMethodName, Integer domainId);

    @Query("SELECT pme.id FROM PaymentMethodEntity pme WHERE pme.paymentMethodName = :paymentMethodName AND pme.domainId = :domainId")
    Long getPymentMethodId(@Param("paymentMethodName")String paymentMethodName, @Param("domainId")Integer domainId);

    @Transactional
    void deleteByPaymentMethodNameAndDomainId(String paymentMethodName, Integer domainId);
}
