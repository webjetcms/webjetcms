package sk.iway.iwcm.components.basket.delivery_methods.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface DeliveryMethodsRepository extends DomainIdRepository<DeliveryMethodEntity, Long> {

    public List<DeliveryMethodEntity> findAllByDeliveryMethodNameAndDomainId(String deliveryMethodName, Integer domainId);

    //ExceptIt is currentlz editing entity, so it wont failt at check
    @Query("SELECT dme.supportedCountriesStr FROM DeliveryMethodEntity dme WHERE dme.deliveryMethodName = :deliveryMethodName AND dme.domainId = :domainId AND dme.id != :exceptId")
    List<String> getHandledCountriesByDeliveryMethod(@Param("deliveryMethodName")String deliveryMethodName, @Param("domainId")Integer domainId, @Param("exceptId")Long exceptId);
}
