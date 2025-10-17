package sk.iway.iwcm.components.basket.delivery_methods.rest;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethod;

@Service
@DeliveryMethod(
    nameKey = "apps.eshop.delivery_methods.in_store"
)
public class InStoreService extends BaseDeliveryMethod {}
