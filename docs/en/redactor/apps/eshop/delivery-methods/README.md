# Methods of delivery

The Delivery Methods app allows you to set supported delivery methods for each country.

![](datatable.png)

## Configure the delivery method

Each delivery method has at least the following fields:
- **Method of delivery**, an immutable value representing the type of delivery method. Supported delivery methods are defined programmatically.
- **Name**, the name displayed to the customer during the order. You can enter the translation key. If you leave blank, the delivery method will be used.
- **Supported countries**, selecting the country (or multiple countries) for which this delivery method will be available. The list of supported countries can be set by a configuration transformation `basketInvoiceSupportedCountries`.
- **Price without VAT**, value representing the delivery price excluding VAT
- **VAT `[%]`**, value representing the VAT rate in percent
- **Price with VAT**, the value representing the delivery price including VAT (calculated automatically based on the price excluding VAT and the VAT rate)
- **Order of arrangement**, a numeric value to arrange the delivery method in e-commerce.

![](editor.png)

Editor **may contain additional fields**, depending on the implementation of the specific delivery method.

!> When creating a new record, you can select the delivery method using the drop-down list in the top left corner of the page.

## Validation of the delivery method

You must select at least one country for which this delivery method will be available.

Also, when creating/editing a delivery method record, you do not enter a value in the field **Price without VAT** or **VAT `[%]`**, a value of 0 will be automatically added, i.e. the delivery will be free of charge.

## New type of delivery method

Defining a new delivery method (type) is possible by programming `BackeEnd` Functionality. More information [for the programmer](../../../../custom-apps/apps/eshop/delivery-methods/README.md).
