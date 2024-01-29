package sk.iway.iwcm.components.seo.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.seo.jpa.GooglePositionEntity;
import sk.iway.iwcm.components.seo.jpa.GooglePositionRepository;
import sk.iway.iwcm.components.seo.jpa.ManagementKeywordsEntity;
import sk.iway.iwcm.components.seo.jpa.ManagementKeywordsRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/seo/google-position")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class GooglePositionRestController extends DatatableRestControllerV2<GooglePositionEntity, Long> {

    private final GooglePositionRepository googlePositionRepository;
    private final ManagementKeywordsRepository managementKeywordsRepository;

    @Autowired
    public GooglePositionRestController(GooglePositionRepository googlePositionRepository, ManagementKeywordsRepository managementKeywordsRepository) {
        super(googlePositionRepository);
        this.googlePositionRepository = googlePositionRepository;
        this.managementKeywordsRepository = managementKeywordsRepository;
    }

    @Override
    public void beforeSave(GooglePositionEntity entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<GooglePositionEntity> getAllItems(Pageable pageable) {
        int keywordId = Tools.getIntValue(getRequest().getParameter("keywordId"), -1);
        if(keywordId == -1) return new DatatablePageImpl<>( new ArrayList<>() );

        Date[] dateRangeArr = StatService.processDateRangeString(getRequest().getParameter("searchDayDate"));
        Page<GooglePositionEntity> items = googlePositionRepository.findAllByKeywordIdAndDayDateBetween(keywordId, dateRangeArr[0], dateRangeArr[1], pageable);
        return new DatatablePageImpl<>( items );
    }

    @Override
    public Page<GooglePositionEntity> searchItem(Map<String, String> params, Pageable pageable, GooglePositionEntity search) {
        int keywordId = Tools.getIntValue(getRequest().getParameter("keywordId"), -1);
        if(keywordId == -1) return new DatatablePageImpl<>( new ArrayList<>() );

        Date[] dateRangeArr = StatService.processDateRangeString(getRequest().getParameter("searchDayDate"));
        Page<GooglePositionEntity> items = googlePositionRepository.findAllByKeywordIdAndDayDateBetween(keywordId, dateRangeArr[0], dateRangeArr[1], pageable);
        return new DatatablePageImpl<>( items );
    }

    @RequestMapping(
        value="/lineChartData", params={"keywordId", "dayDate"})
    @ResponseBody
    public Map<String, List<GooglePositionEntity>> getLineChartData(
                            @RequestParam("keywordId") Integer keywordId,
                            @RequestParam("dayDate") String stringRange) {
        keywordId = Tools.getIntValue(keywordId, -1);
        if(keywordId == -1) return new HashMap<>();

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        Map<String, List<GooglePositionEntity>> dataMap = new HashMap<>();

        //Series name is combination of name + domain
        String seriesName = "";
        ManagementKeywordsEntity seoKeyword = managementKeywordsRepository.getById(Long.valueOf(keywordId));
        if (seoKeyword != null) {
            seriesName += seoKeyword.getName();
            seriesName += " ( " + seoKeyword.getDomain() + " )";
        }

        dataMap.put(seriesName, googlePositionRepository.findAllByKeywordIdAndDayDateBetween(keywordId, dateRangeArr[0], dateRangeArr[1], PageRequest.of(0, 100)).getContent() );
        return dataMap;
    }

    @RequestMapping(value="/keywordTitle", params={"keywordId"}, produces = "text/plain;charset=UTF-8")
    public String getKeywordTitle(@RequestParam("keywordId") Integer keywordId) {
        if(keywordId < 0) return "";
        return (new SimpleQuery()).forString("SELECT name FROM seo_keywords WHERE seo_keyword_id=?", keywordId);
    }
}
