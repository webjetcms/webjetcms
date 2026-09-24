package sk.iway.iwcm.stat.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.stat.heat_map.HeatMapAccess;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService;
import sk.iway.iwcm.stat.heat_map.HeatMapStorage;
import sk.iway.iwcm.stat.jpa.HeatMapPageDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/** Exposes only authorized, read-only heat-map reports and bounded image tiles. */
@RestController
@RequestMapping("/admin/rest/stat/heat-map")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class HeatMapRestController extends DatatableRestControllerV2<HeatMapPageDTO, Long> {
    private final HeatMapAccess access;
    private final HeatMapHistoryService historyService;

    public HeatMapRestController(HeatMapAccess access, HeatMapHistoryService historyService) {
        super(null);
        this.access = access;
        this.historyService = historyService;
    }

    @Override
    public Page<HeatMapPageDTO> getAllItems(Pageable pageable) {
        return pageTotals();
    }

    @Override
    public Page<HeatMapPageDTO> searchItem(Map<String, String> params, Pageable pageable, HeatMapPageDTO search) {
        return pageTotals();
    }

    private Page<HeatMapPageDTO> pageTotals() {
        String domain = access.currentDomain(getRequest());
        String dateRange = getRequest().getParameter("dateRange");
        if (Tools.isEmpty(dateRange)) dateRange = getRequest().getParameter("fixed_dateRange");
        if (Tools.isEmpty(dateRange)) dateRange = getRequest().getParameter("searchDayDate");
        if (Tools.isEmpty(dateRange)) dateRange = getRequest().getParameter("searchdayDate");
        LocalDate[] range = localRange(dateRange);
        int rootGroup = Tools.getIntValue(getRequest().getParameter("searchRootDir"), -1);
        List<HeatMapPageDTO> rows = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : HeatMapStorage.getPageCounts(domain, range[0], range[1]).entrySet()) {
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(entry.getKey(), false);
            if (!access.canViewDocument(getUser(), domain, doc)) continue;
            if (rootGroup > 0 && doc.getGroupId() != rootGroup
                    && !("," + GroupsDB.getInstance().getParents(doc.getGroupId()) + ",").contains("," + rootGroup + ",")) continue;
            HeatMapPageDTO row = new HeatMapPageDTO();
            row.setId(Long.valueOf(doc.getDocId()));
            row.setName(doc.getTitle());
            row.setUrl(DocDB.getURLFromDocId(doc.getDocId(), getRequest()));
            row.setClicks(entry.getValue());
            rows.add(row);
        }
        rows.sort(Comparator.comparing(HeatMapPageDTO::getClicks).reversed().thenComparing(HeatMapPageDTO::getId));
        return new DatatablePageImpl<>(rows);
    }

    public record WidthCount(int width, long clicks) { }

    @GetMapping("/widths")
    public List<WidthCount> widths(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange) {
        access.requireDocument(getRequest(), docId);
        LocalDate[] range = localRange(dateRange);
        return HeatMapStorage.getWidths(access.currentDomain(getRequest()), docId, range[0], range[1]).entrySet().stream()
                .map(entry -> new WidthCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(WidthCount::clicks).reversed().thenComparingInt(WidthCount::width)).toList();
    }

    @GetMapping("/metadata")
    public Map<String, Object> metadata(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange, @RequestParam("width") int width) {
        checkWidth(width);
        DocDetails doc = access.requireDocument(getRequest(), docId);
        LocalDate[] range = localRange(dateRange);
        HeatMapHistoryService.PreviewSelection selection = historyService.resolve(getRequest(), docId, dateRange);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", doc.getTitle());
        result.put("url", DocDB.getURLFromDocId(docId, getRequest()));
        result.put("width", width);
        result.put("clicks", HeatMapStorage.getWidths(access.currentDomain(getRequest()), docId, range[0], range[1]).getOrDefault(width, 0L));
        result.put("tileSize", HeatMapStorage.TILE_SIZE);
        result.put("historyId", selection.historyId());
        result.put("effectiveFrom", selection.effectiveFrom());
        result.put("source", selection.source());
        result.put("historicalUnavailable", selection.historicalUnavailable());
        result.put("multipleVersions", selection.multipleVersions());
        return result;
    }

    @GetMapping(value = "/tile", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> tile(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange, @RequestParam("width") int width,
            @RequestParam("tileX") int tileX, @RequestParam("tileY") int tileY) throws IOException {
        checkWidth(width);
        if (tileX < 0 || tileY < 0 || tileX > HeatMapStorage.MAX_COORDINATE / HeatMapStorage.TILE_SIZE
                || tileY > HeatMapStorage.MAX_COORDINATE / HeatMapStorage.TILE_SIZE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        access.requireDocument(getRequest(), docId);
        LocalDate[] range = localRange(dateRange);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(HeatMapStorage.getTile(access.currentDomain(getRequest()), docId, range[0], range[1], width, tileX, tileY));
    }

    private LocalDate[] localRange(String value) {
        Date[] dates = access.dateRange(value);
        ZoneId zone = ZoneId.systemDefault();
        return new LocalDate[] { dates[0].toInstant().atZone(zone).toLocalDate(), dates[1].toInstant().atZone(zone).toLocalDate() };
    }

    private void checkWidth(int width) {
        if (width < 1 || width > HeatMapStorage.MAX_VIEWPORT_WIDTH) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @Override
    public void beforeSave(HeatMapPageDTO entity) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    public boolean deleteItem(HeatMapPageDTO entity, long id) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
