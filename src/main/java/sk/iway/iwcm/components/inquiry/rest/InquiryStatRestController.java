package sk.iway.iwcm.components.inquiry.rest;

import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.inquiry.jpa.InquiryAnswerRepository;
import sk.iway.iwcm.components.inquiry.jpa.InquiryRepository;
import sk.iway.iwcm.components.inquiry.jpa.InquiryUsersVoteEntity;
import sk.iway.iwcm.components.inquiry.jpa.InquiryUsersVoteRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

@RestController
@RequestMapping("/admin/rest/inquiry-stat")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuInquiry')")
@Datatable
public class InquiryStatRestController extends DatatableRestControllerV2<InquiryUsersVoteEntity, Long> {

    private final InquiryRepository ir;
    private final InquiryAnswerRepository iar;
    private final InquiryUsersVoteRepository iuvr;
    private final UserDetailsRepository udr;
    
    @Autowired
    public InquiryStatRestController(InquiryUsersVoteRepository iuvr, InquiryRepository ir, InquiryAnswerRepository iar, UserDetailsRepository udr) {
        super(iuvr);
        this.ir = ir;
        this.iar = iar;
        this.iuvr = iuvr;
        this.udr = udr;
    }

    @Override
    public Page<InquiryUsersVoteEntity> getAllItems(Pageable pageable) {
        Page<InquiryUsersVoteEntity> page = super.getAllItemsIncludeSpecSearch(new InquiryUsersVoteEntity(), null);

        int questionId = Tools.getIntValue(getRequest().getParameter(InquiryStatService.QUESTION_ID), -1);
        return InquiryStatService.getPreparedPage(questionId, page, iar, iuvr, udr);
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<InquiryUsersVoteEntity> root, CriteriaBuilder builder) {
        InquiryStatService.getTableData(params, predicates, root, builder);
        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public Page<InquiryUsersVoteEntity> searchItem(Map<String, String> params, Pageable pageable, InquiryUsersVoteEntity search) {
        Page<InquiryUsersVoteEntity> page = super.searchItem(params, pageable, search);

        int questionId = Tools.getIntValue(getRequest().getParameter(InquiryStatService.QUESTION_ID), -1);
        return InquiryStatService.getPreparedPage(questionId, page, iar, iuvr, udr);
    }

    @GetMapping(value = "/supportValues", produces = "application/json; charset=UTF-8")
    public String getStatSupportValues(@RequestParam Long questionId) {
        JSONObject statSupportValues = new JSONObject();

        //Question text
        statSupportValues.put("questionText", ir.getQuestionTextById(questionId));

        //Answers
        statSupportValues.put("answers", InquiryStatService.getPreparedAnswers(questionId, iar));

        //Users that answered (voted)
        statSupportValues.put("users", InquiryStatService.getPrepareUsersNames(questionId, iuvr, udr));

        return statSupportValues.toString();
    }

    @RequestMapping(
        value="/pieChartData",
        params={InquiryStatService.QUESTION_ID, InquiryStatService.DAY_DATE, InquiryStatService.ANSWER_ID, InquiryStatService.USER_ID})
    public List<LabelValueInteger> getPieChartData(
        @RequestParam(InquiryStatService.QUESTION_ID) Long questionId,
        @RequestParam(InquiryStatService.DAY_DATE) String dayDate,
        @RequestParam(InquiryStatService.ANSWER_ID) Long answerId,
        @RequestParam(InquiryStatService.USER_ID) Long userId ) {
        
        //We need it as DatatablePageImpl, so we can access the options
        DatatablePageImpl<InquiryUsersVoteEntity> page = new DatatablePageImpl<>( super.getAllItemsIncludeSpecSearch(new InquiryUsersVoteEntity(), null) );

        return InquiryStatService.getPieChartData(page);
    }

    @RequestMapping(
        value="/lineChartData",
        params={InquiryStatService.QUESTION_ID, InquiryStatService.DAY_DATE, InquiryStatService.ANSWER_ID, InquiryStatService.USER_ID})
    public Map<String, List<InquiryStatService.LineChartData>> getLineChartData(
        @RequestParam(InquiryStatService.QUESTION_ID) Long questionId,
        @RequestParam(InquiryStatService.DAY_DATE) String dayDate,
        @RequestParam(InquiryStatService.ANSWER_ID) Long answerId,
        @RequestParam(InquiryStatService.USER_ID) Long userId ) { 

        //We need it as DatatablePageImpl, so we can access the options
        DatatablePageImpl<InquiryUsersVoteEntity> page = new DatatablePageImpl<>( super.getAllItemsIncludeSpecSearch(new InquiryUsersVoteEntity(), null) );

        return InquiryStatService.getLineChartData(page, dayDate);
    }    
}