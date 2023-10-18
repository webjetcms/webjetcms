package sk.iway.iwcm.components.translation_keys.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TranslationKeyRepository extends JpaRepository<TranslationKeyEntity, Long> {

    List<TranslationKeyEntity> findAllByLng(String lng);

    List<TranslationKeyEntity> findAllByKeyStartsWith(String key);

    TranslationKeyEntity findByKeyAndLng(String key, String lng);

    List<TranslationKeyEntity> findAllByOrderByUpdateDateAsc();

    @Transactional
    Long deleteByKeyAndLng(String key, String lng);

    List<TranslationKeyEntity> findAllByKey(String key);
}