package sk.iway.iwcm.components.basket.support;

import java.math.BigDecimal;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Data;

/**
 * Data transfer object for delivery/payment methods
 */
@Data
public class MethodDto {
    String id;
    String title;
    BigDecimal priceVat;

    public MethodDto(String id, String title, BigDecimal priceVat) {
        this.id = id;
        this.title = title;
        this.priceVat = priceVat;
    }

    public BigDecimal getLocalPriceVat(HttpServletRequest request) {
        return SupportService.getLocalPriceVat(getPriceVat(), request);
    }
}
