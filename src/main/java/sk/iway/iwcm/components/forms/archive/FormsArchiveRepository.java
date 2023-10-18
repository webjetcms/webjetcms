package sk.iway.iwcm.components.forms.archive;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.components.forms.FormsRepositoryInterface;

import java.util.Date;
import java.util.List;

@Repository
public interface FormsArchiveRepository extends FormsRepositoryInterface<FormsArchiveEntity> {

    @Override
    @Transactional
    @Modifying
    @Query(value = "UPDATE FormsArchiveEntity SET lastExportDate = :lastExportDate WHERE id IN :formIds")
    public void updateLastExportDate(@Param("lastExportDate")Date lastExportDate, @Param("formIds")List<Long> formIds);
}
