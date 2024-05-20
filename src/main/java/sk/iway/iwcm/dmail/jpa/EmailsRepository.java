package sk.iway.iwcm.dmail.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface EmailsRepository extends DomainIdRepository<EmailsEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM EmailsEntity WHERE campainId = :campainId AND recipientUserId IN :userIds AND domainId = :domainId")
    public void deleteCampainEmail(@Param("campainId")Long campainId, @Param("userIds")List<Integer> userIds, @Param("domainId")Integer domainId);

    @Query(value = "SELECT ee.recipientEmail FROM EmailsEntity ee WHERE ee.campainId = :campainId AND ee.domainId = :domainId")
    List<String> getAllCampainEmails(@Param("campainId")Long campainId, @Param("domainId")Integer domainId);

    //campaigns recipients - page
    Page<EmailsEntity> findAllByCampainIdAndDomainId(Long campaingId, Integer domainId, Pageable pageable);

    //campaigns recipients - list
    List<EmailsEntity> findAllByCampainIdAndDomainIdOrderByIdDesc(Long campaingId, Integer domainId);

    //campaigns opens
    Page<EmailsEntity> findAllByCampainIdAndDomainIdAndSeenDateIsNotNull(Long campaingId, Integer domainId, Pageable pageable);

    @SuppressWarnings("all")
    @Transactional
    @Modifying
    @Query(value = "UPDATE EmailsEntity SET createdByUserId=:createdByUserId, url=:url, subject=:subject, senderName=:senderName, senderEmail=:senderEmail, replyTo=:replyTo, ccEmail=:ccEmail, bccEmail=:bccEmail, sendAt=:sendAt, attachments=:attachments, campainId=:campaignId WHERE campainId = :oldCampaignId AND domainId = :domainId")
    public void updateCampaingEmails(@Param("createdByUserId")Integer createdByUserId, @Param("url")String url, @Param("subject")String subject, @Param("senderName")String senderName, @Param("senderEmail")String senderEmail, @Param("replyTo")String replyTo, @Param("ccEmail")String ccEmail, @Param("bccEmail")String bccEmail, @Param("sendAt")Date sendAt, @Param("attachments")String attachments, @Param("campaignId")Long campaignId, @Param("oldCampaignId")Long oldCampaignId, @Param("domainId")Integer domainId);

    @Query(value = "SELECT COUNT(ee.id) FROM EmailsEntity ee WHERE ee.campainId = :campainId AND ee.domainId = :domainId")
    Integer getNumberOfCampaingEmails(@Param("campainId")Long campainId, @Param("domainId")Integer domainId);

    @Query(value = "SELECT COUNT(ee.id) FROM EmailsEntity ee WHERE ee.sentDate IS NOT NULL AND ee.campainId = :campainId AND ee.domainId = :domainId")
    Integer getNumberOfSentEmails(@Param("campainId")Long campainId, @Param("domainId")Integer domainId);

    @Transactional
    @Modifying
    public void deleteByCampainIdAndDomainId(Long campainId, Integer domainId);

}
