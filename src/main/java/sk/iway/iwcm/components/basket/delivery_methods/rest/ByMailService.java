package sk.iway.iwcm.components.basket.delivery_methods.rest;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.basket.support.SupportMethod;

@Service
@SupportMethod(
    nameKey = "apps.eshop.delivery_methods.by_mail"
)
public class ByMailService extends BaseDeliveryMethod {}