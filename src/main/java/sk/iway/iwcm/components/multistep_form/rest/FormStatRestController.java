package sk.iway.iwcm.components.multistep_form.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/rest/multistep-form-stat")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_form')")
public class FormStatRestController {

    @Autowired
    private FormStatService formStatService;

    /**
     * Returns the complete statistics payload for the selected multi-step form.
     * The response contains summary metrics, chart data, bonus chart data, and validation error chart data
     * filtered by the request date range parameters.
     *
     * @param formName the form name identifier from the {@code form-name} request parameter
     * @param request the current HTTP request used for localization and statistics filters
     * @return HTTP 200 with serialized statistics JSON, or HTTP 400 when statistics cannot be generated
     */
    @GetMapping(value="/get-stat-data", params={"form-name"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, HttpServletRequest request) {
        try {
            JSONObject allStatData = formStatService.getFormStatData(formName, request);
            return ResponseEntity.ok(allStatData.toString());
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Regenerates chart data for a single form item after chart display settings are changed.
     * This endpoint is used when the selected chart type or item-specific settings require fresh chart
     * data without reloading the complete statistics payload.
     *
     * @param formName the form name identifier from the {@code form-name} request parameter
     * @param itemFormId the form item identifier from the {@code item-form-id} request parameter
     * @param request the current HTTP request used for localization and statistics filters
     * @return HTTP 200 with serialized chart data JSON, or HTTP 400 when chart data cannot be generated
     */
    @GetMapping(value="/get-chart-data", params={"form-name", "item-form-id"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, @RequestParam("item-form-id") String itemFormId, HttpServletRequest request) {
        try {
            JSONArray chartData = formStatService.getFormStatChartData(formName, itemFormId, request);
            return ResponseEntity.ok(chartData.toString());
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    };
}
