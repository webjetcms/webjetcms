package sk.iway.iwcm.components.basket.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface BasketInvoicesRepository extends DomainIdRepository<BasketInvoiceEntity, Long> {
    Page<BasketInvoiceEntity> findAllByLoggedUserIdAndDomainId(Integer loggedUserId, Integer domainId, Pageable pageable);

    List<BasketInvoiceEntity> findAllByLoggedUserIdAndDomainIdOrderByCreateDateDesc(Integer loggedUserId, Integer domainId);

    @Query("SELECT bie.statusId FROM BasketInvoiceEntity bie WHERE bie.id = :id AND bie.domainId = :domainId")
    Integer getStatusId(@Param("id") Long id, @Param("domainId") Integer domainId);

    @Query("""
        SELECT bie.createDate AS createDate,
               bie.statusId AS statusId,
               bie.deliveryMethod AS deliveryMethod,
               bie.paymentMethod AS paymentMethod,
               bie.priceToPayVat AS priceToPayVat,
               bie.priceToPayNoVat AS priceToPayNoVat,
               bie.currency AS currency
        FROM BasketInvoiceEntity bie
        WHERE bie.domainId = :domainId
          AND bie.createDate >= :dateFrom
          AND bie.createDate <= :dateTo
        ORDER BY bie.createDate ASC
        """)
    List<BasketInvoiceStatsProjection> findAllForStatistics(
        @Param("domainId") Integer domainId,
        @Param("dateFrom") Date dateFrom,
        @Param("dateTo") Date dateTo
    );
}
