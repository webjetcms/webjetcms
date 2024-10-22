package sk.iway.iwcm.system.elfinder;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FolderPropertiesRepository extends JpaRepository<FolderPropertiesEntity, Long>, JpaSpecificationExecutor<FolderPropertiesEntity> {
    Optional<FolderPropertiesEntity> findByDirUrl(String dirUrl);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM FolderPropertiesEntity fpe WHERE (lower(fpe.dirUrl) LIKE :dirUrl)")
    public void deleteByPathLike(@Param("dirUrl")String path);
}