package sk.iway.iwcm.components.inquiry.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerEntity;
import sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/inquiry-answer")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuInquiry')")
@Datatable
public class InquiryAnswerRestController extends DatatableRestControllerV2<InquiryAnswerEntity, Long> {

    private final InquiryAnswerRepository inquiryAnswerRepository;

    @Autowired
    public InquiryAnswerRestController(InquiryAnswerRepository inquiryAnswerRepository) {
        super(inquiryAnswerRepository);
        this.inquiryAnswerRepository = inquiryAnswerRepository;
    }

    @Override
    public Page<InquiryAnswerEntity> getAllItems(Pageable pageable) {
        Integer questionId = Tools.getIntValue(getRequest().getParameter("questionId"), -1);
        DatatablePageImpl<InquiryAnswerEntity> page = null;

        if(questionId == -1) {
            Identity user = UsersDB.getCurrentUser(getRequest());
            page = new DatatablePageImpl<>(inquiryAnswerRepository.findAllByQuestionIdAndDomainId(-user.getUserId(), CloudToolsForCore.getDomainId(), pageable));
        } else page = new DatatablePageImpl<>(inquiryAnswerRepository.findAllByQuestionIdAndDomainId(questionId, CloudToolsForCore.getDomainId(), pageable));

        return page;
    }

    @Override
    public InquiryAnswerEntity getOneItem(long id) {
        if(id == -1) {
            InquiryAnswerEntity entity = new InquiryAnswerEntity();
            Integer questionId = Tools.getIntValue(getRequest().getParameter("questionId"), -1);

            //If returned question id is -1 (new question) set temporaly current -userId as questionId
            if(questionId == -1) {
                Identity user = UsersDB.getCurrentUser(getRequest());
                entity.setQuestionId(-user.getUserId());
            } else entity.setQuestionId(questionId);

            return entity;
        } else return inquiryAnswerRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
    }

    @Override
    public void beforeSave(InquiryAnswerEntity entity) {
        if (entity.getDomainId() == null) entity.setDomainId(CloudToolsForCore.getDomainId());
        if (entity.getAnswerClicks()==null) entity.setAnswerClicks(0);
    }
}
