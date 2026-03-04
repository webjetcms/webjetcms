package sk.iway.iwcm.components.multistep_form.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/rest/multistep-form-stat")
public class FormStatRestController {

    @Autowired
    private FormStatService formStatService;

    @GetMapping(value="/get-stat-data", params={"form-name"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, HttpServletRequest request) {
        try {
            JSONObject allStatData = formStatService.getFormStatData(formName, request);
            return ResponseEntity.ok(allStatData.toString());
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

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
