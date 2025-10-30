package sk.iway.iwcm.components.basket.support;

import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;

import lombok.Data;
import sk.iway.iwcm.common.BasketTools;

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
        return BasketTools.convertToBasketDisplayCurrency(getPriceVat(), request);
    }
}
