package sk.iway.iwcm.components.basket.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/rest/eshop/stats")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
public class BasketStatsRestController {

    private final BasketStatsService basketStatsService;

    public BasketStatsRestController(BasketStatsService basketStatsService) {
        this.basketStatsService = basketStatsService;
    }

    @GetMapping
    public BasketStatsDTO getStats(
        @RequestParam(value = "dayDate", required = false) String dayDate,
        @RequestParam(value = "currency", required = false) String currency,
        HttpServletRequest request
    ) {
        return basketStatsService.getStats(dayDate, currency, request);
    }
}
