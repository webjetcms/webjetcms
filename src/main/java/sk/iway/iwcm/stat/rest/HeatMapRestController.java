package sk.iway.iwcm.stat.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
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
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.stat.heat_map.HeatMapAccess;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService;
import sk.iway.iwcm.stat.heat_map.HeatMapStorage;
import sk.iway.iwcm.stat.heat_map.HeatMapHotspots;
import sk.iway.iwcm.stat.heat_map.HeatMapCoordinateMode;
import sk.iway.iwcm.stat.heat_map.HeatMapStorage.WidthCount;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.ReportRange;
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
    private static final int MAX_VISIBLE_VERSIONS = 100;
    private static final long WIDTH_CACHE_MILLIS = 30_000;

    private final HeatMapAccess access;
    private final HeatMapHistoryService historyService;
    private final Map<WidthReportKey, CachedWidths> widthReports = Collections.synchronizedMap(new LRUMap<>(64));

    private record WidthReportKey(String domain, int docId, LocalDate from, LocalDate to) { }

    private static final class CachedWidths {
        private List<WidthCount> counts;
        private long expiresAt;

        // Only requests for the same report wait for its database query.
        synchronized List<WidthCount> get(WidthReportKey key) {
            if (counts == null || Tools.getNow() >= expiresAt) {
                counts = HeatMapStorage.getWidths(key.domain(), key.docId(), key.from(), key.to());
                expiresAt = Tools.getNow() + WIDTH_CACHE_MILLIS;
            }
            return counts;
        }
    }

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
        ReportRange range = ReportRange.of(access.dateRange(dateRange));
        int rootGroup = Tools.getIntValue(getRequest().getParameter("searchRootDir"), -1);
        List<HeatMapPageDTO> rows = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : HeatMapStorage.getPageCounts(domain, range.firstDay(), range.lastDay()).entrySet()) {
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

    @GetMapping("/widths")
    public List<WidthCount> widths(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange,
            @RequestParam(value = "versionId", required = false) Integer versionId) {
        access.requireDocument(getRequest(), docId);
        ReportRange range = clickRange(docId, dateRange, versionId);
        return reportWidths(access.currentDomain(getRequest()), docId, range);
    }

    private List<WidthCount> reportWidths(String domain, int docId, ReportRange range) {
        WidthReportKey key = new WidthReportKey(domain, docId, range.firstDay(), range.lastDay());
        return widthReports.computeIfAbsent(key, ignored -> new CachedWidths()).get(key);
    }

    @GetMapping("/metadata")
    public Map<String, Object> metadata(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange, @RequestParam("width") int width,
            @RequestParam(value = "mode", defaultValue = "DOCUMENT") HeatMapCoordinateMode mode,
            @RequestParam(value = "versionId", required = false) Integer versionId) {
        checkWidth(width);
        DocDetails doc = access.requireDocument(getRequest(), docId);
        HeatMapHistoryService.PreviewSelection selection = historyService.resolve(getRequest(), docId, dateRange, versionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", doc.getTitle());
        result.put("url", DocDB.getURLFromDocId(docId, getRequest()));
        result.put("width", width);
        ReportRange range = selection.clickRange();
        String domain = access.currentDomain(getRequest());
        WidthCount group = reportWidths(domain, docId, range).stream()
                .filter(item -> item.width() == width && item.mode() == mode).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        result.put("mode", mode);
        result.put("clicks", group.clicks());
        result.put("previewWidth", group.previewWidth());
        result.put("containerSelector", containerSelector(domain));
        result.put("tileSize", HeatMapStorage.TILE_SIZE);
        result.put("historyId", selection.historyId());
        result.put("effectiveFrom", selection.effectiveFrom());
        result.put("source", selection.source());
        result.put("historicalUnavailable", selection.historicalUnavailable());
        result.put("clickRange", selection.clickRange());
        result.put("versions", selection.versions().stream().limit(MAX_VISIBLE_VERSIONS).toList());
        result.put("multipleVersions", versionId == null && selection.versions().size() > 1);
        return result;
    }

    /** Returns ranked regions only after applying the page permission and selected version interval. */
    @GetMapping("/hotspots")
    public ResponseEntity<HeatMapHotspots.Report> hotspots(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange, @RequestParam("width") int width,
            @RequestParam(value = "mode", defaultValue = "DOCUMENT") HeatMapCoordinateMode mode,
            @RequestParam(value = "versionId", required = false) Integer versionId) {
        checkWidth(width);
        access.requireDocument(getRequest(), docId);
        ReportRange range = clickRange(docId, dateRange, versionId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(HeatMapStorage.getHotspots(
                access.currentDomain(getRequest()), docId, range.firstDay(), range.lastDay(), width, mode));
    }

    @GetMapping("/area")
    public ResponseEntity<HeatMapStorage.AreaCount> area(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange, @RequestParam("width") int width,
            @RequestParam(value = "mode", defaultValue = "DOCUMENT") HeatMapCoordinateMode mode,
            @RequestParam("x1") int x1, @RequestParam("y1") int y1,
            @RequestParam("x2") int x2, @RequestParam("y2") int y2,
            @RequestParam(value = "versionId", required = false) Integer versionId) {
        checkWidth(width);
        if (x1 < 0 || y1 < 0 || x2 <= x1 || y2 <= y1
                || x2 > HeatMapStorage.MAX_COORDINATE + 1 || y2 > HeatMapStorage.MAX_COORDINATE + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        access.requireDocument(getRequest(), docId);
        ReportRange range = clickRange(docId, dateRange, versionId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(HeatMapStorage.getAreaCount(
                access.currentDomain(getRequest()), docId, range.firstDay(), range.lastDay(), width, mode, x1, y1, x2, y2));
    }

    @GetMapping(value = "/binary/tile", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> tile(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange, @RequestParam("width") int width,
            @RequestParam(value = "mode", defaultValue = "DOCUMENT") HeatMapCoordinateMode mode,
            @RequestParam("tileX") int tileX, @RequestParam("tileY") int tileY,
            @RequestParam(value = "versionId", required = false) Integer versionId) throws IOException {
        checkWidth(width);
        if (tileX < 0 || tileY < 0 || tileX > HeatMapStorage.MAX_COORDINATE / HeatMapStorage.TILE_SIZE
                || tileY > HeatMapStorage.MAX_COORDINATE / HeatMapStorage.TILE_SIZE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        access.requireDocument(getRequest(), docId);
        ReportRange range = clickRange(docId, dateRange, versionId);
        String domain = access.currentDomain(getRequest());
        double maximumDensity = HeatMapStorage.getMaximumDensity(domain, docId, range.firstDay(), range.lastDay(), width, mode);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(HeatMapStorage.getTile(domain, docId, range.firstDay(), range.lastDay(), width, mode, tileX, tileY, maximumDensity));
    }

    private ReportRange clickRange(int docId, String value, Integer versionId) {
        Date[] dates = access.dateRange(value);
        if (versionId != null) return historyService.versionRange(docId, dates, versionId);
        return ReportRange.of(dates);
    }

    private void checkWidth(int width) {
        if (width < 1 || width > HeatMapStorage.MAX_VIEWPORT_WIDTH) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    /** Resolves domain overrides for the public page without changing the administration request context. */
    private String containerSelector(String domain) {
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean == null) return Constants.getString("statHeatMapMainContainerSelector");
        String originalDomain = requestBean.getDomain();
        try {
            requestBean.setDomain(domain);
            return Constants.getString("statHeatMapMainContainerSelector");
        } finally {
            requestBean.setDomain(originalDomain);
        }
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
