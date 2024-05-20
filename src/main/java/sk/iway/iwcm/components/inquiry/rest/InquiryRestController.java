package sk.iway.iwcm.components.inquiry.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerEntity;
import sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerRepository;
import sk.iway.iwcm.components.inquiry.jpa.InquiryEditorFields;
import sk.iway.iwcm.components.inquiry.jpa.InquiryEntity;
import sk.iway.iwcm.components.inquiry.jpa.InquiryRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/inquiry")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuInquiry')")
@Datatable
public class InquiryRestController extends DatatableRestControllerV2<InquiryEntity, Long> {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository iar;

    @Autowired
    public InquiryRestController(InquiryRepository inquiryRepository, InquiryAnswerRepository iar) {
        super(inquiryRepository);
        this.inquiryRepository = inquiryRepository;
        this.iar = iar;
    }

    @Override
    public InquiryEntity getOneItem(long id) {
        InquiryEntity entity = null;
        if(id == -1) {
            //Creating, delete old tmp InquiryAnswers (tmp answers are with questionId set as -userId)
            Identity user = UsersDB.getCurrentUser(getRequest());
            iar.deleteAnswers(-user.getUserId(), CloudToolsForCore.getDomainId());

            entity = new InquiryEntity();
            processFromEntity(entity, ProcessItemAction.CREATE);
        } else {
            entity = inquiryRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
        }
        return entity;
    }

    @Override
    public InquiryEntity processFromEntity(InquiryEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() == null) {
            InquiryEditorFields ief = new InquiryEditorFields();
            ief.fromInquiryEntity(entity, getProp(), action);
        }

        return entity;
    }

    @Override
    public void beforeSave(InquiryEntity entity) {
        if (entity.getDomainId() == null) entity.setDomainId(CloudToolsForCore.getDomainId());
        if (entity.getTotalClicks()==null) entity.setTotalClicks(0);
    }

    @Override
    public void afterSave(InquiryEntity entity, InquiryEntity saved) {
        //Check if we must permanently save tmp answers
        Identity user = UsersDB.getCurrentUser(getRequest());
        List<InquiryAnswerEntity> answersList = iar.findAllByQuestionIdAndDomainId(-user.getUserId(), CloudToolsForCore.getDomainId());
        for(InquiryAnswerEntity answer : answersList) answer.setQuestionId(saved.getId().intValue());

        //Update them
        iar.saveAll(answersList);
    }

    //After question delete, delete all answers under this question
    @Override
    public void afterDelete(InquiryEntity entity, long id) {
        iar.deleteAnswers(entity.getId().intValue(), CloudToolsForCore.getDomainId());
    }

    /**
     * Vrati zoznam uz existujucich skupin
     * @param term
     * @return
    */
    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term) {

        List<String> ac = new ArrayList<>();

        //Get all where group name is like %term%, and distict because its autocomplete list and we dont want duplicity
        List<InquiryEntity> groupNamesList =  inquiryRepository.findDistinctAllByQuestionGroupLikeAndDomainId("%" + term + "%", CloudToolsForCore.getDomainId());

        //Loop gained entities and add group name to autcomplete list "ac"
        for(InquiryEntity entity : groupNamesList)
            if (ac.contains(entity.getQuestionGroup()) == false) ac.add(entity.getQuestionGroup());

        return ac;
    }
}
