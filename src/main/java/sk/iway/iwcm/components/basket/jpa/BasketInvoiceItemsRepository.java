package sk.iway.iwcm.components.basket.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface BasketInvoiceItemsRepository extends DomainIdRepository<BasketInvoiceItemEntity, Long> {
    Page<BasketInvoiceItemEntity> findAllByInvoiceIdAndDomainId(Long invoiceId, Integer domainId, Pageable pageable);
    List<BasketInvoiceItemEntity> findAllByInvoiceIdAndDomainId(Long invoiceId, Integer domainId);
    List<BasketInvoiceItemEntity> findAllByBrowserIdAndDomainId(Long browserId, Integer domainId);

    List<BasketInvoiceItemEntity> findAllByBrowserIdAndItemsBasketInvoiceNullAndDomainId(Long browserId, Integer domainId);

    @Query(value = "SELECT biie FROM BasketInvoiceItemEntity biie WHERE biie.itemId = :itemId AND biie.browserId = :browserId AND biie.domainId = :domainId AND biie.itemsBasketInvoice IS NULL")
    List<BasketInvoiceItemEntity> findBasketItemInvoiceNull(@Param("itemId") Long itemId, @Param("browserId") Long browserId, @Param("domainId") Integer domainId);

    @Query(value = "SELECT biie FROM BasketInvoiceItemEntity biie WHERE biie.itemId = :itemId AND biie.browserId = :browserId AND biie.domainId = :domainId AND biie.invoiceId = :invoiceId")
    List<BasketInvoiceItemEntity> findBasketItem(@Param("itemId") Long itemId, @Param("browserId") Long browserId, @Param("domainId") Integer domainId, @Param("invoiceId") Integer invoiceId);

    @Query(value = "SELECT biie FROM BasketInvoiceItemEntity biie WHERE biie.itemId = :itemId AND biie.browserId = :browserId AND biie.domainId = :domainId AND biie.itemNote = :itemNote AND biie.itemsBasketInvoice IS NULL")
    List<BasketInvoiceItemEntity> findBasketItemInvoiceNull(@Param("itemId") Long itemId, @Param("browserId") Long browserId, @Param("domainId") Integer domainId, @Param("itemNote") String itemNote);

    @Query(value = "SELECT biie FROM BasketInvoiceItemEntity biie WHERE biie.itemId = :itemId AND biie.browserId = :browserId AND biie.domainId = :domainId AND biie.invoiceId = :invoiceId AND biie.itemNote = :itemNote")
    List<BasketInvoiceItemEntity> findBasketItem(@Param("itemId") Long itemId, @Param("browserId") Long browserId, @Param("domainId") Integer domainId, @Param("invoiceId") Integer invoiceId, @Param("itemNote") String itemNote);

    @Transactional
    @Modifying
    @Query(value = "UPDATE BasketInvoiceItemEntity biie SET biie.invoiceId = :invoiceId WHERE biie.browserId = :browserId AND biie.invoiceId IS NULL AND biie.domainId = :domainId")
    void updateInvoiceId(@Param("invoiceId") Long invoiceId, @Param("browserId") Long browserId, @Param("domainId") Integer domainId);

    @Query(value = "SELECT DISTINCT biie.browserId FROM BasketInvoiceItemEntity biie WHERE biie.invoiceId = :invoiceId AND biie.domainId = :domainId")
    Optional<Long> getBrowserIdByInvoiceId(@Param("invoiceId") Long invoiceId, @Param("domainId") Integer domainId);

    Optional<BasketInvoiceItemEntity> findByInvoiceIdAndItemIdAndDomainId(Long invoiceId, Long itemId, Integer domainId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BasketInvoiceItemEntity biie WHERE biie.invoiceId = :invoiceId AND biie.domainId = :domainId")
    void deleteByInvoiceId(@Param("invoiceId") Long invoiceId, @Param("domainId") Integer domainId);
}