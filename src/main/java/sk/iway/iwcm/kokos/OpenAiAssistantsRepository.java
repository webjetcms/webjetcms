package sk.iway.iwcm.kokos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface OpenAiAssistantsRepository extends DomainIdRepository<OpenAiAssistantsEntity, Long> {
    List<OpenAiAssistantsEntity> findAllByNameLikeAndDomainId(String name, Integer domainId);

    @Query("SELECT oaa.name FROM OpenAiAssistantsEntity oaa WHERE oaa.name LIKE :name AND oaa.domainId = :domainId")
    List<String> getAssistantNames(@Param("name") String name, @Param("domainId") Integer domainId);

    Optional<OpenAiAssistantsEntity> findFirstByNameAndDomainId(String name, Integer domainId);
}