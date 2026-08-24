package sk.iway.iwcm.components.basket.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Exposes basket statistics to administrators with basket permissions.
 */
@RestController
@RequestMapping("/admin/rest/eshop/stats")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
public class BasketStatsRestController {

    private final BasketStatsService basketStatsService;

    public BasketStatsRestController(BasketStatsService basketStatsService) {
        this.basketStatsService = basketStatsService;
    }

    /**
     * Returns basket statistics for the requested interval, currency, and invoice statuses.
     *
     * @param dayDate  encoded reporting interval; the service default is used when absent
     * @param currency  requested reporting currency
     * @param statusIds  optional invoice statuses to include
     * @param request  current request used for localization and payment labels
     * @return aggregated basket statistics
     */
    @GetMapping
    public BasketStatsDTO getStats(
        @RequestParam(value = "dayDate", required = false) String dayDate,
        @RequestParam(value = "currency", required = false) String currency,
        @RequestParam(value = "status", required = false) List<Integer> statusIds,
        HttpServletRequest request
    ) {
        return basketStatsService.getStats(dayDate, currency, statusIds, request);
    }
}
