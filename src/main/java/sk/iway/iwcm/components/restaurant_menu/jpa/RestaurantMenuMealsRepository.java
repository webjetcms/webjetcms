package sk.iway.iwcm.components.restaurant_menu.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface RestaurantMenuMealsRepository extends DomainIdRepository<RestaurantMenuMealsEntity, Long> {
    List<RestaurantMenuMealsEntity> findAllByCathegoryAndDomainId(String cathegory, Integer domainId);
}