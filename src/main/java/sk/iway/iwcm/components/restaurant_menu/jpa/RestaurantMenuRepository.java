package sk.iway.iwcm.components.restaurant_menu.jpa;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface RestaurantMenuRepository extends DomainIdRepository<RestaurantMenuEntity, Long> {
    List<RestaurantMenuEntity> findAllByDayDateAndDomainId(Date dayDate, Integer domainId);

    @Query(value = "SELECT MAX(rm.priority) FROM RestaurantMenuEntity rm INNER JOIN RestaurantMenuMealsEntity rmm WHERE rm.mealId = rmm.id AND rmm.cathegory = :cathegory AND rm.dayDate = :dayDate AND rm.domainId = :domainId")
    Optional<Integer> findMaxPriorityByCathegory(@Param("cathegory") String cathegory, @Param("dayDate") Date dayDate, @Param("domainId") Integer domainId);

    List<RestaurantMenuEntity> findAllByDayDateBetweenAndDomainId(Date from, Date to, Integer domainId);
}