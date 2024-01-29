package sk.iway.iwcm.dmail.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EmailsRepository extends JpaRepository<EmailsEntity, Long>, JpaSpecificationExecutor<EmailsEntity> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM EmailsEntity WHERE campainId = :campainId AND recipientUserId IN :userIds")
    public void deleteCampainEmail(@Param("campainId")Long campainId, @Param("userIds")List<Integer> userIds);

    @Query(value = "SELECT recipient_email FROM emails WHERE campain_id = ?1", nativeQuery=true)
    List<String> getAllCampainEmails(Long campaingId);

    //campaigns recipients - page
    Page<EmailsEntity> findAllByCampainId(Long campaingId, Pageable pageable);

    //campaigns recipients - list
    List<EmailsEntity> findAllByCampainIdOrderByIdDesc(Long campaingId);

    //campaigns opens
    Page<EmailsEntity> findAllByCampainIdAndSeenDateIsNotNull(Long campaingId, Pageable pageable);

    @Query(value = "SELECT email_id FROM emails WHERE campain_id = ?1", nativeQuery=true)
    List<Long> getEmailsIds(Long campaingId);

    @SuppressWarnings("all")
    @Transactional
    @Modifying
    @Query(value = "UPDATE EmailsEntity SET createdByUserId=:createdByUserId, url=:url, subject=:subject, senderName=:senderName, senderEmail=:senderEmail, replyTo=:replyTo, ccEmail=:ccEmail, bccEmail=:bccEmail, sendAt=:sendAt, attachments=:attachments, campainId=:campaignId WHERE campainId = :oldCampaignId")
    public void updateCampaingEmails(@Param("createdByUserId")Integer createdByUserId, @Param("url")String url, @Param("subject")String subject, @Param("senderName")String senderName, @Param("senderEmail")String senderEmail, @Param("replyTo")String replyTo, @Param("ccEmail")String ccEmail, @Param("bccEmail")String bccEmail, @Param("sendAt")Date sendAt, @Param("attachments")String attachments, @Param("campaignId")Long campaignId, @Param("oldCampaignId")Long oldCampaignId);

    @Query(value = "SELECT COUNT(email_id) FROM emails WHERE campain_id = ?1", nativeQuery=true)
    Integer getNumberOfCampaingEmails(Long campaingId);

    @Query(value = "SELECT COUNT(email_id) FROM emails WHERE sent_date IS NOT NULL AND campain_id = ?1", nativeQuery=true)
    Integer getNumberOfSentEmails(Long campaingId);

    @Transactional
    @Modifying
    public void deleteByCampainId(Long campainId);

}
