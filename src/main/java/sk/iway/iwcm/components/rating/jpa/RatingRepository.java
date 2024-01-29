package sk.iway.iwcm.components.rating.jpa;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
    
    List<RatingEntity> findAllByOrderByIdAsc();
    
    Optional<RatingEntity> findById(Long id);

    List<RatingEntity> findAllByUserIdOrderById(Integer userId);

    List<RatingEntity> findAllByDocId(Integer docId);

    Optional<RatingEntity> findByUserIdAndDocIdOrderByInsertDateDesc(Integer userId, Integer docId);

    Optional<RatingEntity> findByUserIdAndDocIdAndInsertDateGreaterThanEqualOrderByInsertDateDesc(Integer userId, Integer docId, Date insertDate);
}
