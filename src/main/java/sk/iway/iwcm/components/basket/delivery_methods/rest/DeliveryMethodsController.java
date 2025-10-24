package sk.iway.iwcm.components.basket.delivery_methods.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.basket.rest.EshopService;
import sk.iway.iwcm.components.basket.support.MethodDto;

@RestController
public class DeliveryMethodsController {

    @GetMapping(value = "/apps/eshop/modeOfTransports", produces = "application/json")
    public List<MethodDto> getDeliveryMethodsDtos(@RequestParam("country" )String country, HttpServletRequest request) {
        return EshopService.getInstance().getModeOfTransports(request, country);
    }
}